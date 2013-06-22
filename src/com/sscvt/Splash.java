package com.sscvt;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class Splash extends Activity {
	protected boolean _active = true;
	protected int _splashTime = 3000;
	private boolean isActivityRunning = true;
	private final String TAG = "Splash";
	

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        Log.d(TAG, "onCreate");
        StartAnimations();
        Thread splashTread = new Thread() {
            @Override
            public void run() {
                try {
                    int waited = 0;
                    while(_active && (waited < _splashTime)) {
                        sleep(100);
                        if(_active) {
                            waited += 100;
                        }
                    }
                } catch(InterruptedException e) {
                    // do nothing
                } finally {
                    finish();
    				if(isActivityRunning)
    					startActivity(new Intent(getApplicationContext(), Ads.class));
                }
            }
        };
        splashTread.start();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            _active = false;
        }
        return true;
    }
    
    private void StartAnimations() {
    	Log.d(TAG, "animation");
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade);
        anim.reset();
        ImageView l=(ImageView) findViewById(R.id.splashImageView);
        l.clearAnimation();
        l.startAnimation(anim);
 
 
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(TAG,"onDestory()");
		isActivityRunning = false;
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		overridePendingTransition(R.anim.alpha, R.anim.alpha);
	}


}