package de.uni_freiburg.tf.landmarkset;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;
import java.nio.ByteBuffer;

public class LandmarkService extends Service implements
        MessageApi.MessageListener,
        NodeApi.NodeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener {


    private final String TAG = "Landmark Service";
    private final String deletePath = "/deleteFile";
    private final String syncPath = "/syncFile";
    private final String newData = "/newData";
    private final String newDestination = "/destChange";


    private String action;

    //have to be static, otherwise the kmlFile and mGoogleApiClient will be null in a onMessageReceive
    private static GoogleApiClient mGoogleApiClient;
    private static KmlCreate kmlFile;
    private static boolean mobileConnected;
    private static boolean isApiConnected;

    private LocationRequest locationRequest;

    private boolean isRunning = false;
    private boolean firstLaunch = true;

    private SensorManager mSensorManager;
    private Sensor gravitySensor;
    private Sensor geoMagneticSensor;
    private float[] gravity;
    private float[] geomagnetic;
    private boolean hasMagnetometer;
    private boolean hasAcceleration;

    private final IBinder mBinder = new LocalBinder();
    private LandmarkServiceCallbacks serviceCallbacks;

    private static Location destLocation = new Location("By my Self");;
    private Location actLocation;

    //class used for the client Binder
    public class LocalBinder extends Binder {
        LandmarkService getService() {
            return LandmarkService.this;
        }
    }

    //public LandmarkService(){}

    @Override
    public IBinder onBind(Intent intent) {

        action = intent.getAction();
        //create the google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)

                .build();

        mGoogleApiClient.connect();

        //init the gravity and magnetic sensor
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        geoMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        if (geoMagneticSensor == null) {
            hasMagnetometer = false;
        } else {
            hasMagnetometer = true;
        }

        if (gravitySensor == null) {
            hasAcceleration = false;
        } else {
            hasAcceleration = true;
        }

        if (hasAcceleration && hasMagnetometer) {
            mSensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, geoMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        geomagnetic = new float[3];
        gravity = new float[3];

        //create or open the kml file where to write the placemarks
        kmlFile = new KmlCreate(this.getApplicationContext().getFilesDir());

        isRunning = true;

        //set the location
        //in further use this will be done by receiving a message from the mobile phone
        //destLocation.setLatitude(48.01262);
        //destLocation.setLongitude(7.83504);
        //destLocation.setAltitude(300);

        return mBinder;
    }

    public boolean onUnbind(Intent intent){
        stopSelf();
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        isApiConnected = false;
        isRunning = false;
        if (hasAcceleration && hasMagnetometer) {
            mSensorManager.unregisterListener(this);
            mSensorManager.unregisterListener(this);
        }
    }

    public void setLandmarkCallbacks(LandmarkServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }


    public void onPeerDisconnected(Node peer) {
        //super.onPeerDisconnected(peer);
        if (serviceCallbacks != null) {
            serviceCallbacks.onConnectionChange(false);
        }
        Log.e(TAG, "Mobile disconnected from watch");
        mobileConnected = false;

    }

    public void onPeerConnected(Node peer) {
        //super.onPeerConnected(peer);
        if (serviceCallbacks != null) {
            serviceCallbacks.onConnectionChange(true);
        }
        Log.e(TAG, "Mobile connected to watch");
        mobileConnected = true;
    }


    public void onMessageReceived(MessageEvent messageEvent) {
        //super.onMessageReceived(messageEvent);
        byte[] destBytes;


        /*if (!isApiConnected) {
            mGoogleApiClient.connect();
            while (!isApiConnected) ;
        }*/

        if (messageEvent.getPath().equals(deletePath)) {
            Log.e(TAG, "Delete Message Received");
            kmlFile.resetKmlFile();
            syncWithMobil();

        }
        if (messageEvent.getPath().equals(syncPath)) {
            Log.e(TAG, "Sync Message Received");
            syncWithMobil();
        }

        if (messageEvent.getPath().equals(newDestination)){
            Log.i(TAG, "New destination received");
            destBytes = messageEvent.getData();
            ByteBuffer bb = ByteBuffer.allocate(28);

            bb.put(destBytes);

            destLocation.setLatitude(bb.getDouble(0));
            destLocation.setLongitude(bb.getDouble(8));
            destLocation.setAltitude(bb.getDouble(16));
            destLocation.setBearing(bb.getFloat(24));
            Log.i(TAG, "Destination written");

            //convert Back bytes to location and set it to destination
        }

        /*
        if (!isRunning) {
            mGoogleApiClient.disconnect();
        }*/
    }

    private void syncWithMobil() {
        try {
            ParcelFileDescriptor kmlPFD = ParcelFileDescriptor.open(kmlFile.getFile(),
                    ParcelFileDescriptor.MODE_READ_ONLY);
            Asset kmlAsset = Asset.createFromFd(kmlPFD);
            PutDataRequest request = PutDataRequest.create("/kmlFile");
            request.putAsset("Positions", kmlAsset);
            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        } catch (IOException e) {
            Log.e(TAG, "File for asset not found");
        }
    }

    private void setMobileConnected() {
        mobileConnected = true;
    }

    private void setMobileDisconnected() {
        mobileConnected = false;
    }

    //this function must not be called from onCreate, onResume, ect.
    //otherwise the app will be stuck!!!
    public boolean getMobilConnection() {


        if (firstLaunch) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.
                            getConnectedNodes(mGoogleApiClient).await();

                    if (nodes.getNodes().size() > 0) {
                        setMobileConnected();
                    } else {
                        setMobileDisconnected();
                    }
                }
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Can not get mobile connection status");
            }
            firstLaunch = false;
        }

        return mobileConnected;
    }

    public void sendMessage(final String path) {
        new Thread(new Runnable() {
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.
                        getConnectedNodes(mGoogleApiClient).await();

                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.
                            sendMessage(mGoogleApiClient, node.getId(), path, null).await();
                }
            }
        }).start();
    }

    public boolean getAbsOrientation(float[] bearing) {
        float[] orientation = new float[3];
        float[] rotationR = new float[16];
        float[] rotationI = new float[16];

        double helpRad;

        //if the system has a magnetic and a acceleration sensor set the bearing in the location
        if (hasAcceleration && hasMagnetometer) {
            if (mSensorManager.getRotationMatrix(rotationR, rotationI, gravity, geomagnetic)) {

                mSensorManager.getOrientation(rotationR, orientation);
                //helper variable needed to deal with conversion between double and float
                helpRad = orientation[0];
                helpRad = Math.toDegrees(helpRad);

                bearing[0] = (float) helpRad;

                return true;
            } else {
                Log.e(TAG, "not able to get orientation");
            }
        }
        return false;
    }

    public void savePosition() {
        float[] bearing = new float[1];
        Location location = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (getAbsOrientation(bearing)) {
            location.setBearing(bearing[0]);
        }

        Log.e(TAG, "\nGot location" +
                "\nLatitude:  " + String.valueOf(location.getLatitude()) +
                "\nLongitude:  " + String.valueOf(location.getLongitude()) +
                "\nAccuracy:" + String.valueOf(location.getAccuracy()));


        //add the location to the kmlFile
        kmlFile.addLocation(location);

        sendMessage(newData);
    }

    public Location getDestLocation(){
        return destLocation;
    }

    @Override
    public void onConnected(Bundle bundle) {
        isApiConnected = true;

        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);

        //setup the service to get the location
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2);
        locationRequest.setFastestInterval(2);
        locationRequest.setSmallestDisplacement(2);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Connection Suspended");
        isApiConnected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Faild");
        isApiConnected = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Display the latitude and longitude in the UI
        Log.e(TAG, "Latitude:  " + String.valueOf(location.getLatitude()) +
                "\nLongitude:  " + String.valueOf(location.getLongitude()));

        if(action != null) {
            if(action.equals("/savePos")) {
                //add way point to save position way
                kmlFile.addWayPoint(location, "Save Position");
                Log.i(TAG, "Save Position waypoint writen to file");
            }
            else if(action.equals("/findBack")){
                //add way point to find back way
                kmlFile.addWayPoint(location, "Find Back");
                Log.i(TAG, "Find Back waypoint writen to file");
            }
        }
        actLocation = location;
        if (serviceCallbacks != null) {
            serviceCallbacks.onRelativeLocationChange(location.distanceTo(destLocation),
                    location.bearingTo(destLocation));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //get the values of the gravity Sensor
        if (event.sensor == gravitySensor) {
            if (event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
                gravity = event.values.clone();
            }
        }
        //get the values of the earth magnetic sensor
        if (event.sensor == geoMagneticSensor) {
            if (event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
                geomagnetic = event.values.clone();
            }
        }
        if((event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                || event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)
                && event.sensor == geoMagneticSensor){
            if (serviceCallbacks != null) {
                serviceCallbacks.onCalibrationChange(false, event.accuracy);
            }
        }
        if (serviceCallbacks != null && actLocation != null) {
            serviceCallbacks.onRelativeLocationChange(actLocation.distanceTo(destLocation),
                    actLocation.bearingTo(destLocation));
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if(sensor == geoMagneticSensor) {
            if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                    || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                if (serviceCallbacks != null) {
                    serviceCallbacks.onCalibrationChange(false, accuracy);
                }
            } else {
                if (serviceCallbacks != null) {
                    serviceCallbacks.onCalibrationChange(true, accuracy);
                }
            }
        }
    }
}
