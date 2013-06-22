package com.sscvt.helper;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sscvt.R;
import com.sscvt.helper.SscvtParser.Item;

public class CustomAdapter extends BaseAdapter {

	private Context context;
	private ArrayList<Item> items;
	private ArrayList<Boolean> wasAnimationShown;
	ListView CustomList;
	WebView myWebView = null;

	public CustomAdapter(Context context, ArrayList<Item> items, ListView list) {
		this.context = context;
		this.items = items;
		this.CustomList = list;
		wasAnimationShown = new ArrayList<Boolean>();
		for(int i =0 ; i < items.size(); i++){
			wasAnimationShown.add(false);
		}
	}

	public void addItem(Item item) {
		if (!items.contains(item)) {
			items.add(item);
		}
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * private void initializeWebView(){ //loads the WebView completely zoomed
	 * out myWebView.getSettings().setBuiltInZoomControls(true);
	 * //myWebView.getSettings().setDisplayZoomControls(false);
	 * myWebView.getSettings().setDefaultZoom(ZoomDensity.FAR); //loads the
	 * WebView in mobile style
	 * myWebView.getSettings().setLayoutAlgorithm(LayoutAlgorithm
	 * .SINGLE_COLUMN); // disable scroll on touch /*
	 * myWebView.setOnTouchListener(new View.OnTouchListener() { public boolean
	 * onTouch(View v, MotionEvent event) { return (event.getAction() ==
	 * MotionEvent.ACTION_MOVE); } }); }
	 */

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int groupPosition, View view, ViewGroup parent) {
		// TODO Auto-generated method stub
		Item item = (Item) getItem(groupPosition);
		if (view == null) {
			LayoutInflater inf = (LayoutInflater) context
					.getSystemService(context.LAYOUT_INFLATER_SERVICE);
			view = inf.inflate(R.layout.list_item, null);
		}
		/** setup parent view of list */
		// set title
		TextView titleText = (TextView) view.findViewById(R.id.eventsItem);
		titleText.setText(item.getTitle());
		//set title's font
		Typeface tf = Typeface.createFromAsset(context.getAssets(),
				"fonts/K Farnaz.ttf");
		titleText.setTypeface(tf);
		// set date
		TextView dateText = (TextView) view.findViewById(R.id.eventsDate);
		dateText.setText(item.getDate());
		// set icon
		ImageView expandIcon = (ImageView) view.findViewById(R.id.expandIcon);

		// animate element
		int startOffset = 100;
		if (groupPosition < wasAnimationShown.size()) {
			if (!wasAnimationShown.get(groupPosition)) {
				Animation animation = AnimationUtils.loadAnimation(context,
						R.anim.fast_fade);
				animation.setStartOffset(startOffset * groupPosition);
				view.startAnimation(animation);
				wasAnimationShown.set(groupPosition, true);
			}
		}
		return view;
	}
	
	


}
