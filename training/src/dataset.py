# -*- coding: utf-8 -*-
# @Time    : 18-3-7 2:55 PM
# @Author  : edvard_hua@live.com
# @FileName: dataset.py
# @Software: PyCharm

import tensorflow as tf

from dataset_augment import pose_random_scale, pose_rotation, pose_flip, pose_resize_shortestedge_random, \
    pose_crop_random, pose_to_img
from dataset_prepare import CocoMetadata
from os.path import join
from pycocotools.coco import COCO
import multiprocessing

BASE = ""
BASE_PATH = ""
TRAIN_JSON = "ai_challenger_train.json"
VALID_JSON = "ai_challenger_valid.json"

TRAIN_ANNO = None
CONFIG = None


def set_config(config):
    global CONFIG, BASE, BASE_PATH
    CONFIG = config
    BASE = CONFIG['imgpath']
    BASE_PATH = CONFIG['datapath']


def _parse_function(imgId):
    """
    :param imgId:
    :return:
    """
    global TRAIN_ANNO
    img_meta = TRAIN_ANNO.loadImgs([imgId])[0]
    anno_ids = TRAIN_ANNO.getAnnIds(imgIds=imgId)
    img_anno = TRAIN_ANNO.loadAnns(anno_ids)
    idx = img_meta['id']
    img_path = join(BASE, img_meta['file_name'])

    img_meta_data = CocoMetadata(idx, img_path, img_meta, img_anno, sigma=4.0)
    img_meta_data = pose_random_scale(img_meta_data)
    img_meta_data = pose_rotation(img_meta_data)
    img_meta_data = pose_flip(img_meta_data)
    img_meta_data = pose_resize_shortestedge_random(img_meta_data)
    img_meta_data = pose_crop_random(img_meta_data)
    return pose_to_img(img_meta_data)


def _set_shapes(img, heatmap):
    img.set_shape([CONFIG['input_width'], CONFIG['input_height'], 3])
    heatmap.set_shape(
        [CONFIG['input_width'] / CONFIG['scale'], CONFIG['input_height'] / CONFIG['scale'], CONFIG['n_kpoints']])
    return img, heatmap


def _get_dataset_pipline(json_filename, batch_size, epoch, buffer_size):
    global TRAIN_ANNO

    TRAIN_ANNO = COCO(
        join(BASE_PATH, json_filename)
    )
    imgIds = TRAIN_ANNO.getImgIds()

    dataset = tf.data.Dataset.from_tensor_slices(imgIds)

    # 在 map 之前 shuffle
    dataset.shuffle(buffer_size)
    dataset = dataset.map(
        lambda imgId: tuple(
            tf.py_func(
                func=_parse_function,
                inp=[imgId],
                Tout=[tf.float32, tf.float32]
            )
        ), num_parallel_calls=CONFIG['multiprocessing_num'])

    dataset = dataset.map(_set_shapes, num_parallel_calls=CONFIG['multiprocessing_num'])
    dataset = dataset.batch(batch_size).repeat(epoch)
    dataset = dataset.prefetch(100)

    return dataset


def get_train_dataset_pipline(batch_size=32, epoch=10, buffer_size=1):
    return _get_dataset_pipline(TRAIN_JSON, batch_size, epoch, buffer_size, )
