package com.malcolmmaima.dishi.Controller.Utils;

import android.util.Log;

public class SplitTimeString {
    String TAG = "SplitTimeString";
    public String[] Split(String timeStamp){

        String[] arrSplit = {};

        if(timeStamp != null){

            try {
                arrSplit = timeStamp.split(":");
            } catch (Exception e){
                Log.e(TAG, "Split: ", e);
            }

        }
        return arrSplit;
    }
}
