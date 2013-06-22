package com.sscvt;

import java.util.Calendar;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.coboltforge.slidemenu.SlideMenu;
import com.coboltforge.slidemenu.SlideMenuInterface.OnSlideMenuItemClickListener;
import com.sscvt.Arrivals.sendEmail;
import com.sscvt.gmail.GMailSender;
import com.sscvt.helper.SettingsActivity;

public class Arrivals extends SherlockActivity implements OnSlideMenuItemClickListener {
	public final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
	          "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
	          "\\@" +
	          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
	          "(" +
	          "\\." +
	          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
	          ")+"
	      );

	final OnDateSetListener myOnDateSetListener=new OnDateSetListener()
    {
	   public void onDateSet(DatePicker arg0, int year, int month, int dayOfMonth) {
		   EditText dateEditText = (EditText) findViewById(R.id.arrivalDateEditText);
		   dateEditText.setText(dayOfMonth+"/"+month+"/"+year);
	   }
    };
    
    final OnTimeSetListener myOnTimeSetListener = new OnTimeSetListener()
    {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	
			   EditText arrivalTimeEditText = (EditText) findViewById(R.id.arrivalTimeEditText);
			   arrivalTimeEditText.setText(hourOfDay+":"+minute);
		}
    };
    
	private final String TAG = "Arrivals";
	private ViewFlipper flipper;
	private SlideMenu slidemenu;
	private static final int animationDuration = 333;
	private final int ADS = 1;
	private final int EVENTS = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.arrivals);
        setupActionBar();
        flipper=(ViewFlipper) findViewById(R.id.my_flipper);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	// Populates the activity's options menu.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
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
    
    public void onClick(View v){
    	
    	Calendar myCalendar = Calendar.getInstance();
		 switch(v.getId()){
		 case R.id.arrivalDateEditText:
			 DatePickerDialog myDatePickerDialog = new DatePickerDialog(Arrivals.this,myOnDateSetListener,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH));
			 myDatePickerDialog.show();
			 break;
		 case R.id.arrivalTimeEditText:
			 TimePickerDialog myTimePickerDialog = new TimePickerDialog(Arrivals.this,myOnTimeSetListener,myCalendar.get(Calendar.HOUR),myCalendar.get(Calendar.MINUTE),false);
			 myTimePickerDialog.show();
			 break;
		 case R.id.btn_send:
			 send();
			 break;
		 case R.id.btn_next:
			 if(flipper.getDisplayedChild()==0){	//on first view
				RadioGroup hotelRadio = (RadioGroup)findViewById(R.id.hotelRadioGroup);
		    	int selectedHotel= hotelRadio.getCheckedRadioButtonId();
		    	if(selectedHotel==R.id.yesRadioButton)
		        	flipper.setDisplayedChild(2);
		    	else
		    		flipper.showNext();
			 }else if(flipper.getDisplayedChild()==1){	//on second view
				 flipper.showNext();
			 }
				 
		 	break;
		 }
		
		
    	if(!foundErrors()){
    		//startNextActivity();
    		
    	}else{
    		/*new AlertDialog.Builder(Arrivals.this)
 		   .setMessage(getResources().getText(R.string.correct_erros))
 	       .setCancelable(false)
 	       .setPositiveButton(getResources().getText(R.string.ok), new DialogInterface.OnClickListener() {
 	           public void onClick(DialogInterface dialog, int id) {
 	               // do nothing
 	           }
 	       })
 		.show();*/
    	}
    }
    
    public String[] collectData(){
    	
    	String[] selected = new String[17];
    	
    	EditText nameEditText = (EditText) findViewById(R.id.nameEditText);
    	selected[0] = getResources().getString(R.string.full_name) + ": " 
    					+ nameEditText.getText().toString() + "\n";
    	
    	RadioGroup genderRadio = (RadioGroup)findViewById(R.id.genderRadioGroup);
    	int selectedGender = genderRadio.getCheckedRadioButtonId();
    	switch(selectedGender){
    	case R.id.maleRadioButton:
    		selected[1] = getResources().getString(R.string.gender) + ": " 
    						+ getResources().getString(R.string.male) + "\n";
    		break;
    	case R.id.femaleRadioButton:
    		selected[1] = getResources().getString(R.string.gender) + ": " 
    		  			 + getResources().getString(R.string.female) + "\n";
    		break;
    	}
    	
    	EditText mobileEditText = (EditText) findViewById(R.id.mobielEditText);
    	selected[2] = getResources().getString(R.string.phone_num) + ": " 
    					+ mobileEditText.getText().toString() + "\n";
    	
    	EditText emailEditText = (EditText) findViewById(R.id.emailEditText);
    	selected[3] = getResources().getString(R.string.email) + ": " 
    					+ emailEditText.getText().toString() + "\n\n"
    					+ " ----------------------------------- " + "\n\n";
    	
    	EditText line1EditText = (EditText) findViewById(R.id.line1EditText);
    	selected[4] = getResources().getString(R.string.address_line_1) + ": " 
						+ line1EditText.getText().toString() + "\n";
    	EditText line2EditText = (EditText) findViewById(R.id.line2EditText);
    	selected[5] = getResources().getString(R.string.address_line_2) + ": " 
						+ line2EditText.getText().toString() + "\n";
    	EditText cityEditText = (EditText) findViewById(R.id.cityEditText);
    	selected[6] = getResources().getString(R.string.city) + ": " 
						+ cityEditText.getText().toString() + "\n";
    	EditText stateEditText = (EditText) findViewById(R.id.stateEditText);
    	selected[7] = getResources().getString(R.string.state) + ": " 
						+ stateEditText.getText().toString() + "\n";
    	EditText zipEditText = (EditText) findViewById(R.id.zipEditText);
    	selected[8] = getResources().getString(R.string.zip) + ": " 
						+ zipEditText.getText().toString() + "\n";
    	EditText countryEditText = (EditText) findViewById(R.id.countryEditText);
    	selected[9] = getResources().getString(R.string.country) + ": " 
						+ countryEditText.getText().toString() + "\n\n"
						+ " ----------------------------------- " + "\n\n";
    	
    	
    	EditText escortsEditText = (EditText) findViewById(R.id.escortsEditText);
    	selected[10] = getResources().getString(R.string.escorts) + ": " 
							+ escortsEditText.getText().toString() + "\n";
    	EditText childrenEditText = (EditText) findViewById(R.id.childrenEditText);
    	selected[11] = getResources().getString(R.string.children) + ": " 
							+ childrenEditText.getText().toString() + "\n";
    	EditText bagsEditText = (EditText) findViewById(R.id.bagsEditText);
    	selected[12] = getResources().getString(R.string.bags) + ": " 
							+ bagsEditText.getText().toString() + "\n";
    	EditText arrivalDateEditText = (EditText) findViewById(R.id.arrivalDateEditText);
    	selected[ 13] = getResources().getString(R.string.arrival_date) + ": " 
							+ arrivalDateEditText.getText().toString() + "\n";
    	EditText arrivalTimeEditText = (EditText) findViewById(R.id.arrivalTimeEditText);
    	selected[14] = getResources().getString(R.string.arrival_time) + ": " 
							+ arrivalTimeEditText.getText().toString() + "\n";
    	EditText arrivalPlaceEditText = (EditText) findViewById(R.id.arrivalPlaceEditText);
    	selected[15] = getResources().getString(R.string.arrival_place) + ": " 
							+ arrivalPlaceEditText.getText().toString() + "\n";
    	EditText ticketNumberEditText = (EditText) findViewById(R.id.ticketNumberEditText);
    	selected[16] = getResources().getString(R.string.ticket_number) + ": " 
							+ ticketNumberEditText.getText().toString() + "\n";
    	return selected;
    }
  
    public boolean foundErrors(){
    	boolean foundErrors = false;
    	
		EditText nameEditText = (EditText) findViewById(R.id.nameEditText);
		TextView nameErrorTextView = (TextView)findViewById(R.id.nameErrorTextView);
		 if(isEditTextEmpty(nameEditText, nameErrorTextView)) 
			 foundErrors = true;

		RadioGroup genderRadio = (RadioGroup)findViewById(R.id.genderRadioGroup);
		TextView genderErrorRadio = (TextView)findViewById(R.id.genderErrorTextView);
		if(isRadioGroupEmpty(genderRadio, genderErrorRadio)) 
			foundErrors = true;
		
		EditText mobileEditText = (EditText) findViewById(R.id.mobielEditText);
		TextView mobileErrorTextView = (TextView)findViewById(R.id.mobileErrorTextView);
		if(isEditTextEmpty(mobileEditText, mobileErrorTextView))
			foundErrors = true;

		EditText emailEditText = (EditText) findViewById(R.id.emailEditText);
		TextView emailErrorTextView = (TextView)findViewById(R.id.emailErrorTextView);
		if(isEmailEmpty(emailEditText, emailErrorTextView))
			foundErrors = true;
		
		RadioGroup hotelRadio = (RadioGroup)findViewById(R.id.hotelRadioGroup);
		TextView hotelErrorRadio = (TextView)findViewById(R.id.hotelErrorTextView);
		if(isRadioGroupEmpty(hotelRadio, hotelErrorRadio))
			foundErrors = true;
		 
		return foundErrors;
    }
    
    public boolean isEditTextEmpty(EditText myEditText, TextView myTextView){
    	if(myEditText.getText().toString().isEmpty()){
    		myTextView.setVisibility(View.VISIBLE);
			return true;
		}else{
			myTextView.setVisibility(View.GONE);
			return false;
		}
    }
    
    public boolean isRadioGroupEmpty(RadioGroup myRadioGroup, TextView myTextView){
    	if(myRadioGroup.getCheckedRadioButtonId() == -1){
    		myTextView.setVisibility(View.VISIBLE);
    		return true;
		}else{
			myTextView.setVisibility(View.GONE);
			return false;
		}
    }
    
    public boolean isEmailEmpty(EditText myEditText, TextView myTextView){
    	if(!EMAIL_ADDRESS_PATTERN.matcher(myEditText.getText().toString()).matches()){
    		Log.e(TAG, "email is invalid");
    		myTextView.setVisibility(View.VISIBLE);
			return true;
		}else{
			Log.e(TAG, "email is vaild");
			myTextView.setVisibility(View.GONE);
			return false;
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
		((TextView) v.findViewById(R.id.title)).setText(getResources().getString(R.string.arrivals_button));
		
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
		slidemenu.init(this, R.menu.slide, this, animationDuration, 3);
		
	}
	
	public void send(){
	 	Toast.makeText(getApplicationContext(), getResources().getText(R.string.sending_email), Toast.LENGTH_SHORT).show();	        	
	 	String[] data = collectData();
    	String body = "";
    	for(int i = 0; i<data.length; i++){
    		body +=  data[i] + "\n";
    	}
    	
	new sendEmail().execute(body);  	
    }
 
	class sendEmail extends AsyncTask<String, Void, Boolean> {
        protected Boolean doInBackground(String... body) {
        	try {   
        		GMailSender sender = new GMailSender("sscvtsender@gmail.com", "sscvt1234");
                sender.sendMail("Test",   
                		body[0],   
                        "sscvtsender@gmail.com",   
                        "yasirghunaim@gmail.com"); 
            } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            	return false;
            } 
        	return true;
        	
        }

        protected void onPostExecute(Boolean isSuccess) {
        	if(isSuccess){
        		new AlertDialog.Builder(Arrivals.this)
        		   .setMessage(getResources().getText(R.string.email_sent))
        	       .setCancelable(false)
        	       .setPositiveButton(getResources().getText(R.string.ok), new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   Intent a = new Intent(getBaseContext(),Arrivals.class);
        	               a.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	               startActivity(a);
        	           }
        	       })
        		.show();
        	}else{
        		new AlertDialog.Builder(Arrivals.this)
        		   .setMessage(getResources().getText(R.string.email_not_sent))
        	       .setCancelable(false)
        	       .setPositiveButton(getResources().getText(R.string.ok), new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	               // do nothing
        	           }
        	       })
        		.show();
        	}
        }
     }

	@Override
	public void onSlideMenuItemClick(final int itemId) {
		// TODO Auto-generated method stub
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				switch (itemId) {
				case R.id.events_button:
					
					Intent intent = new Intent(getBaseContext(), Ads.class);
					Bundle b = new Bundle();
					b.putInt("activity", EVENTS);
					intent.putExtras(b);
					startActivity(intent);
					
					break;
				case R.id.ads_button:
					Intent intent1 = new Intent(getBaseContext(), Ads.class);
					Bundle b1 = new Bundle();
					b1.putInt("activity", ADS);
					intent1.putExtras(b1);
					startActivity(intent1);
					
					break;
				case R.id.gallery_button:
					startActivity(new Intent(getBaseContext(), Gallery.class));
					break;
				case R.id.arrivals_button:
					startActivity(new Intent(getBaseContext(), Arrivals.class));
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
}
