FROM nvidia/cuda:8.0-cudnn6-devel-ubuntu16.04

MAINTAINER zengzihua (edvard_hua@live.com)

# pip source, use when you located in mainland, china
ENV DOUBAN_SOURCE="https://pypi.douban.com/simple"

RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
        libcurl3-dev \
        libfreetype6-dev \
        libpng12-dev \
        libzmq3-dev \
        pkg-config \
        python3 \
        python3-pip \
        zlib1g-dev \
        python-opencv \
        cython \
        > /dev/null \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN pip3 --no-cache-dir -q install -i $DOUBAN_SOURCE --upgrade pip setuptools

RUN pip3 --no-cache-dir -q install -i $DOUBAN_SOURCE  \
        matplotlib==2.1.0 \
        numpy==1.13.3 \
        scipy==1.0.0 \
        sklearn==0.0 \
        pandas==0.21.0 \
        seaborn==0.8.1 \
        simplejson==3.10.0 \
        tensorpack==0.8.0 \
        requests \
        opencv-python

# Configure the build for our CUDA configuration.
ENV CI_BUILD_PYTHON=python \
    LD_LIBRARY_PATH=/usr/local/cuda/extras/CUPTI/lib64:${LD_LIBRARY_PATH} \
    CUDNN_INSTALL_PATH=/usr/lib/x86_64-linux-gnu \
    PYTHON_BIN_PATH=/usr/bin/python \
    PYTHON_LIB_PATH=/usr/local/lib/python2.7/dist-packages \
    TF_NEED_CUDA=1 \
    TF_CUDA_VERSION=8.0 \
    TF_CUDA_COMPUTE_CAPABILITIES=3.0,3.5,5.2,6.0,6.1,7.0 \
    TF_CUDNN_VERSION=6

# Configure so file
RUN ln -s /usr/local/cuda/lib64/stubs/libcuda.so /usr/local/cuda/lib64/stubs/libcuda.so.1 && \
    LD_LIBRARY_PATH=/usr/local/cuda/lib64/stubs:${LD_LIBRARY_PATH}

# Install TensorFlow
RUN pip3 -q install -i $DOUBAN_SOURCE tensorflow-gpu==1.4.0 Cython

RUN apt-get update && apt-get install -yq python3 python3-dev python3-pip python3-setuptools python3-tk

# Install cocoapi package
COPY cocoapi /cocoapi/
RUN cd /cocoapi/PythonAPI && python3 setup.py build_ext --inplace && python3 setup.py build_ext install

# Clean
RUN rm -rf /tmp/pip \
    && rm -rf /root/.cache

# Training default setting
ENV SCRIPT_PATH="src/train.py" \
    BASE_PATH="/workspace" \
    LOG_PATH="" \
    PARAMETERS_FILE=""

COPY .config /root/.config/

CMD cd $BASE_PATH && env DISPLAY=0.0 python3 $SCRIPT_PATH $PARAMETERS_FILE & tensorboard --logdir=$LOG_PATH
