package com.example.james.trackmylocation;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainScreen extends AppCompatActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback {

    private Switch locationswitch;
    private GPSTracker gps;
    private Spinner spinner;
    double longitude, latitude;
    String PROJECT_ID = "location-tracker-1478206108627";
    String authorizedEntity = PROJECT_ID;
    String scope = "GCM";
    String token;
    InstanceID id;
    String uniqueid;
    Activity activity;

    //Google Api Client For Location Support
    GoogleApiClient googleApiClient;

    //Location Objects
    Location locationobj;
    LocationRequest locationRequest;
    LocationListener locationListener;


    //Map Objects
    MapFragment mapfragment;
    GoogleMap gMap;
    // Databasecon databasecon = new Databasecon();


    String url = "http://35.164.7.20/FirstTry.php";
    String reduced_id,temp_reduced_id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        activity = this;
        //token = InstanceID.getInstance(this).getToken(authorizedEntity, scope);
        //Generate Map Fragment
        mapfragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapfragment.getMapAsync(this);

        addItemsOnSpinner();


        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();



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
        googleApiClient.connect();


    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
         int test_constant = 1;
        //ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, test_constant);
        locationobj = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (locationobj != null){

            UpdateMap();

        }
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationobj = location;
                UpdateMap();
                }
        };
        setLocationRequest();
        startlocationupdates();

        }

    public void setLocationRequest(){
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void startlocationupdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,locationListener);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }
    public void UpdateMap(){
        float zoomLevel = 19.0f;

        gMap.addMarker(new MarkerOptions()
                .position(new LatLng(locationobj.getLatitude(),locationobj.getLongitude()))
                .title("Last Position"));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationobj.getLatitude(),locationobj.getLongitude()),zoomLevel));
        sendlocation();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
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
                params.put("lati", String.valueOf(locationobj.getLatitude()));
                params.put("longi", String.valueOf(locationobj.getLongitude()));
                params.put("UniqueID",reduced_id.substring(0,64));
                Log.d("UNIID",reduced_id.substring(0,64));
                return params;
            }
        };
        Queue.add(stringRequest);
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

        }

    }
    public void addItemsOnSpinner() {
        spinner = (Spinner) findViewById(R.id.Spinner);
        List<String> list = new ArrayList<String>();
        list.add("1 LineDescription");
        list.add("2 LineDescription");
        list.add("3 LineDescription");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list);
        spinner.setAdapter(dataAdapter);
    }


}

