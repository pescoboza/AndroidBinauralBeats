package com.example.frequencyplayer;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Thanks to http://www.labbookpages.co.uk/audio/javaWavFiles.html

public class Binaural {

    // Built in features
    public static final int SAMPLE_RATE = 44100;
    public static final short BIT_DEPTH = 16;
    public static final short NUM_CHANNELS = 2;
    public static final String FILE_EXTENSION = ".wav";

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
    private static int[] rightChannel;
    private static int[] leftChannel;
    private static boolean isBuffersFull = false;

    // Remembered values to avoid unnecessary buffer generation
    private static double lastFrequency;
    private static double lastBeat;
    private static double lastShiftDeg;
    private static double lastPeriodDuration;

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

    // Fills internal buffers with a single period of frequency repeated n times.
    public static void generateBuffers(double frequency, double beat, double shiftDeg, double durationSec){

        if (durationSec <= 0) throw new IllegalArgumentException("Duration must me positive.");

        // Return early if the buffers are already full with the exact same parameters
        if (isBuffersFull &&
                frequency == lastFrequency &&
                beat == lastBeat &&
                shiftDeg == lastShiftDeg &&
                durationSec == lastPeriodDuration){
            Log.d("binaural", "Buffers already generated with the same parameters.");
            return;
        }

        // Clear the current data buffers
        clearBuffers();

        // Create buffers for the raw data of a single period
        int[] rightSinglePeriod = createSinWavePeriod(frequency, 0.0);
        int[] leftSinglePeriod = createSinWavePeriod(frequency + beat, shiftDeg);

        // Number of samples to match the duration desired
        int numSamplesToMatchDuration =  (int)(durationSec*SAMPLE_RATE);

        // Number of times each channel will be concatenated at the end of itself to match the
        // desired duration while keeping perfect loopability
        int numConcatsRight = numSamplesToMatchDuration/rightSinglePeriod.length;
        int numConcatsLeft = numSamplesToMatchDuration/leftSinglePeriod.length;

        // Splice the periods to match the desired channel length
        rightChannel = Util.concatNTimes(rightSinglePeriod, numConcatsRight);
        leftChannel = Util.concatNTimes(leftSinglePeriod, numConcatsLeft);

        // Set the remembered values to the provided ones
        isBuffersFull = true;
        lastFrequency = frequency;
        lastBeat = beat;
        lastShiftDeg = shiftDeg;
        lastPeriodDuration = durationSec;
    }

    public static void clearBuffers(){
        rightChannel = null;
        leftChannel = null;
        isBuffersFull = false;
    }


    // Writes the wav file into cache and returns the absolute path.
    public static File[] writeWaveFiles(String rightBaseName, String leftBaseName, Context context){
        if (!isBuffersFull){
            throw new IllegalStateException("Audio data buffers must be generated first.");
        }

        File[] files = new File[NUM_CHANNELS];

        // Right channel file
        try {
            // Create right channel file
            File outputFileRight = File.createTempFile(rightBaseName, FILE_EXTENSION, context.getCacheDir());
            WavFile wavFileRightChannel = WavFile.newWavFile(outputFileRight, NUM_CHANNELS, rightChannel.length, BIT_DEPTH, SAMPLE_RATE);

            // Write right channel
            for (int value : rightChannel) {

                int[][] temp = new int[NUM_CHANNELS][1];
                temp[0][0] = value;

                wavFileRightChannel.writeFrames(temp, 1);
                Log.d("binarual", String.format("Created cache file \"%s%s\".",  rightBaseName, FILE_EXTENSION));
            }
            wavFileRightChannel.close();

            files[0] = outputFileRight;

        }catch(IOException e){
            e.printStackTrace();
            Log.d("binarual", String.format("Error creating cache \"file %s%s\".", rightBaseName, FILE_EXTENSION));
        }catch (WavFileException e){
            e.printStackTrace();
            Log.d("binarual", "WavFile writing error.");
        }

        // Left channel file
        try{
            // Create left channel file
            File outputFileLeft = File.createTempFile(leftBaseName, FILE_EXTENSION, context.getCacheDir());
            WavFile wavFileLeftChannel = WavFile.newWavFile(outputFileLeft  , NUM_CHANNELS, rightChannel.length, BIT_DEPTH, SAMPLE_RATE);

            // Write left channel
            for (int value : leftChannel){

                int[][] temp = new int[NUM_CHANNELS][1];
                temp[1][0] = value; // 1 offset for left channel

                wavFileLeftChannel.writeFrames(temp, 1);
            }
            wavFileLeftChannel.close();

            files[1] = outputFileLeft;

            Log.d("binarual", String.format("Created cache file \"%s%s\".",  leftBaseName, FILE_EXTENSION));
        }catch (IOException e){
            e.printStackTrace();
            Log.d("binarual", String.format("Error creating cache \"file %s%s\".", leftBaseName, FILE_EXTENSION));
        }catch (WavFileException e){
            e.printStackTrace();
            Log.d("binarual", "WavFile writing error.");
        }

        return files;
    }
}
