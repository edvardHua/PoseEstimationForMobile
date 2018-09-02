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

from dataset_augment import pose_random_scale, pose_rotation, pose_flip, pose_resize_shortestedge_random, \
    pose_crop_random, pose_to_img
from dataset_prepare import CocoMetadata
from os.path import join
from pycocotools.coco import COCO
import multiprocessing

BASE = "/root/hdd"
BASE_PATH = ""
TRAIN_JSON = "ai_challenger_train.json"
VALID_JSON = "ai_challenger_valid.json"

TRAIN_ANNO = None
VALID_ANNO = None
CONFIG = None


def set_config(config):
    global CONFIG, BASE, BASE_PATH
    CONFIG = config
    BASE = CONFIG['imgpath']
    BASE_PATH = CONFIG['datapath']


def _parse_function(imgId, is_train, ann=None):
    """
    :param imgId:
    :return:
    """

    global TRAIN_ANNO
    global VALID_ANNO

    if ann is not None:
        if is_train == True:
            TRAIN_ANNO = ann
        else:
            VALID_ANNO = ann
    else:
        if is_train == True:
            anno = TRAIN_ANNO
        else:
            anno = VALID_ANNO

    img_meta = anno.loadImgs([imgId])[0]
    anno_ids = anno.getAnnIds(imgIds=imgId)
    img_anno = anno.loadAnns(anno_ids)
    idx = img_meta['id']
    img_path = join(BASE, img_meta['file_name'])

    img_meta_data = CocoMetadata(idx, img_path, img_meta, img_anno, sigma=6.0)
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


def _get_dataset_pipeline(anno, batch_size, epoch, buffer_size, is_train=True):

    imgIds = anno.getImgIds()

    dataset = tf.data.Dataset.from_tensor_slices(imgIds)

    dataset.shuffle(buffer_size)
    dataset = dataset.map(
        lambda imgId: tuple(
            tf.py_func(
                func=_parse_function,
                inp=[imgId, is_train],
                Tout=[tf.float32, tf.float32]
            )
        ), num_parallel_calls=CONFIG['multiprocessing_num'])

    dataset = dataset.map(_set_shapes, num_parallel_calls=CONFIG['multiprocessing_num'])
    dataset = dataset.batch(batch_size).repeat(epoch)
    dataset = dataset.prefetch(100)

    return dataset


def get_train_dataset_pipeline(batch_size=32, epoch=10, buffer_size=1):
    global TRAIN_ANNO

    anno_path = join(BASE_PATH, TRAIN_JSON)
    print("preparing annotation from:", anno_path)
    TRAIN_ANNO = COCO(
        anno_path
    )
    return _get_dataset_pipeline(TRAIN_ANNO, batch_size, epoch, buffer_size, True)

def get_valid_dataset_pipeline(batch_size=32, epoch=10, buffer_size=1):
    global VALID_ANNO

    anno_path = join(BASE_PATH, VALID_JSON)
    print("preparing annotation from:", anno_path)
    VALID_ANNO = COCO(
        anno_path
    )
    return _get_dataset_pipeline(VALID_ANNO, batch_size, epoch, buffer_size, False)
