package com.liveunite.activities;

import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.appyvet.rangebar.RangeBar;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.interfaces.LiveUniteApi;
import com.liveunite.models.DeletePostResponce;
import com.liveunite.models.UpdateSettingRequest;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.network.Urls;
import com.liveunite.utils.Constant;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class DiscoverySetting extends AppCompatActivity {

    RangeBar sDistance,sAge;
    TextView tvDistance,tvAge,tvMessage;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery_setting);
        context = DiscoverySetting.this;
        init();
        setUpToolbar();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }
    private void update() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Urls.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LiveUniteApi liveUniteApi = retrofit.create(LiveUniteApi.class);


        UpdateSettingRequest postRequest = new UpdateSettingRequest();
        postRequest.setAndroidAppToken(Singleton.getInstance().getUserDetails().getAndroidAppToken());
        postRequest.setFbId(LiveUnite.getInstance().getPreferenceManager().getFbId());
        postRequest.setId(LiveUnite.getInstance().getPreferenceManager().getUserID());
        postRequest.setMaxAge(Integer.parseInt(sAge.getRightPinValue()));
        postRequest.setMaxDistance(Integer.parseInt(sDistance.getRightPinValue()));
        postRequest.setMinAge(Integer.parseInt(sAge.getLeftPinValue()));

        Call<ArrayList<DeletePostResponce>> deletePostResponceCall = liveUniteApi.updateSetting(postRequest);

        deletePostResponceCall.enqueue(new Callback<ArrayList<DeletePostResponce>>() {
            @Override
            public void onResponse(Call<ArrayList<DeletePostResponce>> call, Response<ArrayList<DeletePostResponce>> response) {
                Log.e("Responce delete Post",response.body().toString());
                int statusCode = response.code();
                if (statusCode == 200 && response.body().size() > 0)
                {
                    if (response.body().get(0).getSuccess().equals("1"))
                    {
                      //  Toast.makeText(context,"Successfully updated discovery setting!!",Toast.LENGTH_SHORT).show();
                        Singleton.getInstance().getUserDetails().setMaxDistance(Integer.parseInt(sDistance.getRightPinValue()));
                        Singleton.getInstance().getUserDetails().setMinAge(Integer.parseInt(sAge.getLeftPinValue()));
                        Singleton.getInstance().getUserDetails().setMaxAge(Integer.parseInt(sAge.getRightPinValue()));
                    }
                }

            }

            @Override
            public void onFailure(Call<ArrayList<DeletePostResponce>> call, Throwable t) {
                Log.e("ResponcedeletePost fail",t.toString());
            }
        });
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("Discovery Setting");
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id)
        {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        sDistance = (RangeBar) findViewById(R.id.sDistance);
        sAge = (RangeBar)findViewById(R.id.sAge);
        tvDistance = (TextView)findViewById(R.id.tvDistance);
        tvAge = (TextView)findViewById(R.id.tvAge);
        tvMessage = (TextView)findViewById(R.id.tvMessage);
        setUpMessage();
        setDistance();
        setAge();
    }

    private void setUpMessage() {
        if (Singleton.getInstance().getUserDetails().getMaxDistance() == -99 ||
                Singleton.getInstance().getUserDetails().getMinAge() == -99 ||
                Singleton.getInstance().getUserDetails().getMaxAge() == -99 )
        {
            tvMessage.setText("Currently it is set as default \n(Once we have more users you can customize it)");
        }else
        {
            tvMessage.setText("");
        }
    }

    private void setDistance() {
       sDistance.setTickStart(Constant.DISTANCE_START);
        sDistance.setTickEnd(Constant.DISTANCE_END);
        sDistance.setTickInterval(Constant.DISTANCE_INTERVAL);
        float maxDistance = Singleton.getInstance().getUserDetails().getMaxDistance();
        if (maxDistance >= Constant.DISTANCE_START && maxDistance<=Constant.DISTANCE_END)
        {
            sDistance.setSeekPinByValue(maxDistance);
        }else
        {
            sDistance.setSeekPinByValue(Constant.DISTANCE_END);
        }
        setDistanceText();
        sDistanceChange();
    }

    private void setDistanceText() {
        tvDistance.setText(sDistance.getRightPinValue() + " Km");
    }

    private void sDistanceChange() {
        sDistance.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
                setDistanceText();
                update();
            }
        });
    }
    private void sAgeChange() {
        sAge.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
                setAgeText();
                update();
            }
        });
    }

    private void setAge() {
        sAge.setTickStart(Constant.AGE_START);
        sAge.setTickEnd(Constant.AGE_END);
        sAge.setTickInterval(Constant.AGE_INTERVAL);
        float maxAge = Singleton.getInstance().getUserDetails().getMaxAge();
        float minAge = Singleton.getInstance().getUserDetails().getMinAge();
        if (minAge >= Constant.AGE_START && maxAge<=Constant.AGE_END)
        {
            sAge.setRangePinsByValue(minAge,maxAge);
        }else
        {
            sAge.setRangePinsByValue(Constant.AGE_START,Constant.AGE_END);
        }
        setAgeText();
        sAgeChange();
    }

    private void setAgeText() {
        tvAge.setText(sAge.getLeftPinValue() + "-" + sAge.getRightPinValue() + "years");
    }
}
