package com.hm.myapplication2.utils

import android.content.Context
import android.provider.Settings

/**
 * Created by Harry Mehta on 07/08/20 at 2:19 PM
 */
class Utils {

    companion object {

        /**
         * get device name
         *
         * @param context
         * @return
         */
         public fun getDeviceName(context : Context ):String = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name");

        /**
         * get Extension from file name
         *
         * @param mimeType
         * @return
         */
        public fun getExtensionFromMimeType(mimeType:String) : String {
             when (mimeType){
                 "image/jpeg" -> return "jpeg"
                  "image/png" -> return  "png"
                 else  -> return "jpeg"
             }
        }

    }


}