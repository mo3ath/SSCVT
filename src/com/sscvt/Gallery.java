package com.sscvt;

import android.app.Activity;
import android.os.Bundle;

public class Gallery extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gallery);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	

}
