package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
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
        LandmarkServiceCallbacks{

    private TextView distanceText;
    private ImageView arrow;
    private String TAG = "Find Back APP";

    private LandmarkService mService;
    private boolean mBound = false;
    private final FindBack findBack = this;

    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private CardFragment cardFragment;

    private float imageDegree;


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

        imageDegree = 0;

        //prevent the screen from turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    protected void onStart(){
        super.onStart();
        Intent intent = new Intent(this, LandmarkService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void onResume(){
        super.onResume();
    }

    protected void onPause(){
        super.onPause();
    }

    protected void onStop(){
        super.onStop();
        if(mBound){
            unbindService(mConnection);
            mBound = false;
        }
    }


    /*this function tests if the watch has a GPS receiver*/
    public boolean hasGPS(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }


    @Override
    public void onConnectionChange(boolean connected) {
        if(!hasGPS()) {
            if (connected) {
                //if the system has no GPS receiver remove the notification
                //that the system cannot take placmarks and enable the save position button
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.detach(cardFragment);
                fragmentTransaction.commit();
            } else {
                //if the system has no GPS receiver show the notification
                //that the system cannot take placmarks and disable the save position button
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.attach(cardFragment);
                fragmentTransaction.commit();
            }
        }
    }


    @Override
    public void onRelativeLocationChange(float dist, float bearingToDestination) {
        float bearing;
        float[] bearingToNorth = new float[1];
        int dist_int;


        dist_int = (int) dist;
        distanceText.setText(String.valueOf(dist_int) + "m");

        mService.getAbsOrientation(bearingToNorth);

        bearing = bearingToNorth[0] - bearingToDestination;

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

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LandmarkService.LocalBinder binder = (LandmarkService.LocalBinder) service;
            mService = binder.getService();

            mService.setLandmarkCallbacks(findBack);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
}
