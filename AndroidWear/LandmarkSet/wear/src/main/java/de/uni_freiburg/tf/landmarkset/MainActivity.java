package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.view.CardScrollView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    private LocationClient mLocationClient;
    private LocationRequest locationRequest;
    private boolean connected = false;

    private static final String TAG = "MyWearActivity";

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason.
            // resultCode holds the error code.
        }else {
            return false;
        }
    }

    @Override
    public void onLocationChanged(Location location){

        // Display the latitude and longitude in the UI
        Log.e(TAG,"Latitude:  " + String.valueOf( location.getLatitude()) +
                "\nLongitude:  " + String.valueOf( location.getLongitude()));
    }

    public void onConnected(Bundle dataBundle){
        //Log.e("MyWearActivity", "Connected");
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        connected = true;

        locationRequest = LocationRequest.create();

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationRequest.setInterval(2);

        locationRequest.setFastestInterval(2);

        locationRequest.setSmallestDisplacement(2);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

    }



    public void onConnectionFailed(ConnectionResult connectionResult){
        Log.e(TAG, "Connection Faild");
        connected = false;
    }

    public void onConnectionSuspended(int cause){
        Log.e(TAG, "Connection Suspended");
        connected = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                //mTextView = (TextView) stub.findViewById(R.id.text);

                //CardScrollView cardScrollView = (CardScrollView) findViewById(R.id.card_scroll_view);
                //cardScrollView.setCardGravity(Gravity.BOTTOM);
            }
        });

        //Log.e("MyWearActivity", "Play Service available:" + servicesConnected());

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.e(TAG, "Activity Result:" + resultCode);
    }



    public void showToast (View view){
        Log.e(TAG, "Save Position Button is pressed");
        Toast.makeText(this, "Button Pressed", Toast.LENGTH_SHORT).show();

        Location location = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        Log.e("MyWearActivity", "\nGot location" +
                "\nLatitude:  " + String.valueOf( location.getLatitude()) +
                "\nLongitude:  " + String.valueOf( location.getLongitude()) +
                "\nAccuracy:" + String.valueOf(location.getAccuracy()));


    }
}
