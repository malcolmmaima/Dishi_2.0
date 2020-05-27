package com.malcolmmaima.dishi.View.Maps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.malcolmmaima.dishi.Controller.Services.TrackingService;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.AboutActivity;
import com.malcolmmaima.dishi.View.Activities.LocationSettings;
import com.malcolmmaima.dishi.View.Activities.SecurityPin;

import io.fabric.sdk.android.services.common.SafeToast;

public class ViewMapLocation extends AppCompatActivity implements OnMapReadyCallback {
    String TAG = "ViewMapLocation";
    private GoogleMap mMap;
    Double latitude, longitude;
    FirebaseUser user;
    String myPhone;
    DatabaseReference myRef, myLocationRef;
    int zoomLevel;
    VerticalSeekBar zoomMap;
    FirebaseAuth mAuth;
    LatLng mapLocation;
    ValueEventListener LocationListener;
    Marker myMarker;
    AppCompatButton clearLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_map);

        zoomMap = findViewById(R.id.verticalSeekbar);
        clearLocation = findViewById(R.id.clearLocationBtn);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("My Location");

        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            try {
                latitude = getIntent().getDoubleExtra("lat", 1.2921);
                longitude = getIntent().getDoubleExtra("lon", 36.8219);
                mapLocation = new LatLng(latitude, longitude);
            } catch (Exception e){
                Log.e(TAG, "onCreate: ", e);
            }

            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);

            myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if(locked == true){
                                    Intent slideactivity = new Intent(ViewMapLocation.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            myRef.child("zoom_filter").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        zoomLevel = dataSnapshot.getValue(Integer.class);

                        zoomMap.setProgress(zoomLevel);
                    } catch (Exception e){
                        zoomLevel = 15;
                        zoomMap.setProgress(zoomLevel);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            zoomMap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                    //Synchronize the filter settings in realtime to firebase for a more personalized feel
                    zoomLevel = progress;
                    try {
                        //mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mapLocation, zoomLevel));
                    } catch (Exception e){

                    }
                    myRef.child("zoom_filter").setValue(progress).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Write failed
                            Log.e(TAG, "onFailure: ", e);
                        }
                    });

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

        clearLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog clearLoca = new AlertDialog.Builder(v.getContext())
                        //set message, title, and icon
                        .setMessage("Clear location data from our server?")
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        try {
                                            UserModel myDetails = dataSnapshot.getValue(UserModel.class);
                                            if (myDetails.getAccount_type().equals("1")) {
                                                DatabaseReference activeOrdersRef = FirebaseDatabase.getInstance().getReference("my_orders/" + myPhone);
                                                activeOrdersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if (dataSnapshot.exists()) {
                                                            AlertDialog notAllowed = new AlertDialog.Builder(v.getContext())
                                                                    .setMessage("Not allowed! You have an active order. Kindly complete order and try again.")
                                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {

                                                                        }
                                                                    }).create();
                                                            notAllowed.show();
                                                        } else {
                                                            deletemyLocation();
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });
                                            }

                                            if (myDetails.getAccount_type().equals("3")) {
                                                DatabaseReference rideRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/" + myPhone);
                                                rideRequests.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if (dataSnapshot.exists()) {
                                                            AlertDialog notAllowed = new AlertDialog.Builder(v.getContext())
                                                                    .setMessage("Not allowed! You have ride requests.")
                                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {

                                                                        }
                                                                    }).create();
                                                            notAllowed.show();
                                                        } else {
                                                            deletemyLocation();
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });
                                            }
                                        } catch (Exception e){
                                            SafeToast.makeText(ViewMapLocation.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "onDataChange: ", e);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do nothing

                            }
                        })//setNegativeButton

                        .create();
                clearLoca.show();
            }
        });
    }

    private void deletemyLocation() {
        DatabaseReference myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
        myLocationRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                stopService(new Intent(ViewMapLocation.this, TrackingService.class));
                finish();
                SafeToast.makeText(ViewMapLocation.this, "Location data deleted!", Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        //Incase user has stopped tracking service
        startService(new Intent(this, TrackingService.class));

        mMap = googleMap;
        //Initialize Location
        mapLocation = new LatLng(latitude, longitude);
        myMarker = mMap.addMarker(new MarkerOptions().position(mapLocation).title("My Location"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(mapLocation));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mapLocation, zoomLevel));

        LocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myMarker.remove();
                try {
                    LiveLocationModel myRealtimeLocation = dataSnapshot.getValue(LiveLocationModel.class);
                    latitude = myRealtimeLocation.getLatitude();
                    longitude = myRealtimeLocation.getLongitude();

                    mapLocation = new LatLng(latitude, longitude);
                    myMarker = mMap.addMarker(new MarkerOptions().position(mapLocation).title("Current Location"));
                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(mapLocation));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mapLocation, zoomLevel));
                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myLocationRef.addValueEventListener(LocationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber();
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if(locked == true){
                                    Intent slideactivity = new Intent(ViewMapLocation.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(new Intent(ViewMapLocation.this, TrackingService.class));
        try {
            myLocationRef.removeEventListener(LocationListener);
        } catch (Exception e){
            Log.e(TAG, "onDestroy: ", e);
        }
    }
}
