package com.example.frequencyplayer;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

// Thanks to http://www.labbookpages.co.uk/audio/javaWavFiles.html

public class Binaural {

    // Built in features
    public static final int SAMPLE_RATE = 44100;
    public static final short BIT_DEPTH = 16;
    public static final short NUM_CHANNELS = 2;
    public static final String FILE_EXTENSION = ".wav";

    private class ChannelNum{
        static final int RIGHT = 0;
        static final short LEFT = 1;
    }

    // Caduceus frequencies in Hz with integer exponents [185, 201].
    public final static Map<Integer, Double> CADUCEUS_FREQUENCIES =  new HashMap<Integer, Double>(){{
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



    // Data buffers
    private static int[][] channelsData;
    private static int channelLength = 0; // Number of samples per channel
    private static boolean isBuffersFull = false;

    // Remembered values to avoid unnecessary buffer generation
    private static double lastFrequency;
    private static double lastBeat;
    private static double lastShiftDeg;
    private static int lastNumLoops;

    // Creates an audio buffer with a single period of sine.
    private static int[] createSinWavePeriod( double frequency, double shiftDeg) {

        double shiftRad = Math.toRadians(shiftDeg);
        int numSamplesPerPeriod = (int)(SAMPLE_RATE / frequency);

        int[] buffer = new int[numSamplesPerPeriod];

        for (int i = 0; i< numSamplesPerPeriod; i++) {
            double angle = 2.0*Math.PI*i/numSamplesPerPeriod;
            buffer[i] = (short)(Math.sin(angle + shiftRad) * 32767); // 32767: [-1 1] double to short
        }
        return buffer;
    }

    // Fills internal buffers with the discrete number of period loops to approximate the duration.
    public static void generateBuffers(double frequency, double beat, double shiftDeg, double durationMs){
        generateBuffers(frequency, beat, shiftDeg, (int)(durationMs*1000/frequency));
    }

    // Fills internal buffers with a single period of frequency repeated n times.
    public static void generateBuffers(double frequency, double beat, double shiftDeg, int numLoops){
        // Return early if the buffers are already full with the exact same parameters
        if (isBuffersFull &&
                frequency == lastFrequency &&
                beat == lastBeat &&
                shiftDeg == lastShiftDeg &&
                numLoops == lastNumLoops){
            Log.d("binaural", "Buffers already generated with the same parameters.");
            return;
        }

        if (numLoops <= 0){
            throw new IllegalArgumentException("Number of loops must me positive.");
        }

        // Clear the current data buffers
        clearBuffers();

        // Create buffers for the raw data of a single period
        int[] rightOnePeriod = createSinWavePeriod(frequency, 0.0);
        int[] leftOnePeriod = createSinWavePeriod(frequency + beat, shiftDeg);

        // Repeat the period the needed number of times to fill the total sample size, cutting
        // the excess of the the longest one to make both match
        channelLength =  numLoops * Math.min(rightOnePeriod.length, leftOnePeriod.length);
        channelsData = new int[NUM_CHANNELS][channelLength];
        channelsData[ChannelNum.RIGHT] = Util.concatTillLength(rightOnePeriod, channelLength);
        channelsData[ChannelNum.LEFT] = Util.concatTillLength(leftOnePeriod, channelLength);

        // Set the remembered values to the provided ones
        isBuffersFull = true;
        lastFrequency = frequency;
        lastBeat = beat;
        lastShiftDeg = shiftDeg;
        lastNumLoops = numLoops;
    }

    public static void clearBuffers(){
        channelsData = null;
        channelLength = 0;
        isBuffersFull = false;
    }


    // Writes the wav file into cache and returns the absolute path.
    public static File writeWaveFile(String baseName, Context context){
        if (!isBuffersFull){
            throw new IllegalStateException("Audio data buffers must be generated first.");
        }

        try{
            File outputFile = File.createTempFile(baseName, FILE_EXTENSION, context.getCacheDir());
            WavFile wavFile = WavFile.newWavFile(outputFile, NUM_CHANNELS, channelLength, BIT_DEPTH, SAMPLE_RATE);
            wavFile.writeFrames(channelsData, 0, channelLength);
            wavFile.close();
            Log.d("binarual", String.format("Created cache file \"%s%s\".", baseName, FILE_EXTENSION));
            return outputFile;
        }catch (IOException e){
            e.printStackTrace();
            Log.d("binarual", String.format("Error creating cache \"file %s%s\".", baseName, FILE_EXTENSION));
        }catch (WavFileException e){
            e.printStackTrace();
            Log.d("binarual", "WavFile writing error.");
        }
        return null;
    }
}
