package net.dahanne.android.asynctasksdemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.Window;
import android.widget.ImageView;

public class MyAsyncTask extends AsyncTask<String, Integer, Bitmap> implements
		OnCancelListener {
	private final ImageView mImageView;
	private ProgressDialog progressDialog;
	private final Context mContext;

	public MyAsyncTask(ImageView imageView, Context context) {
		super();
		mImageView = imageView;
		mContext = context;
	}

	@Override
	protected Bitmap doInBackground(String... parameters) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		publishProgress(5000);
		Bitmap b = ActionChooser.downloadBitmap(parameters[0]);
		publishProgress(10000);
		return b;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// initialize progress bar
		((Activity) mContext).getWindow().setFeatureInt(
				Window.FEATURE_PROGRESS, 0);

		// start a status dialog
		progressDialog = ProgressDialog.show(mContext, "Please be patient !",
				"Downloading the picture", true, true, this);

	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		((Activity) mContext).getWindow().setFeatureInt(
				Window.FEATURE_PROGRESS, values[0]);
	}

	@Override
	protected void onPostExecute(Bitmap b) {
		mImageView.setImageBitmap(b);
		progressDialog.dismiss();
	}

	public void onCancel(DialogInterface arg0) {
		// we UI has received the order to cancel, let's notify the worker
		// thread to cancel NOW !
		this.cancel(true);

	}

}
