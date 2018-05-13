This repository currently implemented the Convolutional Pose Machine (CPM) using TensorFlow. Instead of normal convolution, inverted residuals (also known as Mobilenet V2) module has been used inside the CPM for faster inference. More experimental models will release as time goes by.

Hence, the respository contains:

* Code of training model
* Code of converting model to TensorFlow Lite
* Android Demo
* IOS Demo (TODO)


> You can buy me a coke if you think my work is helpful for you. <br>
> ETH address: 0x8fcF32D797968B64428ab2d8d09ce2f74143398E


## Training

***

### Dependencies:

* Python3
* TensorFlow >= 1.4

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

The traing dataset only contains single person images and it come from the competition of [AI Challenger](). I transfer the annotation into COCO format for using the data augument code from [tf-pose-estimation](https://github.com/ildoonet/tf-pose-estimation) respository.

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
input_width: 256
input_height: 256
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

2. Edit the parameters files in experiments folder, it contains almost all the hyper-parameters and other configuration you need to define in training. After that, passing the parameters file to start the training:

```bash
cd training
python3 src/train.py experiments/mv2_cpm.cfg
```

It's take 12 hour to training the model on 3 Nvidia 1080Ti graphics cards, below is the corresponding plot on tensorboard.

![image](https://github.com/edvardHua/PoseEstimationForMobile/raw/master/images/loss_lastlayer_heat.png)

### Pretain model

Can be download [here](https://github.com/edvardHua/PoseEstimationForMobile/blob/master/android_demo/app/src/main/assets/mv2-cpm-224.tflite).


## Android Demo

***

After you training the model, the following command can transfer the model into tflite.

```bash
# Convert to frozen pb.
cd training
python3 src/gen_frozen_pb.py \
--checkpoint=<you_training_model_path>/model-xxx --output_graph=<you_output_model_path>/model-xxx.pb \
--size=256 --model=mv2_cpm_2

# Convert to tflite.
# See https://github.com/tensorflow/tensorflow/blob/master/tensorflow/docs_src/mobile/tflite/devguide.md for more information.
bazel-bin/tensorflow/contrib/lite/toco/toco \
--input_file=<you_output_model_path>/model-xxx.pb \
--output_file=<you_output_tflite_model_path>/mv2-cpm.tflite \
--input_format=TENSORFLOW_GRAPHDEF --output_format=TFLITE \
--inference_type=FLOAT \
--input_shape="1,256,256,3" \
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
      imageSizeX: Int = 256,
      imageSizeY: Int = 256,
      outputW: Int = 128,
      outputH: Int = 128,
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

Finally, import the project in `Android Studio` and run in you smartphone.

Also, you can download the [apk] of demo..

## IOS Demo (TODO) 

***

If you are an IOS enthusiast who are interested in this project and want to migrate to ios, we welcome to submit a pull request.

## Reference

***

[1] [Paper of Convolutional Pose Machines](https://arxiv.org/abs/1602.00134)
[2] [Paper of MobileNet V2](https://arxiv.org/pdf/1801.04381.pdf)
[3] [Repository of tf-pose-estimation](https://github.com/ildoonet/tf-pose-estimation)
[4] [Devlope guide of TensorFlow Lite](https://github.com/tensorflow/tensorflow/tree/master/tensorflow/docs_src/mobile/tflite)


## License

***

[Apache License 2.0](https://github.com/edvardHua/PoseEstimationForMobile/blob/master/LICENSE)
