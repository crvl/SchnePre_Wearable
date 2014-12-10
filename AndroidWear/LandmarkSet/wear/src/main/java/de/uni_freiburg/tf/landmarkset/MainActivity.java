package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.support.wearable.view.CardScrollView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
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
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        MessageApi.MessageListener {

    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    private LocationClient mLocationClient;
    private LocationRequest locationRequest;
    private boolean connected = false;
    private KmlCreate kmlFile;
    private final String deletePath = "/deleteFile";
    private final String syncPath = "/syncFile";

    private static final String TAG = "MyWearActivity";

    @Override
    public void onLocationChanged(Location location){

        // Display the latitude and longitude in the UI
        Log.e(TAG,"Latitude:  " + String.valueOf( location.getLatitude()) +
                "\nLongitude:  " + String.valueOf( location.getLongitude()));
    }

    public void onMessageReceived(MessageEvent messageEvent){
        if(messageEvent.getPath().equals(deletePath)){
            Log.e(TAG,"Delete Message Received");
            kmlFile.resetKmlFile();
            syncWithMobil();

        }
        if(messageEvent.getPath().equals(syncPath)){
            Log.e(TAG,"Sync Message Received");
            kmlFile.initKmlFile();
            syncWithMobil();
        }
    }

    public void onConnected(Bundle dataBundle){
        //Log.e("MyWearActivity", "Connected");
        //Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
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

        kmlFile = new KmlCreate(this.getApplicationContext().getFilesDir());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void onResume(){
        super.onResume();
        Log.e(TAG, "OnResume");
        mGoogleApiClient.connect();
        Wearable.MessageApi.addListener(mGoogleApiClient,this);
    }

    protected void onPause(){
        super.onPause();
        mGoogleApiClient.disconnect();
        Wearable.MessageApi.removeListener(mGoogleApiClient,this);
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

    public void savePosButton (View view){
        Log.e(TAG, "Save Position Button is pressed");
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

    private void syncWithMobil(){
        try {
            ParcelFileDescriptor kmlPFD = ParcelFileDescriptor.open(kmlFile.getFile(),
                    ParcelFileDescriptor.MODE_READ_ONLY);
            Asset kmlAsset = Asset.createFromFd(kmlPFD);
            PutDataRequest request = PutDataRequest.create("/kmlFile");
            request.putAsset("Positions", kmlAsset);
            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        }catch (IOException e){
            Log.e(TAG, "File for asset not found");
        }
    }
}
