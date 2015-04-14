package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

//import com.google.android.gms.common.api.Result;


public class SavePosition extends Activity implements LandmarkServiceCallbacks{

    private SavePosition mainActivity = this;


    private LandmarkService mService;
    private boolean mBound = false;

    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private CardFragment cardFragmentNoGPS;
    private CardFragment cardFragmentCalibrationNedded;

    private CircledImageView saveGpsButton;

    private final String TAG = "MyWearActivity";



    /*this function will be called when the connection to the mobile phone changes*/
    public void onConnectionChange(boolean connected){

        if(!hasGPS()) {
            if (connected) {
                //if the system has no GPS receiver remove the notification
                //that the system cannot take placmarks and enable the save position button
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.detach(cardFragmentNoGPS);
                fragmentTransaction.commit();
                saveGpsButton.setClickable(true);
            } else {
                //if the system has no GPS receiver show the notification
                //that the system cannot take placmarks and disable the save position button
                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.attach(cardFragmentNoGPS);
                fragmentTransaction.commit();
                saveGpsButton.setClickable(false);
            }
        }
    }

    //this function is needed to implement the LandmarkServiceCallbacks,
    // but in this context it is not used
    public void onRelativeLocationChange(float dist, float bearing){

    }

    //this function is called, when the state of the reliability of a sensor has changed.
    //in this context it is used to show a information that a recalibration is needed.
    public void onCalibrationChange(boolean calibrated, int accuray){
        if(calibrated) {
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.detach(cardFragmentCalibrationNedded);
            fragmentTransaction.commit();
            saveGpsButton.setClickable(true);
        }else{
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.attach(cardFragmentCalibrationNedded);
            fragmentTransaction.commit();
            saveGpsButton.setClickable(false);
        }
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
        cardFragmentNoGPS = CardFragment.create("No GPS", "The watch has no connection " +
                "to the mobile phone and no GPS receiver!");
        cardFragmentCalibrationNedded = CardFragment.create("Calibration Needed",
                "Calibration is needed! Make the usual eight pattern!");
        fragmentTransaction.add(R.id.watch_view_stub, cardFragmentNoGPS);
        fragmentTransaction.add(R.id.watch_view_stub, cardFragmentCalibrationNedded);
        fragmentTransaction.detach(cardFragmentNoGPS);
        fragmentTransaction.detach(cardFragmentCalibrationNedded);
        fragmentTransaction.commit();

        //prevent the screen from turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    /*this function will be called by the system when restarting the app without destroying it before*/
    public void onStart(){
        super.onStart();
        Intent intent = new Intent(this, LandmarkService.class);
        intent.setAction("/savePos");
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /*this function will also called from the system on app start and
    when the app come to foreground again*/
    protected void onResume(){
        super.onResume();
    }
    /*this function will be called when the app go to background*/
    protected void onPause(){
        super.onPause();
    }


    /*this function will be called on app close by the system*/
    public void onStop(){
        super.onStop();
        if(mBound){
            unbindService(mConnection);
            mBound = false;
        }
    }

    /*this function will be called by the system on destroying the app*/
    public void onDestroy(){
        super.onDestroy();
    }

    /*this function will be called when the button of this activity is pressed*/
    public void savePosButton (View view){



        Log.e(TAG, "Save Position Button is pressed");

        Vibrator vibe = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        //test if the mobile phone is connected
        if(!mService.getMobilConnection()){
            Log.e(TAG, "Mobil is not connected");

            if(!hasGPS()) {
                Log.e(TAG, "The device can no get its Position alone" +
                        " because it has no GPS receiver");

                fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.attach(cardFragmentNoGPS);
                fragmentTransaction.commit();

                saveGpsButton.setClickable(false);

                return;
            }
        }

        //show a small message that the position was saved
        Toast.makeText(this, "Position Saved", Toast.LENGTH_SHORT).show();

        //get the latest location
        mService.savePosition();


        //let the watch vibrate for 250ms
        if(vibe.hasVibrator()){
            vibe.vibrate(250);
        }
    }

    /*this function tests if the watch has a GPS receiver*/
    public boolean hasGPS(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LandmarkService.LocalBinder binder = (LandmarkService.LocalBinder) service;
            mService = binder.getService();

            mService.setLandmarkCallbacks(mainActivity);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

}

