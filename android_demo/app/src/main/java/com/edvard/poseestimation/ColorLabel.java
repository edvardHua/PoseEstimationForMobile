package com.edvard.poseestimation;

import android.graphics.Color;

/**
 * Created by edvard on 18-3-23.
 */

public enum ColorLabel {

    /*
    * Top = 0 # top
    * Neck = 1 # 脖子
    * RShoulder = 2 # 右肩膀
    * RElbow = 3 # 右肘部
    * RWrist = 4 # 右手腕
    * LShoulder = 5 # 左肩膀
    * LElbow = 6 # 左肘部
    * LWrist = 7 # 左手腕
    * RHip = 8 # 右臀部
    * RKnee = 9 # 右膝盖
    * RAnkle = 10 # 右踝关节
    * LHip = 11 # 左臀部
    * LKnee = 12 # 左膝盖
    * LAnkle = 13 # 左踝关节
    * Background = 14 # 背景
    *
    */

    COLOR_TOP(0, "top", Color.parseColor("#980000")),
    COLOR_NECK(0, "脖子", Color.parseColor("#ff0000")),
    COLOR_RSHOULDER(0, "右肩膀", Color.parseColor("#ff9900")),
    COLOR_RELBOW(0, "右肘部", Color.parseColor("#ffff00")),
    COLOR_RWRIST(0, "右手腕", Color.parseColor("#00ff00")),
    COLOR_LSHOULDER(0, "左肩膀", Color.parseColor("#00ffff")),
    COLOR_LELBOW(0, "左肘部", Color.parseColor("#4a86e8")),
    COLOR_LWRIST(0, "左手腕", Color.parseColor("#0000ff")),
    COLOR_RHIP(0, "右臀部", Color.parseColor("#9900ff")),
    COLOR_RKNEE(0, "右膝盖", Color.parseColor("#274e13")),
    COLOR_RANKLE(0, "右踝关节", Color.parseColor("#e6b8af")),
    COLOR_LHIP(0, "左臀部", Color.parseColor("#0c343d")),
    COLOR_LKNEE(0, "左膝盖", Color.parseColor("#1c4587")),
    COLOR_LANKLE(0, "左踝关节", Color.parseColor("#073763")),
    COLOR_BACKGROUND(0, "背景", Color.parseColor("#20124d"));

    private int mIndex;
    private String mLabel;
    private int mColor;

    ColorLabel(int index, String lable, int color) {
        mIndex = index;
        mLabel = lable;
        mColor = color;
    }

    public int getIndex() {
        return mIndex;
    }

    public String getLabel() {
        return mLabel;
    }

    public int getColor() {
        return mColor;
    }
}
