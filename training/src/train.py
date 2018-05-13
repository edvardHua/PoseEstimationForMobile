# Copyright 2018 Zihua Zeng (edvard_hua@live.com)
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ===================================================================================
# -*- coding: utf-8 -*-

import tensorflow as tf
import os
import time
import numpy as np
import configparser
import dataset

from datetime import datetime

from dataset import get_train_dataset_pipline
from networks import get_network
from dataset_prepare import CocoPose
from dataset_augment import set_network_input_wh, set_network_scale


def get_train_input(batchsize, epoch):
    train_ds = get_train_dataset_pipline(batch_size=batchsize, epoch=epoch, buffer_size=100)
    iter = train_ds.make_one_shot_iterator()
    _ = iter.get_next()
    return _[0], _[1]


def get_loss_and_output(model, batchsize, input_image, input_heat, reuse_variables=None):
    losses = []

    with tf.variable_scope(tf.get_variable_scope(), reuse=reuse_variables):
        _, pred_heatmaps_all = get_network(model, input_image, True)

    for idx, pred_heat in enumerate(pred_heatmaps_all):
        loss_l2 = tf.nn.l2_loss(tf.concat(pred_heat, axis=0) - input_heat, name='loss_heatmap_stage%d' % idx)
        losses.append(loss_l2)

    total_loss = tf.reduce_sum(losses) / batchsize
    total_loss_ll_heat = tf.reduce_sum(loss_l2) / batchsize
    return total_loss, total_loss_ll_heat, pred_heat


def average_gradients(tower_grads):
    """
    Get gradients of all variables.
    :param tower_grads:
    :return:
    """
    average_grads = []

    # get variable and gradients in differents gpus
    for grad_and_vars in zip(*tower_grads):
        # calculate the average gradient of each gpu
        grads = []
        for g, _ in grad_and_vars:
            expanded_g = tf.expand_dims(g, 0)
            grads.append(expanded_g)
        grad = tf.concat(grads, 0)
        grad = tf.reduce_mean(grad, 0)

        v = grad_and_vars[0][1]
        grad_and_var = (grad, v)
        average_grads.append(grad_and_var)
    return average_grads


def main(argv=None):
    # load config file and setup
    params = {}
    config = configparser.ConfigParser()
    config_file = "experiments/mv2_cpm.cfg"
    if len(argv) != 1:
        config_file = argv[1]
    config.read(config_file)
    for _ in config.options("Train"):
        params[_] = eval(config.get("Train", _))

    os.environ['CUDA_VISIBLE_DEVICES'] = params['visible_devices']

    gpus_index = params['visible_devices'].split(",")
    params['gpus'] = len(gpus_index)

    if not os.path.exists(params['modelpath']):
        os.makedirs(params['modelpath'])
    if not os.path.exists(params['logpath']):
        os.makedirs(params['logpath'])

    dataset.set_config(params)
    set_network_input_wh(params['input_width'], params['input_height'])
    set_network_scale(params['scale'])

    training_name = '{}_batch-{}_lr-{}_gpus-{}_{}x{}_{}'.format(
        params['model'],
        params['batchsize'],
        params['lr'],
        params['gpus'],
        params['input_width'], params['input_height'],
        config_file.replace("/", "-").replace(".cfg", "")
    )

    with tf.Graph().as_default(), tf.device("/cpu:0"):
        input_image, input_heat = get_train_input(params['batchsize'], params['max_epoch'])
        global_step = tf.Variable(0, trainable=False)
        learning_rate = tf.train.exponential_decay(float(params['lr']), global_step,
                                                   decay_steps=10000, decay_rate=float(params['decay_rate']), staircase=True)
        opt = tf.train.AdamOptimizer(learning_rate, epsilon=1e-8)
        tower_grads = []
        reuse_variable = False

        # multiple gpus
        for i in range(params['gpus']):
            with tf.device("/gpu:%d" % i):
                with tf.name_scope("GPU_%d" % i):
                    loss, last_heat_loss, pred_heat = get_loss_and_output(params['model'], params['batchsize'], input_image, input_heat, reuse_variable)
                    reuse_variable = True
                    grads = opt.compute_gradients(loss)
                    tower_grads.append(grads)

        grads = average_gradients(tower_grads)
        for grad, var in grads:
            if grad is not None:
                tf.summary.histogram("gradients_on_average/%s" % var.op.name, grad)

        apply_gradient_op = opt.apply_gradients(grads, global_step=global_step)
        for var in tf.trainable_variables():
            tf.summary.histogram(var.op.name, var)

        MOVING_AVERAGE_DECAY = 0.99
        variable_averages = tf.train.ExponentialMovingAverage(MOVING_AVERAGE_DECAY, global_step)
        variable_to_average = (tf.trainable_variables() + tf.moving_average_variables())
        variables_averages_op = variable_averages.apply(variable_to_average)

        update_ops = tf.get_collection(tf.GraphKeys.UPDATE_OPS)
        with tf.control_dependencies(update_ops):
            train_op = tf.group(apply_gradient_op, variables_averages_op)

        saver = tf.train.Saver(max_to_keep=100)

        tf.summary.scalar("learning_rate", learning_rate)
        tf.summary.scalar("loss", loss)
        tf.summary.scalar("loss_lastlayer_heat", last_heat_loss)
        summary_merge_op = tf.summary.merge_all()

        pred_result_image = tf.placeholder(tf.float32, shape=[params['batchsize'], 480, 640, 3])
        pred_result__summary = tf.summary.image("pred_result_image", pred_result_image, params['batchsize'])

        init = tf.global_variables_initializer()
        config = tf.ConfigProto()
        # occupy gpu gracefully
        config.gpu_options.allow_growth = True
        with tf.Session(config=config) as sess:
            init.run()

            coord = tf.train.Coordinator()
            threads = tf.train.start_queue_runners(sess=sess, coord=coord)

            summary_writer = tf.summary.FileWriter(os.path.join(params['logpath'], training_name), sess.graph)
            total_step_num = params['num_train_samples'] * params['max_epoch'] // (params['batchsize'] * params['gpus'])
            print("Start training...")
            for step in range(total_step_num):
                start_time = time.time()
                _, loss_value, lh_loss, in_image, in_heat, p_heat = sess.run(
                    [train_op, loss, last_heat_loss, input_image, input_heat, pred_heat]
                )
                duration = time.time() - start_time

                if step != 0 and step % params['per_update_tensorboard_step'] == 0:
                    # False will speed up the training time.
                    if params['pred_image_on_tensorboard'] is True:
                        result = []
                        for index in range(params['batchsize']):
                            r = CocoPose.display_image(
                                    in_image[index,:,:,:],
                                    in_heat[index,:,:,:],
                                    p_heat[index,:,:,:],
                                    True
                                )
                            result.append(
                                r.astype(np.float32)
                            )

                        comparsion_of_pred_result = sess.run(
                            pred_result__summary,
                            feed_dict={
                                pred_result_image: np.array(result)
                            }
                        )
                        summary_writer.add_summary(comparsion_of_pred_result, step)

                    # print train info
                    num_examples_per_step = params['batchsize'] * params['gpus']
                    examples_per_sec = num_examples_per_step / duration
                    sec_per_batch = duration / params['gpus']
                    format_str = ('%s: step %d, loss = %.2f, last_heat_loss = %.2f (%.1f examples/sec; %.3f sec/batch)')
                    print(format_str % (datetime.now(), step, loss_value, lh_loss, examples_per_sec, sec_per_batch))

                    # tensorboard visualization
                    merge_op = sess.run(summary_merge_op)
                    summary_writer.add_summary(merge_op, step)

                # save model
                if step % params['per_saved_model_step'] == 0:
                    checkpoint_path = os.path.join(params['modelpath'], training_name, 'model')
                    saver.save(sess, checkpoint_path, global_step=step)
            coord.request_stop()
            coord.join(threads)


if __name__ == '__main__':
    tf.app.run()