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


def display_image():
    """
    display heatmap & origin image
    :return:
    """
    from dataset_prepare import CocoMetadata, CocoPose
    from pycocotools.coco import COCO
    from os.path import join
    from dataset import _parse_function

    BASE_PATH = "/root/hdd/ai_challenger"

    import os
    # os.chdir("..")

    ANNO = COCO(
        join(BASE_PATH, "ai_challenger_valid.json")
    )
    train_imgIds = ANNO.getImgIds()

    img, heat = _parse_function(train_imgIds[100], ANNO)

    CocoPose.display_image(img, heat, pred_heat=heat, as_numpy=False)

    from PIL import Image
    for _ in range(heat.shape[2]):
        data = CocoPose.display_image(img, heat, pred_heat=heat[:, :, _:(_ + 1)], as_numpy=True)
        im = Image.fromarray(data)
        im.save("test_heatmap/heat_%d.jpg" % _)


def saved_model_graph():
    """
    save the graph of model and check it in tensorboard
    :return:
    """

    from os.path import join
    from network_mv2_cpm_2 import build_network
    import tensorflow as tf
    import os

    INPUT_WIDTH = 256
    INPUT_HEIGHT = 256
    os.environ['CUDA_VISIBLE_DEVICES'] = ''

    input_node = tf.placeholder(tf.float32, shape=(1, INPUT_WIDTH, INPUT_HEIGHT, 3),
                                name='image')
    build_network(input_node, False)

    config = tf.ConfigProto()
    config.gpu_options.allow_growth = True
    with tf.Session(config=config) as sess:
        train_writer = tf.summary.FileWriter(
            join("tensorboard/test_graph/"),
            sess.graph
        )
        sess.run(tf.global_variables_initializer())


def metric_prefix(input_width, input_height):
    """
    output the calculation of you model
    :param input_width:
    :param input_height:
    :return:
    """
    import tensorflow as tf
    from networks import get_network
    import os
    os.environ['CUDA_VISIBLE_DEVICES'] = ''

    input_node = tf.placeholder(tf.float32, shape=(1, input_width, input_height, 3),
                                name='image')
    get_network("mv2_cpm_2", input_node, False)
    config = tf.ConfigProto()
    config.gpu_options.allow_growth = True
    run_meta = tf.RunMetadata()
    with tf.Session(config=config) as sess:
        opts = tf.profiler.ProfileOptionBuilder.float_operation()
        flops = tf.profiler.profile(sess.graph, run_meta=run_meta, cmd='op', options=opts)

        opts = tf.profiler.ProfileOptionBuilder.trainable_variables_parameter()
        params = tf.profiler.profile(sess.graph, run_meta=run_meta, cmd='op', options=opts)

        print("opts {:,} --- paras {:,}".format(flops.total_float_ops, params.total_parameters))
        sess.run(tf.global_variables_initializer())


def run_with_frozen_pb(img_path, input_w_h, frozen_graph, output_node_names):
    import tensorflow as tf
    import cv2
    import numpy as np
    import os
    os.environ['CUDA_VISIBLE_DEVICES'] = ''
    from dataset_prepare import CocoPose
    with tf.gfile.GFile(frozen_graph, "rb") as f:
        restored_graph_def = tf.GraphDef()
        restored_graph_def.ParseFromString(f.read())

    tf.import_graph_def(
        restored_graph_def,
        input_map=None,
        return_elements=None,
        name=""
    )

    graph = tf.get_default_graph()
    image = graph.get_tensor_by_name("image:0")
    output = graph.get_tensor_by_name("%s:0" % output_node_names)

    image_0 = cv2.imread(img_path)
    w, h, _ = image_0.shape
    image_ = cv2.resize(image_0, (input_w_h, input_w_h), interpolation=cv2.INTER_AREA)

    with tf.Session() as sess:
        heatmaps = sess.run(output, feed_dict={image: [image_]})
        CocoPose.display_image(
            # np.reshape(image_, [1, input_w_h, input_w_h, 3]),
            image_,
            None,
            heatmaps[0,:,:,:],
            False
        )
        # save each heatmaps to disk
        from PIL import Image
        for _ in range(heatmaps.shape[2]):
            data = CocoPose.display_image(image_, heatmaps[0,:,:,:], pred_heat=heatmaps[0, :, :, _:(_ + 1)], as_numpy=True)
            im = Image.fromarray(data)
            im.save("test/heat_%d.jpg" % _)


if __name__ == '__main__':
    # saved_model_graph()
    metric_prefix(256, 256)
    # run_with_frozen_pb(
    #     "/root/hdd/ai_challenger/train/0a9396675bf14580eb08c37e0b8a69a0299afb03.jpg",
    #     256,
    #     "./model-260000.pb",
    #     "Convolutional_Pose_Machine/stage_5_out"
    # )
    # display_image()

