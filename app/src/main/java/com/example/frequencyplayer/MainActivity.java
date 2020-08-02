package com.example.frequencyplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioRecord;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // Default options
    private static final double DEFAULT_BEAT = 1.0;
    private static final double DEFAULT_SHIFT = 180.0;
    private static final double DEFAULT_FREQUENCY = Binaural.CADUCEUS_FREQUENCIES.get(196); // 49.96882653 Hz

    private static final String CUSTOM_CLIP_BASENAME = "customBinauralSound";
    private static final String CUSTOM_RIGHT_CLIP_SUFFIX = "R";
    private static final String CUSTOM_LEFT_CLIP_SUFFIX = "L";
    private static final double LOOPED_SAMPLE_DURATION_SEC = 0.6;
    private static final int MAX_STREAMS = 6;
    private static final int SAMPLE_RATE = Binaural.SAMPLE_RATE;
    private static final short BIT_DEPTH = Binaural.BIT_DEPTH;

    private static SoundPool soundPool;
    private static int customRightSoundId;
    private static int customLeftSoundId;
    private static int customRightSoundStreamId;
    private static int customLeftSoundStreamId;
    // EditTexts
    private EditText et_frequency;
    private EditText et_beat;
    private EditText et_shift;

    // Buttons
    // private Button bt_play;
    // private Button bt_stop;
    // private Button bt_default;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize audio attributes
        {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(MAX_STREAMS)
                    .setAudioAttributes(audioAttributes)
                    .build();
        }

        // Initialize buttons
        // bt_play = findViewById(R.id.bt_play);
        // bt_stop = findViewById(R.id.bt_stop);
        // bt_default = findViewById(R.id.bt_default);

        // Initialize inputs
        et_frequency = findViewById(R.id.et_frequency);
        et_beat = findViewById(R.id.et_beat);
        et_shift = findViewById(R.id.et_shift);

        customRightSoundId = 0;
        customLeftSoundId = 0;
        customRightSoundStreamId = 0;
        customLeftSoundStreamId = 0;
    }

    private static Pair<Double, Boolean> validateValue(String str){
        double value = 0.0;
        boolean isErr = false;

        try{
            value = Double.parseDouble(str);
            if (value <= 0){ isErr = true; }
        }
        catch (NumberFormatException e){ isErr = true; }

        if (isErr){
            return new Pair<Double, Boolean>(0.0, false);
        }
        return new Pair<Double, Boolean>(value, true);
    }

    public void bt_play_onClick(View view) {


        // Get the string from the EditText objects
        String frequencyStr = et_frequency.getText().toString();
        String beatStr = et_beat.getText().toString();
        String shiftStr = et_shift.getText().toString();

        // Validate all numbers parsed from text
        Pair<Double, Boolean> frequencyVal = validateValue(frequencyStr);
        Pair<Double, Boolean> beatVal = validateValue(beatStr);
        Pair<Double, Boolean> shiftVal = validateValue(shiftStr);

        // Use default values for invalid inputs
        final double frequency = frequencyVal.second ? frequencyVal.first : DEFAULT_FREQUENCY;
        final double beat = beatVal.second ? beatVal.first : DEFAULT_BEAT;
        final double shift = shiftVal.second ? shiftVal.first : DEFAULT_SHIFT;

        // Debug logging info
        Log.d("appActivity", String.format("Frequency: %.5f Beat: %.5f Shift: %.5f", frequency, beat, shift));

        // Create runnable process for multithreading
        Runnable composeAndPlay = new Runnable() {
            @Override
            public void run() {

                // Generate the wav audio buffers
                Binaural.generateBuffers(frequency, beat, shift, LOOPED_SAMPLE_DURATION_SEC);

                // Create a .wav file from the PCM data
                Context context = getApplicationContext();
                File[] wavFiles = Binaural.writeWaveFiles(CUSTOM_CLIP_BASENAME + CUSTOM_RIGHT_CLIP_SUFFIX, CUSTOM_CLIP_BASENAME + CUSTOM_LEFT_CLIP_SUFFIX, context);


                // Load the right channel .wav file into the SoundPool
                Log.d("appActivity", "Loading file \""+ wavFiles[0].getAbsolutePath() + "\".");
                customRightSoundId = soundPool.load(wavFiles[0].getAbsolutePath() ,1);

                // Delete the right channel cache file
                if (!wavFiles[0].delete()) throw new RuntimeException("Could not delete right channel cache file.");


                // Load the left channel .wav file into the SoundPool
                Log.d("appActivity", "Loading file \""+ wavFiles[1].getAbsolutePath() + "\".");
                customLeftSoundId = soundPool.load(wavFiles[1].getAbsolutePath() ,1);

                // Delete the left channel cache file
                if (!wavFiles[0].delete()) throw new RuntimeException("Could not delete left channel cache file.");


                // Play the sound in a loop
                customRightSoundStreamId = soundPool.play(customRightSoundId, 1, 1, MAX_STREAMS, -1, 1);
                customLeftSoundStreamId = soundPool.play(customLeftSoundId, 1,1,MAX_STREAMS, -1, 1);


                Log.d("appActivity", "*PLAY*");
            }
        };

        Thread thread = new Thread(composeAndPlay);
        thread.start();

    }

    private void resetDefaultValuesInEditTexts(){
        // Reset the default parameters of the EditText objects
        et_frequency.setText(String.valueOf(DEFAULT_FREQUENCY));
        et_beat.setText(String.valueOf(DEFAULT_BEAT));
        et_shift.setText(String.valueOf(DEFAULT_SHIFT));
        Log.d("appActivity", "*RESET*");
    }

    public void bt_default_onClick(View view) {
        // Stop the audio
        stopSound();
        resetDefaultValuesInEditTexts();
    }

    private static void stopSound(){
        soundPool.stop(customSoundStreamId);
        Log.d("appActivity", "*STOP*");
    }

    public void bt_stop_onClick(View view) {
        // Make the sound pool go silent
        stopSound();
        soundPool.unload(customSoundId);
    }
}