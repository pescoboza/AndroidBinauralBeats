package com.example.frequencyplayer;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;
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
    private static int bufferLength = 0;

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

            bufferLength = channelLen;

        }else {
            rightChannel = right;
            leftChannel = left;

            bufferLength = numSamplesPerPeriod;
        }

        isBuffersFull = true;
    }

    public static void clearBuffers(){
        rightChannel = null;
        leftChannel = null;
        bufferLength = 0;
        isBuffersFull = false;
    }

    private static ByteBuffer generateWavHeader(){
        int dataLength = 0;
        short numChannels = (short)2;
        int SAMPLE_RATE = 0;
        int bitsPerSample = 0;

        // TODO: Finish wav header generator. http://www.topherlee.com/software/pcm-tut-wavformat.html

        ByteBuffer buff = ByteBuffer.allocate(44);
        buff.put("RIFF".getBytes(), 0 ,4);            //  1 -  4 : "RIFF"
        buff.putInt(0);                                             //  5 -  8 : Size of the overall file - 8 bytes, in bytes (32-bit integer)
        buff.put("WAVE".getBytes(), 0, 4);            //  9 - 12 : File type header: "WAVE"
        buff.put("fmt\0".getBytes(),0,4);             // 13 - 16 : Format chunk marker: "fmt\0" (with trailing null)
        buff.putInt(dataLength);                                    // 17 - 20 : Length of format data as listed above (32-bit integer)
        buff.putShort((short)1);                                    // 21 - 22 : Type of format (1 is PCM) (2 byte integer)
        buff.putShort(numChannels);                                 // 23 - 24 : Number of channels (2 byte integer)
        buff.putInt(SAMPLE_RATE);                                   // 25 - 28 : Sample rate (32-bit integer)
        buff.putInt((int)(SAMPLE_RATE*bitsPerSample*numChannels/8));// 29 - 32 : (sampleRate*bitsPerSample*numChannels)/8 (32-bit integer)
        buff.putInt((int)bitsPerSample*numChannels);                // 33 - 34 : (bitsPerSample*numChannels)/8 TODO: figure this one out.
        buff.putShort((short)bitsPerSample);                        // 35 - 36 : Bits per sample
        buff.put("data".getBytes(), 0, 4);            // 37 - 40 : "data" chunk header
        buff.putInt(dataLength);                                    // 41 - 44 : File size (data)

        return buff;






    }

    public static int writeWaveFile(String fileName){
        if (!isBuffersFull){
            return -1;
        }


        int bufferSize = rightChannel.length;







    }

}
