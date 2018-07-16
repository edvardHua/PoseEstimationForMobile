
```bash
# input_shape = [1, 192, 192, 3]
# output_shape = [1, 48, 48, 14]
python3 src/gen_tflite_coreml.py \
--frozen_pb=hourglass/model-374000.pb \
--output_node_name=hourglass_out_3 \
--output_path=hourglass \
--type=coreml
```
