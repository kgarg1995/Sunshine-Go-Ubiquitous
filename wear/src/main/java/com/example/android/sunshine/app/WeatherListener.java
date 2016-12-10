package com.example.android.sunshine.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class WeatherListener extends WearableListenerService
		implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {
	private static GoogleApiClient mGoogleApiClient;
	private static WeatherInterface weatherInterface;

	private static final String TAG = WeatherListener.class.getSimpleName();
	private static final long TIMEOUT_MS = 3000;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate()");
		mGoogleApiClient = new GoogleApiClient.Builder(WeatherListener.this)
				.addApi(Wearable.API)
				.addConnectionCallbacks(WeatherListener.this)
				.addOnConnectionFailedListener(WeatherListener.this)
				.build();
		mGoogleApiClient.connect();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	public static void setWeatherInterface(WeatherInterface weatherInterface) {
		WeatherListener.weatherInterface = weatherInterface;
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEventBuffer) {
		for (DataEvent dataEvent : dataEventBuffer) {
			if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
				DataMap dataMap = DataMapItem
						.fromDataItem(dataEvent.getDataItem())
						.getDataMap();
				String path = dataEvent.getDataItem().getUri().getPath();
				if (path.equals("/weather")) {
					double maxTemp = dataMap.getDouble("maxTemp");
					double minTemp = dataMap.getDouble("minTemp");
					Asset weather = dataMap.getAsset("icon");
					weatherInterface.onWeatherChanged(
							maxTemp, minTemp, loadBitmapFromAsset(weather));
				}
			}
		}
	}

	private Bitmap loadBitmapFromAsset(Asset asset) {
		if (asset == null) {
			throw new IllegalArgumentException("Asset must be non-null");
		}
		ConnectionResult result = mGoogleApiClient
				.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
		if (!result.isSuccess()) {
			return null;
		}
		// convert asset into a file descriptor and block until it's ready
		InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
				mGoogleApiClient, asset).await().getInputStream();
		mGoogleApiClient.disconnect();

		if (assetInputStream == null) {
			Log.w(TAG, "Requested an unknown Asset.");
			return null;
		}
		// decode the stream into a bitmap
		return BitmapFactory.decodeStream(assetInputStream);
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		Log.d(TAG, "onConnected");
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.d(TAG, String.format("onConnectionSuspended(%d)", i));
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Log.d(TAG, "onConnectionFailed(" + connectionResult + ")");
	}

	interface WeatherInterface {
		void onWeatherChanged(double maxTemp, double minTemp, Bitmap weather);
	}
}
