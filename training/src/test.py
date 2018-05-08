# -*- coding: utf-8 -*-
# @Time    : 18-3-12 下午5:55
# @Author  : edvard_hua@live.com
# @FileName: test.py
# @Software: PyCharm


def display_image():
    """
    display heatmap & origin image
    :return:
    """
    from image_parsing import CocoMetadata, CocoPose
    from pycocotools.coco import COCO
    from os.path import join
    from dataset import _parse_function

    BASE_PATH = "/root/hdd/baidu/ai_challenger"

    import os
    # os.chdir("..")

    ANNO = COCO(
        join(BASE_PATH, "ai_challenger_valid.json")
    )
    train_imgIds = ANNO.getImgIds()

    img, heat = _parse_function(train_imgIds[199], outer_anno=ANNO)

    CocoPose.display_image(img, heat, pred_heat=heat, as_numpy=False)

    from PIL import Image
    for _ in range(heat.shape[2]):
        data = CocoPose.display_image(img, heat, pred_heat=heat[:, :, _:(_ + 1)], as_numpy=True)
        im = Image.fromarray(data)
        im.save("test/heat_%d.jpg" % _)


def saved_model_graph():
    """
    save the graph of model and check it in tensorboard
    :return:
    """

    from os.path import join
    from network_mv2_cpm import build_network
    import tensorflow as tf
    import os

    INPUT_WIDTH = 256
    INPUT_HEIGHT = 256
    os.environ['CUDA_VISIBLE_DEVICES'] = '0'

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
    from network_mv2_cpm import build_network
    import os
    os.environ['CUDA_VISIBLE_DEVICES'] = '0'

    input_node = tf.placeholder(tf.float32, shape=(1, input_width, input_height, 3),
                                name='image')
    build_network(input_node, False)
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


if __name__ == '__main__':
    metric_prefix(224, 224)

