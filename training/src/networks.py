# -*- coding: utf-8 -*-
# @Time    : 18-3-6 3:20 PM
# @Author  : zengzihua@huya.com
# @FileName: data_filter.py
# @Software: PyCharm

from network_mv2_cpm import build_network


def get_network(type, input, trainable=True):
    if type == 'mv2_cpm':
        net, loss = build_network(input, trainable)

    return net, loss
