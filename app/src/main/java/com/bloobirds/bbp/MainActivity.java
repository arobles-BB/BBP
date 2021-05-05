package com.bloobirds.bbp;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bbp.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class is both Activity and a Loader at the same time! (welcome to java multi inheritance)
 * We are going to use Loaders (outdated since Android 9) for Backward compatibility.
 * A Loader focuses in data like this one, will monitor changes for an specific data source
 * Every Loader works on a separated Thread
 */
public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "BBP - CallLog"; // ID to the log trace
    private static final int URL_LOADER = 1;
    private static final String MY_PREFS_NAME = "BBprefs";
    private static final String MY_TOKEN = "BB.TOKEN";

    private TextView callLogsTextView; // main canvas to write down results

    /**
     * callback method called when the system needs the activity to be created.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        // our view only wors in portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // set the view
        setContentView(R.layout.main);
        initialize();
    }

    /**
     * Internal method to initialize view, permissions and session with the server
     */
    private void initialize() {
        Log.d(TAG, "initialize()");

        Button btnCallLog = (Button) findViewById(R.id.btn_call_log);

        btnCallLog.setOnClickListener(v -> {
            Log.d(TAG, "initialize() >> initialize loader");
            getLoaderManager().initLoader(URL_LOADER, null, MainActivity.this); // onCreate will be called if the button is pressed
        });

        callLogsTextView = (TextView) findViewById(R.id.call_logs);

        int prLog = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG);

        if (prLog == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, URL_LOADER);

        // One of the multiple ways of sharing info between Loaders
        SharedPreferences sp = getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE);
        // If we never had a token, ask for one
        if (!sp.contains(MY_TOKEN)) login(sp);
        
    }

    /**
     * Internal method to open a valid session with the server
     * TODO: really open the session
     * TODO: show the dialog for user/pass (finish MyLoginView.java and registration.xml)
     * @param sp
     */
    private void login(SharedPreferences sp) {
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(MY_TOKEN);

        // If you don't have the user/pass do some new input View
        // Do some login logic
        String bb_token ="";
        String bb_user="";

        // Once you have the token, store it for future uses
        editor.putString(MY_TOKEN, bb_token);
    }

    /**
     * callback method called when the system needs the loader to be created.
     * @param loaderID
     * @param args
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
        Log.d(TAG, "onCreateLoader() >> loaderID : " + loaderID);

        switch (loaderID) {
            // we could be implementing multiple Cursors in the same class, we need to identify which one
            case URL_LOADER:
                // Returns a new CursorLoader
                return new CursorLoader(
                        this,   // Parent activity context
                        CallLog.Calls.CONTENT_URI,        // Table to query
                        null,     // Projection to return
                        null,            // No selection clause
                        null,            // No selection arguments
                        null             // Default sort order
                );
            default:
                return null;
        }

    }

    /**
     * Called once the Cursor has been fully loaded
     * @param loader
     * @param managedCursor
     */
    @RequiresApi(api = Build.VERSION_CODES.N) // That's because the "sort predicates". If you take them out, you may take this as well
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor managedCursor) {
        Log.d(TAG, "onLoadFinished()");

        StringBuilder sb = new StringBuilder();
        ArrayList<CallRecord> callLog = new ArrayList<CallRecord>();

        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

        while (managedCursor.moveToNext()) {
            String phNumber = managedCursor.getString(number);
            String callType = managedCursor.getString(type);
            String callDate = managedCursor.getString(date);
            Date callDayTime = new Date(Long.parseLong(callDate));
            String callDuration = managedCursor.getString(duration);
            String dir = null;

            int callTypeCode = Integer.parseInt(callType);
            switch (callTypeCode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "Outgoing";
                    break;

                case CallLog.Calls.INCOMING_TYPE:
                    dir = "Incoming";
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    dir = "Missed";
                    break;
                default: // we omit other call types...
                    dir = "Other";
                    break;
            }

            Calendar calendar = new GregorianCalendar();
            calendar.setTime(callDayTime);
            int year       = calendar.get(Calendar.YEAR);
            int month      = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            int hourOfDay  = calendar.get(Calendar.HOUR_OF_DAY); // 24 hour clock
            int minute     = calendar.get(Calendar.MINUTE);

            callLog.add(new CallRecord(calendar.getTime(),callDuration,phNumber,callType,dir));

            Log.d(TAG, "Log ["+dir+"] ["+phNumber+"] ["+dayOfMonth+"-"+month+"-"+year+"]  ["+callDuration+"s]");
        }

        managedCursor.close();

        sb.append("Total calls: "+callLog.size());

        // REMOVE TO LIMIT VERSION CODES
        // ---8<------8<------8<------8<------8<------8<------8<---

        Predicate<CallRecord> out = CallRecord -> Integer.parseInt(CallRecord.getCallType()) == CallLog.Calls.OUTGOING_TYPE ;
        Predicate<CallRecord> in = CallRecord -> Integer.parseInt(CallRecord.getCallType()) == CallLog.Calls.INCOMING_TYPE;
        Predicate<CallRecord> miss = CallRecord -> Integer.parseInt(CallRecord.getCallType()) == CallLog.Calls.MISSED_TYPE;

        List<CallRecord> resultOut = new ArrayList<>();
        List<CallRecord> resultIn = new ArrayList<>();
        List<CallRecord> resultMiss = new ArrayList<>();
        for (CallRecord callRecord : callLog) {
            if (out.test(callRecord)) {
                resultOut.add(callRecord);
            } else if (in.test(callRecord)) {
                resultIn.add(callRecord);
            } else if (miss.test(callRecord)) {
                resultMiss.add(callRecord);
            }
        }

        // StringBuffer will be rendered as HTML
        sb.append("<BR>Total outbound: "+resultOut.size());
        sb.append("<BR>Total inbound: "+resultIn.size());
        sb.append("<BR>Total missed: "+resultMiss.size());

        // ---8<------8<------8<------8<------8<------8<------8<---

        callLogsTextView.setText(Html.fromHtml(sb.toString()));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset()");
        // we need to do nothing right now
        // probably check if the token still valid
    }
}