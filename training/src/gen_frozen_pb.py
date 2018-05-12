# -*- coding: utf-8 -*-
# @Time    : 18-3-17 上午9:33
# @Author  : zengzihua@huya.com
# @FileName: gen_pure_pb.py
# @Software: PyCharm

import tensorflow as tf
import argparse
from networks import get_network
import os

from pprint import pprint

os.environ['CUDA_VISIBLE_DEVICES'] = ''
parser = argparse.ArgumentParser(description='Tensorflow Pose Estimation Graph Extractor')
parser.add_argument('--model', type=str, default='mv2_cpm', help='')
parser.add_argument('--size', type=int, default=224)
parser.add_argument('--checkpoint', type=str, default='', help='checkpoint path')
parser.add_argument('--output_node_names', type=str, default='Convolutional_Pose_Machine/stage_5_out')
parser.add_argument('--output_graph', type=str, default='./model.pb', help='output_freeze_path')

args = parser.parse_args()

input_node = tf.placeholder(tf.float32, shape=[1, args.size, args.size, 3], name="image")

with tf.Session() as sess:
    net = get_network(args.model, input_node, trainable=False)
    saver = tf.train.Saver()
    saver.restore(sess, args.checkpoint)

    input_graph_def = tf.get_default_graph().as_graph_def()
    output_graph_def = tf.graph_util.convert_variables_to_constants(
        sess,  # The session
        input_graph_def,  # input_graph_def is useful for retrieving the nodes
        args.output_node_names.split(",")
    )

with tf.gfile.GFile(args.output_graph, "wb") as f:
    f.write(output_graph_def.SerializeToString())