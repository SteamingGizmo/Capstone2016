package com.example.james.trackmylocation;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;

import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Spinner;


import android.widget.TextView;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;


public class MainScreen extends Activity implements
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

    TextView Bus_Stop;

    String stoplookup[] = {"","97ExpressToWestBankStops","97ExpressToUBCOStops","8ToUBCO","8ToOkanaganCollege" };

    Timer timer;
    ArrayList<LatLng> simulated_positions;
    Marker simulated_marker;
    Integer n = 0;
    Boolean visible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
       // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_screen);
        activity = this;

        //Generate Map Fragment
        mapfragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapfragment.getMapAsync(this);
        Bus_Stop = (TextView) findViewById(R.id.Selected_Stop);


        addItemsOnSpinner();

        Asyncgetiid asyncgetiid = new Asyncgetiid();
        asyncgetiid.execute();

        simulated_positions = new ArrayList<LatLng>();




        try{
            InputStream csvStream = getAssets().open("97express.txt");
            InputStreamReader csvStreamReader = new InputStreamReader(csvStream);
            CSVReader csvReader = new CSVReader(csvStreamReader);
            csvReader.readNext();
            String[] line;



            while((line = csvReader.readNext()) != null){
                // polyline_points_lat.add(line[0]);
                simulated_positions.add(new LatLng(Double.parseDouble(line[1]),Double.parseDouble(line[0])));
            }
            csvStream.close();

        }catch (IOException e){
            e.printStackTrace();
            Log.d("IOException","IOException");

        }


         timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Timer","TimerRun");
                        if(gMap != null) {
                            if (simulated_marker == null) {
                                simulated_marker = gMap.addMarker(new MarkerOptions()
                                        .position(simulated_positions.get(0))
                                        .title("Position")
                                        .visible(visible)
                                        .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("buslocation",148,187)))
                                );
                            } else {

                                simulated_marker.setPosition(simulated_positions.get(n));
                                n++;
                            }
                            if(n == simulated_positions.size()){
                                n = 1;
                            }
                        }
                    }
                });
                                }
            };
        timer.schedule(timerTask,1000,1000);
    };





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
        list.add("Please Select A Route");
        list.add("97 Express To West Kelowna");
        list.add("97 Express To UBCO");
        list.add("8 To UBCO");
        list.add("8 To Okanagan College");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
              //  Toast.makeText(MainScreen.this, String.valueOf(id), Toast.LENGTH_SHORT).show();

            if(mBound) {
                if (backgroundservice.LocationIsNotNull()) {
                    Find_Closest_Stop(position);
                    Toast.makeText(MainScreen.this, String.valueOf(position), Toast.LENGTH_SHORT).show();
                }
            }
            if(position != 0){
                Polyline polyline_97express = getRoutePolygon(position,true);
            }
            if(position == 1 || position == 2 ){

                if(simulated_marker != null){
                    simulated_marker.setVisible(true);
                }

            }else{
                if(simulated_marker != null) {
                    simulated_marker.setVisible(false);
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
        //FetchStaticSchedule(poi);



    }

   public void FetchStaticSchedule(final String stop) {
       RequestQueue queue = Volley.newRequestQueue(this);

       StringRequest stringRequest = new StringRequest(Request.Method.POST, bus_url, new Response.Listener<String>() {
           @Override
           public void onResponse(String response) {

               String local_time="";
               next_bus = response.replaceAll("\\s+","");




               SimpleDateFormat sourceformat = new SimpleDateFormat("HH:mm:ss");
               sourceformat.setTimeZone(TimeZone.getTimeZone("UTC"));
               try{
                   Date parsed = sourceformat.parse(next_bus);
                   //TimeZone timeZone = TimeZone.getDefault();
                   SimpleDateFormat destFormat = new SimpleDateFormat("h:mm a");
                   destFormat.setTimeZone(TimeZone.getDefault());
                   local_time = destFormat.format(parsed);
                   Log.i("Local Time",local_time);

               }catch (Exception e){
                   Log.i("Date Parse",e.toString());
               }
               Log.i("EchoResponce", next_bus);
                TextView Scheduled_Time = (TextView) findViewById(R.id.Scheduled_Time);
               //Scheduled_Time.setText(local_time);
               //Toast.makeText(getApplicationContext(), next_bus, Toast.LENGTH_SHORT).show();





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
               params.put("BusStop",stop);
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

    public void Find_Closest_Stop(final int stop_number) {
        RequestQueue queue = Volley.newRequestQueue(this);

       StringRequest stringRequest = new StringRequest(Request.Method.POST, stops_url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

            //Log.i("closeststop",response);

                Log.i("asde",response);
              //  stop_locations = new JSONObject(response)
                //Toast.makeText(activity, closest_stops[0], Toast.LENGTH_SHORT).show();

                try{
                    stop_locations = new JSONArray(response);
                }
                catch (JSONException e){
                Log.d("Stop Failure", e.toString());
                    Toast.makeText(MainScreen.this, "Failure", Toast.LENGTH_SHORT).show();
                }
                if(stop_locations != null){
                   String Stop_name = "";
                    double Closest_Stop_Latitude, Closest_Stop_Longitude;

                    try{
                     Stop_name =  stop_locations.getJSONObject(0).getString("name");
                        Closest_Stop_Latitude = stop_locations.getJSONObject(0).getDouble("latitude");
                        Closest_Stop_Longitude = stop_locations.getJSONObject(0).getDouble("longitude");

                         if(Stop_name != null){
                            Bus_Stop.setText(Stop_name);
                             LatLng Closest_Stop = new LatLng(Closest_Stop_Latitude,Closest_Stop_Longitude);
                             gMap.addMarker(new MarkerOptions()
                             .title(Stop_name)
                             .position(Closest_Stop)
                             .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("busstopicon",148,187))));
                             gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(Closest_Stop,15.0f));

                        }
                    }
                    catch(JSONException e){

                    }
                    if(Stop_name != null){
                        FetchStaticSchedule(Stop_name);
                    }
                    Log.d("Stop_name", Stop_name);
                }


              //  Log.d("Array", stop_locations.names());

            }

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
                params.put("stop",stoplookup[stop_number]);
                return params;
            }
        };
        queue.add(stringRequest);

    }

    public Bitmap resizeMapIcons(String iconName, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName,"drawable",getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap,width,height,false);
        return resizedBitmap;
    }

    public Polyline getRoutePolygon(int route_id, Boolean display ){

        ArrayList<LatLng> polyline_points = new ArrayList<LatLng>();
        Polyline polyline;
        try{
            InputStream csvStream = getAssets().open("97express.txt");
            InputStreamReader csvStreamReader = new InputStreamReader(csvStream);
            CSVReader csvReader = new CSVReader(csvStreamReader);
            csvReader.readNext();
            String[] line;



            while((line = csvReader.readNext()) != null){
                // polyline_points_lat.add(line[0]);
                polyline_points.add(new LatLng(Double.parseDouble(line[1]),Double.parseDouble(line[0])));
            }
            csvStream.close();
            polyline = gMap.addPolyline(new PolylineOptions()
                    .addAll(polyline_points)
                    .width(8)
                    .color(Color.CYAN)
                    .visible(display));
            return(polyline);


        }catch (IOException e){
            e.printStackTrace();
            Log.d("IOException","IOException");
            return(null);
        }
    }

}

