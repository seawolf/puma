package org.macno.puma.manager;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class ActivityManager {

	private Context mContext;
	
	public ActivityManager (Context context) {
		mContext = context;
	}

	public JSONObject getStream(String stream) {
		return loadCache(stream);
	}

	public boolean saveStream(String stream, JSONObject activity) {
		return saveCache(stream,activity);
	}
	
	public boolean deleteStream(String stream) {
		return deleteCache(stream);
	}

	private boolean deleteCache(String fileName) {
		File cacheDir = mContext.getDir("data",Context.MODE_PRIVATE);
		File cacheFile = new File(cacheDir, fileName);
		if(cacheFile.exists()) {
			return cacheFile.delete();
		}
		return false;
	}

	private JSONObject loadCache(String fileName) { // TODO verificare se non sia meglio caricare sempre, anche quelli "vecchi"
		File cacheDir = mContext.getDir("data",Context.MODE_PRIVATE);
		File cacheFile = new File(cacheDir, fileName);
		if(cacheFile.exists()) {
			
			FileInputStream fis = null;
			BufferedReader r = null;
			try {
				fis = new FileInputStream(cacheFile);
				r = new BufferedReader(new InputStreamReader(fis));
				StringBuilder total = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
				    total.append(line);
				}
				JSONObject ret = new JSONObject(total.toString());
				return ret;
			} catch(FileNotFoundException e) {
				// Cancellato
			} catch (IOException e) {
				Log.e(APP_NAME, "Failed to read cache for " + fileName + ": " + e.getMessage());
			} catch(JSONException e) {
				Log.e(APP_NAME, "Failed to parse cache for " + fileName + ": " + e.getMessage());
			} finally {
				try {
					if(r != null) {
						r.close();
					}
					if(fis != null) {
						fis.close();
					}
				} catch (IOException e) {
					// Nothing... 
				}
			}
		
		}
		return null;
	}

	private boolean saveCache(String fileName, JSONObject data) {
		File cacheDir = mContext.getDir("data",Context.MODE_PRIVATE);
		File cacheFile = new File(cacheDir, fileName);
		if(cacheFile.exists()) {
			cacheFile.delete();
		}
		try {
			FileOutputStream fos = new FileOutputStream(cacheFile);
			fos.write(data.toString().getBytes());
			fos.close();
			Log.d(APP_NAME, "Cache for " + fileName + " saved in " + cacheFile.getAbsolutePath());
			return true;
		} catch(IOException e) {
			Log.e(APP_NAME, "Failed to write cache for " + fileName + ": " + e.getMessage());
			return false;
		}
	}
}
