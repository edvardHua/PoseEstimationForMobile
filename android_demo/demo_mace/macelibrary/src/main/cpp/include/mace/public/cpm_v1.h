// Copyright 2018 Xiaomi, Inc.  All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// This is a generated file. DO NOT EDIT!

#ifndef MACE_CODEGEN_MODELS_CPM_V1_CPM_V1_H_
#define MACE_CODEGEN_MODELS_CPM_V1_CPM_V1_H_

#include <string>
#include "src/main/cpp/include/mace/public/mace.h"

namespace mace {
namespace cpm_v1 {


const unsigned char *LoadModelData(const std::string &model_data_file);

void UnloadModelData(const unsigned char *model_data);

const std::shared_ptr<NetDef> CreateNet();

const std::string ModelName();

const std::string ModelChecksum();

const std::string ModelBuildTime();

const std::string ModelBuildOptions();

}  // namespace cpm_v1
}  // namespace mace

#endif  // MACE_CODEGEN_MODELS_CPM_V1_CPM_V1_H_
