package com.sscvt;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public class Contact extends SherlockActivity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

    	//setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact);
		setupActionBar();	
	}

	public void onClick(View v) {
			switch(v.getId()){
			case R.id.phone_button:
				String uri = "tel:+17755728822";
				Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
				phoneIntent.setData(Uri.parse(uri));
				startActivity(phoneIntent);
				break;
			case R.id.email_button:
				Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"sscbvt@gmail.com"});
		        emailIntent.setType("message/rfc822");
		        startActivity(Intent.createChooser(emailIntent, "Send email..."));
				break;
			case R.id.facebook_button:
				String facebookUrl = "https://www.facebook.com/SSCVT";
				Intent facebookIntent = new Intent(Intent.ACTION_VIEW);
				facebookIntent.setData(Uri.parse(facebookUrl));
				startActivity(facebookIntent);
				break;
			case R.id.youtube_button:
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/user/SSCVT")));
				break;
			}
	}
	

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
        		finish();
        		return true;
        default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	private void setupActionBar(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}else{
            BitmapDrawable bg = (BitmapDrawable)getResources().getDrawable(R.drawable.bar_striped);
            bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setBackgroundDrawable(bg);

            BitmapDrawable bgSplit = (BitmapDrawable)getResources().getDrawable(R.drawable.bar_striped_split_img);
            bgSplit.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            getSupportActionBar().setSplitBackgroundDrawable(bgSplit);
        }
	}

}
