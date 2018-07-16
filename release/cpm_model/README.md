```bash
# input_shape: [1, 192, 192, 3]
# output_shape: [1, 96, 96, 14]
python3 src/gen_tflite_coreml.py \
--frozen_pb=cpm/model-tiny.pb \
--output_node_name=Convolutional_Pose_Machine/stage_5_out \
--type=tflite \
--output_path=cpm
```
