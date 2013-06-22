package com.sscvt;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParserException;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;
import com.coboltforge.slidemenu.SlideMenu;
import com.coboltforge.slidemenu.SlideMenuInterface.OnSlideMenuItemClickListener;

import com.sscvt.helper.AdsDBAdapter;
import com.sscvt.helper.CustomAdapter;
import com.sscvt.helper.EventsDBAdapter;
import com.sscvt.helper.NetworkReceiver;
import com.sscvt.helper.SettingsActivity;
import com.sscvt.helper.SscvtParser;
import com.sscvt.helper.SscvtParser.Item;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Prayer extends SherlockActivity implements
		OnSlideMenuItemClickListener, OnTouchListener {
	private static final String isnrvURL ="http://www.isnrv.org"; 
	private static final int animationDuration = 333;
	private ProgressDialog loadingDialog;
	// The BroadcastReceiver that tracks network connectivity changes.
	public static NetworkReceiver receiver;
	public final String TAG = "Prayer";
	private MenuItem refreshIcon;
	private SlideMenu slidemenu;
	private float downXValue, downYValue;
	private final int ADS = 1;
	private final int EVENTS = 2;
    

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//get selected activity (Ads or Events)
		setContentView(R.layout.prayer);
		View v = (View) findViewById(R.id.prayer_table);
		v.setOnTouchListener(this);
		Log.d(TAG, "onCreate()");
		// Register BroadcastReceiver to track connection changes.
		IntentFilter filter = new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION);
		receiver = new NetworkReceiver(this);
		this.registerReceiver(receiver, filter);

		receiver.updateConnectedFlags();
		
		parsePage();
	}
	

	@Override
	protected void onStart() {
		super.onStart();
		setupActionBar();
	}

	private void parsePage() {
		if (receiver.isConnected()) {
           new parseWebsite().execute("");
        }else { // no internet access
        	showErrorMessage();
        }
	}

	// Displays an error if the app is unable to load content.
	private void showErrorMessage() {
		Toast.makeText(this, R.string.connection_error, Toast.LENGTH_LONG)
				.show();
	}

	// AsyncTask used to download XML feed from stackoverflow.com.
	private class parseWebsite extends
			AsyncTask<String, Void, Elements> {

		@Override
		protected void onPreExecute() {
			startRefreshAnimation();
		}

		@Override
		protected Elements doInBackground(String... urls) {

			try {
				Document doc = Jsoup.connect(isnrvURL).get();
				Element body = doc.body();
				Elements prayerTimes = body.getElementsByClass("prayer-time");
				if(prayerTimes.size() == 11)
					return prayerTimes;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Elements prayerTimes) {
			stopRefreshAnimation();
			if (prayerTimes == null) { // an error occured while parsing
				showErrorMessage();
			}else{
				Toast.makeText(getApplicationContext(),
						getResources().getText(R.string.update_successful),
						Toast.LENGTH_SHORT).show();
				populateTable(prayerTimes);
			}
		}
	}

	private void populateTable(Elements prayerTimes){
		TableLayout prayerTable = (TableLayout) findViewById(R.id.prayer_table);
		prayerTable.removeAllViews();
		int prayerNames[] = {R.string.fajr, R.string.duhr, R.string.asr, R.string.magrib, R.string.isha};
		
		for(int i=0; i<10; i=i+2){
			TableRow tr = (TableRow)getLayoutInflater().inflate(R.layout.prayer_row_style, null);
			TableLayout.LayoutParams parms = new TableLayout.LayoutParams( LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
			float pixelToDp = getResources().getDisplayMetrics().density;
			parms.setMargins(0, (int)(5*pixelToDp), 0, (int)(5*pixelToDp));
			tr.setLayoutParams(parms);
			TextView titleTextView = (TextView)getLayoutInflater().inflate(R.layout.prayer_text_style, null);
			titleTextView.setText(getResources().getString(prayerNames[i/2]));
			titleTextView.setLayoutParams(new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
			TextView athanTextView = (TextView)getLayoutInflater().inflate(R.layout.prayer_text_style, null);
			athanTextView.setText(prayerTimes.get(i).text());
			athanTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			athanTextView.setLayoutParams(new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
			TextView iqamaTextView = (TextView)getLayoutInflater().inflate(R.layout.prayer_text_style, null);
			iqamaTextView.setText(prayerTimes.get(i+1).text());
			iqamaTextView.setLayoutParams(new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
			iqamaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			tr.addView(iqamaTextView);
			tr.addView(athanTextView);
			tr.addView(titleTextView);
			prayerTable.addView(tr);	
		}
	}
	
	// show animation of refresh
	public void startRefreshAnimation() {

		Log.d(TAG, "startRefereshAnimation");
		if (getApplicationContext() != null && refreshIcon != null) {
			/* Attach a rotating ImageView to the refresh item as an ActionView */
			LayoutInflater inflater = (LayoutInflater) getApplicationContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ImageView iv = (ImageView) inflater.inflate(
					R.layout.refresh_action_view, null);

			Animation rotation = AnimationUtils.loadAnimation(
					getApplicationContext(), R.anim.clockwise_refresh);
			rotation.setRepeatCount(Animation.INFINITE);
			iv.startAnimation(rotation);

			refreshIcon.setActionView(iv);
		} else {
			loadingDialog = new ProgressDialog(Prayer.this);
			loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			loadingDialog.setMessage(getResources()
					.getString(R.string.updating));
			loadingDialog.show();
		}
	}

	public void stopRefreshAnimation() {
		if (getApplicationContext() != null && refreshIcon != null) {
			refreshIcon.getActionView().clearAnimation();
			refreshIcon.setActionView(null);
		} else {
			if(loadingDialog!= null)
				loadingDialog.dismiss();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "onConfigurationChanged");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		if (receiver != null) {
			this.unregisterReceiver(receiver);
		}
	}
	
	// Populates the activity's options menu.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	// Handles the user's menu selection.
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			refreshIcon = item;
			parsePage();
			return true;
		case R.id.menu:
			if(!slidemenu.isMenuVisible())
				slidemenu.show();
			else
				slidemenu.hide();
			return true;
		case R.id.settings:
			Intent settingsActivity = new Intent(getBaseContext(),
					SettingsActivity.class);
			startActivity(settingsActivity);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		//setup actionbar view
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		LayoutInflater inflator = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflator.inflate(R.layout.actionbar_view, null);
		((TextView)v.findViewById(R.id.title)).setText(getResources().getString(R.string.prayer_button));
		actionBar.setCustomView(v);
		
		//setup actionbar background
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(
					R.drawable.bar_striped);
			bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			actionBar.setBackgroundDrawable(bg);

			BitmapDrawable bgSplit = (BitmapDrawable) getResources()
					.getDrawable(R.drawable.bar_striped_split_img);
			bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			actionBar.setSplitBackgroundDrawable(bgSplit);
		}
		
		//setup slidemenu
		slidemenu = (SlideMenu) findViewById(R.id.slideMenu);
		slidemenu.init(this, R.menu.slide, this, animationDuration, 7);
	}

	@Override
	public void onSlideMenuItemClick(int itemId) {
		Intent intent = new Intent(getBaseContext(), Ads.class);
		Bundle b = new Bundle();
		switch (itemId) {
		case R.id.events_button:
				b.putInt("activity", EVENTS);
				intent.putExtras(b);
				startActivity(intent); 
			break;
		case R.id.ads_button:
				b.putInt("activity", ADS);
				intent.putExtras(b);
				startActivity(intent);
			break;
		case R.id.gallery_button:
			startActivity(new Intent(getBaseContext(), Gallery.class));
			break;
		case R.id.arrivals_button:
			startActivity(new Intent(getBaseContext(),Arrivals.class)); 
			break;
		case R.id.profile_button:
			break;
		case R.id.holiday_button:
			String holidayUrl = "http://www.ihg.com/holidayinn/hotels/us/en/blacksburg/roabb/hoteldetail?_corpId=100272909&ratePreference=ILIK1";
			Intent holidayIntent = new Intent(Intent.ACTION_VIEW);
			holidayIntent.setData(Uri.parse(holidayUrl));
			startActivity(holidayIntent);
			break;
		case R.id.guide_button:
			String guideUrl = "http://sscvt.org/uploads/3/1/8/7/3187519/blacksburg_va_guide_final_2012.pdf";
			Intent guideIntent = new Intent(Intent.ACTION_VIEW);
			guideIntent.setData(Uri.parse(guideUrl));
			startActivity(guideIntent);
			break;
		case R.id.contact_button:
			startActivity(new Intent(getBaseContext(), Contact.class));
			break;
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		//Log.d(TAG, "onTouch");

		// Get the action that was done on this touch event
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			// store the X, Y value when the user's finger was pressed down
			downXValue = event.getX();
			downYValue = event.getY();
			return true;
		}

		case MotionEvent.ACTION_UP: {
			// Get the X, Y value when the user released his/her finger
			float currentX = event.getX();
			float currentY = event.getY();

			// going forwards: pushing stuff to the left (showing menu)
			if (((downXValue - currentX) > 65)
					&& (Math.abs(downYValue - currentY) < 100) || (downXValue - currentX) > 300) {
				slidemenu.show();
				return true;
			} 
			// going backwards: pushing stuff to the right (hiding menu)
			else if (((downXValue - currentX) < -40)
					&& (Math.abs(downYValue - currentY) < 100) || ((downXValue - currentX) < -250)) {
				slidemenu.hide();
				return true;
			}
			break;
		}
		}

		return false;
	}

}
