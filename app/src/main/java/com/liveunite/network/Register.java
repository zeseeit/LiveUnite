package com.liveunite.network;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.liveunite.LiveUniteMains.LiveUnite;
import com.liveunite.activities.HomeActivity;
import com.liveunite.activities.LoginActivity;
import com.liveunite.activities.Splash;
import com.liveunite.chat.config.Constants;
import com.liveunite.chat.core.ChatCentre;
import com.liveunite.chat.gcm.LiveUniteGCMIntentService;
import com.liveunite.chat.helper.Optimizer;
import com.liveunite.chat.helper.VolleyUtils;
import com.liveunite.infoContainer.OpenCameraType;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.interfaces.LiveUniteApi;
import com.liveunite.models.RegisterRequest;
import com.liveunite.models.UserDetails;
import com.liveunite.utils.ChangeActivity;
import com.liveunite.utils.Constant;
import com.liveunite.utils.ExtraMethods;
import com.liveunite.utils.ShowProgressDialog;
import com.liveunite.opencamera.CameraActivity;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Vishwesh on 10-10-2016.
 */

public class Register {
    private Context context;
    private int DEVICE_REGISTER_TIMEOUT = 1 * 60 * 1000;    // 1 minute

    public void uploadOnServer(final Context context, final RegisterRequest registerRequest, final boolean firstTime, final ShowProgressDialog showProgressDialog) {
        this.context = context;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Urls.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LiveUniteApi liveUniteApi = retrofit.create(LiveUniteApi.class);
        Call<ArrayList<UserDetails>> registerResponseCall = liveUniteApi.getRegStatus(registerRequest);
        registerResponseCall.enqueue(new Callback<ArrayList<UserDetails>>() {

            @Override
            public void onResponse(Call<ArrayList<UserDetails>> call, Response<ArrayList<UserDetails>> response) {
                int statusCode = response.code();

                ArrayList<UserDetails> userDetails = response.body();

                Log.d("Register", "response " + userDetails.get(0).getSuccess());
                if (userDetails != null) {

                    if (firstTime) {
                        showProgressDialog.dismiss();
                    }

                    if (userDetails.size() > 0 && userDetails.get(0).getSuccess().equals("1")) {

                        saveOrUpdateUserDetails(userDetails.get(0));

                        if(!(LiveUnite.getInstance().getPreferenceManager().getFbId().length()>0)) {

                            Log.d("Optimization"," Register first time register");

                            LiveUnite.getInstance().getPreferenceManager().setFbId(userDetails.get(0).getFbId());

                            OpenCameraType.getInstance().setOpenPostCamera(true);
                            new ChangeActivity().change(context, HomeActivity.class);
                            new ChangeActivity().change(context, CameraActivity.class);

                            ((LoginActivity) context).finish();


                        }

                        new Optimizer().logTime("Register: uploadOnServer() > ");

                    }
                }
            }

            @Override
            public void onFailure(Call<ArrayList<UserDetails>> call, Throwable t) {
                Log.e("Register", "onFailure " + call.toString());
            }

        });


    }

    private void saveOrUpdateUserDetails(UserDetails userDetails) {

        LiveUnite.getInstance().getPreferenceManager().setBio(userDetails.getBio());
        LiveUnite.getInstance().getPreferenceManager().setLiveUniteId(userDetails.getId());
        LiveUnite.getInstance().getPreferenceManager().setMaxAge(userDetails.getMaxAge());
        LiveUnite.getInstance().getPreferenceManager().setMinAge(userDetails.getMinAge());
        LiveUnite.getInstance().getPreferenceManager().setMaxDist(userDetails.getMaxDistance());

    }

    public void getDetailsFromFB(final Context context, final boolean firstTime) {

        this.context = context;

        final ShowProgressDialog showProgressDialog = new ShowProgressDialog();
        if (firstTime) {

            showProgressDialog.create(context);
            showProgressDialog.show();
        }

        final RegisterRequest registerRequest = new RegisterRequest();

        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        if (object != null) {
                            String email = "NA", birthday = "NA", first_name = "NA", last_name = "NA", phone = "NA", id = "NA", gender = "NA";
                            try {
                                if (object.has("email")) {
                                    email = object.getString("email");
                                    Log.d("Register", "Email Id " + email);
                                }
                                if (object.has("birthday")) {
                                    birthday = object.getString("birthday"); // 01/31/1980 format
                                }
                                if (object.has("first_name")) {
                                    first_name = object.getString("first_name") + " ";
                                }
                                if (object.has("last_name")) {
                                    last_name = object.getString("last_name");
                                }
                                if (object.has("id")) {
                                    id = object.getString("id");
                                }
                                if (object.has("gender")) {
                                    gender = object.getString("gender");
                                }

                                Log.e("LoginActivity", id);

                                registerRequest.setFbId(id);
                                registerRequest.setFirst_name(first_name);
                                registerRequest.setLast_name(last_name);
                                registerRequest.setEmail(email);
                                registerRequest.setDateOfBirth(birthday);
                                registerRequest.setGender(gender);
                                registerRequest.setPhone(phone);

                                if (Singleton.getInstance().getUserDetails() != null) {
                                    registerRequest.setLatitude(Singleton.getInstance().getUserLocationModal().getLatitude());
                                    registerRequest.setLongitude(Singleton.getInstance().getUserLocationModal().getLongitude());
                                }

                                saveToSharedPref(registerRequest);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.d("Register", "uploading on server");

                            LiveUnite.getInstance().getPreferenceManager().setUserTitle(first_name + " " + last_name);
                            registerDeviceOnServerForChat();
                            new Optimizer().logTime("Register:  > getDetailsFromFb() > uploadOnServer() ");
                            uploadOnServer(context, registerRequest, firstTime, showProgressDialog);

                        } else {
                            Log.d("Register", "object is null");
                            new ExtraMethods().completeLogOut(context);
                            new ChangeActivity().change(context, LoginActivity.class);

                            if (firstTime) {
                                ((LoginActivity) context).finish();
                            } else {
                                ((Splash) context).finish();
                            }

                        }

                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "email,birthday,id,gender,first_name,last_name");
        request.setParameters(parameters);
        request.executeAsync();

    }

    private void saveToSharedPref(RegisterRequest registerRequest) {

        LiveUnite.getInstance().getPreferenceManager().setFirstName(registerRequest.getFirst_name());
        LiveUnite.getInstance().getPreferenceManager().setLastName(registerRequest.getLast_name());
        LiveUnite.getInstance().getPreferenceManager().setPhone(registerRequest.getPhone());
        LiveUnite.getInstance().getPreferenceManager().setEmail(registerRequest.getEmail());
        LiveUnite.getInstance().getPreferenceManager().setGender(registerRequest.getGender());
        LiveUnite.getInstance().getPreferenceManager().setDob(registerRequest.getDateOfBirth());
        LiveUnite.getInstance().getPreferenceManager().setLat(registerRequest.getLatitude());
        LiveUnite.getInstance().getPreferenceManager().setLong(registerRequest.getLongitude());

    }

    private void registerDeviceOnServerForChat() {

        new Optimizer().logTime("Register:  > registerDeviceForChat() ");

        Intent intent = new Intent(context, LiveUniteGCMIntentService.class);
        context.startService(intent);

        new Optimizer().logTime("Register: registerDeviceForChat() > ");

    }

}