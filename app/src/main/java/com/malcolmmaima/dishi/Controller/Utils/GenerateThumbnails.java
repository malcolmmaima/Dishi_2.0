package com.malcolmmaima.dishi.Controller.Utils;

import android.util.Log;

public class GenerateThumbnails {

    /**
     * https://www.geeksforgeeks.org/insert-a-string-into-another-string-in-java/
     *
     * So firebase takes care of image resizing, when the storage bucket detects a new image uploaded,
     * it generates different preset image sizes (200x200, 400x400, 680x680). The job of this function is to append
     * the different sizes onto the original uploaded image link. Since the original has a download token that is shared among
     * the presets we need to do a bit of string processing
     */
    int indexSmall, indexMedium, indexBig;
    String smallXY = "_200x200";
    String mediumXY = "_400x400";
    String bigXY = "_680x680";

    public String GenerateSmall(String original){

        if(original.contains(".jpg")){
            indexSmall = original.indexOf(".jpg")-1;
        }

        else if(original.contains(".png")){
            indexSmall = original.indexOf(".png")-1;
        }


        // Create a new string
        String newString = new String();

        for (int i = 0; i < original.length(); i++) {

            // Insert the original string character
            // into the new string
            newString += original.charAt(i);

            if (i == indexSmall) {

                // Insert the string to be inserted
                // into the new string
                newString += smallXY;
            }
        }

        return newString;
    }

    public String GenerateMedium(String original){

        if(original.contains(".jpg")){
            indexMedium = original.indexOf(".jpg")-1;
        }

        else if(original.contains(".png")){
            indexMedium = original.indexOf(".png")-1;
        }

        // Create a new string
        String newString = new String();

        for (int i = 0; i < original.length(); i++) {

            // Insert the original string character
            // into the new string
            newString += original.charAt(i);

            if (i == indexMedium) {

                // Insert the string to be inserted
                // into the new string
                newString += mediumXY;
            }
        }

        return newString;
    }

    public String GenerateBig(String original){

        if(original.contains(".jpg")){
            indexBig = original.indexOf(".jpg")-1;
        }

        else if(original.contains(".png")){
            indexBig = original.indexOf(".png")-1;
        }

        // Create a new string
        String newString = new String();

        for (int i = 0; i < original.length(); i++) {

            // Insert the original string character
            // into the new string
            newString += original.charAt(i);

            if (i == indexBig) {

                // Insert the string to be inserted
                // into the new string
                newString += bigXY;
            }
        }

        return newString;
    }

}
