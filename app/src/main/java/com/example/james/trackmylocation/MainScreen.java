package com.example.james.trackmylocation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainScreen extends AppCompatActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback {

    private Switch locationswitch;
    private GPSTracker gps;
    double longitude, latitude;

    //Google Api Client For Location Support
    GoogleApiClient googleApiClient;

    //Location Objects
    Location locationobj;
    LocationRequest locationRequest;
    LocationListener locationListener;

    TextView longitudetext, latitutetext, velocitytext;

    //Map Objects
    MapFragment mapfragment;
    GoogleMap gMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        //Generate Map Fragment
        mapfragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapfragment.getMapAsync(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();

        longitudetext = (TextView) findViewById(R.id.longitude);
        latitutetext = (TextView) findViewById(R.id.latitude);
        velocitytext = (TextView) findViewById(R.id.velocity);


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Test", "onResume: is running ");
        locationswitch = (Switch) findViewById(R.id.locationswitch);
        locationswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    googleApiClient.connect();
                    Log.i("LocationSwitchStatus", "Switch Enabled");
                } else {
                    googleApiClient.disconnect();
                }
            }
        });


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

            UpdateUi();
        }
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationobj = location;
                UpdateUi(); }
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
    public void UpdateUi(){
        float zoomLevel = 19.0f;
        latitutetext.setText(String.valueOf(locationobj.getLatitude()));
        longitudetext.setText(String.valueOf(locationobj.getLongitude()));
        velocitytext.setText(String.valueOf(locationobj.getSpeed()));
        gMap.addMarker(new MarkerOptions()
                .position(new LatLng(locationobj.getLatitude(),locationobj.getLongitude()))
                .title("Last Position"));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationobj.getLatitude(),locationobj.getLongitude()),zoomLevel));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
    }

}
