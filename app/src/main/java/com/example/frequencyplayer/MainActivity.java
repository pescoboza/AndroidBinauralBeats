package com.example.frequencyplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    // Default options
    private static final double DEFAULT_BEAT = 1.0;
    private static final double DEFAULT_SHIFT = 180.0;
    private static final double DEFAULT_FREQUENCY = Binaural.CADUCEUS_FREQUENCIES.get(196); // 49.96882653 Hz

    private static final String CUSTOM_CLIP_NAME = "custom";
    private static final double LOOPED_SAMPLE_DURATION = 1.61803;
    private static final int MAX_STREAMS = 6;
    private static final int SAMPLE_RATE = Binaural.SAMPLE_RATE;
    private static final short BIT_DEPTH = Binaural.BIT_DEPTH;

    private static SoundPool soundPool;
    private static AudioRecord audioRecord;

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



        // Create a sound pool, check for SDK API version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

            // Initialize audio attributes
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(MAX_STREAMS)
                    .setAudioAttributes(audioAttributes)
                    .build();
        }else{
            soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        }



        // Initialize buttons
        // bt_play = findViewById(R.id.bt_play);
        // bt_stop = findViewById(R.id.bt_stop);
        // bt_default = findViewById(R.id.bt_default);

        // Initialize inputs
        et_frequency = findViewById(R.id.et_frequency);
        et_beat = findViewById(R.id.et_beat);
        et_shift = findViewById(R.id.et_shift);


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
        double frequency =  frequencyVal.second ? frequencyVal.first: DEFAULT_FREQUENCY;
        double beat = beatVal.second ? beatVal.first : DEFAULT_BEAT;
        double shift = shiftVal.second ? shiftVal.first : DEFAULT_SHIFT;

        // Debug logging info
        Log.d("inputDebugger",String.format("Frequency: %.5f Beat: %.5f Shift: %.5f", frequency, beat, shift));

        // Generate the wav audio buffers
        Binaural.generateBuffers(frequency, beat, shift, LOOPED_SAMPLE_DURATION);

        // Create a .wav file from the PCM data
        Binaural.writeWaveFile(CUSTOM_CLIP_NAME + ".wav");

        // Load the .wav file into the SoundPool
        soundPool.load(CUSTOM_CLIP_NAME, MAX_STREAMS);

        // Set the sound to looping
        soundPool.setLoop(MAX_STREAMS, -1);

        soundPool.pl;

    }

    public void bt_default_onClick(View view) {


    }

    public void bt_stop_onClick(View view) {
    }
}