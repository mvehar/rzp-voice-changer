/////////////////////////////////////////////////////////////////////////////
///
/// Example Android Application/Activity that allows processing WAV 
/// audio files with SoundTouch library
///
/// Copyright (c) Olli Parviainen
///
////////////////////////////////////////////////////////////////////////////////
//
// $Id: SoundTouch.java 210 2015-05-14 20:03:56Z oparviai $
//
////////////////////////////////////////////////////////////////////////////////


package com.vehar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.vehar.soundtouchandroid.R;

import java.util.ArrayList;
import java.util.List;

//App states
public class VoiceChanger extends AppCompatActivity implements OnClickListener {
    TextView textViewConsole = null;
    SeekBar pitchBar = null;
    Button buttonRecord = null;
    Button buttonProcess = null;
    Button buttonPlayOriginal = null;
    Button buttonPlayProcessed = null;


    private AudioRecording recorder = null;

    private boolean isRecording = false;
    private boolean isRecordered = false;
    private boolean isProcessing = false;
    private boolean isProcessed = false;

    String TAG = "voiceChanger";


    StringBuilder consoleText = new StringBuilder();

    /// Called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        Permissions();

        pitchBar = (SeekBar) findViewById(R.id.pitchBar);
        buttonRecord = (Button) findViewById(R.id.buttonRecord);

        buttonPlayOriginal = (Button) findViewById(R.id.buttonPlayOriginal);
        buttonPlayProcessed = (Button) findViewById(R.id.buttonPlayProcessed);


        pitchBar.setProgress(10);

        recorder = new AudioRecording();

    }


    /// Function to append status text onto "console box" on the Activity
    public void appendToConsole(final String text) {
        // run on UI thread to avoid conflicts
        runOnUiThread(new Runnable() {
            public void run() {
                consoleText.append(text);
                consoleText.append("\n");
                textViewConsole.setText(consoleText);
            }
        });
    }


    /// Button click handler
    @Override
    public void onClick(View arg0) {
        int i = arg0.getId();
        if (i == R.id.buttonRecord) {
            if (!isRecording && !isProcessing) {
                isRecording = true;
                record();
            } else {
                stopRecording();
                isRecording = false;
                isRecordered = true;
                buttonPlayOriginal.setEnabled(true);
            }


        } else if (i == R.id.buttonPlayOriginal) {
            if (isRecordered) {
                recorder.playOriginal();
            }

        } else if (i == R.id.buttonPlayProcessed) {
            if (isRecordered) {
                recorder.playProcessed();
            }
        } else if (i == R.id.buttonQR) {
            Intent intentGenQr = new Intent(this, QRCodeServer.class);
            startActivity(intentGenQr);

        }

    }


    private void stopRecording() {
        buttonRecord.setText("Record");

        recorder.stopRecording();
    }

    private void record() {
        buttonRecord.setText("Stop recording");

        recorder.startRecording(pitchBar.getProgress()-10);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    private boolean Permissions() {
        int permissionWRITE_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionRECORD = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int permissionWIFI = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        List<String> listPermissionsNeeded = new ArrayList<String>();

        if (permissionWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionRECORD != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (permissionWIFI != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
            return false;
        }

        return true;


    }


}