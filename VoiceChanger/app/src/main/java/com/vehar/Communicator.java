package com.vehar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.vehar.soundtouchandroid.R;

import java.io.IOException;

public class Communicator extends AppCompatActivity implements OnClickListener {

    Button buttonQR = null;
    Button buttonReadQR = null;
    Button buttonStartSending = null;

    TextView textMode = null;
    TextView sendStat = null;

    SeekBar pitchBar = null;
    //
    private boolean isSending = false;
    private String IP = null;
    private int PORT = 6767;

    //
    AudioStreamingTwoWay streamer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        buttonQR = (Button) findViewById(R.id.buttonQR);
        buttonReadQR = (Button) findViewById(R.id.buttonReadQR);
        buttonStartSending = (Button) findViewById(R.id.buttonStartSending);
        textMode = (TextView) findViewById(R.id.textMode);
        sendStat = (TextView) findViewById(R.id.sendStat);
        pitchBar = (SeekBar) findViewById(R.id.pitchBar);

        streamer = new AudioStreamingTwoWay(sendStat);

    }

    @Override
    protected void onDestroy(){

        System.out.println("Stopping..");

        if(streamer != null){
            streamer.stop();
        }

        super.onDestroy();

    }

    @Override
    public void onClick(View arg0) {
        int i = arg0.getId();
        if (i == R.id.buttonQR) {
            Intent intentGenQr = new Intent(this, QRCodeServer.class);
            startActivity(intentGenQr);

        } else if (i == R.id.buttonReadQR) {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt("Scan a barcode");
            integrator.setBeepEnabled(false);
            integrator.initiateScan();
            integrator.setOrientationLocked(true);

        } else if (i == R.id.buttonStartSending) {
            if (isSending) {
                //TODO: Stop sending
                Toast.makeText(this, "Stopping...", Toast.LENGTH_SHORT).show();

                streamer.stop();
                isSending = !isSending;
            } else {
                Toast.makeText(this, "Starting...", Toast.LENGTH_SHORT).show();

                try {
                    streamer.startStreamingTransfer(IP, PORT, pitchBar.getProgress()-10, this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isSending = !isSending;
            }


        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                IP = result.getContents();
                textMode.setText("Sending to IP: " + result.getContents());
                buttonStartSending.setVisibility(View.VISIBLE);
                pitchBar.setVisibility(View.VISIBLE);

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }


    }

}
