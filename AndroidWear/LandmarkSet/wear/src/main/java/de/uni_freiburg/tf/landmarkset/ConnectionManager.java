package de.uni_freiburg.tf.landmarkset;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;

/**
 * Created by Georg on 11.12.2014.
 */
public class ConnectionManager extends WearableListenerService {



    private final String TAG = "Connection Manager";
    private final String deletePath = "/deleteFile";
    private final String syncPath = "/syncFile";

    private boolean firstLaunch = true;

    //have to be static, otherwise the kmlFile and mGoogleApiClient will be null in a onMessageReceive
    private static GoogleApiClient mGoogleApiClient;
    private static KmlCreate kmlFile;
    private static boolean mobileConnected;
    private static MainActivity owner;

    public ConnectionManager(GoogleApiClient googleApiClient, KmlCreate kmlCreate, MainActivity caller){
        mGoogleApiClient = googleApiClient;
        kmlFile = kmlCreate;
        owner = caller;
    }

    public ConnectionManager(){}

    public void setApiClient(GoogleApiClient apiClient){
        mGoogleApiClient = apiClient;
    }

    public void setKmlFile (KmlCreate kmlCreate){
        kmlFile = kmlCreate;
    }

    public void onPeerDisconnected(Node peer){
        super.onPeerDisconnected(peer);
        Log.e(TAG, "Mobile disconnected from watch");
        mobileConnected = false;

        owner.onConnectionChange(false);
    }

    public void onPeerConnected(Node peer){
        super.onPeerConnected(peer);
        Log.e(TAG, "Mobile connected to watch");
        mobileConnected = true;

        owner.onConnectionChange(true);
    }

    public void onMessageReceived(MessageEvent messageEvent){
        super.onMessageReceived(messageEvent);
        if(messageEvent.getPath().equals(deletePath)){
            Log.e(TAG,"Delete Message Received");
            kmlFile.resetKmlFile();
            syncWithMobil();

        }
        if(messageEvent.getPath().equals(syncPath)){
            Log.e(TAG,"Sync Message Received");
            syncWithMobil();
        }
    }

    private void syncWithMobil(){
        try {
            ParcelFileDescriptor kmlPFD = ParcelFileDescriptor.open(kmlFile.getFile(),
                    ParcelFileDescriptor.MODE_READ_ONLY);
            Asset kmlAsset = Asset.createFromFd(kmlPFD);
            PutDataRequest request = PutDataRequest.create("/kmlFile");
            request.putAsset("Positions", kmlAsset);
            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        }catch (IOException e){
            Log.e(TAG, "File for asset not found");
        }
    }

    private void setMobileConnected(){
        mobileConnected = true;
    }

    private void setMobileDisconnected(){
        mobileConnected = false;
    }

    //this function must not be called from onCreate, onResume, ect.
    //otherwise the app will be stuck!!!
    public boolean getMobilConnection(){


        if(firstLaunch) {
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


}
