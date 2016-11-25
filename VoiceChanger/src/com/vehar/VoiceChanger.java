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


package net.surina;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;

import net.surina.soundtouch.SoundTouch;
import net.surina.soundtouchexample.R;

import java.io.File;
import java.io.IOException;
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


    private String TEMP_FILE_IN = null;
    private String TEMP_FILE_OUT = null;

    String TAG = "voiceChanger";


    StringBuilder consoleText = new StringBuilder();

    /// Called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        Permissions();

        textViewConsole = (TextView) findViewById(R.id.textViewResult);
        pitchBar = (SeekBar) findViewById(R.id.pitchBar);
        buttonRecord = (Button) findViewById(R.id.buttonRecord);

        buttonProcess = (Button) findViewById(R.id.buttonProcess);
        buttonPlayOriginal = (Button) findViewById(R.id.buttonPlayOriginal);
        buttonPlayProcessed = (Button) findViewById(R.id.buttonPlayProcessed);

        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        File tempDir = new File(dir, "VoiceChanger");
        tempDir.mkdirs();

        TEMP_FILE_OUT = tempDir + File.separator + "processed.wav";
        TEMP_FILE_IN = tempDir + File.separator + "recorded.wav";

        pitchBar.setProgress(20);

        recorder = new AudioRecording(TEMP_FILE_IN);


        //Button buttonRecord = (Button)findViewById(R.id.buttonSelectSrcFile);
        //Button buttonFileOutput = (Button)findViewById(R.id.buttonSelectOutFile);
        //Button buttonProcess = (Button)findViewById(R.id.buttonProcess);

        // Check soundtouch library presence & version
        checkLibVersion();

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


    /// print SoundTouch native library version onto console
    protected void checkLibVersion() {
        String ver = SoundTouch.getVersionString();
        appendToConsole("SoundTouch native library version = " + ver);
    }


    /// Button click handler
    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.buttonRecord:
                if (!isRecording && !isProcessing) {
                    isRecording = true;
                    record();
                } else {
                    stopRecording();
                    isRecording = false;
                    isRecordered = true;
                    buttonPlayOriginal.setEnabled(true);
                    buttonProcess.setEnabled(true);
                }

                break;
            case R.id.buttonPlayOriginal:
                if (isRecordered && !isProcessing) {
                    playWavFile(TEMP_FILE_IN);
                }
                break;

            case R.id.buttonPlayProcessed:
                if (isProcessed) {
                    playWavFile(TEMP_FILE_OUT);
                }
                break;

            case R.id.buttonProcess:
                if (isRecordered) {
                    isProcessing = true;
                    process();
                    isProcessing = false;
                    isProcessed = true;

                }
                break;

            case R.id.buttonQR:
                Intent intentGenQr = new Intent(this, QRCodeServer.class);
                startActivity(intentGenQr);
                break;

            case R.id.buttonReadQR:
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt("Scan a barcode");
                integrator.setBeepEnabled(false);
                integrator.initiateScan();
                integrator.setOrientationLocked(true);
                break;
        }

    }


    private void stopRecording() {
        Toast.makeText(this, "Stopped! " + TEMP_FILE_IN, Toast.LENGTH_SHORT).show();
        buttonRecord.setText("Record");

        recorder.stopRecording();
    }

    private void record() {
        Toast.makeText(this, "Recording.... " + TEMP_FILE_IN, Toast.LENGTH_SHORT).show();
        buttonRecord.setText("Stop recording");

        recorder.startRecording();

    }


    /// Play audio file
    protected void playWavFile(String fileName) {
        /*
        File file2play = new File(fileName);
        Intent i = new Intent();
        i.setAction(android.content.Intent.ACTION_VIEW);
        i.setDataAndType(Uri.fromFile(file2play), "audio/*");
        startActivity(i);
*/
        MediaPlayer mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            System.out.println("Jeba cela");
        }
    }


    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }


    /// Helper class that will execute the SoundTouch processing. As the processing may take
    /// some time, run it in background thread to avoid hanging of the UI.
    protected class ProcessTask extends AsyncTask<ProcessTask.Parameters, Integer, Long> {
        /// Helper class to store the SoundTouch file processing parameters
        public final class Parameters {
            String inFileName;
            String outFileName;
            float tempo;
            float pitch;
        }


        /// Function that does the SoundTouch processing
        public final long doSoundTouchProcessing(Parameters params) {

            SoundTouch st = new SoundTouch();
            st.setTempo(params.tempo);
            st.setPitchSemiTones(params.pitch);
            st.setSampleRate(AudioRecording.RECORDER_SAMPLERATE);
            st.setChannels(2);

            Log.i("SoundTouch", "process file " + params.inFileName);
            long startTime = System.currentTimeMillis();
            int res = st.processFile(params.inFileName, params.outFileName);
            long endTime = System.currentTimeMillis();
            float duration = (endTime - startTime) * 0.001f;

            Log.i("SoundTouch", "process file done, duration = " + duration);
            appendToConsole("Processing done, duration " + duration + " sec.");
            if (res != 0) {
                String err = SoundTouch.getErrorString();
                appendToConsole("Failure: " + err);
                return -1L;
            }

			/*
            // Play file if so is desirable
			if (checkBoxPlay.isChecked())
			{
				playWavFile(params.outFileName);
			}
			*/
            return 0L;
        }


        /// Overloaded function that get called by the system to perform the background processing
        @Override
        protected Long doInBackground(Parameters... aparams) {
            return doSoundTouchProcessing(aparams[0]);
        }

    }


    /// process a file with SoundTouch. Do the processing using a background processing
    /// task to avoid hanging of the UI
    protected void process() {
        try {
            ProcessTask task = new ProcessTask();
            ProcessTask.Parameters params = task.new Parameters();


            // parse processing parameters
            params.inFileName = TEMP_FILE_IN;
            params.outFileName = TEMP_FILE_OUT;
            params.tempo = 0.01f * 100;
            params.pitch = pitchBar.getProgress() - 10;

            // update UI about status
            appendToConsole("Process audio file :" + params.inFileName + " => " + params.outFileName);
            appendToConsole("Tempo = " + params.tempo);
            appendToConsole("Pitch adjust = " + params.pitch);

            Toast.makeText(this, "Starting to process file " + params.inFileName + "...", Toast.LENGTH_SHORT).show();

            // start SoundTouch processing in a background thread
            task.execute(params);
//			task.doSoundTouchProcessing(params);	// this would run processing in main thread
            isProcessing = false;
            buttonPlayProcessed.setEnabled(true);

        } catch (Exception exp) {
            exp.printStackTrace();
        }

    }

    private boolean Permissions() {
        int permissionWRITE_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionRECORD = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        List<String> listPermissionsNeeded = new ArrayList<String>();

        if (permissionWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionRECORD != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
            return false;
        }

        return true;


    }


}