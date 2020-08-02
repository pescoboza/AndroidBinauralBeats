package com.example.frequencyplayer;

import android.util.Pair;

public class Util {
    // Concatenates the given array to match exactly the given length, cutting if necessary
    public static short[] concatTillLength( short[] src, int length){
        short[] buff = new short[length];
        for (int i = 0; i < length; i++){
            buff[i] = src[i % src.length];
        }
        return buff;
    }

    // Concatenates the given array to match exactly the given length, cutting if necessary
    public static int[] concatTillLength( int[] src, int length){
        int[] dst = new int[length];
        for (int i = 0; i < length; i++){
            dst[i] = src[i % src.length];
        }
        return dst;
    }

    public static int[] concatNTimes(int[] src, int numTimes){
        int length = src.length * numTimes;
        int[] dst = new int[length];
        for (int i = 0; i < length; i++){
            dst[i] = src[i % src.length];
        }
        return dst;
    }


    public static Pair<String, String> splitFileName(String fileName){
        String[] nameAndExt = fileName.split("\\.(?=[^\\.]+$)");
        Pair<String, String> pair = new Pair<String, String>(nameAndExt[0], nameAndExt[1]);
        return pair;
    }


}
