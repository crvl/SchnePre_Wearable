package de.uni_freiburg.tf.landmarkset;

import android.content.Context;
import android.location.Location;

import java.io.File;

/**
 * Created by Georg on 26.11.2014.
 */
public class KmlCreate {

    private static final String defaultfilename = "savedLocations.kml";
    private File kmlData;

    public KmlCreate(String dir, String filename){
        kmlData = new File(dir, filename);


    }

    public KmlCreate(String dir){
        this(dir, defaultfilename);
    }
    /*
    //For this constructor the context of the calling activity must be known
    //Therefor a method must be searched
    public KmlCreate(){
        this(Context.getApplicationContext().getFilesDir(), defaultfilename);
    }
    */
    public void addLocation(Location location){

    }
}
