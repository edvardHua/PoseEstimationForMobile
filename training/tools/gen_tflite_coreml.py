# -*- coding: utf-8 -*-
# @Time    : 18-7-16 上午10:26
# @Author  : edvard_hua@live.com
# @FileName: gen_tflite_coreml.py
# @Software: PyCharm

import argparse
import os

os.environ['CUDA_VISIBLE_DEVICES'] = ''
parser = argparse.ArgumentParser(description="Tools for convert frozen_pb into tflite or coreml.")
parser.add_argument("--frozen_pb", type=str, default="./hourglass/model-360000.pb", help="Path for storing checkpoint.")
parser.add_argument("--input_node_name", type=str, default="image", help="Name of input node name.")
parser.add_argument("--output_node_name", type=str, default="hourglass_out_3", help="Name of output node name.")
parser.add_argument("--output_path", type=str, default="./hourglass", help="Path for storing tflite & coreml")
parser.add_argument("--type", type=str, default="coreml", help="tflite or coreml")

args = parser.parse_args()

output_filename = args.frozen_pb.rsplit("/", 1)[1]
output_filename = output_filename.split(".")[0]


if "tflite" in args.type:
    import tensorflow as tf
    output_filename += ".tflite"
    converter = tf.contrib.lite.TocoConverter.from_frozen_graph(
        args.frozen_pb,
        [args.input_node_name],
        [args.output_node_name]
    )
    tflite_model = converter.convert()
    open(os.path.join(args.output_path, output_filename), "wb").write(tflite_model)
    print("Generate tflite success.")
elif "coreml" in args.type:
    import tfcoreml as tf_converter
    output_filename += ".mlmodel"
    tf_converter.convert(tf_model_path=args.frozen_pb,
                         mlmodel_path = os.path.join(args.output_path, output_filename),
                         image_input_names = ["%s:0" % args.input_node_name],
                         output_feature_names = ['%s:0' % args.output_node_name])
    print("Generate CoreML success.")





