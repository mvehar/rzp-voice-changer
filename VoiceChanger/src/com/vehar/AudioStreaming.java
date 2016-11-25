package net.surina;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import net.surina.soundtouch.SoundTouch;

/**
 * Created by matejv on 22.11.2016.
 */

public class AudioStreaming {
    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_FLOAT;


    private AudioRecord recorder = null;
    AudioTrack track = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;


    int BufferElements2Rec = 2048; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    int bufferSize = 0;


    public AudioStreaming() {
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE , RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement, AudioTrack.MODE_STREAM);


        //TODO : Streaming server init

    }

    public void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        track.play();

        recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            public void run() {

                processAndStream();

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

            track.stop();

            recorder = null;
            recordingThread = null;
        }

    }

    //Conversion of short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void processAndStream() {
        // Write the output audio in byte
        float sData[] = new float[BufferElements2Rec * BytesPerElement];
        float processedData[] = new float[BufferElements2Rec * BytesPerElement];

        SoundTouch st = new SoundTouch();
        st.setTempo(100);
        st.setPitchSemiTones(0);
        st.setSampleRate(RECORDER_SAMPLERATE);
        st.setChannels(2);



//        FileOutputStream os = null;
//        try {
//            os = new FileOutputStream(FILE + TMP);
//        } catch (F=ileNotFoundException e) {
//            e.printStackTrace();
//        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec,AudioRecord.READ_BLOCKING);

            //TODO: Process
            float data[] = process(st, sData);


            //TODO: Send / Play
            if(data != null){
                track.write(data, 0, data.length,AudioTrack.WRITE_NON_BLOCKING);

            }

//
//            try {
//                // writes the data to file from buffer stores the voice buffer
//                byte bData[] = short2byte(sData);
//
////                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }



    }

    protected float[] process(SoundTouch st, float[] input ) {
        float[] out = null;
        try {

            long startTime = System.currentTimeMillis();
            out = st.processSamples(input, 2);
            long endTime = System.currentTimeMillis();
            float duration = (endTime - startTime) * 0.001f;

            Log.i("SoundTouch", "process file done, duration = " + duration);


        } catch (Exception exp) {
            exp.printStackTrace();
        }

        return out;
    }

}