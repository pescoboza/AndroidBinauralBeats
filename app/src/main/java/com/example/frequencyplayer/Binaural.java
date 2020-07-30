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

    private static byte[] generateWavHeader(){

        ByteBuffer buff = ByteBuffer.allocate(44);
        buff.put("RIFF".getBytes(), 0 ,4);    //  1 - 4 : "RIFF"
        buff.putInt(0);                                     //  5 - 8 : Size of the overall file - 8 bytes, in bytes (32-bit integer)
        buff.put("WAVE".getBytes(), 0, 4);    //  9 - 12 : File type header: "WAVE"
        buff.put("fmt\0".getBytes(),0,4);     // 13 - 16 : Format chunk marker: "fmt\0"
        buff.put("\0\0\0\0".getBytes(),0,4);  // 17 - 20 : Length of format data as listed above
        buff.putShort((short)1);                            // 21 - 22 : Type of format (1 is PCM) - 2 byte integer
        buff.putShort((short)2);                            // 23 - 23 : Number of channels - 2 byte integer



        byte[] h = new byte[44];

        // 0 - 3 : "RIFF"
        h[0] = 'R';
        h[1] = 'I';
        h[2] = 'F';
        h[3] = 'F';

        // 4 - 7: file size
        h[4] = ' ';
        h[5] = ' ';
        h[6] = ' ';
        h[7] = ' ';

        // 8 - 11 : "WAVE"
        h[8] = 'W';
        h[9] = 'A';
        h[10] = 'V';
        h[11] = 'E';

        // 12 - 15 : "fmt "
        h[12] = 'f';
        h[13] = 'm';
        h[14] = 't';
        h[15] = '\0';

        // 16 - 19 : length of format
        h[16] = ' ';
        h[17] = ' ';
        h[18] = ' ';
        h[19] = ' ';

        // 20 - 21 : type of format (1 is PCM) - 2 byte integer
        h[20] = ' ';
        h[21] = ' ';

        // 22 - 23 : number of channels - 2 byte integer
        h[22] = ' ';
        h[23] = ' ';

        // 24 - 27 : sample rate - 32 byte integer
        h[24] = ' ';
        h[25] = ' ';
        h[26] = ' ';
        h[27] = ' ';

        // 28 - 31 : sampleRate * bitsPerSample * channels / 8
        h[28] = ' ';
        h[29] = ' ';
        h[30] = ' ';
        h[31] = ' ';

        // 32 -33
        // TODO: Finish wav header generator. http://www.topherlee.com/software/pcm-tut-wavformat.html



        return h;
    }

    public static int writeWaveFile(String fileName){
        if (!isBuffersFull){
            return -1;
        }


        int bufferSize = rightChannel.length;







    }

}
