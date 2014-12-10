package de.uni_freiburg.tf.landmarkset;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener


{


    private GoogleApiClient wearGoogleApiClient;

    private final String TAG = "Landmark mobile App";
    private final String deletePath = "/deleteFile";
    private final String syncPath = "/syncFile";
    private String wearNode;

    public void delete_data(View view){

        Log.e(TAG,"Delete Data Button pressed");
        sendMessage(deletePath);

    }

    public void sync_data(View view){

        Log.e(TAG, "Sync Data Button pressed");
        sendMessage(syncPath);
    }

    private void sendMessage(final String path){
        new Thread(new Runnable(){
            public void run(){
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.
                        getConnectedNodes(wearGoogleApiClient).await();

                for(Node node : nodes.getNodes()){
                    MessageApi.SendMessageResult result = Wearable.MessageApi.
                            sendMessage(wearGoogleApiClient,node.getId(),path, null).await();
                }
            }
        }).start();
    }

    public void onConnected(Bundle dataBundle){
        Wearable.DataApi.addListener(wearGoogleApiClient, this);

    }

    public void onConnectionSuspended(int i){

    }

    public void onConnectionFailed(ConnectionResult connectionResult){
        Log.e(TAG, "API connection failer");
    }

    public void onDataChanged(DataEventBuffer dataEvents){
        Log.e(TAG,"onDataChange was called");
        ParcelFileDescriptor pfd;
        for(DataEvent event : dataEvents){
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/kmlFile")){
                Log.e(TAG,"Events: " + event.getDataItem().getAssets().get("Positions"));
                DataItemAsset dia = event.getDataItem().getAssets().get("Positions");
                loadKmlFileFromAsset(dia);
            }
        }
    }

    public File loadKmlFileFromAsset (DataItemAsset dia){

        File receivedKmlFile;
        File receivedKmlDir;
        PrintWriter printWriter;

        if (dia == null){
            throw new IllegalArgumentException("Asset must be non-null");
        }

        ConnectionResult result = wearGoogleApiClient.blockingConnect(500, TimeUnit.MILLISECONDS);

        if (!result.isSuccess()){
            return null;
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(wearGoogleApiClient, dia)
                .await().getInputStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(assetInputStream));
        String line;
        try {

            if(isExternalStorageWritable()){
                receivedKmlDir = new File(Environment.getExternalStorageDirectory(),"landmarks");
                if(!receivedKmlDir.exists()){
                    receivedKmlDir.mkdir();
                }
                receivedKmlFile = new File(receivedKmlDir,"landmark.kml");

                if (!receivedKmlFile.exists()) {
                    if (!receivedKmlFile.createNewFile()) {
                        Log.e(TAG, "Create kml file failed");
                    }
                }

                Log.e(TAG,"File Path: " + receivedKmlFile.getAbsolutePath());

                printWriter = new PrintWriter(receivedKmlFile);

                while ((line = br.readLine()) != null) {
                    Log.e(TAG, line);

                    printWriter.println(line);
                }
                printWriter.flush();
                printWriter.close();
                br.close();
                assetInputStream.close();

                return receivedKmlFile;
            }
            else {
                Log.e(TAG, "Can not write to file because external storage is unavailable");
            }

        }catch (IOException e){
            Log.e(TAG,"Read transferred file failed" + e.getMessage());
        }
        return null;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wearGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

    }

    protected void onPause(){
        super.onPause();
        Wearable.DataApi.removeListener(wearGoogleApiClient, this);
        wearGoogleApiClient.disconnect();
    }

    protected void onResume(){
        super.onResume();
        wearGoogleApiClient.connect();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}


