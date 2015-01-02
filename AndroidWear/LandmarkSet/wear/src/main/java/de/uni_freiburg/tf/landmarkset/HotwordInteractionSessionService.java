package de.uni_freiburg.tf.landmarkset;

import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;

/**
 * Created by Georg on 02.01.2015.
 */
public class HotwordInteractionSessionService  extends VoiceInteractionSessionService{

    public VoiceInteractionSession onNewSession(Bundle args){
        return new HotwordInteractionSession(this);
    }
}
