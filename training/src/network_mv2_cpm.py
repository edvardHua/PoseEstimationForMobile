# -*- coding: utf-8 -*-
# @Time    : 18-4-12 5:12 PM
# @Author  : edvard_hua@live.com
# @FileName: network_mv2_cpm.py
# @Software: PyCharm

import tensorflow as tf
import tensorflow.contrib.slim as slim

from network_base import max_pool, upsample, inverted_bottleneck, convb, is_trainable

N_KPOINTS = 15
STAGE_NUM = 6

out_channel_ratio = lambda d: max(int(d * 0.75), 8)
up_channel_ratio = lambda d: int(d * 1.)
out_channel_cpm = lambda d: max(int(d * 0.75), 8)


def build_network(input, trainable):
    is_trainable(trainable)

    net = convb(input, 3, 3, out_channel_ratio(32), 2, name="Conv2d_0")

    with tf.variable_scope('MobilenetV2'):
        mv2_branch_0 = inverted_bottleneck(net, 1, out_channel_ratio(16), 0, 3, scope="MobilenetV2_part_0")
        mv2_branch_1 = slim.stack(mv2_branch_0, inverted_bottleneck,
                                  [
                                      (up_channel_ratio(6), out_channel_ratio(24), 1, 3),
                                      (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                  ], scope="MobilenetV2_part_1")

        mv2_branch_2 = slim.stack(mv2_branch_1, inverted_bottleneck,
                                  [
                                      (up_channel_ratio(6), out_channel_ratio(32), 1, 3),
                                      (up_channel_ratio(6), out_channel_ratio(32), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(32), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(32), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(32), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 1, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(64), 0, 3),
                                  ], scope="MobilenetV2_part_2")

        mv2_branch_3 = slim.stack(mv2_branch_2, inverted_bottleneck,
                                  [
                                      (up_channel_ratio(6), out_channel_ratio(96), 1, 3),
                                      (up_channel_ratio(6), out_channel_ratio(96), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(96), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(96), 0, 3),
                                      (up_channel_ratio(6), out_channel_ratio(96), 0, 3)
                                  ], scope="MobilenetV2_part_3")

        cancat_mv2 = tf.concat(
            [
                max_pool(mv2_branch_0, 4, 4, 4, 4, name="mv2_0_max_pool"),
                max_pool(mv2_branch_1, 2, 2, 2, 2, name="mv2_1_max_pool"),
                upsample(mv2_branch_2, 2, name="mv2_2_upsample"),
                upsample(mv2_branch_3, 4, name="mv2_3_upsample")
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
                lastest_channel_size = 256

            _ = slim.stack(inputs, inverted_bottleneck,
                             [
                                 (2, out_channel_cpm(32), 0, kernel_size),
                                 (up_channel_ratio(4), out_channel_cpm(32), 0, kernel_size),
                                 (up_channel_ratio(4), out_channel_cpm(32), 0, kernel_size),
                                 (2, out_channel_ratio(lastest_channel_size), 0, 1),
                                 (1, N_KPOINTS, 0, 1)
                             ], scope="stage_%d" % stage_number)

            prev = _
            cpm_out = upsample(_, 4, "stage_%d_out" % stage_number)
            l2s.append(cpm_out)

    return cpm_out, l2s
