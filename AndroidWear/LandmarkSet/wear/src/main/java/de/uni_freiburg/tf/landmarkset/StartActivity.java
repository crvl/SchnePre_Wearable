package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

//this class is the entery point for the wearable app
//it shows to buttons to select if someone want to save points or want to find back to a point
public class StartActivity extends Activity {
    private Button savePosButton;
    private Button findBackButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                //get the save Position button
                savePosButton = (Button) stub.findViewById(R.id.button_save_position);
                findBackButton = (Button) stub.findViewById(R.id.button_find_back);
            }
        });
    }

    //Callbackfuncton for the Button "Save Position"
    public void startSavePosition(View view){
        Intent intent = new Intent(this, SavePosition.class);
        startActivity(intent);
    }

    //Callbackfunction for the button "Find Back"
    public void startFindBack(View view){
        Intent intent = new Intent(this, FindBack.class);
        startActivity(intent);
    }
}
