package com.vehar;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by matejv on 22.11.2016.
 */

public class AudioStreamingTwoWay {
    static final int RECORDER_BPP = 16;
    static final int RECORDER_SAMPLERATE = 44100;
    static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    int BufferElements2Rec = 4096; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    int bufferSize = 0;

    private AudioRecord recorder = null;
    private AudioTrack track = null;
    private Socket client = null;
    private SoundTouch st = null;

    private int PORT = 6767;
    private String IP = null;

    private boolean STREAMING = false;
    private boolean LISTENING = false;
    //Two way
    private boolean TRANSFER = false;

    TextView statLabel = null;
    Activity parentActivity = null;


    public AudioStreamingTwoWay(TextView statLabel) {
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        this.statLabel = statLabel;

    }

    /**********************************************************************************
     LISTEN
     */

    //Start server and listen for input stream;
    public boolean listen(final Activity activity) {
        if(LISTENING) return false;
        parentActivity = activity;

        LISTENING = true;
        //Run thread
        Thread thread1 = new Thread(new Runnable() {

            public void run() {
                try {
                    listenStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                activity.finish();
            }
        }, "Listen Thread");
        thread1.start();

        return true;

    }

    private void listenStream() throws IOException {

        track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE , RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement, AudioTrack.MODE_STREAM);

        ServerSocket socket = new ServerSocket(PORT);

        System.out.println("Waiting for client...");
        client = socket.accept();
        socket.close();

        System.out.println("Client connected.");
        updateLabel("Client connected.");

        DataInputStream in = new DataInputStream(client.getInputStream());

        byte data[];
        track.stop();
        track.flush();
        track.play();

        while(LISTENING  && client.isConnected()){
            data = new byte[4096];
            int readed = in.read(data);
            if(readed>-1){
                track.write(data, 0, readed);
            }
        }
        //Stop listening stream
        if(client.isConnected())
            client.close();
        updateLabel("Disconnected.");


        //Clean Audiotrack
        track.stop();
        track.flush();
        track.release();
        track = null;
        System.out.println("Stopped listening.");


    }


    public void stopListening() {
        LISTENING = false;
    }

    /**********************************************************************************
        STREAM
     */
    public boolean startStreaming(String ip, int port, int pitch, final Activity activity) {
        if(STREAMING) return false;
        parentActivity = activity;

        IP = ip;
        PORT = port;

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        st = new SoundTouch(0,2,RECORDER_SAMPLERATE,BytesPerElement,1.0f,pitch);


        STREAMING = true;

        Thread streamingThread = new Thread(new Runnable() {

            public void run() {
                try {
                    recordAndSend();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                activity.finish();
            }
        }, "Stream Thread");
        streamingThread.start();

        return true;
    }


    private void recordAndSend() throws IOException {
        // Write the output audio in byte
        byte sData[] = new byte[BufferElements2Rec];

        //connect
        System.out.println("trying to connect... ");

        client = new Socket(IP, PORT);

        DataOutputStream out = new DataOutputStream(client.getOutputStream());


        recorder.startRecording();
        System.out.println("Streaming... ");
        updateLabel("Streaming...");


        while (STREAMING && client.isConnected()) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);

            st.putBytes(sData);

            int st_proc = st.getBytes(sData);
            //track.write(sData,0,st_proc);

            out.write(sData, 0, st_proc);

            System.out.println("Sending: "+st_proc);


            //Flush if large enough samples
            if(st_proc>1024) ;
            out.flush();

        }

        STREAMING =false;
        //Stop recording
        recorder.stop();
        recorder.release();
        recorder = null;

        //Empty SoundTouch buffers
        st.finish();
        int st_proc  = 0;
        do {
            st_proc = st.getBytes(sData);
            out.write(sData,0,st_proc);
            out.flush();
        }while(st_proc!=0);

        //Close connection
        if(client.isConnected())
            client.close();
        updateLabel("Disconnected.");



        client = null;
        System.out.println("Stopped stream ");


    }

    public void stopStreaming(){
        STREAMING = false;
    }

    /*
        TWO WAY .....................
     */

    private boolean startListeningTransfer(final Activity activity,final int pitch) throws IOException {
        if(TRANSFER) return false;
        parentActivity = activity;

        ServerSocket socket = new ServerSocket(PORT);

        System.out.println("Waiting for client...");
        client = socket.accept();
        socket.close();

        System.out.println("Client connected.");
        updateLabel("Client connected.");

        TRANSFER = true;

        //Run thread for listener first - streamer second
        Thread thread1 = new Thread(new Runnable() {

            public void run() {
                try {
                    transfer(pitch);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                activity.finish();
            }
        }, "Listen Thread");
        thread1.start();


        //Stop listening stream
        if(client.isConnected())
            client.close();
        updateLabel("Disconnected.");

        return true;
    }

    private boolean startStreamingTransfer(String ip, int port,final int pitch, final Activity activity) throws IOException {

        if(TRANSFER) return false;
        parentActivity = activity;
        IP = ip;
        PORT = port;

        client = new Socket(IP, PORT);

        System.out.println("Client connected.");
        updateLabel("Client connected.");

        TRANSFER = true;

        //Run thread for listener first - streamer second
        Thread thread1 = new Thread(new Runnable() {

            public void run() {
                try {
                    transfer(pitch);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                activity.finish();
            }
        }, "Listen Thread");
        thread1.start();


        //Stop listening stream
        if(client.isConnected())
            client.close();
        updateLabel("Disconnected.");

        return true;
    }

    private void transfer(int pitch) throws IOException {
        track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE , RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement, AudioTrack.MODE_STREAM);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        st = new SoundTouch(0,2,RECORDER_SAMPLERATE,BytesPerElement,1.0f,pitch);


        TRANSFER = true;

        //Setup streams
        DataInputStream in = new DataInputStream(client.getInputStream());
        DataOutputStream out = new DataOutputStream(client.getOutputStream());

        //Setup start recorder and player
        recorder.startRecording();
        track.stop();
        track.flush();
        track.play();

        byte data[];
        while(TRANSFER  && client.isConnected()){
            data = new byte[4096];

            //SEND DATA
            // gets the voice output from microphone to byte format
            recorder.read(data, 0, BufferElements2Rec);
            st.putBytes(data);
            int st_proc = st.getBytes(data);
            out.write(data, 0, st_proc);
            out.flush();

            System.out.println("Sending: "+st_proc);

            //PLAY received DATA
            data = new byte[4096];
            int readed = in.read(data);
            if(readed>-1){
                track.write(data, 0, readed);
            }
        }

        //Empty SoundTouch buffers
        st.finish();
        int st_proc  = 0;
        do {
            data = new byte[4096];
            st_proc = st.getBytes(data);
            out.write(data,0,st_proc);
            out.flush();
        }while(st_proc!=0);

        //STOP recorder and player
        //Stop recording
        recorder.stop();
        recorder.release();
        recorder = null;

        //Stop playing
        track.stop();
        track.flush();
        track.release();
        track = null;

    }

    /*
        HELPER
     */
    private void updateLabel(final String text){
        parentActivity.runOnUiThread(new Runnable() {
            public void run() {
                statLabel.setText(text);
            }
        });
    }


}