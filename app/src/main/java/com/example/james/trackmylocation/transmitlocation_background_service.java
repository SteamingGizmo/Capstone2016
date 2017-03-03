package com.example.james.trackmylocation;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.appdatasearch.GetRecentContextCall;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;


/**
 * Created by James on 2/16/2017.
 */

public class transmitlocation_background_service extends Service implements  GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    GoogleApiClient googleApiClient;
    Location location;
    LocationRequest locationRequest;
    LocationListener locationListener;
    String url = "http://35.164.7.20/FirstTry.php";
    String iid;

    private final IBinder iBinder = new LocalBinder();

    public class LocalBinder extends Binder{
        transmitlocation_background_service getService(){
            return transmitlocation_background_service.this;
        }
    }


    @Override
    public void onCreate(){
        Log.i("Background Service","Ran");
        Toast.makeText(this,"Service Running",Toast.LENGTH_SHORT).show();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();


    }
    @Override
    public void onConnected(@Nullable Bundle bundle){
    location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            sendlocation();
            }
        };
        setLocationRequest();
    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,locationListener);
    }
    public void setLocationRequest(){
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnectionSuspended(int i){

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult){

    }
    public void sendlocation(){
        RequestQueue Queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("ServerResponce",response);
            }

        },new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {

                // Error handling
                // System.out.println("Something went wrong!");
                error.printStackTrace();
                Log.d("Connection Error", error.getMessage());
            }
        })
        {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<>();
                // the POST parameters:
                params.put("lati", String.valueOf(location.getLatitude()));
                params.put("longi", String.valueOf(location.getLongitude()));
                params.put("UniqueID",iid.substring(0,64));
                //Log.d("UNIID",reduced_id.substring(0,64));
                return params;
            }
        };
        Queue.add(stringRequest);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        iid = intent.getStringExtra("iid");
        googleApiClient.connect();
        return START_STICKY;
    }
    @Override
    public void onDestroy(){

    }
    @Override
    public IBinder onBind(Intent intent){
        return iBinder;
    }
    public Location getLocation(){

        return location;
    }
    public boolean LocationIsNotNull(){

        if (location != null){
            return true;
        }
        else{
            return false;
        }
    }
}
