package io.github.project_travel_mate.travel.mytrips;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dd.processbutton.FlatButton;
import com.gun0912.tedpicker.ImagePickerActivity;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lucasr.twowayview.TwoWayView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import adapters.NestedListView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import io.github.project_travel_mate.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import utils.Constants;

import static utils.Constants.API_LINK;

public class MyTripInfo extends AppCompatActivity {

    @BindView(R.id.image)
    ImageView iv;
    @BindView(R.id.head)
    TextView tite;
    @BindView(R.id.time)
    TextView date;
    @BindView(R.id.newfrriend)
    FlatButton add;
    @BindView(R.id.lv)
    TwoWayView twoway;
    @BindView(R.id.friendlist)
    NestedListView lv;
    @BindView(R.id.fname)
    AutoCompleteTextView frendname;

    private String mId;
    private String mTitle;
    private String mStart;
    private String mEnd;
    private String mCity;
    private String mFriendid;
    private String mNameYet;

    private List<String> mFname;

    private MaterialDialog mDialog;
    private Handler mHandler;
    public static final int INTENT_REQUEST_GET_IMAGES = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trip_info);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        mId = intent.getStringExtra(Constants.EXTRA_MESSAGE_ID);
        String img = intent.getStringExtra(Constants.EXTRA_MESSAGE_IMAGE);

        List<File> mediaimages = new ArrayList<>();
        List<File> imagesuri = new ArrayList<>();
        mFname = new ArrayList<>();

        Picasso.with(this).load(img).into(iv);

        mHandler    = new Handler(Looper.getMainLooper());

        String mainfolder = "/storage/emulated/0/Pictures/";
        File sdDir  = new File(mainfolder);
        File[] sdDirFiles = sdDir.listFiles();
        for (File singleFile : sdDirFiles) {
            if (!singleFile.isDirectory())
                mediaimages.add(singleFile);
        }
        mediaimages.add(null);

        MyTripInfoImagesAdapter ad = new MyTripInfoImagesAdapter(this, mediaimages);
        twoway.setAdapter(ad);

        frendname.setThreshold(1);

        mytrip();

        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @OnTextChanged(R.id.fname) void onTextChanged() {
        mNameYet = frendname.getText().toString();
        if (!mNameYet.contains(" ")) {
            friendautocomplete();
        }
    }

    @OnClick(R.id.newfrriend) void onClick() {
        addfriend();
    }

    private void mytrip() {

        mDialog = new MaterialDialog.Builder(MyTripInfo.this)
                .title(R.string.app_name)
                .content("Fetching trips...")
                .progress(true, 0)
                .show();

        // to fetch mCity names
        String uri = API_LINK + "trip/get-one.php?trip=" + mId;
        Log.v("executing", uri + " ");


        //Set up client
        OkHttpClient client = new OkHttpClient();
        //Execute request
        Request request = new Request.Builder()
                .url(uri)
                .build();
        //Setup callback
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Request Failed", "Message : " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                final String res = Objects.requireNonNull(response.body()).string();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject ob;
                        try {
                            ob = new JSONObject(res);
                            mTitle = ob.getString("mTitle");
                            mStart = ob.getString("start_time");
                            mEnd = ob.getString("end_time");
                            mCity = ob.getString("mCity");

                            tite.setText(mCity);
                            tite = findViewById(R.id.tname);
                            tite.setText(mTitle);
                            final Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(Long.parseLong(mStart) * 1000);
                            final String timeString =
                                    getResources().getString(R.string.text_started_on) +
                                    new SimpleDateFormat("dd-MMM", Locale.US).format(cal.getTime());
                            date.setText(timeString);

                            JSONArray arrr = ob.getJSONArray("users");
                            for (int i = 0; i < arrr.length(); i++) {
                                mFname.add(arrr.getJSONObject(i).getString("name"));
                            }

                            MyTripFriendnameAdapter dataAdapter = new MyTripFriendnameAdapter(MyTripInfo.this, mFname);
                            lv.setAdapter(dataAdapter);

                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        mDialog.dismiss();
                    }
                });

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_REQUEST_GET_IMAGES && resultCode == Activity.RESULT_OK) {
            ArrayList<Uri> imageUris = data.getParcelableArrayListExtra(ImagePickerActivity.EXTRA_IMAGE_URIS);
            Toast.makeText(MyTripInfo.this, "Images added", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    private void friendautocomplete() {

        String uri = API_LINK + "users/find.php?search=" + mNameYet.trim();
        Log.v("EXECUTING", uri);

        //Set up client
        OkHttpClient client = new OkHttpClient();
        //Execute request
        Request request = new Request.Builder()
                .url(uri)
                .build();
        //Setup callback
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Request Failed", "Message : " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, final Response response) {

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray arr;
                        final ArrayList list, list1;
                        try {
                            arr = new JSONArray(Objects.requireNonNull(response.body()).string());

                            list = new ArrayList<>();
                            list1 = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                try {
                                    list.add(arr.getJSONObject(i).getString("name"));
                                    list1.add(arr.getJSONObject(i).getString("mId"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e("ERROR ", "Message : " + e.getMessage());
                                }
                            }
                            ArrayAdapter<String> dataAdapter =
                                    new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_layout, list);
                            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            frendname.setThreshold(1);
                            frendname.setAdapter(dataAdapter);
                            frendname.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                                    mFriendid = list1.get(arg2).toString();
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("ERROR", "Message : " + e.getMessage());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });

            }
        });
    }

    private void addfriend() {

        mDialog = new MaterialDialog.Builder(MyTripInfo.this)
                .title(R.string.app_name)
                .content("Please wait...")
                .progress(true, 0)
                .show();

        String uri = API_LINK + "trip/add-user.php?user=" + mFriendid + "&trip=" + mId;
        Log.v("EXECUTING", uri);

        //Set up client
        OkHttpClient client = new OkHttpClient();
        //Execute request
        Request request = new Request.Builder()
                .url(uri)
                .build();
        //Setup callback
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Request Failed", "Message : " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, final Response response) {

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MyTripInfo.this, "City added", Toast.LENGTH_LONG).show();
                        finish();
                        mDialog.dismiss();
                    }
                });

            }
        });
    }
}
