package com.dealfaro.luca.clicker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;


public class ChatActivity extends ActionBarActivity {

  //last location
    Location lastLocation;
    //latitude
    double lat;
    //longitude
    double lng;

    String targetuserid,myuserid;

    private double lastAccuracy = (double) 1e10;
    private long lastAccuracyTime = 0;

    private static final String LOG_TAG = "lclicker";


    // This is an id for my app, to keep the key space separate from other apps.
    private static final String MY_APP_ID = "MY_ASG2_bboard";

    private static final String SERVER_URL_PREFIX = "https://hw3n-dot-luca-teaching.appspot.com/store/default/";

    // To remember the favorite account.
    public static final String PREF_ACCOUNT = "pref_account";

    // To remember the post we received.
    public static final String PREF_POSTS = "privatepref_posts";

    // Uploader.
    private ServerCall uploader;

    // Remember whether we have already successfully checked in.
    // private boolean checkinSuccessful = false;

    private static final float GOOD_ACCURACY_METERS = 100;

    private ArrayList<String> accountList;

    private class ListElement {
        ListElement() {};

        public String textLabel;
        public String buttonLabel;
        public String msg;
        public String ts;
        public String msgid;
        public String userid;
        public String dest;
        public Boolean conversation;
    }

    private ArrayList<ListElement> aList;

    private class MyAdapter extends ArrayAdapter<ListElement> {

        int resource;
        Context context;

        public MyAdapter(Context _context, int _resource, List<ListElement> items) {
            super(_context, _resource, items);
            resource = _resource;
            context = _context;
            this.context = _context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout newView;

            ListElement w = getItem(position);
            final String dest=w.dest;
            // Inflate a new view if necessary.
            if (convertView == null) {
                newView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource,  newView, true);
            } else {
                newView = (LinearLayout) convertView;
            }

            // Fills in the view.
            TextView tv = (TextView) newView.findViewById(R.id.itemText);
            Button b = (Button) newView.findViewById(R.id.itemButton);
            tv.setText(w.textLabel);
            b.setText(w.buttonLabel);
            ImageView iv=(ImageView)newView.findViewById(R.id.imageView);
            iv.setVisibility(View.GONE);
            if(w.conversation==true){
                iv.setVisibility(View.VISIBLE);
            }else{
                iv.setVisibility(View.GONE);
            }


            // Sets a listener for the button, and a tag for the button as well.
            b.setTag(new Integer(position));
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Reacts to a button press.
                    // Gets the integer tag of the button.
                    String s = v.getTag().toString();
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, s, duration);
                    toast.show();
                }
            });
            b.setVisibility(View.GONE);
            // Set a listener for the whole list item.
            newView.setTag(w.textLabel);
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String s = v.getTag().toString();
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, s, duration);
                    toast.show();


                }
            });

            return newView;
        }
    }

    private MyAdapter aa;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle extras=getIntent().getExtras();
        if(extras!=null){
            targetuserid=extras.getString("targetuserid");
            myuserid=extras.getString("myuserid");
            lat=extras.getDouble("lat");
            lng=extras.getDouble("lng");
        }
        //TextView tv2=(TextView)findViewById(R.id.textView);
        //tv2.setText("private conversation with "+targetuserid);
        //Log.i("Targetuserid",targetuserid);
        //Log.i("Myuserid",myuserid);
        aList = new ArrayList<ListElement>();
        aa = new MyAdapter(this, R.layout.list_element, aList);

        ListView myListView = (ListView) findViewById(R.id.PlistView);
        myListView.setAdapter(aa);
        aa.notifyDataSetChanged();
        ProgressBar pb=(ProgressBar)findViewById(R.id.progressBar2);
        pb.setVisibility(View.GONE);
        refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // First super, then do stuff.
        // Let us display the previous posts, if any.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String result = settings.getString(PREF_POSTS, null);
        if (result != null) {
            try {
                displayResult(result);
            } catch (Exception e) {
                // Removes settings that can't be correctly decoded.
                Log.w(LOG_TAG, "Failed to display old messages: " + result + " " + e);
                SharedPreferences.Editor editor = settings.edit();
                editor.remove(PREF_POSTS);
                editor.commit();
            }
        }
        //get location

        refresh();
    }



    public void pclickButton(View v) {
        Context context = this;
        //don't post if Accuracy>100
/*
        if(lastLocation.getAccuracy()>100){
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, "failed posting", duration);
            toast.show();
            return;
        }
*/
        ProgressBar pb=(ProgressBar)findViewById(R.id.progressBar2);
        pb.setVisibility(View.VISIBLE);
        // Get the text we want to send.
        EditText et = (EditText) findViewById(R.id.PeditText);
        //String msg = et.getText().toString();
        String msg = et.getText().toString();
        // Then, we start the call.
        PostMessageSpec myCallSpec = new PostMessageSpec();
        Date now = new Date();

        myCallSpec.url = SERVER_URL_PREFIX + "put_local.json";
        myCallSpec.context = ChatActivity.this;
        // Let's add the parameters.
        HashMap<String,String> m = new HashMap<String,String>();


        m.put("lat",lat+"");
        m.put("lng",lng+"");
        //m.put("lat",36.994762+"");
        //m.put("lng",-122.064969+"");
        m.put("userid",myuserid);
        m.put("dest",targetuserid);
        m.put("msgid", reallyComputeHash(msg));
        m.put("msg", msg);
        myCallSpec.setParams(m);
        // Actual server call. uploader=ServerCall
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
        et.setText("");


    }

    //function for refreshButton
    public void prefreshButton(View v){
        Context context=this;

        //don't refresh if Accuracy>100
/*
        if(lastLocation.getAccuracy()>100){
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, "failed refreshing", duration);
            toast.show();
            return;
        }
*/
        ProgressBar pb=(ProgressBar)findViewById(R.id.progressBar2);
        pb.setVisibility(View.VISIBLE);
        refresh();
    }

    public void refresh(){
        PostMessageSpec myCallSpec = new PostMessageSpec();
        myCallSpec.url = SERVER_URL_PREFIX + "get_local.json";
        myCallSpec.context = ChatActivity.this;
        HashMap<String,String> m = new HashMap<String,String>();
        m.put("lat",lat+"");
        m.put("lng",lng+"");
        Log.i("Chat lat",lat+"");
        Log.i("Chat lng",lng+"");
        m.put("userid",myuserid);
        m.put("dest",targetuserid);
        //m.put("lat",36.994762+"");
        //m.put("lng",-122.064969+"");
        myCallSpec.setParams(m);
        // Actual server call. uploader=ServerCall
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }

    private String reallyComputeHash(String s) {
        // Computes the crypto hash of string s, in a web-safe format.
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(s.getBytes());
            digest.update("My secret key".getBytes());
            byte[] md = digest.digest();
            // Now we need to make it web safe.
            String safeDigest = Base64.encodeToString(md, Base64.URL_SAFE);
            return safeDigest;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * This class is used to do the HTTP call, and it specifies how to use the result.
     */
    class PostMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                // Do something here, e.g. tell the user that the server cannot be contacted.
                Log.i(LOG_TAG, "The server call failed.");
                ProgressBar pb=(ProgressBar)findViewById(R.id.progressBar2);
                pb.setVisibility(View.GONE);
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, "cannot connect to server", duration);
                toast.show();
            } else {
                // Translates the string result, decoding the Json.
                Log.i(LOG_TAG, "Received string: " + result);
                displayResult(result);
                ProgressBar pb=(ProgressBar)findViewById(R.id.progressBar2);
                pb.setVisibility(View.GONE);
                // Stores in the settings the last messages received.
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_POSTS, result);
                editor.commit();
            }
        }
    }

    //function for displayResult
    private void displayResult(String result) {
        Gson gson = new Gson();
        MessageList ml = gson.fromJson(result, MessageList.class);
        // Fills aList, so we can fill the listView.
        aList.clear();
        for (int i = 0; i < ml.messages.length; i++) {
            ListElement ael = new ListElement();
            ael.textLabel = "Message: "+ml.messages[i].msg+"\n"+getRelevantTimeDiff(ml.messages[i].ts)
                    +"\nTo ID: "+ml.messages[i].dest+"\nFrom ID: "+ml.messages[i].userid;
            ael.buttonLabel = "Click";
            ael.userid=ml.messages[i].userid;
            ael.dest=ml.messages[i].dest;
            ael.msg=ml.messages[i].msg;
            ael.conversation=ml.messages[i].conversation;
            ael.ts=ml.messages[i].ts;
            ael.msgid=ml.messages[i].msgid;
            aList.add(ael);
        }

        aa.notifyDataSetChanged();
    }

    //Format the timestamp. get it from https://piazza.com/class/i7ndxrwv93p6vl?cid=175
//and https://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
    private String getRelevantTimeDiff(String ts){
        DateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        targetFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date timestampDate = null;

        //parse date.
        //this can easily throw a ParseException so you should probably catch that
        try {
            timestampDate = targetFormat.parse(ts);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        //Get current UTC time
        Date now = new Date();
        long timeNow,timestamp;
        timeNow=now.getTime();

        //calculate the time difference
        timestamp=timestampDate.getTime();
        long milliseconds=timeNow-timestamp;

        //int seconds = (int) (milliseconds / 1000) % 60 ;
        int minutes = (int) ((milliseconds / (1000*60)) % 60);
        int hours   = (int) ((milliseconds / (1000*60*60)) % 24);

        String output;
        /*
        if((milliseconds / 1000) >=3600){
            output=String.valueOf(hours )+" hours ago";
        }else{
            output=String.valueOf(minutes )+" minute ago";
        }
        */
        if((milliseconds / 1000) <3600){
            output=String.valueOf(minutes )+" minute ago";
        }else if((milliseconds / 1000) <(3600*24)){
            output=String.valueOf(hours )+" hours ago";
        }else{
            output=String.valueOf(milliseconds/1000/(3600*24) )+" days ago";
        }

        //output=String.valueOf(hours )+"hours "+String.valueOf(minutes )+" minute ago";

        return output;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
