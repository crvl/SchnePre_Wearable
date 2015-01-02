package de.uni_freiburg.tf.landmarkset;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.VoiceInteractionService;
import android.util.Log;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created by Georg on 27.12.2014.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class HotwordInteractionService extends VoiceInteractionService {

    private String savePossitionCommand = "Save Position";
    private String TAG = "Hotword Detection";

    private AlwaysOnHotwordDetector mHotwordDetector;


    private final AlwaysOnHotwordDetector.Callback mHotwordCallback = new AlwaysOnHotwordDetector.Callback() {
        @Override
        public void onAvailabilityChanged(int i) {
            Log.i(TAG, "onAvailabilityChanged");
        }

        @Override
        public void onDetected(AlwaysOnHotwordDetector.EventPayload eventPayload) {
            Log.i(TAG, "onDetected");
        }

        @Override
        public void onError() {
            Log.i(TAG, "onError");
        }

        @Override
        public void onRecognitionPaused() {
            Log.i(TAG, "onRecognitionPaused");
        }

        @Override
        public void onRecognitionResumed() {
            Log.i(TAG, "onRecognitionResumed");
        }
    };

    @Override
    public void onReady(){
        super.onReady();

        mHotwordDetector = createAlwaysOnHotwordDetector(savePossitionCommand,
                Locale.forLanguageTag("en-US"), mHotwordCallback);
    }

    public int onStartCommand(Intent intent, int flags, int startId){


        Bundle args = new Bundle();
        args.putParcelable("intent", new Intent(this, MainActivity.class));
        startSession(args);
        stopSelf(startId);

        return START_NOT_STICKY;
    }

}
