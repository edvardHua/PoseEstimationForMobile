# -*- coding: utf-8 -*-
# @Time    : 18-3-6 3:20 PM
# @Author  : zengzihua@huya.com
# @FileName: data_filter.py
# @Software: PyCharm

import network_mv2_cpm

def get_network(type, input, trainable=True):
    if type == 'mv2_cpm':
        net, loss = network_mv2_cpm.build_network(input, trainable)
    return net, loss
