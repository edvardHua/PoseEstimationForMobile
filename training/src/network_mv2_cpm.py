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
import tensorflow.contrib.slim as slim

from network_base import max_pool, upsample, inverted_bottleneck, separable_conv, convb, is_trainable

N_KPOINTS = 14
STAGE_NUM = 6

out_channel_ratio = lambda d: max(int(d * 0.75), 8)
up_channel_ratio = lambda d: int(d * 1.)
out_channel_cpm = lambda d: max(int(d * 0.75), 8)


def build_network(input, trainable):
    is_trainable(trainable)

    net = convb(input, 3, 3, out_channel_ratio(32), 2, name="Conv2d_0")

    with tf.variable_scope('MobilenetV2'):

        # 128, 112
        mv2_branch_0 = slim.stack(net, inverted_bottleneck,
                                  [
                                      (1, out_channel_ratio(16), 0, 3),
                                      (1, out_channel_ratio(16), 0, 3)
                                  ], scope="MobilenetV2_part_0")

        # 64, 56
        mv2_branch_1 = slim.stack(mv2_branch_0, inverted_bottleneck,
                                  [
                                      (up_channel_ratio(6), out_channel_ratio(24), 1, 3),
                                      (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                  ], scope="MobilenetV2_part_1")

        # 32, 28
        mv2_branch_2 = slim.stack(mv2_branch_1, inverted_bottleneck,
                                  [
                                      (up_channel_ratio(6), out_channel_ratio(32), 1, 3),
                                      (up_channel_ratio(6), out_channel_ratio(32), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(32), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(32), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(32), 0, 3),
                                  ], scope="MobilenetV2_part_2")

        # 16, 14
        mv2_branch_3 = slim.stack(mv2_branch_2, inverted_bottleneck,
                                  [
                                      (up_channel_ratio(6), out_channel_ratio(64), 1, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 0, 3),
                                  ], scope="MobilenetV2_part_3")

        # 8, 7
        mv2_branch_4 = slim.stack(mv2_branch_3, inverted_bottleneck,
                                  [
                                      (up_channel_ratio(6), out_channel_ratio(96), 1, 3),
                                      (up_channel_ratio(6), out_channel_ratio(96), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(96), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(96), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(96), 0, 3)
                                  ], scope="MobilenetV2_part_4")

        cancat_mv2 = tf.concat(
            [
                max_pool(mv2_branch_0, 4, 4, 4, 4, name="mv2_0_max_pool"),
                max_pool(mv2_branch_1, 2, 2, 2, 2, name="mv2_1_max_pool"),
                mv2_branch_2,
                upsample(mv2_branch_3, 2, name="mv2_3_upsample"),
                upsample(mv2_branch_4, 4, name="mv2_4_upsample")
            ]
            , axis=3)

    with tf.variable_scope("Convolutional_Pose_Machine"):
        l2s = []
        prev = None
        for stage_number in range(STAGE_NUM):
            if prev is not None:
                inputs = tf.concat([cancat_mv2, prev], axis=3)
            else:
                inputs = cancat_mv2

            kernel_size = 7
            lastest_channel_size = 128
            if stage_number == 0:
                kernel_size = 3
                lastest_channel_size = 512

            _ = slim.stack(inputs, inverted_bottleneck,
                           [
                               (2, out_channel_cpm(32), 0, kernel_size),
                               (up_channel_ratio(4), out_channel_cpm(32), 0, kernel_size),
                               (up_channel_ratio(4), out_channel_cpm(32), 0, kernel_size),
                           ], scope="stage_%d_mv2" % stage_number)

            _ = slim.stack(_, separable_conv,
                           [
                               (out_channel_ratio(lastest_channel_size), 1, 1),
                               (N_KPOINTS, 1, 1)
                           ], scope="stage_%d_mv1" % stage_number)

            prev = _
            cpm_out = upsample(_, 4, "stage_%d_out" % stage_number)
            l2s.append(cpm_out)

    return cpm_out, l2s
