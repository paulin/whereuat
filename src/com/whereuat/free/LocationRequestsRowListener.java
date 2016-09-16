package com.whereuat.free;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.whereuat.free.activities.LocationRequestDetails;
import com.whereuat.free.activities.WhereMain;
import com.pugetworks.database.LocationRequest;
import com.pugetworks.utils.LogConstants;

public class LocationRequestsRowListener implements OnClickListener{    
	private static final String TAG = "LocationRequestsRowListener";
    private int mPosition;
    private LocationRequestsHolder holder;
    private Activity activity;
    
    LocationRequestsRowListener(int position, LocationRequestsHolder holder, Activity activity){
            mPosition = position;
            this.holder = holder;
            this.activity = activity;
    }
    @Override
    public void onClick(View arg0) {
        LocationRequest requests = (LocationRequest)holder.getItem(mPosition);

        Log.v(LogConstants.MATT_TAG, "Location Request [" + requests + "] pressed");
        
        //Need to decide what I want this todo when you click
        Intent i = new Intent(activity, LocationRequestDetails.class);
        i.putExtra(WhereMain.LOC_REQUEST_ID, requests.getId());
        activity.startActivity(i);
    }               
}    

