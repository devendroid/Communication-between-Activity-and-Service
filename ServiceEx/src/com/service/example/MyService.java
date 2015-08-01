package com.service.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MyService extends Service {
	private NotificationManager mNotificationManager;
	private Timer mTimer = new Timer();
	private int counter = 0, incrementBy = 1;
	private static boolean isRunning = false;

	private List<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
	private int mValue = 0; // Holds last value set by a client.
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SET_INT_VALUE = 3;
	public static final int MSG_SET_STRING_VALUE = 4;

	private final Messenger mMessenger = new Messenger(new IncomingMessageHandler()); // Target we publish for clients to send messages to IncomingHandler.

	private static final String LOGTAG = "MyService";

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(LOGTAG, "Service Started.");
		showNotification();
		mTimer.scheduleAtFixedRate(new MyTask(), 0, 100L);
		isRunning = true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(LOGTAG, "Received start id " + startId + ": " + intent);
		return START_STICKY; // Run until explicitly stopped.
	}

	/**
	 * Display a notification in the notification bar.
	 */
	private void showNotification() {
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = getText(R.string.service_started);
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.service_label), text, contentIntent);
		// Send the notification.
		// We use a layout id because it is a unique number.  We use it later to cancel.
		mNotificationManager.notify(R.string.service_started, notification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(LOGTAG, "onBind");
		return mMessenger.getBinder();
	}

	/**
	 * Send the data to all clients.
	 * @param intvaluetosend The value to send.
	 */
	private void sendMessageToUI(int intvaluetosend) {
		Iterator<Messenger> messengerIterator = mClients.iterator();		
		while(messengerIterator.hasNext()) {
			Messenger messenger = messengerIterator.next();
			try {
				// Send data as an Integer				
				messenger.send(Message.obtain(null, MSG_SET_INT_VALUE, intvaluetosend, 0));

				// Send data as a String
				Bundle bundle = new Bundle();
				bundle.putString("str1", "ab" + intvaluetosend + "cd");
				Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
				msg.setData(bundle);
				messenger.send(msg);

			} catch (RemoteException e) {
				// The client is dead. Remove it from the list.
				mClients.remove(messenger);
			}
		}
	}

	public static boolean isRunning()
	{
		return isRunning;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTimer != null) {mTimer.cancel();}
		counter=0;
		mNotificationManager.cancel(R.string.service_started); // Cancel the persistent notification.
		Log.i("MyService", "Service Stopped.");
		isRunning = false;
	}

	//////////////////////////////////////////
	// Nested classes
	/////////////////////////////////////////

	/**
	 * The task to run...
	 */
	private class MyTask extends TimerTask {
		@Override
		public void run() {
			Log.i(LOGTAG, "Timer doing work." + counter);
			try {
				counter += incrementBy;
				sendMessageToUI(counter);

			} catch (Throwable t) { //you should always ultimately catch all exceptions in timer tasks.
				Log.e("TimerTick", "Timer Tick Failed.", t);            
			}
		}		
	}

	/**
	 * Handle incoming messages from MainActivity
	 */
	private class IncomingMessageHandler extends Handler { // Handler of incoming messages from clients.
		@Override
		public void handleMessage(Message msg) {
			Log.d(LOGTAG,"handleMessage: " + msg.what);
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_SET_INT_VALUE:
				incrementBy = msg.arg1;
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
}