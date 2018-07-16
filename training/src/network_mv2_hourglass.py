# -*- coding: utf-8 -*-
# @Time    : 18-4-12 5:12 PM
# @Author  : edvard_hua@live.com
# @FileName: network_mv2_cpm.py
# @Software: PyCharm

import tensorflow as tf
import tensorflow.contrib.slim as slim

from network_base import max_pool, upsample, inverted_bottleneck, separable_conv, convb, is_trainable

N_KPOINTS = 14
STAGE_NUM = 4

out_channel_ratio = lambda d: int(d * 1.0)
up_channel_ratio = lambda d: int(d * 1.0)

l2s = []


def hourglass_module(inp, stage_nums):
    if stage_nums > 0:
        down_sample = max_pool(inp, 2, 2, 2, 2, name="hourglass_downsample_%d" % stage_nums)

        block_front = slim.stack(down_sample, inverted_bottleneck,
                                 [
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                 ], scope="hourglass_front_%d" % stage_nums)
        stage_nums -= 1
        block_mid = hourglass_module(block_front, stage_nums)
        block_back = inverted_bottleneck(
            block_mid, up_channel_ratio(6), N_KPOINTS,
            0, 3, scope="hourglass_back_%d" % stage_nums)

        up_sample = upsample(block_back, 2, "hourglass_upsample_%d" % stage_nums)

        # jump layer
        branch_jump = slim.stack(inp, inverted_bottleneck,
                                 [
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                     (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                                     (up_channel_ratio(6), N_KPOINTS, 0, 3),
                                 ], scope="hourglass_branch_jump_%d" % stage_nums)

        curr_hg_out = tf.add(up_sample, branch_jump, name="hourglass_out_%d" % stage_nums)
        # mid supervise
        l2s.append(curr_hg_out)

        return curr_hg_out

    _ = inverted_bottleneck(
        inp, up_channel_ratio(6), out_channel_ratio(24),
        0, 3, scope="hourglass_mid_%d" % stage_nums
    )
    return _


def build_network(input, trainable):
    is_trainable(trainable)

    net = convb(input, 3, 3, out_channel_ratio(16), 2, name="Conv2d_0")

    # 128, 112
    net = slim.stack(net, inverted_bottleneck,
                     [
                         (1, out_channel_ratio(16), 0, 3),
                         (1, out_channel_ratio(16), 0, 3)
                     ], scope="Conv2d_1")

    # 64, 56
    net = slim.stack(net, inverted_bottleneck,
                     [
                         (up_channel_ratio(6), out_channel_ratio(24), 1, 3),
                         (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                         (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                         (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                         (up_channel_ratio(6), out_channel_ratio(24), 0, 3),
                     ], scope="Conv2d_2")

    net_h_w = int(net.shape[1])
    # build network recursively
    hg_out = hourglass_module(net, STAGE_NUM)

    for index, l2 in enumerate(l2s):
        l2_w_h = int(l2.shape[1])
        if l2_w_h == net_h_w:
            continue
        scale = net_h_w // l2_w_h
        l2s[index] = upsample(l2, scale, name="upsample_for_loss_%d" % index)

    return hg_out, l2s
