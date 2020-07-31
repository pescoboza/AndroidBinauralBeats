package com.example.frequencyplayer;

import android.content.Context;
import android.os.FileUtils;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

// Thanks to http://www.topherlee.com/software/pcm-tut-wavformat.html
// and http://soundfile.sapp.org/doc/WaveFormat/

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
    private static short[] rightChannel;
    private static short[] leftChannel;
    private static int channelLength = 0; // Number of samples per channel
    private static boolean isBuffersFull = false;

    // Remembered values to avoid unnecessary buffer generation
    private static double lastFrequency;
    private static double lastBeat;
    private static double lastShiftDeg;
    private static int lastNumLoops;

    // Creates an audio buffer with a single period of sine.
    private static short[] createSinWavePeriod( double frequency, double shiftDeg) {

        double shiftRad = Math.toRadians(shiftDeg);
        int numSamplesPerPeriod = (int)(SAMPLE_RATE / frequency);

        short[] buffer = new short[numSamplesPerPeriod];

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
        short[] rightOnePeriod = createSinWavePeriod(frequency, 0.0);
        short[] leftOnePeriod = createSinWavePeriod(frequency + beat, shiftDeg);

        // Repeat the period the needed number of times to fill the total sample size, cutting
        // the excess of the the longest one to make both match
        channelLength =  numLoops * Math.min(rightOnePeriod.length, leftOnePeriod.length);
        rightChannel = Util.concatTillLength(rightOnePeriod, channelLength);
        leftChannel = Util.concatTillLength(leftOnePeriod, channelLength);

        // Set the remembered values to the provided ones
        isBuffersFull = true;
        lastFrequency = frequency;
        lastBeat = beat;
        lastShiftDeg = shiftDeg;
        lastNumLoops = numLoops;
    }

    private static ByteBuffer condenseBuffers(){
        if (!isBuffersFull){
            throw new IllegalStateException("Audio data buffers must be filled first.");
        }

        ByteBuffer buff = ByteBuffer.allocate(numSamples*NUM_CHANNELS*BIT_DEPTH/8);
        for (int i = 0; i < numSamples; i++){
            buff.putShort(rightChannel[i]);
            buff.putShort(leftChannel[i]);
        }
        return buff;
    }

    public static void clearBuffers(){
        rightChannel = null;
        leftChannel = null;
        numSamples = 0;
        isBuffersFull = false;
    }


    private static ByteBuffer generateWavBuffer(){

        // Get the file length in bytes
        final int LENGTH_OF_FORMAT_DATA = 16;
        final int HEADER_LENGTH_BYTES = 44;
        int dataLengthBytes = numSamples*NUM_CHANNELS*BIT_DEPTH/8;
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
        wavBuff.putShort((short)NUM_CHANNELS);                         // 23 - 24 lil: Number of channels (16-bit integer)
        wavBuff.putInt(SAMPLE_RATE);                                   // 25 - 28 lil: Sample rate (32-bit integer)
        wavBuff.putInt((int)(SAMPLE_RATE*BIT_DEPTH*NUM_CHANNELS/8));   // 29 - 32 lil: Byte rate: (sampleRate*bitsPerSample*numChannels)/8 (32-bit integer)
        wavBuff.putShort((short)(BIT_DEPTH*NUM_CHANNELS));             // 33 - 34 lil: Block align: (bitsPerSample*numChannels)/8 (16-bit integer)
        wavBuff.putShort((short)BIT_DEPTH);                            // 35 - 36 lil: Bits per sample
        wavBuff.put("data".getBytes(), 0, 4);            // 37 - 40 big: "data" chunk header
        wavBuff.putInt(dataLengthBytes);                               // 41 - 44 lil: File size (data)


        // Append the byte data to the wav header
        {
            byte[] temp = new byte[dataLengthBytes];
            ByteBuffer condensedChannels = condenseBuffers();
            condensedChannels.position(0);
            condensedChannels.get(temp);
            wavBuff.put(temp);
        }
        return wavBuff;
    }


    // Writes the wav file into cache and returns the absolute path.
    public static File writeWaveFile(String baseName, Context context){
        if (!isBuffersFull){
            throw new IllegalStateException("Audio data buffers must be generated first.");
        }

        ByteBuffer dataBuffer = generateWavBuffer();
        File outputFile = null;
        try{
            outputFile = File.createTempFile(baseName, FILE_EXTENSION, context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(outputFile.getAbsolutePath());
            fos.write(dataBuffer.array());
            fos.close();
            Log.d("binarual", String.format("Created cache file \"%s%s\".", baseName, FILE_EXTENSION));
        }catch (IOException e){
            e.printStackTrace();
            Log.d("binarual", String.format("Error creating cache \"file %s%s\".", baseName, FILE_EXTENSION));
        }

        return outputFile;
    }
}
