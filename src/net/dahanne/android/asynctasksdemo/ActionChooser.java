package net.dahanne.android.asynctasksdemo;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

public class ActionChooser extends Activity implements OnClickListener {
	private static final String IMAGE_URL = "http://192.168.1.101/gallery2/main.php?g2_view=core.DownloadItem&g2_itemId=34&g2_serialNumber=2";
	/** Called when the activity is first created. */
	ImageView mImageView;
	Button buttonNoNewThread;
	Button buttonNewThreadDoesItAll;
	Button buttonNewThreadDelegates;
	Button buttonMyAsyncTask;
	private static final String LOG_TAG = "ActivityChooser";
	String exceptionMessage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// this will enable the progress bar
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);
		mImageView = (ImageView) findViewById(R.id.image_view);
		buttonNoNewThread = (Button) findViewById(R.id.no_new_thread);
		buttonNoNewThread.setOnClickListener(this);
		buttonNewThreadDoesItAll = (Button) findViewById(R.id.new_thread_does_it_all);
		buttonNewThreadDoesItAll.setOnClickListener(this);
		buttonNewThreadDelegates = (Button) findViewById(R.id.new_thread_delegates);
		buttonNewThreadDelegates.setOnClickListener(this);
		buttonMyAsyncTask = (Button) findViewById(R.id.my_async_task);
		buttonMyAsyncTask.setOnClickListener(this);

	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.no_new_thread:
			Bitmap b = downloadBitmap(IMAGE_URL);
			mImageView.setImageBitmap(b);
			break;
		case R.id.new_thread_does_it_all:
			new Thread(new Runnable() {
				public void run() {
					// BOOM ! This worker Thread is not allowed to update the UI
					Bitmap b = downloadBitmap(IMAGE_URL);
					mImageView.setImageBitmap(b);
				}

			}).start();
			break;
		case R.id.new_thread_delegates:
			new Thread(new Runnable() {
				public void run() {
					final Bitmap b = downloadBitmap(IMAGE_URL);
					// This worker thread asks the UI thread to run the update
					mImageView.post(new Runnable() {
						public void run() {
							mImageView.setImageBitmap(b);
						}
					});
				}

			}).start();
			break;

		case R.id.my_async_task:
			MyAsyncTask myAsyncTask = new MyAsyncTask(mImageView, this);
			myAsyncTask.execute(IMAGE_URL);
			break;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.reset:
			mImageView.setImageResource(R.drawable.icon);
			break;

		}
		return false;

	}

	public static Bitmap downloadBitmap(String url) {

		try {
			// it is taking FOREVER !!!
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
		}

		final int IO_BUFFER_SIZE = 4 * 1024;

		// AndroidHttpClient is not allowed to be used from the main thread
		final HttpClient client = new DefaultHttpClient();
		final HttpGet getRequest = new HttpGet(url);

		try {
			HttpResponse response = client.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Log.w("ImageDownloader", "Error " + statusCode
						+ " while retrieving bitmap from " + url);
				return null;
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				try {
					inputStream = entity.getContent();
					// return BitmapFactory.decodeStream(inputStream);
					// Bug on slow connections, fixed in future release.
					return BitmapFactory.decodeStream(new FlushedInputStream(
							inputStream));
				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
					entity.consumeContent();
				}
			}
		} catch (IOException e) {
			getRequest.abort();
			Log.w(LOG_TAG, "I/O error while retrieving bitmap from " + url, e);
		} catch (IllegalStateException e) {
			getRequest.abort();
			Log.w(LOG_TAG, "Incorrect URL: " + url);
		} catch (Exception e) {
			getRequest.abort();
			Log.w(LOG_TAG, "Error while retrieving bitmap from " + url, e);
		} finally {
			if ((client instanceof AndroidHttpClient)) {
				((AndroidHttpClient) client).close();
			}
		}
		return null;
	}

	/*
	 * An InputStream that skips the exact number of bytes provided, unless it
	 * reaches EOF.
	 */
	static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int b = read();
					if (b < 0) {
						break; // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}
}