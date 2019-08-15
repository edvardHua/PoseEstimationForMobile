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
# Reference Paper: https://arxiv.org/pdf/1902.09212.pdf
# Reference Code: https://github.com/leoxiaobin/deep-high-resolution-net.pytorch

import tensorflow as tf
import tensorflow.contrib.slim as slim
from pefm.networks.network_base import upsample, convb, inverted_bottleneck, is_trainable
from pefm.networks.common import output_1d, output_2d

N_KPOINTS = 14
UP_RATIO = lambda d: int(d * 1.0)
OUT_RATIO = lambda d: max(int(d * 0.9), 8)


def set_config(config):
    if not config:
        return None
    global N_KPOINTS, OUT_RATIO, UP_RATIO
    N_KPOINTS = config['out_channel']

    UP_RATIO = lambda d: int(d * config['up_ratio'])
    OUT_RATIO = lambda d: max(int(d * config['out_ratio']), 8)


def _make_transition_layer(input, num_channels_pre_layer, num_channels_cur_layer):
    num_branches_cur = len(num_channels_cur_layer)
    num_branches_pre = len(num_channels_pre_layer)

    branch_layer = []
    for i in range(num_branches_cur):
        if i < num_branches_pre:
            if num_channels_pre_layer[i] != num_channels_cur_layer[i]:
                branch_layer.append(convb(input[i], 3, 3, num_channels_cur_layer[i], 1, "change_channel"))
            else:
                branch_layer.append(input[i])
        else:
            tmp_net = input[-1]
            for j in range(i - num_branches_pre + 1):
                tmp_net = convb(tmp_net, 3, 3, num_channels_cur_layer[i], 2, "down_sample")
            branch_layer.append(tmp_net)

    return branch_layer


def _make_stage(net, num_modules, num_branches, num_blocks, num_channels):
    for i in range(num_modules):
        with tf.variable_scope("high_resolution_module_%d" % i):
            net = _high_resolution_module(net,
                                          num_branches,
                                          num_blocks,
                                          num_channels)
    return net


def _high_resolution_module(input, num_branches, num_blocks, num_channels):
    branches = []
    for i in range(num_branches):
        x = input[i]
        c = x.get_shape().as_list()[-1]
        if c != num_channels[i]:
            x = convb(x, 1, 1, num_channels[i], 1, "change_channel_branch%d" % i)
        for _ in range(num_blocks[i]):
            # x = resnet(x, 3, 3, num_channels[i], False, scope="resnet_b%d_%d" % (i, _))
            x = inverted_bottleneck(x, UP_RATIO(2), num_channels[i], False, 3, "mv2_b%d_%d" % (i, _))
        branches.append(x)

    fuse_layers = []
    for i in range(num_branches):
        curr_fuse = branches[i]
        for j in range(num_branches):
            if j > i:
                tmp_res = convb(branches[j], 1, 1, num_channels[i], 1, "conv1x1_i%d_j%d" % (i, j), tf.nn.relu)
                curr_fuse += upsample(tmp_res, 2 ** (j - i), "upsample_%d_%d" % (i, j))
            elif j == i:
                break
            elif j < i:
                tmp_res = branches[j]
                for k in range(i - j):
                    if k == i - j - 1:
                        # tmp_res = convb(tmp_res, 3, 3, num_channels[j + k + 1], 2, "conv3x3_i%d_j%d_k%d" % (i, j, k),
                        #                 None)
                        tmp_res = inverted_bottleneck(tmp_res, UP_RATIO(2), num_channels[j + k + 1], True, 3,
                                                      "mv2_3x3_i%d_j%d_k%d" % (i, j, k))
                    else:
                        # tmp_res = convb(tmp_res, 3, 3, num_channels[j + k + 1], 2, "conv3x3_i%d_j%d_k%d" % (i, j, k),
                        #                 tf.nn.relu)
                        tmp_res = inverted_bottleneck(tmp_res, UP_RATIO(2), num_channels[j + k + 1], True, 3,
                                                      "mv2_3x3_i%d_j%d_k%d" % (i, j, k))

                curr_fuse += tmp_res
        # curr_fuse = tf.nn.relu(curr_fuse)
        fuse_layers.append(curr_fuse)

    return fuse_layers


def build_network(input, trainable):
    is_trainable(trainable)

    with tf.variable_scope("stage1"):
        net = convb(input, 3, 3, OUT_RATIO(64), 2, name="conv0", act_fn=None)
        # 64, 48
        net = convb(net, 3, 3, OUT_RATIO(64), 2, name="conv1", act_fn=tf.nn.relu)
        # origin is bottleneck, here i used inverted_bottleneck
        stem_block = slim.stack(net, inverted_bottleneck,
                                [
                                    (UP_RATIO(2), OUT_RATIO(64), 0, 3),
                                    (UP_RATIO(2), OUT_RATIO(64), 0, 3),
                                    (UP_RATIO(2), OUT_RATIO(64), 0, 3),
                                    (UP_RATIO(2), OUT_RATIO(64), 0, 3)
                                ], scope="Stem_block")

    with tf.variable_scope("stage2"):
        NUM_MODULE = 1
        NUM_BRANCH = 2
        NUM_BLOCK = [4, 4]
        NUM_CHANNELS = [OUT_RATIO(32), OUT_RATIO(64)]
        layers = _make_transition_layer([stem_block], [OUT_RATIO(64)], NUM_CHANNELS)
        stage2_out = _make_stage(layers, num_modules=NUM_MODULE, num_blocks=NUM_BLOCK, num_branches=NUM_BRANCH,
                                 num_channels=NUM_CHANNELS)

    with tf.variable_scope("stage3"):
        NUM_MODULE = 4
        NUM_BRANCH = 3
        NUM_BLOCK = [4, 4, 4]
        NUM_CHANNELS = [OUT_RATIO(32), OUT_RATIO(64), OUT_RATIO(128)]
        layers = _make_transition_layer(stage2_out, [OUT_RATIO(32), OUT_RATIO(64)], NUM_CHANNELS)
        stage3_out = _make_stage(layers, num_modules=NUM_MODULE, num_blocks=NUM_BLOCK, num_branches=NUM_BRANCH,
                                 num_channels=NUM_CHANNELS)

    with tf.variable_scope("stage4"):
        NUM_MODULE = 3
        NUM_BRANCH = 4
        NUM_BLOCK = [4, 4, 4, 4]
        NUM_CHANNELS = [OUT_RATIO(32), OUT_RATIO(64), OUT_RATIO(128), OUT_RATIO(256)]
        layers = _make_transition_layer(stage3_out, [OUT_RATIO(32), OUT_RATIO(64), OUT_RATIO(128)], NUM_CHANNELS)
        stage4_out = _make_stage(layers, num_modules=NUM_MODULE, num_blocks=NUM_BLOCK, num_branches=NUM_BRANCH,
                                 num_channels=NUM_CHANNELS)
    stage4_up1 = upsample(
        convb(stage4_out[1], 1, 1, OUT_RATIO(32), 1, "final_change_channel1", tf.nn.relu),
        2,
        "final_up1")
    stage4_up2 = upsample(
        convb(stage4_out[2], 1, 1, OUT_RATIO(32), 1, "final_change_channel2", tf.nn.relu),
        4,
        "final_up2")
    stage4_up3 = upsample(
        convb(stage4_out[3], 1, 1, OUT_RATIO(32), 1, "final_change_channel3", tf.nn.relu),
        8,
        "final_up3")
    # out = tf.add_n([stage4_out[0], stage4_up1, stage4_up2, stage4_up3])
    out = tf.concat([stage4_out[0], stage4_up1, stage4_up2, stage4_up3], axis=3)
    final = convb(out, 1, 1, N_KPOINTS, 1, "final_output", tf.nn.relu6)

    final = upsample(final, 2, "final_upsample")

    point_1d_res = output_1d(final)
    point_2d_res = output_2d(final)
    output = tf.identity(final, "output")
    return output, [output], point_1d_res, point_2d_res


if __name__ == '__main__':
    graph = tf.Graph()
    with graph.as_default():
        x = tf.placeholder(dtype=tf.float32, shape=(10, 256, 192, 3))
        a, b, c, d = build_network(x, False)
        print(a.shape)

    # writer = tf.summary.FileWriter(logdir='tmp/tb', graph=graph)
    # writer.flush()
