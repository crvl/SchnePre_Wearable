package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.CardScrollView;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
//import com.google.android.gms.common.api.Result;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    private boolean apiConnected;

    private KmlCreate kmlFile;
    private ConnectionManager conManager;

    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private CardFragment cardFragment;

    private CircledImageView saveGpsButton;

    private final String TAG = "MyWearActivity";

    @Override
    public void onLocationChanged(Location location){

        // Display the latitude and longitude in the UI
        Log.e(TAG,"Latitude:  " + String.valueOf( location.getLatitude()) +
                "\nLongitude:  " + String.valueOf( location.getLongitude()));
    }

    public void onConnectionChange(boolean connected){
        if(!hasGPS()) {
            if (connected) {
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.detach(cardFragment);
                fragmentTransaction.commit();
                saveGpsButton.setClickable(true);
            } else {
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.attach(cardFragment);
                fragmentTransaction.commit();
                saveGpsButton.setClickable(false);
            }
        }
    }


    public void onConnected(Bundle dataBundle){
        apiConnected = true;

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2);
        locationRequest.setFastestInterval(2);
        locationRequest.setSmallestDisplacement(2);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }


    public void onConnectionFailed(ConnectionResult connectionResult){
        Log.e(TAG, "Connection Faild");
        apiConnected = false;
    }

    public void onConnectionSuspended(int cause){
        Log.e(TAG, "Connection Suspended");
        apiConnected = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                saveGpsButton = (CircledImageView) stub.findViewById(R.id.savePosition);
            }
        });

        //create the card to show that no location service is available and hide it
        fragmentManager = getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        cardFragment = CardFragment.create("No GPS", "The watch has no connection " +
                "to the mobile phone and no GPS receiver!");
        fragmentTransaction.add(R.id.watch_view_stub, cardFragment);
        fragmentTransaction.detach(cardFragment);
        fragmentTransaction.commit();


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)

                .build();

        kmlFile = new KmlCreate(this.getApplicationContext().getFilesDir());

        conManager = new ConnectionManager(mGoogleApiClient, kmlFile, this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void onResume(){
        super.onResume();
        Log.e(TAG, "OnResume");
        mGoogleApiClient.connect();

    }

    protected void onPause(){
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    protected void onStart(){
        super.onStart();
    }

    protected void onStop(){
        super.onStop();
    }

    public void savePosButton (View view){
        Log.e(TAG, "Save Position Button is pressed");

        if(!conManager.getMobilConnection()){
            Log.e(TAG, "Mobil is not connected");

            if(!hasGPS()) {
                Log.e(TAG, "The device can no get its Position alone" +
                        " because it has no GPS receiver");

                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.attach(cardFragment);
                fragmentTransaction.commit();

                saveGpsButton.setClickable(false);

                return;
            }
        }

        Toast.makeText(this, "Position Saved", Toast.LENGTH_SHORT).show();

        Location location = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        Log.e("MyWearActivity", "\nGot location" +
                "\nLatitude:  " + String.valueOf( location.getLatitude()) +
                "\nLongitude:  " + String.valueOf( location.getLongitude()) +
                "\nAccuracy:" + String.valueOf(location.getAccuracy()));

        kmlFile.addLocation(location);

        //syncWithMobil();



    }

    public boolean hasGPS(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

}

