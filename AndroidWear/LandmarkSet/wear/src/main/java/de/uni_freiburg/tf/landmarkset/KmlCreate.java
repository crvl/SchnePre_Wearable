package de.uni_freiburg.tf.landmarkset;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 * Created by Georg on 26.11.2014.
 */
public class KmlCreate {

    private static final String defaultfilename = "savedLocations.kml";
    private static final String TAG = "KML Writer";
    private File kmlData;
    private File parentFile;




    public KmlCreate(File dir, String filename){
        kmlData = new File(dir, filename);
        parentFile = dir;
        if (!kmlData.exists()){
            try {
                kmlData.createNewFile();
            }catch (IOException e){
                Log.e(TAG,"could not create file");
            }

        }
        initKmlFile();


    }

    public KmlCreate(File dir){
        this(dir, defaultfilename);
    }
    /*
    //For this constructor the context of the calling activity must be known
    //Therefor a method must be searched
    public KmlCreate(){
        this(Context.getApplicationContext().getFilesDir(), defaultfilename);
    }
    */

    public void initKmlFile(){
        PrintWriter kmlWriter;
        FileReader kmlReader;
        BufferedReader kmlBuffer;

        try {
            kmlWriter = new PrintWriter(kmlData);
            kmlReader = new FileReader(kmlData);
            kmlBuffer = new BufferedReader(kmlReader);
            //if the file is empty write the skeleton of a kml file
            if(kmlBuffer.readLine() == null){
                initKmlData(kmlWriter);
            }
            kmlBuffer.close();
            kmlWriter.flush();
            kmlWriter.close();
        }
        catch (IOException e){
            Log.e(TAG,"KML File can not be opened");
        }


    }

    private void initKmlData(PrintWriter writer) {
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
        writer.println("<Document>");
        writer.println("</Document>");
        writer.println("</kml>");
    }

    public void addLocation(Location location){

        PrintWriter kmlWriter;
        FileReader kmlReader;
        BufferedReader kmlBuffer;
        File tempKml;

        tempKml = new File(parentFile, "tempFile.kml");

        String[] Placemark = createPlacemark(location);
        String tempString;

        try {
            kmlWriter = new PrintWriter(tempKml);
            kmlReader = new FileReader(kmlData);
            kmlBuffer = new BufferedReader(kmlReader);

            //write header to temp file
            for (int i = 0; i < 3; i++) {
                kmlWriter.println(kmlBuffer.readLine());
            }
            //write new placemark to temp file
            for(String s: Placemark){
                kmlWriter.println(s);
            }
            //write the rest of the orginal kml file to the temp file
            while ((tempString = kmlBuffer.readLine()) != null) {
                kmlWriter.println(tempString);
            }

            //overwrite kml data file with temp file
            kmlWriter.flush();
            kmlBuffer.close();
            kmlReader.close();

            if(!(tempKml.renameTo(kmlData))){
                Log.e(TAG,"Rename temp file failed");
            }

        }catch (IOException e){
            Log.e(TAG, "Write placemark failed");
        }




    }

    private String [] createPlacemark (Location location){
        String[] Placemark = new String[10];
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        Placemark[0] = "<Placemark>";
        //adding name to Placemark
        Placemark[1] = "<name>Point fixed on " + dateFormat.format(location.getTime()) + " " +
                timeFormat.format(location.getTime()) + "</name>";
        //adding description to Placemark
        Placemark[2] = "<description>This Point was set by " +
                "the Application Landmark Wear and has a accuracy of: " +
                String.valueOf(location.getAccuracy()) + "m </description>";
        //adding a timestamp to Placemark
        Placemark[3] = "<TimeStamp>";
        Placemark[4] = "<when>" + dateFormat.format(location.getTime()) + "T" +
                timeFormat.format(location.getTime()) + "Z" + "</when>";
        Placemark[5] = "</TimeStamp>";
        //adding the point to Placemark
        Placemark[6] = "<Point>";
        Placemark[7] = "<coordinates>" + String.valueOf(location.getLongitude()) + "," +
                String.valueOf(location.getLatitude()) + "," +
                String.valueOf(location.getAltitude()) +
                "</coordinates>";
        Placemark[8] = "</Point>";
        //end Placemark
        Placemark[9] = "</Placemark>";

        Log.e(TAG, Arrays.toString(Placemark));


        return Placemark;

    }

    public File getFile(){
        return kmlData;
    }
}
