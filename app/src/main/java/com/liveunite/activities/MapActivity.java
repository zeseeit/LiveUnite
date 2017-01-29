package com.liveunite.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.liveunite.models.FeedsResponse;
import com.liveunite.R;
import com.liveunite.infoContainer.Singleton;
import com.liveunite.utils.MapsMethods;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    Context context;
    GoogleMap map;
    private Marker marker;
    private FeedsResponse feedsResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = MapActivity.this;
        if (new MapsMethods().serviceAvailable(this, context)) {
            feedsResponse = Singleton.getInstance().getFeedsResponse();
            if (feedsResponse == null) {
                finish();
            }
            setContentView(R.layout.activity_show_map);
            // Toast.makeText(context, "Map Available", Toast.LENGTH_SHORT).show();
            initMap();
        } else {
            setContentView(R.layout.activity_map);
        }
    }

    private void afterMapReadyStuff() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        map.setMyLocationEnabled(true);
       /* map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
               // map.animateCamera();
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(map.getCameraPosition().target, 7);
                map.moveCamera(update);
                return true;
            }
        });*/
        setUpMapMarkerWindow();
        createMark();
        goToLocation(Singleton.getInstance().getUserLocationModal().getLatitude(),
                Singleton.getInstance().getUserLocationModal().getLongitude(),16);
    }

    private void setUpMapMarkerWindow() {
        if (map != null) {
            map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View v = getLayoutInflater().inflate(R.layout.map_info_window, null);
                    ImageView ivDp = (ImageView) v.findViewById(R.id.ivDP);
                    TextView tvName = (TextView) v.findViewById(R.id.tvName);
                    TextView tvBio = (TextView) v.findViewById(R.id.tvBio);
                    Picasso.with(context).load(feedsResponse.getDpUrl()).resize(48, 48).into(ivDp);
                    tvName.setText(feedsResponse.getFirst_name() + " " + feedsResponse.getLast_name());
                    tvBio.setText(feedsResponse.getBio());
                    return v;
                }
            });
        }
    }

    private void createMark() {
        Picasso.with(context).load(feedsResponse.getDpUrl())
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, 148, 148, true);
                        MarkerOptions options = new MarkerOptions().title("LiveUnite")
                                .position(new LatLng(feedsResponse.getLatitude(),
                                        feedsResponse.getLongitude())).icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                        marker = map.addMarker(options);

                       /* GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                                        .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                                        .positionFromBounds(new LatLngBounds(new LatLng(feedsResponse.getLatitude(),
                                                feedsResponse.getLongitude()), new LatLng(feedsResponse.getLatitude(),
                                                feedsResponse.getLongitude())))
                                //  .transparency((float) 1)
                        );*/
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });


    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    private boolean initMap() {
        if (map == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

        }
        return map != null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        afterMapReadyStuff();
    }


    private void goToLocation(double lat, double lng, float zoom) {
        LatLng latLng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        map.moveCamera(update);
    }

}
