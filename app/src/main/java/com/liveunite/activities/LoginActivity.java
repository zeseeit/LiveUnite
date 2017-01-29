package com.liveunite.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.liveunite.interfaces.LiveUniteApi;
import com.liveunite.models.ModalLoginPagerItem;
import com.liveunite.R;
import com.liveunite.network.Register;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.CheckInternetConnection;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    Context context;
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    LiveUniteApi liveUniteApi;
    private static final String TAG = LoginActivity.class.getSimpleName();
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private ViewPager mViewPager;
    private CustomPagerAdapter mCustomPagerAdapter;
    private ArrayList<ModalLoginPagerItem> arrayList = new ArrayList<>();
    ArrayList<TextView> dots = new ArrayList<>();
    TextView tvTerms;
    LinearLayout llDots;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        context = LoginActivity.this;
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    new Register().getDetailsFromFB(context, true);
                }
            }
        };
        createArrayList();
        init();
        inilizeDots();
        setViewPagerAdapter();
        setOnViewChangeViewPager();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void setViewPagerAdapter() {
        mViewPager.setAdapter(mCustomPagerAdapter);
    }

    private void init() {

        llDots = (LinearLayout) findViewById(R.id.dots);
        tvTerms = (TextView) findViewById(R.id.tvTermsLink);
        tvTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ChangeActivity().change(context, TermsOfUse.class);
            }
        });
        mCustomPagerAdapter = new CustomPagerAdapter(context, arrayList);
        mViewPager = (ViewPager) findViewById(R.id.pager);
    }

    private void setOnViewChangeViewPager() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.size(); i++) {
                    setLightImage(dots.get(i));
                }
                setDarkImage(dots.get(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void inilizeDots() {
        for (int i = 0; i < arrayList.size(); i++) {
            TextView textView = new TextView(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(24, 24);
            layoutParams.setMargins(8, 0, 8, 4);
            textView.setLayoutParams(layoutParams);
            textView.setBackgroundResource(R.drawable.circle_background_light);
            dots.add(textView);
            llDots.addView(textView);
        }

        if (dots.size() != 0) {
            setDarkImage(dots.get(0));
        }
    }

    private void setLightImage(TextView textView) {
        textView.setBackgroundResource(R.drawable.circle_background_light);
    }

    private void setDarkImage(TextView textView) {
        textView.setBackgroundResource(R.drawable.circle_background);
    }

    private void createArrayList() {
        arrayList = new ArrayList<>();
        ModalLoginPagerItem modalLoginPagerItem = new ModalLoginPagerItem();
        modalLoginPagerItem.text = "Discover nearby people based on your geographical location";
        modalLoginPagerItem.url = R.drawable.view_pager_one;
        arrayList.add(modalLoginPagerItem);
        modalLoginPagerItem = new ModalLoginPagerItem();
        modalLoginPagerItem.text = "Express yourself without worrying about likes and comment";
        modalLoginPagerItem.url = R.drawable.view_pager_two;
        arrayList.add(modalLoginPagerItem);
        modalLoginPagerItem = new ModalLoginPagerItem();
        modalLoginPagerItem.text = "Direct message your interest for genuine and meaningful interaction";
        modalLoginPagerItem.url = R.drawable.view_pager_three;
        arrayList.add(modalLoginPagerItem);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (new CheckInternetConnection(context).isConnectedToInternet()) {
            initView();
        } else {
            new CheckInternetConnection(context).showDialog();
        }
    }

    private void initView() {
        FacebookSdk.sdkInitialize(getApplicationContext());

        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("email", "user_birthday"));

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                handeleFBAccessToken(loginResult.getAccessToken());
                //

            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });


    }

    private void handeleFBAccessToken(AccessToken accessToken) {
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Unable to login in firebase " + task.getException() + " " + task.getResult());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        firebaseAuth.addAuthStateListener(firebaseAuthListener);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    protected void onStop() {
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        firebaseAuth.removeAuthStateListener(firebaseAuthListener);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
//        Toast.makeText(context,Profile.getCurrentProfile().getFirstName() + " " + Profile.getCurrentProfile().getLastName(),Toast.LENGTH_SHORT).show();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Login Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }
}

class CustomPagerAdapter extends PagerAdapter {

    Context mContext;
    LayoutInflater mLayoutInflater;
    ArrayList<ModalLoginPagerItem> arrayList = new ArrayList<>();


    public CustomPagerAdapter(Context context, ArrayList<ModalLoginPagerItem> arrayListURL) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.arrayList = arrayListURL;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View itemView = mLayoutInflater.inflate(R.layout.login_activity_pager_item, container, false);

        ImageView imageView = (ImageView) itemView.findViewById(R.id.ivImage);
        TextView tvDescription = (TextView) itemView.findViewById(R.id.tvDescription);

        tvDescription.setText(arrayList.get(position).text);
        imageView.setImageResource(arrayList.get(position).url);
        /*Picasso.with(mContext).
                load("https://storage.cloud.google.com/liveunite-37d35.appspot.com/images/1478283537019_2.jpg")
                .placeholder(R.drawable.view_pager_one)
                .resize(500,500).into(imageView);*/


        container.addView(itemView);

        return itemView;
    }


    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((RelativeLayout) object);
    }


}

