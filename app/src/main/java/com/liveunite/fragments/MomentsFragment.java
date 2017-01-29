package com.liveunite.fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.activities.HomeActivity;
import com.liveunite.activities.MapActivity;
import com.liveunite.adapter.AdapterPosts;
import com.liveunite.interfaces.LiveUniteApi;
import com.liveunite.models.FeedsRequest;
import com.liveunite.models.FeedsResponse;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.services.UploadService;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.CheckInternetConnection;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ExtraMethods;
import com.liveunite.utils.UpdateLocations;
import com.liveunite.network.ServiceGenerator;

import java.io.File;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class MomentsFragment extends Fragment {

    private RecyclerView recyclerView;
    Context context;
    Toolbar tToolbar;
    private boolean isLoading;
    private int visibleThreshold = 10;
    private int lastVisibleItem, totalItemCount;
    AdapterPosts mAdapter;
    ArrayList<FeedsResponse> arrayList = new ArrayList<>();
    int onGoingUploads=0;
    int count=0;


    static MomentsFragment momentsFragment;

    // The elevation of the toolbar when content is scrolled behind
    private static final float TOOLBAR_ELEVATION = 14f;
    // To save/restore recyclerview state on configuration changes
    private static final String STATE_RECYCLER_VIEW = "state-recycler-view";
    private static final String STATE_VERTICAL_OFFSET = "state-vertical-offset";
    private static final String STATE_SCROLLING_OFFSET = "state-scrolling-direction";
    private static final String STATE_TOOLBAR_ELEVATION = "state-toolbar-elevation";
    private static final String STATE_TOOLBAR_TRANSLATION_Y = "state-toolbar-translation-y";
    SwipeRefreshLayout swipeRefreshLayout;

    private int verticalOffset;
    // Determines the scroll UP/DOWN offset
    private int scrollingOffset;
    private LiveUniteApi liveUniteApi;
    FeedsRequest feedsRequest = new FeedsRequest();
    private boolean isMoreDataAvailable = true;
    private boolean canRefresh = true;
    private boolean isUploading = false;
    ProgressBar progressBar;

    TextView errorPanel;
    TextView retryBtn;


    public MomentsFragment() {
        // Required empty public constructor
    }

    public static MomentsFragment getMomentsFragment() {
        return momentsFragment;
    }

    public static void setMomentsFragment(MomentsFragment momentsFragment) {
        MomentsFragment.momentsFragment = momentsFragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_moments, container, false);
        context = getContext();
        setMomentsFragment(this);
        setUpRequest();
        initView(view);
        tToolbar = HomeActivity.mToolbar;

        if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                HomeActivity.mToolbar.setElevation(savedInstanceState.getFloat(STATE_TOOLBAR_ELEVATION));
            }
            HomeActivity.mToolbar.setTranslationY(savedInstanceState.getFloat(STATE_TOOLBAR_TRANSLATION_Y));
            verticalOffset = savedInstanceState.getInt(STATE_VERTICAL_OFFSET);
            scrollingOffset = savedInstanceState.getInt(STATE_SCROLLING_OFFSET);
            recyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable(STATE_RECYCLER_VIEW));
        }


        return view;
    }

    private void setUpRequest() {
        this.feedsRequest = new ExtraMethods().getFeedRequest(LiveUnite.getInstance().getPreferenceManager().getFbId(),"1");
    }

    private void setAdapter() {
        arrayList = new ArrayList<>();
        mAdapter = new AdapterPosts(context, arrayList, Singleton.getInstance().getScreenWidth()) {
            @Override
            public void distanceClick(FeedsResponse mFeedsResponse) {
                Singleton.getInstance().setFeedsResponse(mFeedsResponse);
                new ChangeActivity().change(context, MapActivity.class);
            }

            @Override
            public void onRetry(FeedsResponse mFeedsResponse, int position) {
                Intent intent = new Intent(context,UploadService.class);
                intent.putExtra(Constant.UPLOAD_FILENAME,mFeedsResponse.getFile().getName());
                intent.putExtra(Constant.UPLOAD_CAPTION,mFeedsResponse.getCaption().trim());
                intent.putExtra(Constant.UPLOAD_AUTODELETE,false);
                intent.putExtra(Constant.UPLOAD_TYPE,Constant.TYPE_PICTURE);
                context.startService(intent);
                if (arrayList.size()>position)
                {
                    arrayList.remove(position);
                    ((HomeActivity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyDataChanged();
                        }
                    });
                }
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

    @Override
    public void onResume() {
        super.onResume();
        setUpRequest();
    }

    private void load(int index) {
        // arrayList = new ArrayList<>();
        Log.e("Optimization"," feeds load request");
        count=0;
        feedsRequest.setFromLimit(index);
        feedsRequest.setToLimit(index + Constant.LOAD_FEEDS_LIMITS);

        UpdateLocations locations = new UpdateLocations(context);
        locations.initiateLocationFetch();

        if (!locations.getLocationStatus())
        {
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            errorPanel.setVisibility(View.VISIBLE);
            errorPanel.setText("Unable To Get Your Location ! ");
            retryBtn.setVisibility(View.VISIBLE);
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        errorPanel.setVisibility(View.GONE);
        retryBtn.setVisibility(View.GONE);


        Log.d("LocationTest"," long "+Singleton.getInstance().getUserLocationModal().getLongitude()+" lat "+Singleton.getInstance().getUserLocationModal().getLatitude());
        feedsRequest.setLongitude(Singleton.getInstance().getUserLocationModal().getLongitude());
        feedsRequest.setLatitude(Singleton.getInstance().getUserLocationModal().getLatitude());
        final Call<ArrayList<FeedsResponse>> feedsResponseCall = liveUniteApi.getFeeds(feedsRequest);

        feedsResponseCall.enqueue(new Callback<ArrayList<FeedsResponse>>() {
            @Override
            public void onResponse(Call<ArrayList<FeedsResponse>> call, Response<ArrayList<FeedsResponse>> response) {
                int statusCode = response.code();

                if ( statusCode == 200 && response.isSuccessful() && response.body() != null) {
                    count++;
                    arrayList.clear();
                    Log.e("Optimization", "Response "+response.body());
                    arrayList.addAll(response.body());
                    if (arrayList.size() > 0) {
                        mAdapter.notifyDataChanged();
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(context,"Please check your network connection",Toast.LENGTH_SHORT).show();
                    Log.e("Optimization", " Response Error " + String.valueOf(response.code()));
                }
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<ArrayList<FeedsResponse>> call, Throwable t) {
                Log.e("Onfail MomentsFragm", " Response Error " + t.getMessage());
                Toast.makeText(context,"Please check your network connection",Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void initView(View view) {
        inilizeRetrofit();

        errorPanel = (TextView) view.findViewById(R.id.errorPanel);

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);

        retryBtn = (TextView) view.findViewById(R.id.retryBtn);
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
                load(0);
            }
        });


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (canRefresh) {
                    load(0);
                } else if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }

            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        progressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        if (new CheckInternetConnection(context).isConnectedToInternet()) {
            setAdapter();
        } else {
            new CheckInternetConnection(context).showDialog();
        }


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                //for toolbar
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (scrollingOffset > 0) {
                        if (verticalOffset > HomeActivity.mToolbar.getHeight()) {
                            toolbarAnimateHide();
                        } else {
                            toolbarAnimateShow(verticalOffset);
                        }
                    } else if (scrollingOffset < 0) {
                        if (tToolbar.getTranslationY() < HomeActivity.mToolbar.getHeight() * 0.6 &&
                                verticalOffset > tToolbar.getHeight()) {
                            toolbarAnimateHide();
                        } else {
                            toolbarAnimateShow(verticalOffset);
                        }
                    }
                }

                //for loading more feeds

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //for toolbar
                verticalOffset = recyclerView.computeVerticalScrollOffset();
                scrollingOffset = dy;
                int toolbarYOffset = (int) (dy + HomeActivity.mToolbar.getTranslationY());
                HomeActivity.mToolbar.animate().cancel();
                if (scrollingOffset > 0) {
                    if (toolbarYOffset < HomeActivity.mToolbar.getHeight()) {
                        if (verticalOffset > HomeActivity.mToolbar.getHeight()) {
                            toolbarSetElevation(TOOLBAR_ELEVATION);
                        }
                        HomeActivity.mToolbar.setTranslationY(toolbarYOffset);
                    } else {
                        toolbarSetElevation(0);
                        HomeActivity.mToolbar.setTranslationY(tToolbar.getHeight());
                    }
                } else if (scrollingOffset < 0) {
                    if (toolbarYOffset < 0) {
                        if (verticalOffset <= 0) {
                            toolbarSetElevation(0);
                        }
                        HomeActivity.mToolbar.setTranslationY(0);
                    } else {
                        if (verticalOffset > tToolbar.getHeight()) {
                            toolbarSetElevation(TOOLBAR_ELEVATION);
                        }
                        HomeActivity.mToolbar.setTranslationY(toolbarYOffset);
                    }
                }
            }
        });


    }

    private void inilizeRetrofit() {
        liveUniteApi = new ExtraMethods().getLiveUniteAPI();
    }

    private void loadMore(int index) {
        //alternate check location to enhance battery and speed
        UpdateLocations locations = new UpdateLocations(context);
        locations.initiateLocationFetch();

        if (count%2 == 0 && !locations.getLocationStatus())
        {
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            errorPanel.setVisibility(View.VISIBLE);
            errorPanel.setText("Unable To Get Your Location ! ");
            retryBtn.setVisibility(View.VISIBLE);
            return;

        }

        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        errorPanel.setVisibility(View.GONE);
        retryBtn.setVisibility(View.GONE);

        setUpRequest();
        arrayList.add(null);
        mAdapter.notifyItemInserted(arrayList.size() - 1);

        feedsRequest.setFromLimit(Constant.LOAD_FEEDS_LIMITS*count);
        feedsRequest.setToLimit(Constant.LOAD_FEEDS_LIMITS);
        final Call<ArrayList<FeedsResponse>> feedsResponseCall = liveUniteApi.getFeeds(feedsRequest);
        feedsResponseCall.enqueue(new Callback<ArrayList<FeedsResponse>>() {
            @Override
            public void onResponse(Call<ArrayList<FeedsResponse>> call, Response<ArrayList<FeedsResponse>> response) {
                int statusCode = response.code();
                count++;
                if (response.isSuccessful() && response.body() != null) {
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


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outState.putFloat(STATE_TOOLBAR_ELEVATION, HomeActivity.mToolbar.getElevation());
        }
        outState.putFloat(STATE_TOOLBAR_TRANSLATION_Y, HomeActivity.mToolbar.getTranslationY());
        outState.putInt(STATE_VERTICAL_OFFSET, verticalOffset);
        outState.putInt(STATE_SCROLLING_OFFSET, scrollingOffset);
        outState.putParcelable(STATE_RECYCLER_VIEW, recyclerView.getLayoutManager().onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void toolbarSetElevation(float elevation) {
        // setElevation() only works on Lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            HomeActivity.mToolbar.setElevation(elevation);
        }
    }

    private void toolbarAnimateShow(final int verticalOffset) {
        tToolbar.animate()
                .translationY(0)
                .setInterpolator(new LinearInterpolator())
                .setDuration(180)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                        toolbarSetElevation(verticalOffset == 0 ? 0 : TOOLBAR_ELEVATION);
                    }
                });
    }

    private void toolbarAnimateHide() {
        tToolbar.animate()
                .translationY(tToolbar.getHeight())
                .setInterpolator(new LinearInterpolator())
                .setDuration(180)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        toolbarSetElevation(0);

                    }
                });
    }

    public void uploadCompleted(boolean success) {
        onGoingUploads--;
        if (success) {

            if (onGoingUploads == 0) {
                canRefresh = true;
            }
            if (onGoingUploads >= 0 && arrayList.size()>onGoingUploads)
            {
                arrayList.get(onGoingUploads).setUploading(false);
                arrayList.get(onGoingUploads).setUploaded(true);
            }
            Toast.makeText(context, "Successfully uploaded", Toast.LENGTH_LONG).show();
        }else
        {
            Toast.makeText(context, "Please check your internet connection and retry", Toast.LENGTH_LONG).show();
            if (onGoingUploads >= 0 && arrayList.size()>onGoingUploads)
            {
                arrayList.get(onGoingUploads).setUploading(false);
            }
        }
        ((HomeActivity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
       // Toast.makeText(context, "Upload " + success, Toast.LENGTH_LONG).show();
    }

    public void uploadStarted(File file, String caption) {
        canRefresh = false;
        isUploading = true;
        onGoingUploads++;
        FeedsResponse feedsResponse = new FeedsResponse();
        feedsResponse.setCaption(caption);
        feedsResponse.setUploaded(false);
        feedsResponse.setUploading(true);
        feedsResponse.setType(Constant.MEDIA_PHOTO_TYPE);
        feedsResponse.setFile(file);
        feedsResponse.setAge(Singleton.getInstance().getUserDetails().getAge());
        arrayList.add(0, feedsResponse);
        ((HomeActivity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAdapter!=null)
                {
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

    }
}
