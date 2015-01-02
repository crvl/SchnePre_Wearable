package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.Vibrator;
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
        LocationListener,
        SensorEventListener{

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    private SensorManager mSensorManager;
    private Sensor gravitySensor;
    private Sensor geoMagneticSensor;
    private boolean apiConnected;
    private boolean running;

    private boolean hasMagnetometer;
    private boolean hasAcceleration;

    private KmlCreate kmlFile;
    private ConnectionManager conManager;

    private float[] gravity;
    private float[] geomagnetic;

    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private CardFragment cardFragment;

    private CircledImageView saveGpsButton;

    private final String TAG = "MyWearActivity";
    private final String newData = "/newData";

    @Override
    public void onLocationChanged(Location location){

        // Display the latitude and longitude in the UI
        Log.e(TAG,"Latitude:  " + String.valueOf( location.getLatitude()) +
                "\nLongitude:  " + String.valueOf( location.getLongitude()));

        kmlFile.addWayPoint(location);
    }
    /*this function will be called when the connection to the mobile phone changes*/
    public void onConnectionChange(boolean connected){

        if(!hasGPS()) {
            if (connected) {
                //if the system has no GPS receiver remove the notification
                //that the system cannot take placmarks and enable the save position button
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.detach(cardFragment);
                fragmentTransaction.commit();
                saveGpsButton.setClickable(true);
            } else {
                //if the system has no GPS receiver show the notification
                //that the system cannot take placmarks and disable the save position button
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.attach(cardFragment);
                fragmentTransaction.commit();
                saveGpsButton.setClickable(false);
            }
        }
    }

    /*this function will be called from the sensor api when the accuracy of a sensor change*/
    public final void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    /*this function will be called from the sensor api when the sensor value change*/
    public final void onSensorChanged(SensorEvent event){
        //get the values of the gravity Sensor
        if(event.sensor == gravitySensor){
            if(event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
                gravity = event.values.clone();
            }
        }
        //get the values of the earth magnetic sensor
        if(event.sensor == geoMagneticSensor){
            if(event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
                geomagnetic = event.values.clone();
            }
        }
    }

    /*this function will be called when the google api client is connected*/
    public void onConnected(Bundle dataBundle){
        apiConnected = true;

        //setup the service to get the location
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2);
        locationRequest.setFastestInterval(2);
        locationRequest.setSmallestDisplacement(2);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    /*this function will be called when the connection to the google api client failed*/
    public void onConnectionFailed(ConnectionResult connectionResult){
        Log.e(TAG, "Connection Faild");
        apiConnected = false;
    }
    /*this function will be called when the connection to the google api client is suspended*/
    public void onConnectionSuspended(int cause){
        Log.e(TAG, "Connection Suspended");
        apiConnected = false;
    }

    /*this is the entry point for this activity and
    will be called by the system on creation of the activity*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                //get the save Position button
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

        //create the google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)

                .build();

        //init the gravity and magnetic sensor
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        geoMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        if(geoMagneticSensor == null){hasMagnetometer = false;}
        else{hasMagnetometer = true;}

        if(gravitySensor == null){hasAcceleration = false;}
        else {hasAcceleration = true;}

        //create or open the kml file where to write the placemarks
        kmlFile = new KmlCreate(this.getApplicationContext().getFilesDir());

        //create the instance of the connection manager
        conManager = new ConnectionManager(mGoogleApiClient, kmlFile, this);

        //prevent the screen from turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        geomagnetic = new float[3];
        gravity = new float[3];

    }
    /*this function will be called by the system when restarting the app without destroying it before*/
    public void onStart(){
        super.onStart();

    }

    /*this function will also called from the system on app start and
    when the app come to foreground again*/
    protected void onResume(){
        super.onResume();
        mGoogleApiClient.connect();
        if(hasAcceleration && hasMagnetometer) {
            mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, geoMagneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        running = true;
    }
    /*this function will be called when the app go to background*/
    protected void onPause(){
        super.onPause();
        mGoogleApiClient.disconnect();
        if(hasAcceleration && hasMagnetometer) {
            mSensorManager.unregisterListener(this);
            mSensorManager.unregisterListener(this);
        }
        running = false;
    }


    /*this function will be called on app close by the system*/
    public void onStop(){
        super.onStop();
    }

    /*this function will be called by the system on destroying the app*/
    public void onDestroy(){
        super.onDestroy();
    }

    /*this function will be called when the button of this activity is pressed*/
    public void savePosButton (View view){
        float[] orientation = new float[3];
        float[] rotationR = new float[16];
        float[] rotationI = new float[16];
        float bearingToNorth;
        double helpRad;


        Log.e(TAG, "Save Position Button is pressed");

        Vibrator vibe = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        //test if the mobile phone is connected
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

        //show a small message that the position was saved
        Toast.makeText(this, "Position Saved", Toast.LENGTH_SHORT).show();

        //get the latest location
        Location location = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        Log.e("MyWearActivity", "\nGot location" +
                "\nLatitude:  " + String.valueOf( location.getLatitude()) +
                "\nLongitude:  " + String.valueOf( location.getLongitude()) +
                "\nAccuracy:" + String.valueOf(location.getAccuracy()));

        //if the system has a magnetic and a acceleration sensor set the bearing in the location
        if(hasAcceleration && hasMagnetometer) {
            if (mSensorManager.getRotationMatrix(rotationR, rotationI, gravity, geomagnetic)) {

                mSensorManager.getOrientation(rotationR, orientation);
                //helper variable needed to deal with conversion between double and float
                helpRad = orientation[0];
                helpRad = Math.toDegrees(helpRad);

                bearingToNorth = (float) helpRad;

                location.setBearing(bearingToNorth);
            } else {
                Log.e(TAG, "not able to get orientation");
            }
        }

        //add the location to the kmlFile
        kmlFile.addLocation(location);

        //let the watch vibrate for 250ms
        if(vibe.hasVibrator()){
            vibe.vibrate(250);
        }

        //send a message to the mobile phone to notify that there are new data available
        conManager.sendMessage(newData);
    }

    /*this function tests if the watch has a GPS receiver*/
    public boolean hasGPS(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    /*this function returns the state of the api connection*/
    public boolean isApiConnected(){
        return apiConnected;
    }

    /*this function returns the state of activity*/
    public boolean isRunning(){
        return running;
    }
}

