package us.shandian.giga.ui.web;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.support.v7.app.ActionBar;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import us.shandian.giga.R;
import us.shandian.giga.ui.common.ToolbarActivity;
import us.shandian.giga.ui.main.MainActivity;
import us.shandian.giga.util.Utility;
import static us.shandian.giga.BuildConfig.DEBUG;

public class BrowserActivity extends ToolbarActivity
{
	private WebView mWeb;
	private ProgressBar mProgress;
	private EditText mUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Toolbar
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		
		// Custom view
		if (Build.VERSION.SDK_INT < 21) {
			ContextThemeWrapper wrap = new ContextThemeWrapper(this, R.style.Theme_AppCompat);
			LayoutInflater inflater = (LayoutInflater) wrap.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View custom = inflater.inflate(R.layout.browser_url, null);
			getSupportActionBar().setCustomView(custom);
		} else {
			getSupportActionBar().setCustomView(R.layout.browser_url);
		}
		
		// Initialize WebView
		mProgress = Utility.findViewById(this, R.id.progress);
		mUrl = Utility.findViewById(getSupportActionBar().getCustomView(), R.id.browser_url);
		mWeb = Utility.findViewById(this, R.id.web);
		mWeb.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				getSupportActionBar().setDisplayShowCustomEnabled(false);
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				return true;
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				mProgress.setProgress(0);
			}
		});
		mWeb.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView v, int progress) {
				mProgress.setProgress(progress);
			}
			
			@Override
			public void onReceivedTitle(WebView v, String title) {
				getSupportActionBar().setTitle(title);
			}
		});
		mWeb.getSettings().setJavaScriptEnabled(true);
		mWeb.setDownloadListener(new DownloadListener() {

				@Override
				public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
					// Start MainActivity for downloading
					Intent i = new Intent();
					i.setAction(Intent.ACTION_VIEW);
					i.setDataAndType(Uri.parse(url), mimeType);
					startActivity(i);
					finish();
				}
			
		});
		mUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent ev) {
					if (actionId == EditorInfo.IME_ACTION_GO) {
						String url = mUrl.getText().toString();
						
						if (!url.startsWith("http")) {
							url = "http://" + url;
						}
						
						mWeb.loadUrl(url);
						switchCustom();
						return true;
					}
					return false;
				}
		});
		mWeb.addJavascriptInterface(new MyJavascriptInterface(), "HTMLOUT");
		
		mWeb.loadUrl("about:blank");
		
		switchCustom();
		
		mToolbar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switchCustom();
			}
		});
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.browser;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.browser, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			case R.id.detector:
				mWeb.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void switchCustom() {
		int opt = getSupportActionBar().getDisplayOptions();
		
		if ((opt & ActionBar.DISPLAY_SHOW_CUSTOM) != 0) {
			getSupportActionBar().setDisplayShowCustomEnabled(false);
			getSupportActionBar().setDisplayShowTitleEnabled(true);
		} else {
			getSupportActionBar().setDisplayShowCustomEnabled(true);
			getSupportActionBar().setDisplayShowTitleEnabled(false);
			mUrl.setText(mWeb.getUrl());
			mUrl.setSelection(0, mUrl.getText().length());
		}
	}
	
	private void showVideoChoices(final String[] vids) {
		new AlertDialog.Builder(this)
			.setItems(vids, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					Intent i = new Intent();
					i.setAction(Intent.ACTION_VIEW);
					i.setDataAndType(Uri.parse(vids[id]), "application/octet-stream");
					startActivity(i);
					finish();
				}
			})
			.show();
	}
	
	class MyJavascriptInterface {
		private static final String TAG = MyJavascriptInterface.class.getSimpleName();
		
		private static final String PATTERN = "[http|https]+[://]+[0-9A-Za-z:/[-]_#[?][=][.][&]]*";
		private static final String[] VIDEO_SUFFIXES = new String[]{
			".mp4",
			".flv",
			".rm",
			".rmvb",
			".wmv",
			".avi",
			".mkv",
			".webm"
		};
		
		@JavascriptInterface
		public void processHTML(String html) {
			Pattern pattern = Pattern.compile(PATTERN);
			Matcher matcher = pattern.matcher(html);
			
			ArrayList<String> vid = new ArrayList<String>();
			
			while (matcher.find()) {
				String url = matcher.group();
				
				boolean isVid = false;
				
				for (String suffix : VIDEO_SUFFIXES) {
					if (url.contains(suffix)) {
						isVid = true;
						break;
					}
				}
				
				if (isVid) {
					
					vid.add(url);
					
					if (DEBUG) {
						Log.d(TAG, "found url:" + url);
					}
				}
			}
			
			if (vid.size() == 0) return;
			
			String[] arr = new String[vid.size()];
			showVideoChoices(vid.toArray(arr));
		}
	}
}
