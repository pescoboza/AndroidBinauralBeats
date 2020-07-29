package com.example.frequencyplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private static final String CUSTOM_CLIP_NAME = "custom.wav";
    private static final int MAX_STREAMS = 6;
    private static final int SAMPLE_RATE = 16 * 1024;
    private static final int BIT_DEPTH = 16;

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

        // Initialize audio attributes
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        // Create a sound pool
        soundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build();

        AudioFormat = new AudioFormat().Builder().

        AudioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                .setAudioFormat(AudioFormat.E)

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

        String frequencyStr = et_frequency.getText().toString();
        String beatStr = et_beat.getText().toString();
        String shiftStr = et_shift.getText().toString();

        // Validate all numbers parsed from text
        Pair<Double, Boolean> frequencyVal = validateValue(frequencyStr);
        double frequency =  frequencyVal.second ? frequencyVal.first: Binaural.DEFAULT_FREQUENCY;

        Pair<Double, Boolean> beatVal = validateValue(beatStr);
        double beat = beatVal.second ? beatVal.first : Binaural.DEFAULT_BEAT;

        Pair<Double, Boolean> shiftVal = validateValue(shiftStr);
        double shift = shiftVal.second ? shiftVal.first : Binaural.DEFAULT_SHIFT;

        Log.d("inputDebugger",String.format("Frequency: %.5f Beat: %.5f Shift: %.5f", frequency, beat, shift));



        // Generate the raw data buffers for the audio samples
        // Binaural.generateBuffers(frequency, beat, shift);
        // TODO: Add functionality to the sound pool.




        // soundPool.doSomething();

    }

    public void bt_default_onClick(View view) {


    }

    public void bt_stop_onClick(View view) {
    }
}