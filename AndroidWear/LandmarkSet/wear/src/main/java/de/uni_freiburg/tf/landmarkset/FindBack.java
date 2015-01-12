package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.NodeApi;

public class FindBack extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener,
        NodeApi.NodeListener{

    private TextView distanceText;
    private ImageView arrow;
    private String TAG = "Find Back APP";

    private float[] gravity;
    private float[] geomagnetic;

    private boolean hasMagnetometer;
    private boolean hasAcceleration;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    private SensorManager mSensorManager;
    private Sensor gravitySensor;
    private Sensor geoMagneticSensor;

    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private CardFragment cardFragment;

    private Location destination;

    private float imageDegree;
    private float bearingToDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_back);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                distanceText = (TextView) stub.findViewById(R.id.back_distance);
                arrow = (ImageView) stub.findViewById(R.id.dir_arrow);

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


        destination = new Location("By my Self");
        //set the location
        //in further use this will be done by receiving a message from the mobile phone
        destination.setLatitude(48.01262);
        destination.setLongitude(7.83504);
        destination.setAltitude(300);

        imageDegree = 0;

        //prevent the screen from turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    protected void onResume(){
        super.onResume();
        mGoogleApiClient.connect();
        if(hasAcceleration && hasMagnetometer) {
            mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, geoMagneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void onPause(){
        super.onPause();
        mGoogleApiClient.disconnect();
        if(hasAcceleration && hasMagnetometer) {
            mSensorManager.unregisterListener(this);
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onLocationChanged(Location location){
        float dist;
        int dist_int;
        float bearing;

        float[] orientation = new float[3];
        float[] rotationR = new float[16];
        float[] rotationI = new float[16];
        float bearingToNorth = 0;
        double helpRad;

        dist = location.distanceTo(destination);
        dist_int = (int) dist;
        bearingToDestination = location.bearingTo(destination);

        distanceText.setText(String.valueOf(dist_int) + "m");
        // Display the latitude and longitude in the UI
        Log.i(TAG, "Latitude:  " + String.valueOf(location.getLatitude()) +
                "\nLongitude:  " + String.valueOf(location.getLongitude()));

    }

    /*this function will be called from the sensor api when the accuracy of a sensor change*/
    public final void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    /*this function will be called from the sensor api when the sensor value change*/
    public final void onSensorChanged(SensorEvent event){
        float bearing;

        float[] orientation = new float[3];
        float[] rotationR = new float[16];
        float[] rotationI = new float[16];
        float bearingToNorth = 0;
        double helpRad;

        //get the values of the gravity Sensor
        if(event.sensor == gravitySensor){
            if(event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
                gravity = event.values.clone();
            }
        }
        //get the values of the earth magnetic sensor
        if(event.sensor == geoMagneticSensor){
            //if(event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
                geomagnetic = event.values.clone();
            //}
        }
        if(gravity != null && geomagnetic != null) {
            if (mSensorManager.getRotationMatrix(rotationR, rotationI, gravity, geomagnetic)) {

                mSensorManager.getOrientation(rotationR, orientation);
                //helper variable needed to deal with conversion between double and float
                helpRad = orientation[0];
                helpRad = Math.toDegrees(helpRad);

                bearingToNorth = (float) helpRad;
            } else {
                Log.e(TAG, "not able to get orientation");
            }

            bearing = bearingToNorth - bearingToDestination;

            Log.i(TAG,"Bearing to North" + String.valueOf(bearingToNorth));
            Log.i(TAG,"Bearing to Destination" + String.valueOf(bearingToDestination));

            RotateAnimation ra = new RotateAnimation(
                    imageDegree,
                    -bearing,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);

            ra.setDuration(210);

            ra.setFillAfter(true);

            arrow.startAnimation(ra);
            imageDegree = -bearing;
        }
    }

    /*this function will be called when the google api client is connected*/
    public void onConnected(Bundle dataBundle){
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

    }
    /*this function will be called when the connection to the google api client is suspended*/
    public void onConnectionSuspended(int cause){
        Log.e(TAG, "Connection Suspended");

    }

    /*this function tests if the watch has a GPS receiver*/
    public boolean hasGPS(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    @Override
    public void onPeerConnected(Node node) {
        if(!hasGPS()) {
            //if the system has no GPS receiver remove the notification
            //that the system cannot take placmarks and enable the save position button
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.detach(cardFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onPeerDisconnected(Node node) {
        if(!hasGPS()) {
            //if the system has no GPS receiver show the notification
            //that the system cannot take placmarks and disable the save position button
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.attach(cardFragment);
            fragmentTransaction.commit();
        }
    }
}
