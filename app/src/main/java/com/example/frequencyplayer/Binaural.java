package com.example.frequencyplayer;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.HashMap;
import java.util.Map;

public class Binaural {

    // Caduceus frequencies in Hz with integer exponents [185, 201].
    private static Map<Integer, Double> CADUCEUS_FREQUENCIES =  new HashMap<Integer, Double>(){{
        put(185, 0.25109329 );
        put(186, 0.406277478);
        put(187, 0.657370768);
        put(188, 1.063648245);
        put(189, 1.721019013);
        put(190, 2.784667259);
        put(191, 4.505686274);
        put(192, 7.290353535);
        put(193, 11.79603981);
        put(194, 19.08639335);
        put(195, 30.88242217);
        put(196, 49.96882653);
        put(197, 80.85125972);
        put(198, 130.8200863);
        put(199, 211.671346 );
        put(200, 342.4914324);
        put(201, 554.1627785);
    }};

    private static int sampleRate = 16*1024;
    private static int bitDepth = 16;
    private static int sampleDurationMs = 1000;
    private static int bitDepthFactor = (int)((Math.pow(2, Binaural.bitDepth)-1)/2);

    private static int[] rightChannel;
    private static int[] leftChannel;

    private static final double DEFAULT_BEAT = 1.0;
    private static final double DEFAULT_SHIFT = 180.0;
    private static final double DEFAULT_FREQUENCY = CADUCEUS_FREQUENCIES.get(196); //  49.96882653 Hz

    private static boolean isBuffersFull = false;

    public static void setSampleRate(int sampleRate){
        Binaural.sampleRate = sampleRate;
    }

    public static void setBitDepth(int bitDepth){
        Binaural.bitDepth = bitDepth;
        Binaural.bitDepthFactor = (int)((Math.pow(2, Binaural.bitDepth)-1)/2);
    }

    public static void setSampleDurationMs(int ms){
        Binaural.sampleDurationMs = ms;
    }

    private static int[] createSinWaveBuffer(double frequency, double shiftDeg) {
        double shiftRad = Math.toRadians(shiftDeg);
        int numSamplesPerPeriod = (int)(sampleRate / frequency);

        int[] buffer = new int[numSamplesPerPeriod];

        for (int i = 0; i< numSamplesPerPeriod; i++) {
            double angle = 2.0*Math.PI*i/numSamplesPerPeriod;
            buffer[i] = (int)(Math.sin(angle + shiftRad) * bitDepthFactor);
        }
        return buffer;
    }

    public static void generateBuffers(double frequency, double beat, double shiftDeg, double durationMs){
        generateBuffers(frequency, beat, shiftDeg, (int)(durationMs*1000/frequency));
    }

    public static void generateBuffers(double frequency, double beat, double shiftDeg, int numLoops){
        if (numLoops <= 0){
            throw new IllegalArgumentException("Number of loops must me positive.");
        }

        int[] right = createSinWaveBuffer(frequency,0.0);
        int[] left = createSinWaveBuffer(frequency + beat, shiftDeg);

        int numSamplesPerPeriod = right.length;

        if (numLoops > 1){
            int channelLen = numSamplesPerPeriod*numLoops;

            rightChannel = new int[channelLen];
            leftChannel = new int[channelLen];

            for (int i = 0; i < channelLen; i++) {
                int destPos = i*channelLen;
                System.arraycopy(right, 0, rightChannel, destPos, numSamplesPerPeriod);
            }

        }else {
            rightChannel = right;
            leftChannel = left;
        }

        isBuffersFull = true;
    }

    public static void clearBuffers(){
        rightChannel = null;
        leftChannel = null;
        isBuffersFull = true;
    }

    public static int writeWaveFile(String fileName){
        if (!isBuffersFull){
            return -1;
        }


        int bufferSize = rightChannel.length;

        byte[] wavHeader = new byte[44];
        wavHeader[0] = 'R';
        wavHeader[1] = 'I';
        wavHeader[2] = 'F';
        wavHeader[3] = 'F';
        // 4 - 7: file size
        wavHeader[8] = 'W';
        wavHeader[9] = 'A';
        wavHeader[10] = 'V';
        wavHeader[11] = 'V';

        wavHeader[12] = 'f';
        wavHeader[13] = 'm';
        wavHeader[14] = 't';








    }

}
