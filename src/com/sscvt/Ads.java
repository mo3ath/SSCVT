/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

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
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

public class Ads extends SherlockActivity implements
		OnSlideMenuItemClickListener, OnTouchListener {
	private static final String AdsURL = "http://sscvt.org/3/feed";
	private static final String EventsURL = "http://sscvt.org/1/feed";
	private static final int animationDuration = 333;
	private ProgressDialog loadingDialog;
	// The BroadcastReceiver that tracks network connectivity changes.
	public static NetworkReceiver receiver;
	public AdsDBAdapter adb;
	public EventsDBAdapter edb;
	public final String TAG = "Ads";
	private MenuItem refreshIcon;
	private SlideMenu slidemenu;
	private float downXValue, downYValue;
	private CustomAdapter listAdapter;
	private final int ADS = 1;
	private final int EVENTS = 2;
	private int currentActivity = ADS;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// get selected activity (Ads or Events)
		Bundle b = getIntent().getExtras();
		if (b != null)
			currentActivity = b.getInt("activity", ADS);

		setContentView(R.layout.ads);
		View v = (View) findViewById(R.id.list_view);
		v.setOnTouchListener(this);
		Log.d(TAG, "onCreate()");
		// Register BroadcastReceiver to track connection changes.
		IntentFilter filter = new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION);
		receiver = new NetworkReceiver(this);
		this.registerReceiver(receiver, filter);

		receiver.updateConnectedFlags();
		boolean isDataBaseEmpty = true;
		if (currentActivity == ADS) {
			adb = new AdsDBAdapter(this);
			adb.open();
			try {
				isDataBaseEmpty = adb.isEmpty();
			} catch (SQLException e) {
				isDataBaseEmpty = true;
			}
			adb.close();
		} else if (currentActivity == EVENTS) {
			edb = new EventsDBAdapter(this);
			edb.open();
			try {
				isDataBaseEmpty = edb.isEmpty();
			} catch (SQLException e) {
				isDataBaseEmpty = true;
			}
			edb.close();
		}

		if (isDataBaseEmpty) {
			parsePage();
		} else {
			new dataBaseTask().execute("");
		}
		setupActionBar();
	}

	@Override
	protected void onStart() {
		super.onStart();

		
	}

	// Uses AsyncTask subclass to download the XML feed from stackoverflow.com.
	// This avoids UI lock up. To prevent network operations from
	// causing a delay that results in a poor user experience, always perform
	// network operations on a separate thread from the UI.
	private void parsePage() {
		if (receiver.isConnected() && currentActivity == ADS) {
			new DownloadXmlTask().execute(AdsURL);
		} else if (receiver.isConnected() && currentActivity == EVENTS) {
			new DownloadXmlTask().execute(EventsURL);
		} else { // no internet access
			showErrorMessage();
		}
	}

	// Displays an error if the app is unable to load content.
	private void showErrorMessage() {
		Toast.makeText(this, R.string.connection_error, Toast.LENGTH_LONG)
				.show();
	}

	// AsyncTask to load local items from database
	private class dataBaseTask extends AsyncTask<String, Void, ArrayList<Item>> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected ArrayList<Item> doInBackground(String... urls) {
			return getItemsFromDB();
		}

		@Override
		protected void onPostExecute(ArrayList<Item> result) {
			populateList(result);
		}
	}

	// AsyncTask used to download XML feed from stackoverflow.com.
	private class DownloadXmlTask extends
			AsyncTask<String, Void, ArrayList<Item>> {

		@Override
		protected void onPreExecute() {
			startRefreshAnimation();
		}

		@Override
		protected ArrayList<Item> doInBackground(String... urls) {
			try {
				return loadXmlFromNetwork(urls[0]);
			} catch (IOException e) {
				// return getResources().getString(R.string.connection_error);
			} catch (XmlPullParserException e) {
				// return getResources().getString(R.string.xml_error);
			}
			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<Item> result) {
			stopRefreshAnimation();
			if (result == null) { // an error occured while parsing
				showErrorMessage();
			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getText(R.string.update_successful),
						Toast.LENGTH_SHORT).show();
				populateList(result);
			}
		}
	}

	// used to initialize and populate the list
	void populateList(final ArrayList<Item> items) {
		ListView List = (ListView) findViewById(R.id.list_view);
		listAdapter = new CustomAdapter(Ads.this, items, List);
		List.setAdapter(listAdapter);
		List.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Item selectedItem = items.get(position);
				// set up dialog
				Dialog dialog = new Dialog(Ads.this);
				dialog.setContentView(R.layout.list_item_webview);
				dialog.setTitle(selectedItem.getTitle());
				dialog.setCancelable(true);
				// set up webview
				WebView myWebView = (WebView) dialog
						.findViewById(R.id.item_webview);
				myWebView.getSettings().setBuiltInZoomControls(true);
				myWebView.getSettings().setDefaultZoom(ZoomDensity.FAR);
				myWebView.getSettings().setLayoutAlgorithm(
						LayoutAlgorithm.SINGLE_COLUMN);
				myWebView.loadData(selectedItem.getContent(),
						"text/html; charset=UTF-8", null);

				/*
				 * Window window = dialog.getWindow();
				 * WindowManager.LayoutParams wlp = window.getAttributes();
				 * 
				 * wlp.gravity = Gravity.RIGHT; wlp.flags &=
				 * ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
				 * window.setAttributes(wlp);
				 */
				dialog.show();
			}
		});
		updateDB(items);
	}

	// Uploads XML from stackoverflow.com, parses it, and combines it with
	// HTML markup. Returns HTML string.
	private ArrayList<Item> loadXmlFromNetwork(String urlString)
			throws XmlPullParserException, IOException {
		InputStream stream = null;
		SscvtParser eventsParser = new SscvtParser();
		ArrayList<Item> items = null;
		Calendar rightNow = Calendar.getInstance();
		DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");

		try {
			stream = downloadUrl(urlString);
			items = eventsParser.parse(stream);
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} catch (XmlPullParserException e) {
			Log.d(TAG, e.getMessage());
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
		return items;
	}

	// Given a string representation of a URL, sets up a connection and gets
	// an input stream.
	private InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		InputStream stream = conn.getInputStream();
		return stream;
	}

	// update database with new items
	void updateDB(ArrayList<Item> items) {
		if (items.size() > 0) {
			if (currentActivity == ADS) {
				adb.open();
				if (adb.isEmpty()) {
					// insert titles-------------------
					for (int i = 0; i < items.size(); i++) {
						adb.insertTitle(items.get(i).getTitle(), items.get(i)
								.getDate(), items.get(i).getContent(), items
								.get(i).getLink());
					}
					Log.d(TAG, "database: inserting stuff");
				} else {
					// update titles---------------------
					for (int i = 0; i < items.size(); i++) {
						adb.updateTitle(i + 1, items.get(i).getTitle(), items
								.get(i).getDate(), items.get(i).getContent(),
								items.get(i).getLink());
					}
					Log.d(TAG, "database: updating stuff");
				}
				// -------------------
				adb.close();
			} else if (currentActivity == EVENTS) {
				edb.open();
				if (edb.isEmpty()) {
					// insert titles-------------------
					for (int i = 0; i < items.size(); i++) {
						edb.insertTitle(items.get(i).getTitle(), items.get(i)
								.getDate(), items.get(i).getContent(), items
								.get(i).getLink());
					}
					Log.d(TAG, "database: inserting stuff");
				} else {
					// update titles---------------------
					for (int i = 0; i < items.size(); i++) {
						edb.updateTitle(i + 1, items.get(i).getTitle(), items
								.get(i).getDate(), items.get(i).getContent(),
								items.get(i).getLink());
					}
					Log.d(TAG, "database: updating stuff");
				}
				// -------------------
				edb.close();
			}

		}
	}

	// get items from database
	ArrayList<Item> getItemsFromDB() {
		ArrayList<Item> items = new ArrayList<Item>();
		Cursor mCursor = null;
		int counter = 1;
		if (currentActivity == ADS) {
			adb.open();
			mCursor = adb.getTitle(counter);
			// get items from database
			while (mCursor.moveToFirst()) {
				items.add(new Item(mCursor.getString(1), mCursor.getString(2),
						mCursor.getString(3), mCursor.getString(4)));
				counter++;
				mCursor = adb.getTitle(counter);
			}
			// -------------------
			Log.d(TAG, "database: finished reading from database");
			adb.close();
		} else if (currentActivity == EVENTS) {
			edb.open();
			mCursor = edb.getTitle(counter);
			// get items from database
			while (mCursor.moveToFirst()) {
				items.add(new Item(mCursor.getString(1), mCursor.getString(2),
						mCursor.getString(3), mCursor.getString(4)));
				counter++;
				mCursor = edb.getTitle(counter);
			}
			// -------------------
			Log.d(TAG, "database: finished reading from database");
			
			edb.close();
		}

		return items;
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
			loadingDialog = new ProgressDialog(Ads.this);
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
			if (loadingDialog != null)
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
			if (!slidemenu.isMenuVisible())
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
		// setup actionbar view
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		LayoutInflater inflator = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflator.inflate(R.layout.actionbar_view, null);
		if(currentActivity == EVENTS){
			((TextView) v.findViewById(R.id.title)).setText(getResources()
					.getString(R.string.events_button));
		}else{
			((TextView) v.findViewById(R.id.title)).setText(getResources()
					.getString(R.string.ads_button));
		}
		actionBar.setCustomView(v);

		// setup actionbar background
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

		// setup slidemenu
		slidemenu = (SlideMenu) findViewById(R.id.slideMenu);
		if (currentActivity == ADS)
			slidemenu.init(this, R.menu.slide, this, animationDuration, 2);
		else if (currentActivity == EVENTS)
			slidemenu.init(this, R.menu.slide, this, animationDuration, 0);
	}

	@Override
	public void onSlideMenuItemClick(final int itemId) {
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				switch (itemId) {
				case R.id.events_button:	
					if (currentActivity == ADS) {
						Intent intent = new Intent(getBaseContext(), Ads.class);
						Bundle b = new Bundle();
						b.putInt("activity", EVENTS);
						intent.putExtras(b);
						startActivity(intent);
					}
					break;
				case R.id.ads_button:
					if (currentActivity == EVENTS) {
						Intent intent = new Intent(getBaseContext(), Ads.class);
						Bundle b = new Bundle();
						b.putInt("activity", ADS);
						intent.putExtras(b);
						startActivity(intent);
					}
					break;
				case R.id.gallery_button:
					String galleryUrl = "https://plus.google.com/photos/110394289376222111697/albums?banner=pwa";
					Intent galleryIntent = new Intent(Intent.ACTION_VIEW);
					galleryIntent.setData(Uri.parse(galleryUrl));
					startActivity(galleryIntent);
					break;
				case R.id.arrivals_button:
					startActivity(new Intent(getBaseContext(), Arrivals.class));
					break;
				case R.id.profile_button:
					break;
				case R.id.holiday_button:
					String holidayUrl = "http://m.ihg.com/hotels/holidayinn/us/en/modifyhoteldetail/roabb?numberOfAdults=1&rateCode=ILIK1&hotelCode=ROABB&_IATAno=99502056&_corpId=100272909";
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
				case R.id.prayer_button:
					startActivity(new Intent(getBaseContext(), Prayer.class));
					break;
				case R.id.contact_button:
					startActivity(new Intent(getBaseContext(), Contact.class));
					break;
				}
				overridePendingTransition(0,R.anim.fast_fade);
			}
		}, (long)(animationDuration*0.85));
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
