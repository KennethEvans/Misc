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

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;

/**
 * Class to display a local web page.
 */
public class InfoActivity extends Activity implements IConstants {
	private static final String DEFAULT_URL = "file:///android_asset/test.html";
	private WebView mWebView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);

		mWebView = (WebView) findViewById(R.id.webview);
		// mWebView.getSettings().setJavaScriptEnabled(true);
		// mWebView.setWebViewClient(new LocalWebViewClient());

		// Get the URL
		Bundle extras = getIntent().getExtras();
		String url = extras != null ? extras.getString(INFO_URL) : null;
		// TODO For now use the test page
		if (url == null || url.length() == 0) {
			url = DEFAULT_URL;
		}
		mWebView.loadUrl(url);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// When the back button is pressed make it go back in the history if
		// these is one
		if (mWebView != null && keyCode == KeyEvent.KEYCODE_BACK
				&& mWebView.canGoBack()) {
			mWebView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Local WebViewClient. Currently not used.
	 * 
	 */
	// private class LocalWebViewClient extends WebViewClient {
	// @Override
	// public boolean shouldOverrideUrlLoading(WebView view, String url) {
	// if (Uri.parse(url).getHost().equals("www.example.com")) {
	// // This is my web site, so do not override; let my WebView load
	// // the page
	// return false;
	// }
	// // Otherwise, the link is not for a page on my site, so launch
	// // another Activity that handles URLs
	// Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
	// startActivity(intent);
	// return true;
	// }
	//
	// }

}
