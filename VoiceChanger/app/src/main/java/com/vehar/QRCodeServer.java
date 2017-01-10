package com.vehar;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioTrack;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.vehar.soundtouchandroid.R;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Hashtable;

public class QRCodeServer extends AppCompatActivity {

    ImageView qrImage = null;
    TextView stat  =null;


    private AudioTrack track = null;
    private boolean isRecording = false;
    int BufferElements2Rec = 4096; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    int bufferSize = 0;

    AudioStreamingTwoWay listener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        qrImage = (ImageView) findViewById(R.id.qrkoda);
        stat = (TextView) findViewById(R.id.stat);

        String myIP = getLocalIpAddress();

        if(myIP == ""){
            super.onDestroy();
        }

        stat.setText("PORT: 6767, IP: "+myIP);


        try {
            Bitmap qrCode = generateQrCode(myIP);
            qrImage.setImageBitmap(qrCode);

        } catch (WriterException e) {
            e.printStackTrace();
        }
        listener = new AudioStreamingTwoWay(stat);
        try {
            listener.startListeningTransfer(this,1);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy(){
        System.out.println("Stopping..");

        if(listener!= null){
            listener.stop();
        }

        super.onDestroy();
    }

    public Bitmap generateQrCode(String myCodeText) throws WriterException {
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // H = 30% damage

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        int size = 256;

        BitMatrix bitMatrix = qrCodeWriter.encode(myCodeText, BarcodeFormat.QR_CODE, size, size, hintMap);
        int width = bitMatrix.getWidth();

        Bitmap bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                bmp.setPixel(y, x, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }


    public String getLocalIpAddress() {
        String ip = "";

        WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ipaddr = wifiInfo.getIpAddress();

        if(ipaddr>0){
            ip =     Formatter.formatIpAddress(ipaddr);

            System.out.println("Found Wifi IP: "+ip);
            return ip;
        }


        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ip = Formatter.formatIpAddress(inetAddress.hashCode());
                        System.out.println("Found IP: "+ip);

                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return ip;
    }
}
