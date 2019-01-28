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

#include <map>
#include <memory>
#include <string>
#include <vector>

#include "mace/public/mace.h"
#include "mace/public/mace_runtime.h"

namespace mace {

namespace cpm_v1 {

extern const unsigned char *LoadModelData();

extern const std::shared_ptr<NetDef> CreateNet();

extern const std::string ModelName();
extern const std::string ModelChecksum();
extern const std::string ModelBuildTime();
extern const std::string ModelBuildOptions();

}  // namespace cpm_v1

namespace {
std::map<std::string, int> model_name_map {
  std::make_pair("cpm_v1", 0),
};
}  // namespace

MaceStatus CreateMaceEngineFromCode(
    const std::string &model_name,
    const std::string &model_data_file,
    const std::vector<std::string> &input_nodes,
    const std::vector<std::string> &output_nodes,
    const DeviceType device_type,
    std::shared_ptr<MaceEngine> *engine) {
  // load model
  if (engine == nullptr) {
    return MaceStatus::MACE_INVALID_ARGS;
  }
  std::shared_ptr<NetDef> net_def;
  (void)model_data_file;
  const unsigned char * model_data;
  MaceStatus status = MaceStatus::MACE_SUCCESS;
  switch (model_name_map[model_name]) {
    case 0:
      net_def = mace::cpm_v1::CreateNet();
      engine->reset(new mace::MaceEngine(device_type));
      model_data = mace::cpm_v1::LoadModelData();
      status = (*engine)->Init(net_def.get(), input_nodes, output_nodes,
                               model_data);
      break;
   default:
     status = MaceStatus::MACE_INVALID_ARGS;
  }

  return status;
}

}  // namespace mace