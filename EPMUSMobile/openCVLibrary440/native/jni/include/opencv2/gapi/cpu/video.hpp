// This file is part of OpenCV project.
// It is subject to the license terms in the LICENSE file found in the top-level directory
// of this distribution and at http://opencv.org/license.html.
//
// Copyright (C) 2020 Intel Corporation

#ifndef OPENCV_GAPI_CPU_VIDEO_API_HPP
#define OPENCV_GAPI_CPU_VIDEO_API_HPP

#include <opencv2/gapi/gkernel.hpp> // GKernelPackage

namespace cv {
namespace gapi {
namespace video {
namespace cpu {

GAPI_EXPORTS GKernelPackage kernels();

} // namespace cpu
} // namespace video
} // namespace gapi
} // namespace cv


#endif // OPENCV_GAPI_CPU_VIDEO_API_HPP
