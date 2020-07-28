package com.example.frequencyplayer;

import java.util.HashMap;
import java.util.Map;

public class Binaural {

    // Caduceus frequencies in Hz with integer exponents [185, 201].
    private static final double[] CADUCEUS_FREQUENCIES = {
            0.25109329,  // 0
            0.406277478, // 1
            0.657370768, // 2
            1.063648245, // 3
            1.721019013, // 4
            2.784667259, // 5
            4.505686274, // 6
            7.290353535, // 7
            11.79603981, // 8
            19.08639335, // 9
            30.88242217, // 10
            49.96882653, // 11
            80.85125972, // 12
            130.8200863, // 13
            211.671346,  // 14
            342.4914324, // 15
            554.1627785  // 16
    };

    private static int sampleRate = 16*1024;
    private static int bitDepth = 16;
    private static int sampleDurationMs = 1000;

    private static long[] rightChannel;
    private static long[] leftChannel;

    public static final double DEFAULT_BEAT = 1.0;
    public static final double DEFAULT_SHIFT = 180.0;
    public static final double DEFAULT_FREQUENCY = CADUCEUS_FREQUENCIES[11]; //  49.96882653 Hz

    public static void setSampleRate(int sampleRate){
        Binaural.sampleRate = sampleRate;
    }

    public static void setBitDepth(int bitDepth){
        Binaural.bitDepth = bitDepth;
    }

    public static void setSampleDurationMs(int ms){
        Binaural.sampleDurationMs = ms;
    }

    private static long[] createSinWaveBuffer(double frequency, int sampleRate, int bitDepth, double ms, double shiftDeg) {
        int samples = (int)((ms*sampleRate) / 1000);
        int integerFactor = (int)Math.floor((Math.pow(2,bitDepth)-1)/2);

        long[] buffer = new long[samples];

        double period = (double)sampleRate/ frequency;

        for (int i = 0; i < samples; i++){

            double angle = 2.0*Math.PI*i/period;
            buffer[i] = (int)(Math.sin(angle + Math.toRadians(shiftDeg)) * integerFactor);
        }
        return buffer;
    }

    public static void generateBuffers(double frequency, double beat, double shiftDeg){
        Binaural.rightChannel = createSinWaveBuffer(frequency, Binaural.sampleRate, Binaural.bitDepth, Binaural.sampleDurationMs, 0.0);
        Binaural.leftChannel = createSinWaveBuffer(frequency + beat, Binaural.sampleRate, Binaural.bitDepth, Binaural.sampleDurationMs, shiftDeg);
    }

    public static void play(){

    }

    public static void stop(){

    }
}
