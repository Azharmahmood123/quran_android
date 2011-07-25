package com.quran.labs.androidquran;

import java.util.HashMap;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar.IntentAction;
import com.quran.labs.androidquran.common.AyahItem;
import com.quran.labs.androidquran.common.AyahStateListener;
import com.quran.labs.androidquran.common.PageViewQuranActivity;
import com.quran.labs.androidquran.common.QuranPageFeeder;
import com.quran.labs.androidquran.data.QuranInfo;
import com.quran.labs.androidquran.service.AudioServiceBinder;
import com.quran.labs.androidquran.service.QuranAudioService;
import com.quran.labs.androidquran.util.QuranAudioLibrary;
import com.quran.labs.androidquran.util.QuranUtils;

public class QuranViewActivity extends PageViewQuranActivity implements AyahStateListener {

	protected static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
	protected static final String ACTION_NEXT = "ACTION_NEXT";
	protected static final String ACTION_PAUSE = "ACTION_PAUSE";
	protected static final String ACTION_PLAY = "ACTION_PLAY";
	protected static final String ACTION_STOP = "ACTION_STOP";
	protected static final String ACTION_CHANGE_READER = "ACTION_CHANGE_READER";

	private static final String TAG = "QuranViewActivity";
	
	private boolean bounded = false;
	private AudioServiceBinder quranAudioPlayer = null;
	
	private static final int ACTION_BAR_ACTION_CHANGE_READER = 0;
	private static final int ACTION_BAR_ACTION_PREVIOUS = 1;
	private static final int ACTION_BAR_ACTION_PLAY = 2;
	private static final int ACTION_BAR_ACTION_PAUSE = 3;
	private static final int ACTION_BAR_ACTION_STOP = 4;
	private static final int ACTION_BAR_ACTION_NEXT = 5;

	
	private AyahItem lastAyah;
	private int currentReaderId;
	
	HashMap<Integer, IntentAction> actionBarActions = new HashMap<Integer, IntentAction>();
	
//	private TextView textView;
	
	private ServiceConnection conn = new ServiceConnection() {						
		@Override
		public void onServiceDisconnected(ComponentName name) {
			unBindAudioService();
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {							
			quranAudioPlayer = (AudioServiceBinder) service;
			quranAudioPlayer.setAyahCompleteListener(QuranViewActivity.this);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		textView = new TextView(this);
//		textView.setText("");
		bindAudioService();
	}
	
	protected void addActions(){
		super.addActions();
		if(actionBar != null){
			//actionBar.setTitle("QuranAndroid");
			actionBarActions.put(ACTION_BAR_ACTION_PLAY,
					getIntentAction(ACTION_PLAY, android.R.drawable.ic_media_play));
			actionBarActions.put(ACTION_BAR_ACTION_PAUSE, 
					getIntentAction(ACTION_PAUSE, android.R.drawable.ic_media_pause));
			actionBarActions.put(ACTION_BAR_ACTION_NEXT,
					getIntentAction(ACTION_NEXT, android.R.drawable.ic_media_next));
			actionBarActions.put(ACTION_BAR_ACTION_PREVIOUS,
					getIntentAction(ACTION_PREVIOUS, android.R.drawable.ic_media_previous));			
			actionBarActions.put(ACTION_BAR_ACTION_STOP, 
					getIntentAction(ACTION_STOP, R.drawable.stop));
			actionBarActions.put(ACTION_BAR_ACTION_CHANGE_READER,
					getIntentAction(ACTION_CHANGE_READER, R.drawable.mic));
			
//			actionBar.addAction(actionBarActions.get(ACTION_BAR_ACTION_PREVIOUS),
//					ACTION_BAR_ACTION_PREVIOUS);				
//			actionBar.addAction(actionBarActions.get(ACTION_BAR_ACTION_PLAY), 
//					ACTION_BAR_ACTION_PLAY);
//			actionBar.addAction(actionBarActions.get(ACTION_BAR_ACTION_PAUSE), 
//					ACTION_BAR_ACTION_PAUSE);
//			actionBar.addAction(actionBarActions.get(ACTION_BAR_ACTION_STOP),
//					ACTION_BAR_ACTION_STOP);
//			actionBar.addAction(actionBarActions.get(ACTION_BAR_ACTION_NEXT),
//					ACTION_BAR_ACTION_NEXT);	
//			actionBar.addAction(actionBarActions.get(ACTION_BAR_ACTION_CHANGE_READER),
//					ACTION_BAR_ACTION_CHANGE_READER);
			
			for (Integer actionId : actionBarActions.keySet()) {
				 actionBar.addAction(actionBarActions.get(actionId), actionId);
			}
		}		
	}
	
	private IntentAction getIntentAction(String intentAction, int drawable){
		 	Intent i =  new Intent(this, QuranViewActivity.class); 
	        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	        i.setAction(intentAction);
	        IntentAction action = new IntentAction(this, i, drawable);
	        return action;
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		String action = intent.getAction();
		if(quranAudioPlayer != null && action != null){
			if(action.equalsIgnoreCase(ACTION_PLAY)){
				if(quranAudioPlayer.isPaused())
					quranAudioPlayer.resume();
				else{
					Integer[] pageBounds = QuranInfo.getPageBounds(quranPageFeeder.getCurrentPagePosition());
					final AyahItem i = QuranAudioLibrary.getAyahItem(getApplicationContext(), pageBounds[0],
							pageBounds[1], getQuranReaderId());
					lastAyah = i;
					// soura not totall found
					if(QuranUtils.isSouraAudioFound(i.getQuranReaderId(), i.getSoura()) < 0){
						showDownloadDialog(i);
					}else{
						quranAudioPlayer.enableRemotePlay(false);
						playAudio(i);
					}
				}		
			}else if(action.equalsIgnoreCase(ACTION_PAUSE)){
				quranAudioPlayer.pause();
			}else if(action.equalsIgnoreCase(ACTION_NEXT)){
				if(quranAudioPlayer.getCurrentAyah() != null){
					AyahItem ayah = QuranAudioLibrary.getNextAyahAudioItem(this,
							quranAudioPlayer.getCurrentAyah());
					quranAudioPlayer.play(ayah);
				}
			}else if (action.equalsIgnoreCase(ACTION_PREVIOUS)){
				if(quranAudioPlayer.getCurrentAyah() != null){
					AyahItem ayah = QuranAudioLibrary.getPreviousAyahAudioItem(this,
							quranAudioPlayer.getCurrentAyah());
					quranAudioPlayer.play(ayah);
				}
			}else if (action.equalsIgnoreCase(ACTION_STOP)){
			
				lastAyah = null;
				quranAudioPlayer.stop();
			}else if(action.equalsIgnoreCase(ACTION_CHANGE_READER))
				showChangeReaderDialog();
		}
	}
	
	private void showDownloadDialog(final AyahItem i) {

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		LayoutInflater li = LayoutInflater.from(this);
        final View view = li.inflate(R.layout.dialog_download, null);
        Spinner s = (Spinner) view.findViewById(R.id.spinner);
        if(s != null)
        	s.setSelection(getQuranReaderId());
        dialog.setView(view);
		//AlertDialog dialog = new DownloadDialog(this);
		dialog.setMessage("Do you want to download sura");
		dialog.setPositiveButton("download", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {				
				// get reader id
				Spinner s = (Spinner) view.findViewById(R.id.spinner);
				lastAyah = i;
				if(s != null){
					if(s.getSelectedItemPosition() != Spinner.INVALID_POSITION){
						// reader is not default reader
						if(s.getSelectedItemPosition() != i.getQuranReaderId()){
							lastAyah = QuranAudioLibrary.getAyahItem(getApplicationContext(), i.getSoura(), 
									i.getAyah(), s.getSelectedItemPosition());
							currentReaderId = s.getSelectedItemPosition();
						}
					}
				}
				downloadSura(lastAyah.getQuranReaderId(), lastAyah.getSoura(), lastAyah.getAyah());
				dialog.dismiss();
			}
		});
		dialog.setNeutralButton("Stream", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// get reader id
				quranAudioPlayer.enableRemotePlay(true);
				Spinner s = (Spinner) view.findViewById(R.id.spinner);
				lastAyah = i;
				if(s != null){
					if(s.getSelectedItemPosition() != Spinner.INVALID_POSITION){
						// reader is not default reader
						if(s.getSelectedItemPosition() != i.getQuranReaderId()){
							lastAyah = QuranAudioLibrary.getAyahItem(getApplicationContext(), i.getSoura(), 
									i.getAyah(), s.getSelectedItemPosition());
							currentReaderId = s.getSelectedItemPosition();
						}
					}
				}
				quranAudioPlayer.play(lastAyah);
				dialog.dismiss();
			}
		});
		
		dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		AlertDialog diag = dialog.create();
		diag.show();
	}

	private void showChangeReaderDialog(){
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		LayoutInflater li = LayoutInflater.from(this);
        final View view = li.inflate(R.layout.dialog_download, null); 
        Spinner s = (Spinner)view.findViewById(R.id.spinner);
        s.setSelection(getQuranReaderId());
        dialogBuilder.setView(view);
		dialogBuilder.setMessage("Change quran reader");
		dialogBuilder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Spinner s = (Spinner) view.findViewById(R.id.spinner);
				if(s != null && s.getSelectedItemPosition() != Spinner.INVALID_POSITION){
					if(currentReaderId != s.getSelectedItemPosition()){
						currentReaderId = s.getSelectedItemPosition();
					}
				}
			}
		});	
		dialogBuilder.setNegativeButton("Cancel", null);
		dialogBuilder.show();
	}
	
	
	protected void initQuranPageFeeder(){
		if (quranPageFeeder == null) {
			Log.d(TAG, "Quran Feeder instantiated...");
			quranPageFeeder = new QuranPageFeeder(this, quranPageCurler, R.layout.quran_page_layout);
		} else {
			quranPageFeeder.setContext(this, quranPageCurler);
		}
	}
	
	private void unBindAudioService(){
		if (bounded) {
	        // Detach our existing connection.
	        unbindService(conn);
	        if(quranAudioPlayer != null)
	        	quranAudioPlayer.setAyahCompleteListener(null);
	        bounded = false;
	    }
	}
	
	private void bindAudioService(){
		if (!bounded){
			Intent serviceIntent = new Intent(getApplicationContext(), QuranAudioService.class);
			startService(serviceIntent);
			bounded = bindService(serviceIntent, conn, BIND_AUTO_CREATE);
			if(!bounded)
				Toast.makeText(this, "can not bind service", Toast.LENGTH_SHORT);
		}
	}

	private void playAudio(AyahItem ayah){
		if(quranAudioPlayer != null){
			if (ayah == null) {
				Integer[] pageBounds = QuranInfo.getPageBounds(quranPageFeeder.getCurrentPagePosition());
				ayah = QuranAudioLibrary.getAyahItem(getApplicationContext(), pageBounds[0], pageBounds[1], getQuranReaderId());
			}
			quranAudioPlayer.play(ayah);
		}
	}
	
	@Override
	public void onAyahComplete(AyahItem ayah, AyahItem nextAyah) {
		lastAyah = ayah;
		if(nextAyah.getQuranReaderId() != getQuranReaderId() &&
				quranAudioPlayer != null && quranAudioPlayer.isPlaying()){
			quranAudioPlayer.stop();
			lastAyah = QuranAudioLibrary.getAyahItem(this, nextAyah.getSoura(), nextAyah.getAyah(),
					getQuranReaderId());
			quranAudioPlayer.play(lastAyah);
		}
		int page = QuranInfo.getPageFromSuraAyah(nextAyah.getSoura(), nextAyah.getAyah());
		quranPageFeeder.jumpToPage(page);
	}

	@Override
	public void onAyahNotFound(AyahItem ayah) {
		lastAyah = ayah;
		showDownloadDialog(ayah);
	}

	@Override
	protected void loadLastNonConfigurationInstance() {
		super.loadLastNonConfigurationInstance();
		Object [] saved = (Object []) getLastNonConfigurationInstance();
		if (saved != null) {
			Log.d("exp_v", "Adapter retrieved..");
			quranPageFeeder = (QuranPageFeeder) saved[0];
		}
	}
	
	@Override
	protected void onFinishDownload() {
		super.onFinishDownload();
		if (quranAudioPlayer != null) {
			quranAudioPlayer.enableRemotePlay(false);
			playAudio(lastAyah);
		}
	}
	
	// temp method to select default reader id
	// we should put it in another place + we should read reader from preferences
	private int getQuranReaderId() {
		// TODO Auto-generated method stub
		return currentReaderId;
	}

	
}