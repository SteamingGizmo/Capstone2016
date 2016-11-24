package com.example.james.trackmylocation;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Created by James on 11/23/2016.
 */

public class Databasecon extends AsyncTask<String,Void,String> {

    float longitude_send, latitude_send;
    String link = "http://35.164.7.20/SamplePage.php";
    Databasecon() {


    }




    @Override
    protected String doInBackground(String... params) {

        try {
            Log.i("sendlocation", "sendlocation: tryingto send location");
            URL url = new URL(link);
            String data = URLEncoder.encode("lati", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(latitude_send), "UTF-8");
            data += "&" + URLEncoder.encode("longi", "UTF-8") + "=" + URLEncoder.encode((String.valueOf(longitude_send)), "UTF-8");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;

            while((line = reader.readLine()) != null){
                sb.append(line);
                break;
            }
            return sb.toString();

           // wr.close();
            //Log.i("sendlocation","went through");
        } catch( Exception e){
            Log.i("sendlocation",e.toString());

        }
        return "Finished";

    }

}
