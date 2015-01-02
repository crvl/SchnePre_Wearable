package de.uni_freiburg.tf.landmarkset;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;

/**
 * Created by Georg on 02.01.2015.
 */
public class HotwordInteractionSession extends VoiceInteractionSession {
    public HotwordInteractionSession(Context context) {
        super(context);
    }

    private Intent mStartIntent;
    private final String TAG = "HotwordInteractionSession";

    @Override
    public void onCreate(Bundle args){
        super.onCreate(args);
        //showWindow();
        mStartIntent = args.getParcelable("intent");
    }

    public boolean[] onGetSupportedCommands(Caller caller, String[] commands) {
            return new boolean[commands.length];
        }

        @Override
        public void onConfirm(Caller caller, Request request, CharSequence prompt, Bundle extras) {
            Log.i(TAG, "onConfirm: prompt=" + prompt + " extras=" + extras);
            //mText.setText(prompt);
            //mStartButton.setText("Confirm");
            //mPendingRequest = request;
            //mState = STATE_CONFIRM;
            //updateState();
        }

        @Override
        public void onCompleteVoice(Caller caller, Request request, CharSequence message, Bundle extras) {
            Log.i(TAG, "onCompleteVoice: message=" + message + " extras=" + extras);
            //mText.setText(message);
            //mPendingRequest = request;
            //mState = STATE_COMPLETE_VOICE;
            //updateState();
        }

        @Override
        public void onAbortVoice(Caller caller, VoiceInteractionSession.Request request, CharSequence message, Bundle extras) {
            Log.i(TAG, "onAbortVoice: message=" + message + " extras=" + extras);
            mText.setText(message);
            mPendingRequest = request;
            mState = STATE_ABORT_VOICE;
            updateState();
        }

        @Override
        public void onCommand(Caller caller, VoiceInteractionSession.Request request, String command, Bundle extras) {
            Log.i(TAG, "onCommand: command=" + command + " extras=" + extras);
            mText.setText("Command: " + command);
            mStartButton.setText("Finish Command");
            mPendingRequest = request;
            mState = STATE_COMMAND;
            updateState();
        }

        @Override
        public void onCancel(Request request) {
            Log.i(TAG, "onCancel");
            request.sendCancelResult();
        }


}
