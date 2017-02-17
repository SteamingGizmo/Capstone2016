package com.example.james.trackmylocation;

import android.app.IntentService;
import android.content.Intent;


/**
 * Created by James on 2/16/2017.
 */

public class transmitlocation_background_service extends IntentService {

    public transmitlocation_background_service(){
        super("Location_Transmistion_Service");
    }
    @Override
    protected void onHandleIntent(Intent workIntent){

    }


}
