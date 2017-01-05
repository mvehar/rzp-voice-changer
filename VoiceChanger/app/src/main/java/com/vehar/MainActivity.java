package com.vehar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.vehar.soundtouchandroid.R;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private Button buttonTest = null;
    private Button buttonSend = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonTest  =(Button) findViewById(R.id.buttonTest);
        buttonSend  =(Button) findViewById(R.id.buttonSend);

        buttonTest.setOnClickListener(this);
        buttonSend.setOnClickListener(this);


    }

    @Override
    public void onClick(View arg0) {
        Intent intent;

        int i = arg0.getId();
        if (i == R.id.buttonTest) {
            intent = new Intent(this, VoiceChanger.class);
            startActivity(intent);


        } else if (i == R.id.buttonSend) {
            intent = new Intent(this, Communicator.class);
            startActivity(intent);

        }
    }
}
