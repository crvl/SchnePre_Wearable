package de.uni_freiburg.tf.landmarkset;

import android.location.Location;

/**
 * Created by Georg on 11.12.2014.
 */

public interface LandmarkServiceCallbacks{
    void onConnectionChange(boolean connected);

    void onRelativeLocationChange(float dist, float bearing);
}
