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

    public void startSavePosition(View view){
        Intent intent = new Intent(this, SavePosition.class);
        startActivity(intent);
    }

    public void startFindBack(View view){
        Intent intent = new Intent(this, FindBack.class);
        startActivity(intent);
    }
}
