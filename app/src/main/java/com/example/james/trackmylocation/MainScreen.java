package com.example.james.trackmylocation;


import android.app.Activity;
import android.content.Intent;

import android.location.Location;
import android.os.AsyncTask;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import android.widget.Spinner;


import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainScreen extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnPoiClickListener{

    private Spinner spinner;
    double longitude, latitude;
    String PROJECT_ID = "location-tracker-1478206108627";
    String authorizedEntity = PROJECT_ID;
    String scope = "GCM";
    String next_bus = "asdf123";
    String token;
    InstanceID id;
    String uniqueid;
    Activity activity;


    //Map Objects
    MapFragment mapfragment;
    GoogleMap gMap;


    String bus_url = "http://35.164.7.20/BusSchedule.php";
    String reduced_id,temp_reduced_id;


    Intent background_service;
    Boolean service_started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        activity = this;

        //Generate Map Fragment
        mapfragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapfragment.getMapAsync(this);
        background_service = new Intent(getBaseContext(),transmitlocation_background_service.class);


        addItemsOnSpinner();

        Asyncgetiid asyncgetiid = new Asyncgetiid();
        asyncgetiid.execute();


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Test", "onResume: is running ");
//        googleApiClient.connect();


    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        activity.stopService(background_service);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.setOnPoiClickListener(this);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(49.882114,-119.477829),10.0f));
    }


    private class Asyncgetiid extends AsyncTask<String,String,String>{

        String grabid;

        @Override
        protected String doInBackground(String...params){
            id = InstanceID.getInstance(activity);
            try {
                grabid = id.getToken(authorizedEntity, scope);
            } catch(IOException e) {
                e.printStackTrace();
                Log.e("Token",e.getMessage());
            }
            return grabid;
        }
        @Override
        protected void onPostExecute(String iid){
            uniqueid = iid;
            temp_reduced_id = uniqueid.replaceAll("[^\\w\\s]","");
            reduced_id = temp_reduced_id.replaceAll("_","");
            if(!service_started){
                background_service.putExtra("iid",reduced_id);
                activity.startService(background_service);
                service_started = true;
            }
        }

    }
    public void addItemsOnSpinner() {
        spinner = (Spinner) findViewById(R.id.Spinner);
        List<String> list = new ArrayList<String>();
        list.add("97 Express");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(dataAdapter);
    }
    @Override
    public void onPoiClick(PointOfInterest poi){
        // Toast.makeText(getApplicationContext(), "Clicked: " +
       //                 poi.name + "\nPlace ID:" + poi.placeId +
       //                 "\nLatitude:" + poi.latLng.latitude +
       //                 " Longitude:" + poi.latLng.longitude,
       //         Toast.LENGTH_LONG).show();
        String id = poi.placeId;
        Log.i("Poi Click", id);
        FetchStaticSchedule(poi);



    }

   public void FetchStaticSchedule(final PointOfInterest poi) {
       RequestQueue queue = Volley.newRequestQueue(this);

       StringRequest stringRequest = new StringRequest(Request.Method.POST, bus_url, new Response.Listener<String>() {
           @Override
           public void onResponse(String response) {

               next_bus = response;

               Log.i("EchoResponce", next_bus);

               Toast.makeText(getApplicationContext(), next_bus, Toast.LENGTH_SHORT).show();
               Marker clicked_stop = gMap.addMarker(new MarkerOptions()
                       .position(poi.latLng)
                       .title(poi.name)
                       .snippet(next_bus));
               clicked_stop.showInfoWindow();


           }

       }, new Response.ErrorListener() {
           @Override
           public void onErrorResponse(VolleyError error) {

               // Error handling
               // System.out.println("Something went wrong!");
               error.printStackTrace();
               Log.i("Connection Error", error.getMessage());
           }
       }) {
           @Override
           protected Map<String, String> getParams() {
               Map<String, String> params = new HashMap<>();
               // the POST parameters:
               params.put("BusStop", poi.placeId);
               return params;
           }
       };
       queue.add(stringRequest);

   }
}

