/* Released under MIT License http://opensource.org/licenses/MIT */
/* The bluetooth related code is heavily based on code from: Plasty Grove */
/* The Speech related code was based on the YouTube tutorial: #65 Android Application Development Tutorial For Beginners - Text To Speech : Part - 2 */
/* https://www.youtube.com/watch?v=sU38Yhux-3g, https://www.facebook.com/smartherd */
/*Everything else (like glue-ing both together and most important, the idea of the project, by Jan Derogee*/

/*This application prevents the screensaver from kicking in by using the line: android:keepScreenOn = "true" in the file activity_main.xml (nothing more required)*/


/*

Changing Language

You can change language to speak by using setLanguage() function. Lot of languages are supported like Canada, French, Chinese, Germany etc.,
tts.setLanguage(Locale.CHINESE); // Chinese language

Changing Pitch Rate

You can set speed pitch level by using setPitch() function. By default the value is 1.0 You can set lower values than 1.0 to decrease pitch
level or greater values for increase pitch level.
tts.setPitch((float)0.6);

Changing Speed Rate

The speed rate can be set using setSpeechRate(). This also will take default of 1.0 value.file.createNewFile();
 You can double the speed rate by setting 2.0 or make half the speed level by setting 0.5
tts.setSpeechRate((float)2);


 */



package com.blueserial;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import com.blueserial.R;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
//write to file
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.*;
import android.view.*;
import android.content.Context;
import android.os.Environment;



public class MainActivity extends Activity {
	private static final String TAG = "GN-MainActivity";
	private int mMaxChars = 50000;//Default
	private UUID mDeviceUUID;
	private BluetoothSocket mBTSocket;
	private ReadInput mReadThread = null;
	private boolean mIsUserInitiatedDisconnect = false;
    public   int test1 = 10;
	// All controls here
	private TextView mTxtReceive;
	private ScrollView scrollView;
	private CheckBox chkScroll;

	private boolean mIsBluetoothConnected = false;
	private BluetoothDevice mDevice;
	private ProgressDialog progressDialog;

    private TextToSpeech ttsobject;

    private int result;



    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        ActivityHelper.initialize(this);
		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		mDevice = b.getParcelable(Homescreen.DEVICE_EXTRA);
		mDeviceUUID = UUID.fromString(b.getString(Homescreen.DEVICE_UUID));
		mMaxChars = b.getInt(Homescreen.BUFFER_SIZE);
		Log.d(TAG, "test1" + test1);
		mTxtReceive = (TextView) findViewById(R.id.txtReceive);
		scrollView = (ScrollView) findViewById(R.id.viewScroll);
		chkScroll = (CheckBox) findViewById(R.id.chkScroll);
		mTxtReceive.setMovementMethod(new ScrollingMovementMethod());



        ttsobject = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                if(status == TextToSpeech.SUCCESS){
                    result = ttsobject.setLanguage(Locale.ENGLISH);
                    SpeakSentence("Speech synthesis has been enabled.");
                }else{
                    Toast.makeText(getApplicationContext(),"Feature not supported on your device", Toast.LENGTH_SHORT).show();
                }
            }
        });



        SeekBar skbR = (SeekBar) findViewById((R.id.skbRepeat));
        skbR.setMax(20);
        skbR.setProgress(7);
        skbR.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                test1 = progresValue;

            }
        });

        ttsobject.setSpeechRate((float) (1));
        ttsobject.setPitch((float) (1.2));





	}

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(ttsobject != null){
            ttsobject.stop();
            ttsobject.shutdown();
        }
    }



    //public  void gndate(String[] args) {
        //getting current date and time using Date class
        Date gndate = new Date();
        DateFormat df = new SimpleDateFormat("ddMMyy");
        DateFormat tf = new SimpleDateFormat("HH:mm:ss");
    String DateToStr = df.format(gndate);
    String TimeToStr = tf.format(gndate);
        //System.out.println(df.format(dateobj));

    //File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);





    public void HandleButtons(View v){
        switch (v.getId()){
            case R.id.btnDisconnect:
                mIsUserInitiatedDisconnect = true;
                new DisConnectBT().execute();
                break;

            case R.id.btnClearInput:
                mTxtReceive.setText("");
                break;

            case R.id.btnRepeat:
                SpeakSentence(mTxtReceive.getText().toString());
                break;

            case R.id.btnStopSpeaking:
                if(ttsobject != null){
                    ttsobject.stop();
                }
                break;
        }
    }


    private void SpeakSentence(String text){
        if(result == TextToSpeech.LANG_NOT_SUPPORTED || result==TextToSpeech.LANG_MISSING_DATA){
            Toast.makeText(getApplicationContext(),"Feature not supported on your device", Toast.LENGTH_SHORT).show();
        }else {
            ttsobject.speak(text, TextToSpeech.QUEUE_FLUSH, null);  /*speak the text*/
            //ttsobject.speak(text, TextToSpeech.QUEUE_ADD, null); /*it is better to add to the queue, then to wait until it finishes*
            // ttsobject.speak(text, TextToSpeech.QUEUE_FLUSH, null);  /*speak the text*/
            // while (ttsobject.isSpeaking() == true){};               /*wait until finished*/

        }
    }


    private class ReadInput implements Runnable {
		public boolean bStop = false;
		private Thread t;

		public ReadInput() {
			t = new Thread(this, "Input Thread");
			t.start();
		}

		public boolean isRunning(){
            return t.isAlive();
		}

		@Override
		public void run() {
            InputStream is = null;


            int i;
            int hspeed= 0; // hundred mph
            int tspeed=0; // tens mph
            int uspeed= 0; // units mph
            int maxspeed=0;
            int tmpspeed=0;
            long pasttime = System.currentTimeMillis();
			//InputStream inputStream;
            //mTxtReceive.append("start loop  ");
            //byte[] chr = new byte[2];
            char c;
            int crfg = 0 ;

			try {
				//inputStream = mBTSocket.getInputStream();
               is = mBTSocket.getInputStream();

                while((i=is.read())!=-1)
                {
                    // converts integer to character
                    c=(char)i;
//Log.d(TAG, "loop " + c);


                    //System.out.println("crfg= " + crfg + " c= " + c + " Char= " + Character.getNumericValue(c) + " i= " + i );
                    if ( i == 13){
                        crfg = 1;
                    }

                    if (crfg == 2){

                        hspeed =  Character.getNumericValue(c);
                        Log.d(TAG, String.valueOf("hspeed " + hspeed)) ;
                    }

                    if (crfg == 3 ){

                        tspeed =  Character.getNumericValue(c);
                        Log.d(TAG, String.valueOf("tspeed " + tspeed)) ;
                    }

                    if (crfg == 4 ){
                        uspeed =  Character.getNumericValue(c);

                    }




                    if (crfg >= 1) {crfg++ ;}
                   // Log.d(TAG, "crfg = " + crfg ) ;


                    if (crfg == 6){
                        crfg = 0;
                        tmpspeed= ((hspeed * 100) +(tspeed *10)  + uspeed);



                        if (tmpspeed < 300 || tmpspeed > maxspeed) {
                                maxspeed = tmpspeed;
                            }


                        Log.d(TAG, "test1 " +String.valueOf(test1)) ;


                        //long starttime = System.currentTimeMillis();
                        if(System.currentTimeMillis() >= (pasttime + test1*1000 )) { //multiply by 1000 to get milliseconds
                            pasttime = System.currentTimeMillis() + test1*1000;
                            SpeakSentence(Integer.toString(maxspeed));
                            hspeed= 0 ;
                            tspeed=0;
                            uspeed=0;

                            final String strInput = Integer.toString (maxspeed);
//
                           // writeDataToFile(strInput);

                            mTxtReceive.post(new Runnable() {
                                @Override
                                public void run() {
                                    mTxtReceive.append(strInput);
                                    //Uncomment below for testing
                                    mTxtReceive.append(" ");
                                    //mTxtReceive.append("Chars: " + strInput.length() + " Lines: " + mTxtReceive.getLineCount() + "\n");

                                    int txtLength = mTxtReceive.getEditableText().length();
                                    if (txtLength > mMaxChars) {
                                        mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
                                    }

                                    if (chkScroll.isChecked()) {
                                        scrollView.post(new Runnable() {

                                            @Override
                                            public void run() {
                                                scrollView.fullScroll(View.FOCUS_DOWN);
                                            }
                                        });
                                    }
                                }
                            });
                        }



                        //mTxtReceive.append(Integer.toString (maxspeed));


                        //laps++ ;
                        //Log.d(TAG, "laps= " + laps);

                       //end new code
                    }


                    Thread.sleep(1);
maxspeed = 0;

                }

            } catch (IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            catch (InterruptedException e){
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}

		public void stop() {bStop = true;
		}

	}

	private class DisConnectBT extends AsyncTask<Void, Void, Void>{
		@Override
		protected void onPreExecute(){
		}

		@Override
		protected Void doInBackground(Void... params){
			if (mReadThread != null){
				mReadThread.stop();
				while (mReadThread.isRunning()); // Wait until it stops
				mReadThread = null;
			}

			try {
				mBTSocket.close();
			} catch (IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result){
			super.onPostExecute(result);
			mIsBluetoothConnected = false;
			if (mIsUserInitiatedDisconnect){
				finish();
			}
		}
	}

	private void msg(String s){
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPause(){
		if (mBTSocket != null && mIsBluetoothConnected){
			new DisConnectBT().execute();
		}
		Log.d(TAG, "Paused");
		super.onPause();
	}

	@Override
	protected void onResume(){
		if (mBTSocket == null || !mIsBluetoothConnected){
			new ConnectBT().execute();
		}
		Log.d(TAG, "Resumed");
		super.onResume();
	}

	@Override
	protected void onStop(){
		Log.d(TAG, "Stopped");
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState){
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}




	private class ConnectBT extends AsyncTask<Void, Void, Void>{
		private boolean mConnectSuccessful = true;
		@Override
		protected void onPreExecute(){
			progressDialog = ProgressDialog.show(MainActivity.this, "Please wait a moment", "Connecting");// http://stackoverflow.com/a/11130220/1287554
		}


        @Override
		protected Void doInBackground(Void... devices){
			try {
				if (mBTSocket == null || !mIsBluetoothConnected) {
					mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					mBTSocket.connect();
				}
			} catch (IOException e){
				// Unable to connect to device
				e.printStackTrace();
				mConnectSuccessful = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (!mConnectSuccessful) {
				Toast.makeText(getApplicationContext(), "Could not connect to device. Is it a Serial device? Also check if the UUID is correct in the settings", Toast.LENGTH_LONG).show();
				finish();
			} else {
				msg("Connected to device");
				mIsBluetoothConnected = true;
				mReadThread = new ReadInput(); // Kick off input reader
			}

			progressDialog.dismiss();
		}
	}
}
