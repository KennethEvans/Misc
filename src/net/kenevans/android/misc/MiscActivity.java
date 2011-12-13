//Copyright (c) 2011 Kenneth Evans
//
//Permission is hereby granted, free of charge, to any person obtaining
//a copy of this software and associated documentation files (the
//"Software"), to deal in the Software without restriction, including
//without limitation the rights to use, copy, modify, merge, publish,
//distribute, sublicense, and/or sell copies of the Software, and to
//permit persons to whom the Software is furnished to do so, subject to
//the following conditions:
//
//The above copyright notice and this permission notice shall be included
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package net.kenevans.android.misc;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Class that has a ListView of Activities to be launched.
 */
public class MiscActivity extends ListActivity {
	/** Array of items used to populate the ListView when debugging. */
	private static final Data[] DEBUG_DATA = {
			new Data("Track Towers", "Show the current tower location",
					MapLocationActivity.class),
			new Data("Network Information",
					"Get information about the carrier", NetworkActivity.class),
			// new Data("Current Time",
			// "Get the current time in several formats",
			// CurrentTimeActivity.class),
			new Data("WebView", "Test WebView", InfoActivity.class),
			new Data("Application Info", "Display Application Information",
					AppsActivity.class),
			new Data("Message Listener", "Listen to received messages",
					MessageListenerActivity.class),
			new Data("Messages Test1", "Display all messages in "
					+ SMSTestActivity.uri, SMSTestActivity.class),
			new Data("Messages Test2", "Display all messages in "
					+ SMSTest2Activity.uri, SMSTest2Activity.class),
			new Data("Messages", "Display all messages in the SMS database",
					SMSActivity.class),
	// new Data("Test", "Not implemented", null),
	};

	/** Array of items used to populate the ListView for release. */
	private static final Data[] RELEASE_DATA = {
			new Data("Track Towers", "Show the current tower location",
					MapLocationActivity.class),
			new Data("Network Information",
					"Get information about the carrier", NetworkActivity.class),
			new Data("Messages", "Display all messages in the SMS database",
					SMSActivity.class), };

	/** The Array of items actually used to populate the ListView. */
	private Data[] data = RELEASE_DATA;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Use different items when debugging
		if (Utils.isDebugBuild(this)) {
			data = DEBUG_DATA;
		}

		// set the ListAdapter
		setListAdapter(new MiscAdapter(this, data));

		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				if (pos < 0 || pos >= data.length) {
					return;
				}
				if (data[pos].activityClass == null) {
					Toast.makeText(getApplicationContext(),
							data[pos].title + " Selected", Toast.LENGTH_SHORT)
							.show();
				} else {
					Intent i = new Intent(MiscActivity.this,
							data[pos].activityClass);
					try {
						startActivity(i);
					} catch (Exception ex) {
						Utils.excMsg(MiscActivity.this,
								"Error launching activity", ex);
					}
				}
			}
		});
	}

	/**
	 * A class to hold the data used to define the contents of the list item and
	 * to implement the onItemClick handler.
	 */
	private static class Data {
		private String title;
		private String subtitle;
		private Class<?> activityClass;

		public Data(String title, String subtitle, Class<?> activityClass) {
			this.title = title;
			this.subtitle = subtitle;
			this.activityClass = activityClass;
		}

	}

	/**
	 * A custom ListView adapter for our implementation. Based on the efficient
	 * list adapter in the SDK APIDemos list14.java.
	 */
	private static class MiscAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private Data[] data;

		public MiscAdapter(Context context, Data[] data) {
			this.data = data;
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);
		}

		/**
		 * The number of items in the list.
		 * 
		 * @see android.widget.ListAdapter#getCount()
		 */
		public int getCount() {
			return data.length;
		}

		/**
		 * Since the data comes from an array, just returning the index is
		 * sufficient to get at the data. If we were using a more complex data
		 * structure, we would return whatever object represents one row in the
		 * list.
		 * 
		 * @see android.widget.ListAdapter#getItem(int)
		 */
		public Object getItem(int position) {
			return position;
		}

		/**
		 * Use the array index as a unique id.
		 * 
		 * @see android.widget.ListAdapter#getItemId(int)
		 */
		public long getItemId(int position) {
			return position;
		}

		/**
		 * Make a view to hold each row.
		 * 
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		public View getView(int pos, View convertView, ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid
			// unnecessary calls
			// to findViewById() on each row.
			ViewHolder holder;

			// When convertView is not null, we can reuse it directly, there is
			// no need
			// to reinflate it. We only inflate a new View when the convertView
			// supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.list_row, null);

				// Creates a ViewHolder and store references to the two children
				// views
				// we want to bind data to.
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.subtitle = (TextView) convertView
						.findViewById(R.id.subtitle);

				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}

			// Bind the data efficiently with the holder.
			holder.title.setText(data[pos].title);
			holder.subtitle.setText(data[pos].subtitle);

			return convertView;
		}

		static class ViewHolder {
			TextView title;
			TextView subtitle;
		}
	}

}
