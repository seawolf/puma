package org.macno.puma.activity;

import org.macno.puma.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class HomeActivity extends Activity {

	public static final String EXTRA_ACCOUNT_UUID = "extraAccountUUID";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
