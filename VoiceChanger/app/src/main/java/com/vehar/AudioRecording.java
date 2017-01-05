package com.vehar;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matejv on 22.11.2016.
 */

public class AudioRecording {
    static final int RECORDER_BPP = 16;
    static final int RECORDER_SAMPLERATE = 44100;
    static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private AudioTrack track = null;

    private SoundTouch st = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private List<Byte> SOUNDS = new ArrayList<>();
    private List<Byte> PROC_SOUNDS = new ArrayList<>();


    int BufferElements2Rec = 4096; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    int bufferSize = 0;


    public AudioRecording() {
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement, AudioTrack.MODE_STREAM);

        st = new SoundTouch(0, 2, RECORDER_SAMPLERATE, 2, 1.0f, 5);

    }

    public void startRecording(int pitch) {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        st = new SoundTouch(0, 2, RECORDER_SAMPLERATE, BytesPerElement, 1.0f, pitch);


        SOUNDS.clear();
        PROC_SOUNDS.clear();

        recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            public void run() {

                getAudioData();

            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }


    public void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;


            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
    }

    public void playOriginal() {
        track.stop();
        track.flush();

        byte[] sound = new byte[SOUNDS.size()];
        for (int i = 0; i < sound.length; i++) {
            sound[i] = SOUNDS.get(i);
        }
        track.play();
        track.write(sound, 0, sound.length);


    }


    public void playProcessed() {
        track.stop();
        track.flush();


        st.finish();

        byte[] processed = new byte[BufferElements2Rec];
        int st_proc = 0;
        do {
            st_proc = st.getBytes(processed);
            for (int i = 0; i < st_proc; i++) {
                PROC_SOUNDS.add(processed[i]);
            }
        } while (st_proc != 0);


        byte[] sound = new byte[PROC_SOUNDS.size()];
        for (int i = 0; i < PROC_SOUNDS.size(); i++) {
            sound[i] = PROC_SOUNDS.get(i);
        }
        track.play();
        track.write(sound, 0, sound.length);

    }

    public void stop() {
        track.stop();
        track.flush();

    }

    private void getAudioData() {
        // Write the output audio in byte
        byte sData[] = new byte[BufferElements2Rec];

        track.play();

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);

            // track.write(sData,0,BufferElements2Rec);

            //Copy to array
            for (int i = 0; i < BufferElements2Rec; i++) {
                SOUNDS.add(sData[i]);
            }

            st.putBytes(sData);

            int st_proc = st.getBytes(sData);
            //track.write(sData,0,st_proc);

            for (int i = 0; i < st_proc; i++) {
                PROC_SOUNDS.add(sData[i]);
            }

        }

    }


}