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

import tensorflow as tf

def output_1d(stage_out):
    b, h, w, c = stage_out.shape
    flat_out = tf.reshape(stage_out, (-1, h * w, c))
    return tf.identity(tf.argmax(flat_out, axis=1), "output_1d")


def output_2d(stage_out):
    b, h, w, c = stage_out.shape
    tmp = tf.reshape(stage_out, (-1, h * w, c))
    res = tf.argmax(tmp, axis=1)
    h = tf.floor(tf.reshape(tf.divide(res, w), (-1, 1, c)))
    h = tf.cast(h, dtype=tf.float32)
    w = tf.cast(tf.reshape(tf.mod(res, w), (-1, 1, c)), dtype=tf.float32)
    res = tf.concat([w, h], axis=1)
    return tf.identity(res, "output_2d")
