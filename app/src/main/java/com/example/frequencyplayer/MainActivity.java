package com.example.frequencyplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
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

    private static final int LOADING_DELAY_MS = 30;

    private static final String CUSTOM_CLIP_BASENAME = "customBinauralSound";
    private static final String CUSTOM_RIGHT_CLIP_SUFFIX = "R_";
    private static final String CUSTOM_LEFT_CLIP_SUFFIX = "L_";
    private static final double LOOPED_SAMPLE_DURATION_SEC = 1;
    private static final int MAX_STREAMS = 6;

    private static SoundPool soundPool;
    private static boolean isRightSoundReady;
    private static boolean isLeftSoundReady;
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

        // Set the on load listener to notify when the sounds are ready
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (status != 0) {
                    throw new IllegalStateException(String.format("Failed to load sound with id %d.", sampleId));
                }

                if (sampleId == customRightSoundId) {
                    isRightSoundReady = true;
                } else if (sampleId == customLeftSoundId) {
                    isLeftSoundReady = true;
                }
            }
        });

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

        isRightSoundReady = false;
        isLeftSoundReady = false;
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

        // Set the audio channels from the sound pool to not ready
        isLeftSoundReady = false;
        isRightSoundReady = false;

        // Create runnable process for multithreading
        Runnable composeAndPlay = new Runnable() {
            @Override
            public void run() {

                // Generate the wav audio buffers
                Binaural.generateBuffers(frequency, beat, shift, LOOPED_SAMPLE_DURATION_SEC);

                // Create new .wav files from the PCM data
                File[] wavFiles = Binaural.writeWaveFiles(CUSTOM_CLIP_BASENAME + CUSTOM_RIGHT_CLIP_SUFFIX, CUSTOM_CLIP_BASENAME + CUSTOM_LEFT_CLIP_SUFFIX, getApplicationContext());

                // Stop the previous sound
                stopAndUnloadsound();

                // Load cache file into sound pool
                Log.d("appActivity", "Loading file \""+ wavFiles[0].getAbsolutePath() + "\".");
                customRightSoundId = soundPool.load(wavFiles[0].getAbsolutePath() ,1);

                Log.d("appActivity", "Loading file \""+ wavFiles[1].getAbsolutePath() + "\".");
                customLeftSoundId = soundPool.load(wavFiles[1].getAbsolutePath() ,1);

                // Wait for the sounds to load
                {
                    int sleptFor = 0;
                    while (!(isRightSoundReady && isLeftSoundReady)) {
                        try {
                            Thread.sleep(LOADING_DELAY_MS);

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        sleptFor += LOADING_DELAY_MS;
                    }
                    Log.d("appActivity", String.format("Slept for %d ms.", sleptFor));
                }

                // Delete the cache files
                for (File wavFile : wavFiles){
                    if (!wavFile.delete()){
                        throw new IllegalStateException(
                                String.format("Failed to deleted cache file \"%s\".", wavFile.getAbsolutePath()));
                    }
                }

                // Play the sound in a loop
                customRightSoundStreamId = soundPool.play(customRightSoundId, 1, 1, MAX_STREAMS, -1, 1);
                customLeftSoundStreamId = soundPool.play(customLeftSoundId, 1, 1, MAX_STREAMS, -1, 1);

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
        stopAndUnloadsound();
        resetDefaultValuesInEditTexts();
    }

    private static void stopAndUnloadsound(){
        // Stop
        soundPool.stop(customRightSoundStreamId);
        soundPool.stop(customLeftSoundStreamId);

        // Unload
        soundPool.unload(customRightSoundId);
        soundPool.unload(customLeftSoundId);


        isRightSoundReady = false;
        isLeftSoundReady = false;

        Log.d("appActivity", "*STOP*");
    }

    public void bt_stop_onClick(View view) {
        stopAndUnloadsound();
    }

}