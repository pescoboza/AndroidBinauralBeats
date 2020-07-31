package com.example.frequencyplayer;

public class Util {
    // Concatenates the given array to match exactly the given length, cutting if necessary
    public static short[] concatTillLength( short[] src, int length){
        short[] buff = new short[length];
        for (int i = 0; i < length; i++){
            buff[i] = src[i % src.length];
        }
        return buff;
    }


}
