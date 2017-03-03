package com.example.james.trackmylocation;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;

import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Spinner;


import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;



import com.google.android.gms.iid.InstanceID;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainScreen extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnPoiClickListener{

    private Spinner spinner;
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
    Location location;

    String bus_url = "http://35.164.7.20/BusSchedule.php";
    String stops_url = "http://35.164.7.20/FindClosestStop.php";
    String reduced_id,temp_reduced_id;


    Intent background_service_intent;
    Boolean service_started = false;

    transmitlocation_background_service backgroundservice;
    Boolean mBound = false;

    Marker clicked_stop;
    JSONArray stop_locations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        activity = this;

        //Generate Map Fragment
        mapfragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapfragment.getMapAsync(this);



        addItemsOnSpinner();

        Asyncgetiid asyncgetiid = new Asyncgetiid();
        asyncgetiid.execute();


    }

    @Override
    protected void onStart() {
        super.onStart();
        background_service_intent = new Intent(getBaseContext(),transmitlocation_background_service.class);
        bindService(background_service_intent,mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Test", "onResume: is running ");


    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        activity.stopService(background_service_intent);
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(mBound){
            unbindService(mConnection);
            mBound = false;
        }
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
                background_service_intent.putExtra("iid",reduced_id);
                activity.startService(background_service_intent);
                service_started = true;
            }
        }

    }
    public void addItemsOnSpinner() {
        spinner = (Spinner) findViewById(R.id.Spinner);
        List<String> list = new ArrayList<String>();
        list.add("97 Express To West Kelowna");
        list.add("97 Express To UBCO");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
              //  Toast.makeText(MainScreen.this, String.valueOf(id), Toast.LENGTH_SHORT).show();

            if(mBound) {
                if (backgroundservice.LocationIsNotNull()) {
                    Find_Closest_Stop();
                }
            }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
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

               if(clicked_stop != null){clicked_stop.remove();}
               clicked_stop = gMap.addMarker(new MarkerOptions()
                       .position(poi.latLng)
                       .title(poi.name)
                       .snippet(next_bus));
               clicked_stop.showInfoWindow();
               activity.startService(background_service_intent);



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
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
           transmitlocation_background_service.LocalBinder binder = (transmitlocation_background_service.LocalBinder) service;
            backgroundservice = binder.getService();
            mBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    public void Find_Closest_Stop() {
        RequestQueue queue = Volley.newRequestQueue(this);

       StringRequest stringRequest = new StringRequest(Request.Method.POST, stops_url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

            //Log.i("closeststop",response);

                Log.i("asde",response);
              //  stop_locations = new JSONObject(response)
                //Toast.makeText(activity, closest_stops[0], Toast.LENGTH_SHORT).show();


            }
//        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
//                (Request.Method.POST, stops_url, null, new Response.Listener<JSONArray>() {
//
//                    @Override
//                    public void onResponse(JSONArray response) {
//                        Log.i("asdf",response.toString());
//                    }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                // Error handling
                // System.out.println("Something went wrong!");
               // error.printStackTrace();

                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null) {
                    Log.e("Volley", "Error. HTTP Status Code:"+networkResponse.statusCode);
                }

                if (error instanceof TimeoutError) {
                    Log.e("Volley", "TimeoutError");
                }else if(error instanceof NoConnectionError){
                    Log.e("Volley", "NoConnectionError");
                } else if (error instanceof AuthFailureError) {
                    Log.e("Volley", "AuthFailureError");
                } else if (error instanceof ServerError) {
                    Log.e("Volley", "ServerError");
                } else if (error instanceof NetworkError) {
                    Log.e("Volley", "NetworkError");
                } else if (error instanceof ParseError) {
                    Log.e("Volley", "ParseError");
                }

            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                // the POST parameters:
                params.put("lati", String.valueOf(backgroundservice.getLocation().getLatitude()));
                params.put("longi",String.valueOf(backgroundservice.getLocation().getLongitude()));
                return params;
            }
        };
        queue.add(stringRequest);

    }

}

