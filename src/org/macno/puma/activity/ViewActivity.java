package org.macno.puma.activity;

import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.R;
import org.macno.puma.util.ActivityUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ViewActivity extends Activity {

	public static final String EXTRA_ACTIVITY = "extraActivity";
	
	private JSONObject mActivity;
	private Context mContext;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_view);
        Bundle extras = getIntent().getExtras();
        
        String activity="";
        if (savedInstanceState != null) {
        	activity = savedInstanceState.getString(EXTRA_ACTIVITY);
		} else if (extras != null) {
			activity = extras.getString(EXTRA_ACTIVITY);
		}
        try {
        	mActivity = new JSONObject(activity);
        } catch(JSONException e) {
        	
        }
        
        LinearLayout ll_parent = (LinearLayout)findViewById(R.id.ll_activity_parent);
        ll_parent.addView(ActivityUtil.getViewActivity(mContext, mActivity), 0);
		
		EditText debug = (EditText)findViewById(R.id.et_activity_debug);
		try {
			debug.setText(mActivity.toString(3));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(EXTRA_ACTIVITY, mActivity.toString());
		super.onSaveInstanceState(outState);
	}

	
	public static void startActivity(Context context,JSONObject activity) {
		Intent viewActivityIntent = new Intent(context,ViewActivity.class);
		viewActivityIntent.putExtra(ViewActivity.EXTRA_ACTIVITY, activity.toString());
		context.startActivity(viewActivityIntent);
		
	}

}
