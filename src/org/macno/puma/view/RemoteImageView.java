package org.macno.puma.view;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.macno.puma.util.HttpUtil;
import org.macno.puma.util.ImageManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class RemoteImageView extends ImageView {

	private String mRemote;
	private int mResource;
	private ImageManager mImageManager;
	
	public RemoteImageView(Context context, AttributeSet attrs) {
		super(context, attrs, 0);
		mImageManager = ImageManager.getImageManager(context);
	}

	public void setRemoteURI(String uri) {
		if (uri.startsWith("http")) {
			mRemote = uri;
		}
	}

	public void setRemoteURI(HttpUtil httpUtil, String uri) {
		mRemote = uri;
		mImageManager.setHttpUtil(httpUtil);
	}
	
	public void loadImage(int resource) {
		mResource=resource;
		if (mRemote != null) {
			if (mImageManager.contains(mRemote)) {
				setFromLocal();
			} else {
				setImageResource(resource);
				doImageDownload();
			}
		}
	}
	
	public void loadImage() {
		loadImage(mResource);
	}
	
	private void doImageDownload() {
		Runnable runnable = new Runnable() {
			@Override
            public void run() {
            	try {
            		mImageManager.put(mRemote);
            		Log.d(APP_NAME, "Downloaded");
            		mHandler.imageDownloaded();
            	} catch(IOException e){
            		
            	}
            }
		};
		new Thread(runnable).start();
	}
	
	private void setFromLocal() {
		Bitmap bm = mImageManager.get(mRemote);
		if(bm != null)
			setImageBitmap(bm);
		else {
			setImageResource(mResource);
		}
	}
	
	private void endLoadRemote() {
		Bitmap bm = mImageManager.get(mRemote);
		if(bm != null) {
			setImageBitmap(bm);
			Log.d(APP_NAME,"Loaded bitmap.");
		}
	}

	private RemoteImageHandler mHandler = new RemoteImageHandler(this);
	
	static class RemoteImageHandler extends Handler {

		private final WeakReference<RemoteImageView> mTarget; 
		
		private static final int MSG_DOWNLOADED = 1;
		
		RemoteImageHandler(RemoteImageView target) {
			mTarget = new WeakReference<RemoteImageView>(target);
		}
		
		public void handleMessage(Message msg) {
			RemoteImageView target = mTarget.get();
			switch (msg.what) {
			case MSG_DOWNLOADED:
				target.endLoadRemote();
				break;
			}
		}

		public void imageDownloaded() {
			sendEmptyMessage(MSG_DOWNLOADED);
		}
	
	}

}