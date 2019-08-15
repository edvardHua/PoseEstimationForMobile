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

import pefm.networks.network_mv2_cpm
import pefm.networks.network_mv2_hourglass

def get_network(type, input, trainable=True):
    if type == 'mv2_cpm':
        net, loss = network_mv2_cpm.build_network(input, trainable)
    elif type == "mv2_hourglass":
        net, loss = network_mv2_hourglass.build_network(input, trainable)        
    return net, loss
