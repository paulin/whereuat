package com.whereuat.free.activities;

import java.util.Date;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.whereuat.free.GPSService;
import com.whereuat.free.IRemoteGPSService;
import com.whereuat.free.LocationRequestsHolder;
import com.whereuat.free.MessageReceiver;
import com.whereuat.free.R;
import com.pugetworks.database.DbAdapter;
import com.pugetworks.database.LocationRequest;
import com.pugetworks.database.LocationRequestState;
import com.pugetworks.utils.LogConstants;
import com.pugetworks.utils.MultiListAdapter;
import com.pugetworks.utils.PWBaseActivity;

public class WhereMain extends PWBaseActivity {
	
	public static final String FIRST_TIME_KEY = "first time";
	public static final String FROM_NUMBER = "fromnum";
	public static final String LOC_REQUEST_ID = "locationreqid";
	public static final String INSTRUCTIONS = 
				"<h2>The Location Auto-Reply App</h2>" + 
				"<p>ANY OF YOUR contacts that text your phone the phrase %s will get a text reply with a map link to your current position. Read more at <a href=\"http://www.whereu-at.com\">WhereU-At.com</a>. </p>" +
				"<p>Brought to you by Matt Paulin</p>";
	
	private EditText phoneField;
	
	private DbAdapter mDbHelper;	
	private LocationRequestsHolder holder;
    private LayoutInflater mInflater;
    
	// Main adapter that everything is shown through
	private MultiListAdapter adapter; 
	private ListView list;

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{    
	   switch (item.getItemId()) 
	   {        
	      case android.R.id.home:            
	         Intent intent = new Intent(this, InviteFriendsActivity.class);            
	     //    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	         startActivity(intent);            
	         return true;        
	      default:            
	         return super.onOptionsItemSelected(item);    
	   }
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_where_main);
    	// TODO: this is just to test, remove later
        clearPrefs();
        ///
		
		mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);       
		holder = new LocationRequestsHolder(this.getBaseContext(), R.layout.locationrequestlist_row, mInflater, this);	
		
		mDbHelper = new DbAdapter(this);

        TextView magicWord = (TextView)this.findViewById(R.id.main_keyword);
        magicWord.setText(Html.fromHtml(String.format(INSTRUCTIONS, MessageReceiver.MAGIC_WORD)));
        magicWord.setMovementMethod (LinkMovementMethod.getInstance());
        magicWord.setLongClickable(true);
        magicWord.setOnLongClickListener(new OnLongClickListener() {			
			@Override
			public boolean onLongClick(View v) {
				refreshPage();
				return false;
			}
		});
        
        TextView friendsTitle = (TextView)this.findViewById(R.id.main_friends_title);
        friendsTitle.setLongClickable(true);
        friendsTitle.setOnLongClickListener(new OnLongClickListener() {			
			@Override
			public boolean onLongClick(View v) {
//				clearHistory();
				return false;
			}
		});
        
        Button refreshButton = (Button)this.findViewById(R.id.main_refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		refreshPage();
        	}
        });
        
        Button restartGPS = (Button)this.findViewById(R.id.main_restart_service_button);
        restartGPS.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		restartGPS();
        	}
        });

        Button inviteFriendsButton = (Button)this.findViewById(R.id.main_invite_friends_button);
        inviteFriendsButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		inviteFriends();
        	}
        });
        
        phoneField = (EditText)this.findViewById(R.id.main_number_edittext);
        
		// create our list and custom adapter
		adapter = new MultiListAdapter();
		adapter.addAdapter(holder);

		list = (ListView) findViewById(R.id.history_view_custom_list);
		list.setAdapter(adapter);	

    	initService();

    }

	private void clearPrefs() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
		editor.putString(FIRST_TIME_KEY, null);
		editor.commit();
	}
    
    @Override
	protected void onResume() {
		super.onResume();
		if(firstTime())
		{
			inviteFriends();
		}
		Log.v(LogConstants.MATT_TAG, "Resuming...");

		loadData();
	}  
    
    /**
     * if the user logs in for the first time, the method logs this in preferences and returns true. 
     * Otherwise, returns false
     */
	private boolean firstTime() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String firstTime = prefs.getString(FIRST_TIME_KEY, null);
		if(firstTime != null && firstTime.length() > 0)
		{
			return false;
		}else
		{
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(FIRST_TIME_KEY, "user opened the app before");
			editor.commit();
			return true;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseService();
	}
    
    private void fireGPS() {
    	String fromNumber = phoneField.getText().toString();
		
		fromNumber = MessageReceiver.getOnlyNumerics(fromNumber);
		// create a new intent and have the user do something?

		if (fromNumber.length() < 10) {
			Toast.makeText(this, "You must input a 10 diget code " + fromNumber, Toast.LENGTH_LONG).show();			
		} else {
			//Create a new location request
			LocationRequest request = new LocationRequest();
			request.setLocReqDate(new Date());
			request.setReqFrom(fromNumber);
			request.setLocationState(LocationRequestState.NEW);
			request.setChanged(true);
			
			//Add it to the database
			DbAdapter mDbHelper =  new DbAdapter(this);

			mDbHelper.open();
			mDbHelper.saveLocationRequest(request);
			mDbHelper.close();

			Intent i = new Intent();
			i.setClass(this, GPSService.class);
			this.startService(i);

			Log.i(LogConstants.MATT_TAG, "Started the GPS Receiver");
			Toast.makeText(this, "Sending location to " + fromNumber, Toast.LENGTH_LONG).show();			
		}
    }
    
    private void refreshPage() {
    	loadData();
//		String fromNumber = "12067079367";
//		String msg1 = "Here's my location: http://bit.ly/EX2/ (sent via whereU@)";
//		String msg2 = "Here's my location: http://bitly/EX2/ (sent via whereU@)";
//
//		SmsManager sms = SmsManager.getDefault();
//		Log.v(LogConstants.MATT_TAG, "GPS Sending now");
//		sms.sendTextMessage(fromNumber, null, msg1, null, null);
//		sms.sendTextMessage(fromNumber, null, msg2, null, null);
    	
//		SmsManager sms = SmsManager.getDefault();
//		sms.sendTextMessage("12067079367", null, "Testing", null, null);
//		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + "12067079367"));
//		intent.putExtra("sms_body", "Testing 2");
//		startActivity(intent);
    }
    
    private void inviteFriends() {
 		Intent inviteFriendsIntent = new Intent(WhereMain.this, InviteFriendsActivity.class);
 		this.startActivity(inviteFriendsIntent);
 		
     }

    private void restartGPS() {
		//Fire up the service
		try {
			resentText();
		} catch (RemoteException e) {
			Log.e(LogConstants.MATT_TAG, "Started the GPS Receiver");
		}
    }
    
    private void clearHistory() {
		mDbHelper.open();
		mDbHelper.deleteAllLocationRequests();
		mDbHelper.close();
		loadData();
    }
    
	@Override
	public boolean usesGPS() {
		return false;
	}    
	
    private void loadData() {
    	holder.clear();
		mDbHelper.open();
		holder.addRequests(mDbHelper.fetchAllLocationRequests());
		mDbHelper.close();
		
		list.invalidateViews();  //Magic line of code that refreshes all of the views in this list


    }
    
	//=================== Service related code
	private IRemoteGPSService service;
    private RemoteGPSServiceConnection conn;
    
    class RemoteGPSServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			service = IRemoteGPSService.Stub
					.asInterface((IBinder) boundService);
			Log.d(LogConstants.MATT_TAG, "GPSService onServiceConnected");

		}

		public void onServiceDisconnected(ComponentName className) {
			service = null;
			Log.d(LogConstants.MATT_TAG, "GPSService onServiceDisconnected");
		}
    };    
    
   
	private void initService() {
		conn = new RemoteGPSServiceConnection();
		Intent i = new Intent(this, GPSService.class);
		bindService(i, conn, Context.BIND_AUTO_CREATE);		
	}

	private void releaseService() {		
		unbindService(conn);
		conn = null;
	}

	private int resentText() throws RemoteException {
		if (service == null)
			Log.v(LogConstants.MATT_TAG,"Service not available");
		else {
			try {
				return service.resentTexts();
			} catch (DeadObjectException ex) {
				Log.v(LogConstants.MATT_TAG,"Service invocation error");
			}
		}
		return 0;
	}

}
