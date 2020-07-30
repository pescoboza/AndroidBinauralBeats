package com.example.frequencyplayer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

// Thanks to http://www.topherlee.com/software/pcm-tut-wavformat.html
// and http://soundfile.sapp.org/doc/WaveFormat/

public class Binaural {

    // Built in features
    public static final int SAMPLE_RATE = 44100;
    public static final short BIT_DEPTH = 16;
    public static final short NUM_CHANNELS = 2;

    // Caduceus frequencies in Hz with integer exponents [185, 201].
    private final static Map<Integer, Double> CADUCEUS_FREQUENCIES =  new HashMap<Integer, Double>(){{
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

    // Default options
    private static final double DEFAULT_BEAT = 1.0;
    private static final double DEFAULT_SHIFT = 180.0;
    private static final double DEFAULT_FREQUENCY = 49.96882653; // CADUCEUS_FREQUENCIES.get(196);
    private static int sampleDurationMs = 1000;

    // Data buffers
    private static short[] rightChannel;
    private static short[] leftChannel;
    private static int numSamples = 0;
    private static boolean isBuffersFull = false;

    public static void setSampleDurationMs(int ms){
        Binaural.sampleDurationMs = ms;
    }


    private static short[] createSinWaveBuffer(double frequency, double shiftDeg) {
        double shiftRad = Math.toRadians(shiftDeg);
        int numSamplesPerPeriod = (int)(SAMPLE_RATE / frequency);

        short[] buffer = new short[numSamplesPerPeriod];

        for (int i = 0; i< numSamplesPerPeriod; i++) {
            double angle = 2.0*Math.PI*i/numSamplesPerPeriod;
            buffer[i] = (short)(Math.sin(angle + shiftRad) * 32767); // 32767: [-1 1] double to short
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

        short[] right = createSinWaveBuffer(frequency,0.0);
        short[] left = createSinWaveBuffer(frequency + beat, shiftDeg);

        int numSamplesPerPeriod = right.length;

        if (numLoops > 1){
            int channelLen = numSamplesPerPeriod*numLoops;

            rightChannel = new short[channelLen];
            leftChannel = new short[channelLen];

            for (int i = 0; i < channelLen; i++) {
                int destPos = i*channelLen;
                System.arraycopy(right, 0, rightChannel, destPos, numSamplesPerPeriod);
            }

            numSamples = channelLen;

        }else {
            rightChannel = right;
            leftChannel = left;

            numSamples = numSamplesPerPeriod;
        }

        isBuffersFull = true;
    }

    private static ByteBuffer generateWavBuffer(ByteBuffer dataSrc, int numData){

        // Validate numData
        if (numData <= 0){
            throw new IllegalArgumentException("numData must be positive.");
        }

        // Get the file length in bytes
        final int LENGTH_OF_FORMAT_DATA = 16;
        final int HEADER_LENGTH_BYTES = 44;
        int dataLengthBytes = numData*BIT_DEPTH/8;
        int fileLength = HEADER_LENGTH_BYTES + dataLengthBytes;

        // Preallocate for the length of the file
        ByteBuffer wavBuff = ByteBuffer.allocate(HEADER_LENGTH_BYTES + dataLengthBytes);

        // Create WAV file header

        wavBuff.put("RIFF".getBytes(), 0 ,4);            //  1 -  4 big: "RIFF"
        wavBuff.putInt(fileLength);                                    //  5 -  8 lil: Size of the overall file - 8 bytes, in bytes (32-bit integer)
        wavBuff.put("WAVE".getBytes(), 0, 4);            //  9 - 12 big: File type header: "WAVE"
        wavBuff.put("fmt\0".getBytes(),0,4);             // 13 - 16 big: Format chunk marker: "fmt\0" (with trailing null)
        wavBuff.putInt(LENGTH_OF_FORMAT_DATA);                         // 17 - 20 lil: Length of fmt chunk 1, always 16 (32-bit integer)
        wavBuff.putShort((short)1);                                    // 21 - 22 lil: Audio format (1 is PCM) (16-bit integer)
        wavBuff.putShort((short)NUM_CHANNELS);                          // 23 - 24 lil: Number of channels (16-bit integer)
        wavBuff.putInt(SAMPLE_RATE);                                    // 25 - 28 lil: Sample rate (32-bit integer)
        wavBuff.putInt((int)(SAMPLE_RATE*BIT_DEPTH*NUM_CHANNELS/8)); // 29 - 32 lil: Byte rate: (sampleRate*bitsPerSample*numChannels)/8 (32-bit integer)
        wavBuff.putShort((short)(BIT_DEPTH*NUM_CHANNELS));          // 33 - 34 lil: Block align: (bitsPerSample*numChannels)/8 (16-bit integer)
        wavBuff.putShort((short)BIT_DEPTH);                        // 35 - 36 lil: Bits per sample
        wavBuff.put("data".getBytes(), 0, 4);            // 37 - 40 big: "data" chunk header
        wavBuff.putInt(dataLengthBytes);                               // 41 - 44 lil: File size (data)


        // Append the byte data to the wav header
        byte[] temp = new byte[dataLengthBytes];
        dataSrc.get(temp);
        wavBuff.put(temp);

        return wavBuff;
    }

    private static ByteBuffer condenseBuffers(){
        ByteBuffer buff = ByteBuffer.allocate(numSamples*NUM_CHANNELS*BIT_DEPTH/8);


        for (int i = 0; i < numSamples; i++){
            short rightSample = (short)rightChannel[i];
            short leftSample = (short)leftChannel[i];
        }
    }

    public static void clearBuffers(){
        rightChannel = null;
        leftChannel = null;
        numSamples = 0;
        isBuffersFull = false;
    }


    // ByteBuffer dataSrc, int numData, short numChannels, int sampleRate, int bitsPerSample
    public static int writeWaveFile(String fileName, ){
        if (!isBuffersFull){
            throw new IllegalStateException("PCM buffers must be generated first.");
        }



        ByteBuffer wavBuffer = generateWavBuffer();

        for (int i = 0; i < numSamples; i++) {
            // TODO: Write code to generate .wav file.
        }



    }

}
