package org.macno.puma.adapter;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.util.ArrayList;

import org.macno.puma.R;
import org.macno.puma.core.Account;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class StreamPageAdapter extends PagerAdapter {

	private FragmentActivity mContext;
	private Account mAccount;
	
	private ArrayList<StreamPageAdapter.Stream> mStreamNames;
	
	public StreamPageAdapter(FragmentActivity context, Account account) {
		mContext = context;
		mAccount = account;
		prepareStreams();
	}
	
	private void prepareStreams() {
		mStreamNames = new ArrayList<StreamPageAdapter.Stream>();
		Stream inbox = new Stream();
		inbox.id="inbox/major";
		inbox.name="Inbox Major";
		mStreamNames.add(inbox);

		Stream feed = new Stream();
		feed.id="feed/major";
		feed.name="Feed";
		mStreamNames.add(feed);
		
		Stream direct = new Stream();
		direct.id="inbox/direct";
		direct.name="Inbox Direct";
		mStreamNames.add(direct);

		Stream publicFeed = new Stream();
		publicFeed.id="https://ofirehose.com/feed.json";
		publicFeed.name="OFirehose.com feed";
		mStreamNames.add(publicFeed);

	}
	
	@Override
	public int getCount() {
		return mStreamNames.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view==((RelativeLayout)object);
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((RelativeLayout) view);
	}
	
	@Override
	public Object instantiateItem(View collection, int position) {
		Log.d(APP_NAME, "instantiateItem " + position);
		LayoutInflater inflater = 
				(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.stream_list,null);
		TextView tv = (TextView)layout.findViewById(R.id.tv_stream_name);
		Stream stream = mStreamNames.get(position);
	
		tv.setText(stream.name);
		
		ActivityAdapter activityAdapter = new ActivityAdapter(mContext,mAccount,stream.id);
		stream.adapter = activityAdapter;
		ListView activityList = (ListView)layout.findViewById(R.id.activities_list);
		activityList.setOnScrollListener(activityAdapter);
		activityList.setAdapter(activityAdapter);
		
		((ViewPager) collection).addView(layout);
		return layout;
	}

	public void refreshAdapter(int viewid) {
		
		Stream stream = mStreamNames.get(viewid);
		stream.adapter.checkNewActivities();
	}
	
	public void clearCache(int viewid) {
		Stream stream = mStreamNames.get(viewid);
		stream.adapter.clearCache();
	}

	class Stream {
		
		String id;
		String name;
		ActivityAdapter adapter;
		
	}
}
