# -*- coding: utf-8 -*-
# @Time    : 18-4-24 5:48 PM
# @Author  : edvard_hua@live.com
# @FileName: network_base.py
# @Software: PyCharm

import tensorflow as tf
import tensorflow.contrib.slim as slim

_init_xavier = tf.contrib.layers.xavier_initializer()
_init_norm = tf.truncated_normal_initializer(stddev=0.01)
_init_zero = slim.init_ops.zeros_initializer()
_l2_regularizer_00004 = tf.contrib.layers.l2_regularizer(0.00004)
_trainable = True


def is_trainable(trainable=True):
    global _trainable
    _trainable = trainable


def max_pool(inputs, k_h, k_w, s_h, s_w, name, padding="SAME"):
    return tf.nn.max_pool(inputs,
                          ksize=[1, k_h, k_w, 1],
                          strides=[1, s_h, s_w, 1],
                          padding=padding,
                          name=name)


def upsample(inputs, factor, name):
    return tf.image.resize_bilinear(inputs, [int(inputs.get_shape()[1]) * factor, int(inputs.get_shape()[2]) * factor],
                                    name=name)


def inverted_bottleneck(inputs, up_channel_rate, channels, subsample, k_s=3, scope=""):
    with tf.variable_scope("inverted_bottleneck_%s" % scope):
        with slim.arg_scope([slim.batch_norm],
                            decay=0.999,
                            fused=True,
                            is_training=_trainable,
                            activation_fn=tf.nn.relu6):
            stride = 2 if subsample else 1

            output = slim.convolution2d(inputs,
                                        up_channel_rate * inputs.get_shape().as_list()[-1],
                                        stride=1,
                                        kernel_size=[1, 1],
                                        weights_initializer=_init_xavier,
                                        biases_initializer=_init_zero,
                                        normalizer_fn=slim.batch_norm,
                                        weights_regularizer=None,
                                        scope=scope + '_up_pointwise')

            output = slim.separable_convolution2d(output,
                                                  num_outputs=None,
                                                  stride=stride,
                                                  depth_multiplier=1.0,
                                                  kernel_size=k_s,
                                                  weights_initializer=_init_xavier,
                                                  weights_regularizer=_l2_regularizer_00004,
                                                  biases_initializer=None,
                                                  padding="SAME",
                                                  scope=scope + '_depthwise')

            output = slim.convolution2d(output,
                                        channels,
                                        stride=1,
                                        kernel_size=[1, 1],
                                        activation_fn=None,
                                        weights_initializer=_init_xavier,
                                        biases_initializer=_init_zero,
                                        normalizer_fn=slim.batch_norm,
                                        weights_regularizer=None,
                                        scope=scope + '_pointwise')
            if inputs.get_shape().as_list()[-1] == channels:
                output = tf.add(inputs, output)

    return output


def convb(input, k_h, k_w, c_o, stride, name, relu=True):
    with slim.arg_scope([slim.batch_norm], decay=0.999, fused=True, is_training=_trainable):
        output = slim.convolution2d(
            inputs=input,
            num_outputs=c_o,
            kernel_size=[k_h, k_w],
            stride=stride,
            normalizer_fn=slim.batch_norm,
            weights_regularizer=_l2_regularizer_00004,
            weights_initializer=_init_xavier,
            biases_initializer=_init_zero,
            activation_fn=tf.nn.relu if relu else None,
            scope=name
        )
    return output
