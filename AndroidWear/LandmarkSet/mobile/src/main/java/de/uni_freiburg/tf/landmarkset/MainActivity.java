package de.uni_freiburg.tf.landmarkset;

import android.app.Activity;
import android.location.Location;
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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener,
        MessageApi.MessageListener


{


    private GoogleApiClient wearGoogleApiClient;

    private Activity activity;

    private Location destination;

    private GoogleMap map;

    private final String TAG = "Landmark mobile App";
    private final String deletePath = "/deleteFile";
    private final String syncPath = "/syncFile";
    private final String newDataPath  = "/newData";
    private final String newDestination = "/destChange";
    private String wearNode;

    private ArrayList<Location> savedPlaces;

    //this function is called when the delete button is pressed
    public void delete_data(View view){

        Log.e(TAG,"Delete Data Button pressed");
        sendMessage(deletePath, null);

    }

    //this function is called when the sync button is pressed
    public void sync_data(View view){

        Log.e(TAG, "Sync Data Button pressed");
        sendMessage(syncPath, null);
    }

    //sends a destination to the wearable device
    //the destination will be used for "Find Back"
    public void sendDestinationToWear(Location destLocation){
        ByteBuffer bb = ByteBuffer.allocate(28);
        bb.putDouble(destLocation.getLatitude());
        bb.putDouble(destLocation.getLongitude());
        bb.putDouble(destLocation.getAltitude());
        bb.putFloat(destLocation.getBearing());

        sendMessage(newDestination, bb.array());
    }
    //this function gets the points from a kml file and save them in a list
    //fileName: gives the location of the kml file
    //locationList: is the list where the points are saved to
    public ArrayList<Location> getPlacesFromKML(String fileName, ArrayList<Location> locationList){
        //List<Location> locationList;
        File kmlFile;
        FileReader inputReader;
        XmlPullParserFactory factory;
        XmlPullParser parser;
        int eventType;

        locationList.clear();

        if(isExternalStorageWritable()) {
            kmlFile = new File(Environment.getExternalStorageDirectory(), fileName);
            if (!kmlFile.exists()) {
                //file does not exist, so we can not get points out of it
                return null;
            }
            try {
                inputReader = new FileReader(kmlFile);
                factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                parser = factory.newPullParser();

                parser.setInput(inputReader);
                eventType = parser.getEventType();

                //parse the document until the end
                while (eventType != XmlPullParser.END_DOCUMENT){
                    if(eventType == XmlPullParser.START_TAG){
                        //if a placemark is found
                        if(parser.getName().equals("Placemark")){
                            eventType = parser.next();
                            //searching for Point tag in Placemark
                            while (!(eventType == XmlPullParser.START_TAG
                                    && parser.getName().equals("Point"))){
                                if(eventType ==XmlPullParser.END_DOCUMENT){
                                    return null;
                                }
                                eventType = parser.next();
                            }
                            eventType = parser.next();
                            //test if the KML file is well formed and have
                            //the coordinates right after the Point tag
                            while(eventType != XmlPullParser.START_TAG){
                                eventType = parser.next();
                            }

                            if(eventType == XmlPullParser.START_TAG
                                    && parser.getName().equals("coordinates")){
                                eventType = parser.next();
                                if(eventType == XmlPullParser.TEXT){
                                    //decode the string and add the point to the list
                                    locationList.add(decodeCoordinatesString(parser.getText()));
                                }
                            }
                        }
                    }
                    eventType = parser.next();
                }

            }catch (XmlPullParserException e){
                Log.e(TAG, "Parser Exeption");
                return null;
            }catch (IOException e){
                Log.e(TAG, "Parser IO Exeption");
                return null;
            }
        }
        return locationList;
    }

    //this function decode the string from the kml file to a Location object
    //coordinateString: is the String from the kml file
    //returns the decoded Location
    private Location decodeCoordinatesString(String coordinateString) {
        Location location;
        int firstSeparatorPos;
        int secondSeparatorPos;
        int separator = ',';
        String lon;
        String lat;
        String alt;

        location = new Location("From KML File");

        firstSeparatorPos = coordinateString.indexOf(separator);
        secondSeparatorPos = coordinateString.indexOf(separator, firstSeparatorPos + 1);
        lon = coordinateString.substring(0, firstSeparatorPos);
        lat = coordinateString.substring(firstSeparatorPos + 1, secondSeparatorPos);
        alt = coordinateString.substring(secondSeparatorPos + 1);

        location.setLongitude(Double.valueOf(lon));
        location.setLatitude(Double.valueOf(lat));
        location.setAltitude(Double.valueOf(alt));

        return location;
    }

    //this function is called by the system when a message is received
    public void onMessageReceived(MessageEvent messageEvent){
        if(messageEvent.getPath().equals(newDataPath)){
            sendMessage(syncPath, null);
        }
    }

    //with this function a message can be send to the wearable
    //path: with this string the wearable knows which message was send
    //payload: with this byte array data can be send with the message
    private void sendMessage(final String path, final byte[] payload){
        new Thread(new Runnable(){
            public void run(){
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.
                        getConnectedNodes(wearGoogleApiClient).await();

                for(Node node : nodes.getNodes()){
                    MessageApi.SendMessageResult result = Wearable.MessageApi.
                            sendMessage(wearGoogleApiClient,node.getId(),path, payload).await();
                }
            }
        }).start();
    }

    //This function is called by the system when the GoogleApiClient is connected
    public void onConnected(Bundle dataBundle){
        Wearable.DataApi.addListener(wearGoogleApiClient, this);
        Wearable.MessageApi.addListener(wearGoogleApiClient, this);

        //only for testing
        sendDestinationToWear(destination);
    }

    //This function is called by the system when the GoogleApiClient is suspended by the system
    public void onConnectionSuspended(int i){

    }

    //This function is called by the system when the connection to the GoogleApiClient failed
    public void onConnectionFailed(ConnectionResult connectionResult){
        Log.e(TAG, "API connection failer");
    }

    //This function is called by the system when there are new data in the asset from the wearable
    public void onDataChanged(DataEventBuffer dataEvents){

        Log.e(TAG,"onDataChange was called");
        for(DataEvent event : dataEvents){
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/kmlFile")){
                Log.e(TAG,"Events: " + event.getDataItem().getAssets().get("Positions"));
                DataItemAsset dia = event.getDataItem().getAssets().get("Positions");
                loadKmlFileFromAsset(dia);

                getPlacesFromKML("landmarks" + File.separator + "landmark.kml", savedPlaces);
                Log.i(TAG, "Extracted " + savedPlaces.size() + " Points");

                activity.runOnUiThread(new Runnable() {
                    public void run(){
                        Toast.makeText(activity, "New Data received", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    //with this function we decode the data send from the wearable to the watch
    //and save them to the external storage
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

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        map.addMarker(new MarkerOptions().position(new LatLng(48.01262, 7.83504)));

        activity = this;

        wearGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        destination = new Location("MobileApp");

        //should be replaced by clicking on a marker on the map
        destination.setLatitude(48.01262);
        destination.setLongitude(7.83504);
        destination.setAltitude(300);

        savedPlaces = new ArrayList<>();

        getPlacesFromKML("landmarks" + File.separator + "landmark.kml", savedPlaces);
        Log.i(TAG, "Extracted " + savedPlaces.size() + " Points");
    }

    protected void onPause(){
        super.onPause();
        Wearable.DataApi.removeListener(wearGoogleApiClient, this);
        Wearable.MessageApi.removeListener(wearGoogleApiClient, this);
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


