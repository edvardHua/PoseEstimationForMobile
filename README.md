This repository currently implemented the CPM and Hourglass model using TensorFlow. Instead of normal convolution, inverted residuals (also known as Mobilenet V2) module has been used inside the model for **real-time** inference. 


<table>

  <tr>
    <td>Model</td>
    <td>FLOPs</td>
    <td>PCKh</td>
    <td>Inference Time</td>
  </tr>

  <tr>
	<td>CPM</td>
	<td>0.5G</td>
	<td>93.78</td>
	<td rowspan="2">
	~60 FPS on Snapdragon 845 <br/>
	30+ FPS on iPhone x (need more test)
	</td>
  </tr>

  <tr>
	<td>Hourglass</td>
	<td>0.5G</td>
	<td>91.81</td>
  </tr>
</table>

> You can modify the [architectures](https://github.com/edvardHua/PoseEstimationForMobile/tree/master/training/src) of network for training much higher PCKh model.

The respository contains:

* Code of training cpm & hourglass model
* Android demo source code
* IOS demo source code

Below GIF is catch on Mi Mix2s (~60 FPS)

![image](https://github.com/edvardHua/PoseEstimationForMobile/raw/master/images/demo.gif)

You can download the apk as below to test on your device.

| Using Mace (Support GPU) | Using TFlite (Only CPU) |
| --- | --- |
| [PoseEstimation-Mace.apk](https://raw.githubusercontent.com/edvardHua/PoseEstimationForMobile/master/release/PoseEstimation-Mace.apk) | [PoseEstimation-TFlite.apk](https://raw.githubusercontent.com/edvardHua/PoseEstimationForMobile/master/release/PoseEstimation-TFlite.apk) |


> Issue and PR are welcome when you encount any problem.

## Training

***

### Dependencies:

* Python3
* TensorFlow >= 1.4
* Mace

### Dataset:

Training dataset available through [google driver](https://drive.google.com/open?id=1zahjQWhuKIYWRRI2ZlHzn65Ug_jIiC4l).

Unzip it will obtain the following file structure

```bash
# root @ ubuntu in ~/hdd/ai_challenger
$ tree -L 1 .
.
├── ai_challenger_train.json
├── ai_challenger_valid.json
├── train
└── valid
```

The traing dataset only contains single person images and it come from the competition of [AI Challenger](https://challenger.ai/datasets/keypoint). 

* 22446 training examples
* 1500 testing examples

I transfer the annotation into COCO format for using the data augument code from [tf-pose-estimation](https://github.com/ildoonet/tf-pose-estimation) respository.

### Hyper-parameter

In training procedure, we use `cfg` file on `experiments` folder for passing the hyper-parameter.

Below is the content of [mv2_cpm.cfg](https://github.com/edvardHua/PoseEstimationForMobile/blob/master/training/experiments/mv2_cpm.cfg).

```bash
[Train]
model: 'mv2_cpm'
checkpoint: False
datapath: '/root/hdd/ai_challenger'
imgpath: '/root/hdd/'
visible_devices: '0, 1, 2'
multiprocessing_num: 8
max_epoch: 1000
lr: '0.001'
batchsize: 5
decay_rate: 0.95
input_width: 192
input_height: 192
n_kpoints: 14
scale: 2
modelpath: '/root/hdd/trained/mv2_cpm/models'
logpath: '/root/hdd/trained/mv2_cpm/log'
num_train_samples: 20000
per_update_tensorboard_step: 500
per_saved_model_step: 2000
pred_image_on_tensorboard: True
```

The cfg not cover all the parameters of the model, there still have some parameters in the `network_mv2_cpm.py`.

### Train by nvidia-docker

Build the docker by the following command:

```bash
cd training/docker
docker build -t single-pose .
```
or

```
docker pull edvardhua/single-pose
```

Then run the following command to train the model:

```bash
nvidia-docker run -it -d \
-v <dataset_path>:/data5 -v <training_code_path>/training:/workspace \
-p 6006:6006 -e LOG_PATH=/root/hdd/trained/mv2_cpm/log \
-e PARAMETERS_FILE=experiments/mv2_cpm.cfg edvardhua/single-pose
```

Also, it will create the tensorboard on port 6006. Beside, make sure you install the `nvidia-docker`.

### Train by ordinary way

1. install the dependencies.

```bash
cd training
pip3 install -r requirements.txt
```

Beside, you also need to install [cocoapi](https://github.com/cocodataset/cocoapi)

2. Edit the parameters files in experiments folder, it contains almost all the hyper-parameters and other configuration you need to define in training. After that, passing the parameters file to start the training:

```bash
cd training
python3 src/train.py experiments/mv2_cpm.cfg
```

After 12 hour training, the model is almost coverage on 3 Nvidia 1080Ti graphics cards, below is the corresponding plot on tensorboard.

![image](https://github.com/edvardHua/PoseEstimationForMobile/raw/master/images/loss_lastlayer_heat.png)

### Bechmark (PCKh)

Run the follow command to evaluate the value of your PCKh.

```bash
python3 src/benchmark.py --frozen_pb_path=hourglass/model-360000.pb \
--anno_json_path=/root/hdd/ai_challenger/ai_challenger_valid.json \
--img_path=/root/hdd \
--output_node_name=hourglass_out_3
```


### Pretain model

CPM

* [Frozen graph](https://github.com/edvardHua/PoseEstimationForMobile/tree/master/release/cpm_model)
* [TFlite](https://github.com/edvardHua/PoseEstimationForMobile/tree/master/release/cpm_model)
* [CoreML](https://github.com/edvardHua/PoseEstimationForMobile/tree/master/release/cpm_model)

Hourglass

* [Frozen graph](https://github.com/edvardHua/PoseEstimationForMobile/tree/master/release/hourglass_model)
* [TFlite](https://github.com/edvardHua/PoseEstimationForMobile/tree/master/release/hourglass_model)
* [CoreML](https://github.com/edvardHua/PoseEstimationForMobile/tree/master/release/hourglass_model)

## Android Demo

***

Thanks to mace framework, now you can using GPU to run this model on android smartphone.

Following command can transfer model into mace format.

```bash
cd <your-mace-path>
# You transer hourglass or cpm model by changing `yml` file.
python tools/converter.py convert --config=<PoseEstimationForMobilePath>/release/mace_ymls/cpm.yml
```

Then follow the instruction of [mace documentation](https://mace.readthedocs.io/en/latest/user_guide/basic_usage.html) to integrate into android.

For how to invoke the model and parsing output, you can check the [android source code](https://github.com/edvardHua/PoseEstimationForMobile/tree/master/android_demo) i provided.

The benchmark of some socs for average inference time are shown as follow.

Model | Snapdragon 845 | Snapdragon 660 | Hisilicon 960 | Exynos 7420 
---- | --- | --- | --- | --- 
CPM & Hourglass | 17 ms | 30 ms | 42 ms | 103 ms 

Below is the environments i build this demo.

- Operation System: `macOS 10.13.6` (mace not support build under windows now)
- Android Studio: `3.0.1`
- NDK Version: `r16`

**Different environments may encounter different error when you build mace-demo. To avoid this, i suggest using docker.**

```bash
docker pull registry.cn-hangzhou.aliyuncs.com/xiaomimace/mace-dev-lite

docker run -it
	--privileged -d --name mace-dev 
	--net=host 
	-v to/you/path/PoseEstimationForMobile/android_demo/demo_mace:/demo_mace 
	registry.cn-hangzhou.aliyuncs.com/xiaomimace/mace-dev-lite

docker run -it --privileged -d --name mace-dev --net=host \
           -v to/you/path/PoseEstimationForMobile/android_demo/demo_mace:/demo_mace  \
           registry.cn-hangzhou.aliyuncs.com/xiaomimace/mace-dev-lite

# Enter to docker
docker exec -it mace-dev bash

# Exec command inside the docker
cd /demo_mace && ./gradlew build

```

***


Or you can transfer the model into tflite.

```bash
# Convert to frozen pb.
cd training
python3 src/gen_frozen_pb.py \
--checkpoint=<you_training_model_path>/model-xxx --output_graph=<you_output_model_path>/model-xxx.pb \
--size=192 --model=mv2_cpm_2

# If you update tensorflow to 1.9, run following command.
python3 src/gen_tflite_coreml.py \
--frozen_pb=forzen_graph.pb \
--input_node_name='image' \
--output_node_name='Convolutional_Pose_Machine/stage_5_out' \
--output_path='./' \
--type=tflite
 
# Convert to tflite.
# See https://github.com/tensorflow/tensorflow/blob/master/tensorflow/docs_src/mobile/tflite/devguide.md for more information.
bazel-bin/tensorflow/contrib/lite/toco/toco \
--input_file=<you_output_model_path>/model-xxx.pb \
--output_file=<you_output_tflite_model_path>/mv2-cpm.tflite \
--input_format=TENSORFLOW_GRAPHDEF --output_format=TFLITE \
--inference_type=FLOAT \
--input_shape="1,192,192,3" \
--input_array='image' \
--output_array='Convolutional_Pose_Machine/stage_5_out'

```

Then, place the tflite file in `android_demo/app/src/main/assets` and modify the parameters in `ImageClassifierFloatInception.kt`.

```java
......
......
    // parameters need to modify in ImageClassifierFloatInception.kt
    /**
     * Create ImageClassifierFloatInception instance
     *
     * @param imageSizeX Get the image size along the x axis.
     * @param imageSizeY Get the image size along the y axis.
     * @param outputW The output width of model
     * @param outputH The output height of model
     * @param modelPath Get the name of the model file stored in Assets.
     * @param numBytesPerChannel Get the number of bytes that is used to store a single
     * color channel value.
     */
    fun create(
      activity: Activity,
      imageSizeX: Int = 192,
      imageSizeY: Int = 192,
      outputW: Int = 96,
      outputH: Int = 96,
      modelPath: String = "mv2-cpm.tflite",
      numBytesPerChannel: Int = 4
    ): ImageClassifierFloatInception =
      ImageClassifierFloatInception(
          activity,
          imageSizeX,
          imageSizeY,
          outputW,
          outputH,
          modelPath,
          numBytesPerChannel)
......
......
```

Finally, import the project to `Android Studio` and run in you smartphone.

## iOS Demo

***

Thanks to [tucan](https://github.com/tucan9389), now you can run model on iOS.

First, convert model into CoreML model.

```bash
# Convert to frozen pb.
cd training
python3 src/gen_frozen_pb.py \
--checkpoint=<you_training_model_path>/model-xxx --output_graph=<you_output_model_path>/model-xxx.pb \
--size=192 --model=mv2_cpm_2

# Run the following command to get mlmodel
python3 src/gen_tflite_coreml.py \
--frozen_pb=forzen_graph.pb \
--input_node_name='image' \
--output_node_name='Convolutional_Pose_Machine/stage_5_out' \
--output_path='./' \
--type=coreml
```

Then, follow the instruction on [PoseEstimation-CoreML](https://github.com/tucan9389/PoseEstimation-CoreML).


## Reference

***

[1] [Paper of Convolutional Pose Machines](https://arxiv.org/abs/1602.00134) <br/>
[2] [Paper of Stack Hourglass](https://arxiv.org/abs/1603.06937) <br/>
[3] [Paper of MobileNet V2](https://arxiv.org/pdf/1801.04381.pdf) <br/>
[4] [Repository PoseEstimation-CoreML](https://github.com/tucan9389/PoseEstimation-CoreML) <br/>
[5] [Repository of tf-pose-estimation](https://github.com/ildoonet/tf-pose-estimation) <br>
[6] [Devlope guide of TensorFlow Lite](https://github.com/tensorflow/tensorflow/tree/master/tensorflow/docs_src/mobile/tflite) <br/>
[7] [Mace documentation](https://mace.readthedocs.io)

## License

***

[Apache License 2.0](https://github.com/edvardHua/PoseEstimationForMobile/blob/master/LICENSE)
