package com.liveunite.activities;


import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.adapter.AdapterPosts;
import com.liveunite.adapter.AdapterProfileMedia;
import com.liveunite.fragments.SinglePostFragment;
import com.liveunite.interfaces.LiveUniteApi;
import com.liveunite.models.FeedsRequest;
import com.liveunite.models.FeedsResponse;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.CheckInternetConnection;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ExtraMethods;
import com.liveunite.utils.UpdateLocations;
import com.liveunite.network.ServiceGenerator;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class ViewProfile extends AppCompatActivity implements SinglePostFragment.onDelete, EditProfile.onDPchange {

    private static ViewProfile viewProfile;
    private RecyclerView recyclerView;
    Context context;
    AdapterProfileMedia mAdapter;
    ArrayList<FeedsResponse> arrayList;
    Toolbar toolbar;
    int count = 0;


    private LiveUniteApi liveUniteApi;
    FeedsRequest feedsRequest = new FeedsRequest();
    private boolean isMoreDataAvailable = true;
    private boolean isOwner = false;


    public ViewProfile() {
        // Required empty public constructor
    }


    private String profileUserFbId;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);
        viewProfile = this;
        context = this;
        setUpToolbar();

        if(getIntent().getExtras()!=null){
            //others
            profileUserFbId = getIntent().getExtras().getString("fbId");

        }else{
            profileUserFbId = LiveUnite.getInstance().getPreferenceManager().getFbId();

        }

        LiveUnite.getInstance().getPreferenceManager().setIsProfileOwner(profileUserFbId.equals(LiveUnite.getInstance().getPreferenceManager().getFbId()));

    }

    @Override
    protected void onResume() {
        super.onResume();
        initView();
    }

    private void setUpToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Profile");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_view_profile, menu);

        if(profileUserFbId.equals(LiveUnite.getInstance().getPreferenceManager().getFbId())){

            menu.findItem(R.id.iEdit).setVisible(true);

        }else{
            menu.findItem(R.id.iEdit).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.iEdit:
                new ChangeActivity().change(context, EditProfile.class);
                break;
        }
        return true;
    }

    private void setUpRequest() {
        this.feedsRequest = new ExtraMethods().getFeedRequest(profileUserFbId, "0");
    }

    private void setAdapter() {
        arrayList = new ArrayList<>();
        arrayList.add(null); // for header
        mAdapter = new AdapterProfileMedia(context, arrayList, Singleton.getInstance().getScreenWidth() / Constant.MEDIA_IN_ONE_ROW_PROFILE) {
            @Override
            public void postClick(int position, FeedsResponse feedsResponse) {
                Singleton.getInstance().setFeedsResponse(feedsResponse);
                Singleton.getInstance().setTempPosition(position);
                BottomSheetDialogFragment bottomSheetDialogFragment = new SinglePostFragment();
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        };
        mAdapter.setLoadMoreListener(new AdapterPosts.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        int index = arrayList.size() - 1;
                        loadMore(index);
                    }
                });
            }
        });

        recyclerView.setAdapter(mAdapter);
        liveUniteApi = ServiceGenerator.createService(LiveUniteApi.class);
        load(0);
    }


    private void load(int index) {
        count = 0;
        setUpRequest();
        //  arrayList = new ArrayList<>();
        feedsRequest.setFromLimit(0);
        feedsRequest.setToLimit(Constant.LOAD_FEEDS_PROFILE_MEDIA);
        final Call<ArrayList<FeedsResponse>> feedsResponseCall = liveUniteApi.getFeeds(feedsRequest);

        feedsResponseCall.enqueue(new Callback<ArrayList<FeedsResponse>>() {
            @Override
            public void onResponse(Call<ArrayList<FeedsResponse>> call, Response<ArrayList<FeedsResponse>> response) {
                int statusCode = response.code();
                Log.e("MomentsFragment", response.toString());
                if (response.isSuccessful() && response.body() != null && response.body().size() > 0) {
                    if (response.body().get(0).getSuccess().equals("1")) {
                        count++;
                        arrayList.addAll(response.body());
                        mAdapter.notifyDataChanged();
                    }
                } else {
                    Log.e("MomentsFragment", " Response Error " + String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(Call<ArrayList<FeedsResponse>> call, Throwable t) {
                Log.e("Onfail MomentsFragm", " Response Error " + t.getMessage());
            }
        });
    }


    private void initView() {


        inilizeRetrofit();

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, Constant.MEDIA_IN_ONE_ROW_PROFILE);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (position == 0) ? Constant.MEDIA_IN_ONE_ROW_PROFILE : 1;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        if (new CheckInternetConnection(context).isConnectedToInternet()) {
            setAdapter();
        } else {
            new CheckInternetConnection(context).showDialog();
        }

    }

    private void inilizeRetrofit() {
        liveUniteApi = new ExtraMethods().getLiveUniteAPI();
    }

    private void loadMore(int index) {

        UpdateLocations locations = new UpdateLocations(context);
        locations.initiateLocationFetch();

        if (count % 2 == 0 && !locations.getLocationStatus()) {
            Toast.makeText(context, "Unable to get your location", Toast.LENGTH_LONG).show();
            return;
        }
        setUpRequest();
        //add loading progress view
        arrayList.add(null);
        mAdapter.notifyItemInserted(arrayList.size() - 1);

        feedsRequest.setFromLimit(Constant.LOAD_FEEDS_PROFILE_MEDIA * count);
        feedsRequest.setToLimit(Constant.LOAD_FEEDS_PROFILE_MEDIA);
        final Call<ArrayList<FeedsResponse>> feedsResponseCall = liveUniteApi.getFeeds(feedsRequest);
        feedsResponseCall.enqueue(new Callback<ArrayList<FeedsResponse>>() {
            @Override
            public void onResponse(Call<ArrayList<FeedsResponse>> call, Response<ArrayList<FeedsResponse>> response) {
                int statusCode = response.code();
                if (response.isSuccessful() && response.body() != null) {
                    count++;
                    arrayList.remove(arrayList.size() - 1);
                    if (response.body().size() > 0) {
                        arrayList.addAll(response.body());
                    } else {
                        mAdapter.setMoreDataAvailable(false);
                    }
                    mAdapter.notifyDataChanged();
                } else {
                    Log.e("MomentsFragment", " Load More Response Error " + String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(Call<ArrayList<FeedsResponse>> call, Throwable t) {
                Log.e("MomentsFragment", " Load More Response Error " + t.getMessage());
            }
        });
    }

    @Override
    public void onDelete(int position) {
        arrayList.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    public static ViewProfile getInstance() {
        return viewProfile;
    }

    @Override
    public void onDPchange() {
        if (mAdapter != null) {
            mAdapter.notifyDataChanged();
        }
    }
}
