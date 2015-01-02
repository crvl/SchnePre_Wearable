package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class StartActivity extends Activity {

    private String TAG = "START Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    protected void onStart(){
        super.onStart();
        Log.i(TAG, "Start called");
        startService(new Intent(StartActivity.this, HotwordInteractionService.class));


    }

    protected void onStop(){
        super.onStop();
        Log.i(TAG, "Stop Called");
        //stopService(new Intent(StartActivity.this, HotwordInteractionService.class));
    }
}
