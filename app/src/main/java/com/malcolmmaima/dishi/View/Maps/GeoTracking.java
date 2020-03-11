package com.malcolmmaima.dishi.View.Maps;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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
import com.malcolmmaima.dishi.Controller.TrackingService;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class GeoTracking extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Button callNduthi, confirmOrd;
    double myLat, myLong;
    LatLng loggedInUserLoc, trackOrderLatLng;
    Marker myCurrent, providerCurrent;
    Circle myArea;
    Double distance;
    int zoomLevel;
    Double restaurantLat, restaurantLong;
    VerticalSeekBar zoomMap;
    DatabaseReference myRef, customerOrderRef, riderLocationRef, deliveryLocationRef, restaurantLocationRef, customerLocationRef;
    ValueEventListener myRefListener, customerOrderListener, riderLocationListener, deliveryLocationListener, restaurantLocationRefListener, customerLocationRefListener;
    String myPhone, accType, message, callMsg, restaurantPhone, riderPhone, customerPhone;
    ProgressDialog progressDialog;
    LiveLocation riderLocation;
    StaticLocation staticDeliveryLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_tracking);
        progressDialog = new ProgressDialog(GeoTracking.this);

        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        callNduthi = findViewById(R.id.callNduthi);
        confirmOrd = findViewById(R.id.confirmOrd);

        restaurantPhone = getIntent().getStringExtra("restaurantPhone");
        customerPhone = getIntent().getStringExtra("customerPhone");

        restaurantLocationRef = FirebaseDatabase.getInstance().getReference("location/"+restaurantPhone);
        customerLocationRef = FirebaseDatabase.getInstance().getReference("location/"+customerPhone);

        message = "Order delivered?";
        callMsg = "Call?";

        callNduthi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog myQuittingDialogBox = new AlertDialog.Builder(v.getContext())
                        //set message, title, and icon
                        .setMessage(callMsg)
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String phone = restaurantPhone;
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                startActivity(intent);
                            }
                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do nothing

                            }
                        })//setNegativeButton

                        .create();
                myQuittingDialogBox.show();
            }
        });

        confirmOrd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(GeoTracking.this, "Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        //Get logged in user account type
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

        myRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                accType = dataSnapshot.getValue(String.class);
                //Toast.makeText(GeoFireActivity.this, "accType: " + accType, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRef.child("account_type").addValueEventListener(myRefListener);

        /**
         * Get Restaurant Latitude and longitude
         */
        restaurantLocationRefListener =  new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                restaurantLat = dataSnapshot.getValue(Double.class);
                //Toast.makeText(GeoFireActivity.this, "nduthiLat: " + nduthiLat, Toast.LENGTH_LONG).show();
                try {
                    track();
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        restaurantLocationRef.child("latitude").addValueEventListener(restaurantLocationRefListener);

        restaurantLocationRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                restaurantLong = dataSnapshot.getValue(Double.class);
                //Toast.makeText(GeoFireActivity.this, "nduthiLng: " + nduthiLat, Toast.LENGTH_LONG).show();
                try {
                    track();
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("dishi", "GeoFireActivity: " + databaseError);
            }
        };
        restaurantLocationRef.child("longitude").addValueEventListener(restaurantLocationRefListener);
        /**
         * End Get Restaurant Latitude and longitude
         */

        //My latitude longitude coordinates
        customerLocationRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot myCords : dataSnapshot.getChildren()){
                    if(myCords.getKey().equals("latitude")){
                        myLat = myCords.getValue(Double.class);
                    }

                    if(myCords.getKey().equals("longitude")){
                        myLong = myCords.getValue(Double.class);
                    }
                }

                //Toast.makeText(GeoFireActivity.this, "lat: "+ myLat + " long: " + myLong, Toast.LENGTH_SHORT).show();
                try {
                    track();
                } catch (Exception e){

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        customerLocationRef.addValueEventListener(customerLocationRefListener);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("Track Order");

        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        zoomMap = findViewById(R.id.verticalSeekbar);
        zoomMap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                //Synchronize the filter settings in realtime to firebase for a more personalized feel
                zoomLevel = progress;
                try {
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);
                } catch (Exception e){

                }
                myRef.child("zoom_filter").setValue(progress).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Write failed
                                Toast.makeText(GeoTracking.this, "Error: " + e, Toast.LENGTH_SHORT).show();
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

        try {
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
            }); } catch (Exception e){
            Log.d("dishi", "GeoFireActivity: "+ e);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        confirmOrd.setEnabled(false);
        callNduthi.setEnabled(false);

        mMap = googleMap;

        //Incase user has stopped tracking service
        startService(new Intent(this, TrackingService.class));
    }

    private void track() {
        if(accType.equals("1")) {//Customer
            setTitle("Track Order");
            try {
                loggedInUserLoc = new LatLng(myLat, myLong);
                trackOrderLatLng = new LatLng(restaurantLat, restaurantLong); // default is restaurant coordinates

            } catch(Exception e){ }

            try {

                /**
                 * Check if rider node exists in my order node. Basically confirming if restaurant has assigned order to a rider
                 */

                customerOrderRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+myPhone);
                customerOrderListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        /**
                         * Rider set
                         */
                        if(dataSnapshot.child("rider").exists()){
                            riderPhone = dataSnapshot.child("rider").getValue(String.class);
                           //Toast.makeText(GeoTracking.this, "Rider assigned: " + riderPhone, Toast.LENGTH_LONG).show();

                            /**
                             * Rider node exists, get rider location co-ordinates
                             */
                            riderLocationRef = FirebaseDatabase.getInstance().getReference("location/"+riderPhone);
                            riderLocationListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot location) {
                                    riderLocation = location.getValue(LiveLocation.class);
//                                    Toast.makeText(GeoTracking.this, "rider live: ("+riderLocation.getLatitude()
//                                            + ","+riderLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

                                    /**
                                     * Delivery location (Live or static)
                                     */

                                    deliveryLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+myPhone);
                                    deliveryLocationListener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.child("static_address").exists()){
                                                staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocation.class);
                                                Toast.makeText(GeoTracking.this, "delivery static("+ staticDeliveryLocation.getLatitude()
                                                        + ","+staticDeliveryLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

                                                /**
                                                 * Track order movement
                                                 */
                                                try {
                                                    trackOrderLatLng = new LatLng(riderLocation.getLatitude(), riderLocation.getLongitude());
                                                    animateTracking(riderLocation.getLatitude(), riderLocation.getLongitude(),
                                                            trackOrderLatLng, new LatLng(staticDeliveryLocation.getLatitude(), staticDeliveryLocation.getLongitude()));
                                                } catch (Exception e){

                                                }
                                            }

                                            else {
                                                //myLat and myLong variables
//                                                Toast.makeText(GeoTracking.this, "delivery live("+myLat
//                                                        +","+myLong+")", Toast.LENGTH_SHORT).show();

                                                /**
                                                 * Track order movement
                                                 */
                                                try {
                                                    trackOrderLatLng = new LatLng(riderLocation.getLatitude(), riderLocation.getLongitude());
                                                    animateTracking(riderLocation.getLatitude(), riderLocation.getLongitude(),
                                                            trackOrderLatLng, loggedInUserLoc);
                                                } catch (Exception e){

                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    };
                                    deliveryLocationRef.addValueEventListener(deliveryLocationListener);

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            };
                            riderLocationRef.addValueEventListener(riderLocationListener);


                        }

                        /**
                         * Rider not set
                         */
                        else {
                            //Toast.makeText(GeoTracking.this, "rider: false", Toast.LENGTH_SHORT).show();
                            /**
                             * Delivery location (Live or static)
                             */

                            Toast.makeText(GeoTracking.this, "rest: " + restaurantPhone, Toast.LENGTH_SHORT).show();
                            deliveryLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+myPhone);
                            deliveryLocationListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    /**
                                     * Static address exists
                                     */
                                    if(dataSnapshot.child("static_address").exists()){
                                        staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocation.class);
                                        Toast.makeText(GeoTracking.this, "Static address: true", Toast.LENGTH_SHORT).show();

                                        /**
                                         * Track order movement
                                         */
                                        try {
                                            trackOrderLatLng = new LatLng(restaurantLat, restaurantLong);
                                            animateTracking(restaurantLat, restaurantLong,
                                                    trackOrderLatLng, new LatLng(staticDeliveryLocation.getLatitude(), staticDeliveryLocation.getLongitude()));
                                        } catch (Exception e){

                                        }
                                    }

                                    /**
                                     * Static address doesn't exist
                                     */
                                    else {
                                        //myLat and myLong variables
//                                        Toast.makeText(GeoTracking.this, "delivery live("+myLat
//                                                +","+myLong+")", Toast.LENGTH_SHORT).show();

                                        /**
                                         * Track order movement
                                         */
                                        try {
                                            trackOrderLatLng = new LatLng(restaurantLat, restaurantLong);
                                            animateTracking(restaurantLat, restaurantLong,
                                                    trackOrderLatLng, loggedInUserLoc);
                                        } catch (Exception e){

                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            };
                            deliveryLocationRef.addValueEventListener(deliveryLocationListener);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };
                customerOrderRef.addValueEventListener(customerOrderListener);



                confirmOrd.setEnabled(true);
                callNduthi.setEnabled(true);
                //If person making delivery is within 500m radius, send notification
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                message = "Has nduthi or provider delivered your order?";
                callMsg = "Call delivery guy?";


            } catch (Exception e){
                Toast.makeText(GeoTracking.this, "err: " + e.toString(), Toast.LENGTH_SHORT).show();
                confirmOrd.setEnabled(false);
                callNduthi.setEnabled(false);
            }
        }

        if(accType.equals("2")) {//provider
            setTitle("Track Customer");

            try {
                loggedInUserLoc = new LatLng(myLat, myLong);
                trackOrderLatLng = new LatLng(restaurantLat, restaurantLong); // default is restaurant coordinates

            } catch(Exception e){ }

            try {

                /**
                 * Check if rider node exists in my order node. Basically confirming if restaurant has assigned order to a rider
                 */

                customerOrderRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+customerPhone);
                customerOrderListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        /**
                         * Rider set
                         */
                        if(dataSnapshot.child("rider").exists()){
                            riderPhone = dataSnapshot.child("rider").getValue(String.class);
                            //Toast.makeText(GeoTracking.this, "Rider assigned: " + riderPhone, Toast.LENGTH_LONG).show();

                            /**
                             * Rider node exists, get rider location co-ordinates
                             */
                            riderLocationRef = FirebaseDatabase.getInstance().getReference("location/"+riderPhone);
                            riderLocationListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot location) {
                                    riderLocation = location.getValue(LiveLocation.class);
//                                    Toast.makeText(GeoTracking.this, "rider live: ("+riderLocation.getLatitude()
//                                            + ","+riderLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

                                    /**
                                     * Delivery location (Live or static)
                                     */

                                    deliveryLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+myPhone);
                                    deliveryLocationListener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.child("static_address").exists()){
                                                staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocation.class);
//                            Toast.makeText(GeoTracking.this, "delivery static("+ staticDeliveryLocation.getLatitude()
//                                    + ","+staticDeliveryLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

                                                /**
                                                 * Track order movement
                                                 */
                                                try {
                                                    trackOrderLatLng = new LatLng(riderLocation.getLatitude(), riderLocation.getLongitude());
                                                    animateTracking(riderLocation.getLatitude(), riderLocation.getLongitude(),
                                                            trackOrderLatLng, new LatLng(staticDeliveryLocation.getLatitude(), staticDeliveryLocation.getLongitude()));
                                                } catch (Exception e){

                                                }
                                            }

                                            else {
                                                //myLat and myLong variables
//                                                Toast.makeText(GeoTracking.this, "delivery live("+myLat
//                                                        +","+myLong+")", Toast.LENGTH_SHORT).show();

                                                /**
                                                 * Track order movement
                                                 */
                                                try {
                                                    trackOrderLatLng = new LatLng(riderLocation.getLatitude(), riderLocation.getLongitude());
                                                    animateTracking(riderLocation.getLatitude(), riderLocation.getLongitude(),
                                                            trackOrderLatLng, loggedInUserLoc);
                                                } catch (Exception e){

                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    };
                                    deliveryLocationRef.addValueEventListener(deliveryLocationListener);

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            };
                            riderLocationRef.addValueEventListener(riderLocationListener);


                        }

                        /**
                         * Rider not set
                         */
                        else {
//                            Toast.makeText(GeoTracking.this, "rider default: ("+restaurantLat
//                                    +","+restaurantLong+")", Toast.LENGTH_SHORT).show();
                            /**
                             * Delivery location (Live or static)
                             */

                            deliveryLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+customerPhone);
                            deliveryLocationListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    /**
                                     * Static address exists
                                     */
                                    if(dataSnapshot.child("static_address").exists()){
                                        staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocation.class);
//                            Toast.makeText(GeoTracking.this, "delivery static("+ staticDeliveryLocation.getLatitude()
//                                    + ","+staticDeliveryLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

                                        /**
                                         * Track order movement
                                         */
                                        try {
                                            trackOrderLatLng = new LatLng(restaurantLat, restaurantLong);
                                            animateTracking(restaurantLat, restaurantLong,
                                                    trackOrderLatLng, new LatLng(staticDeliveryLocation.getLatitude(), staticDeliveryLocation.getLongitude()));
                                        } catch (Exception e){

                                        }
                                    }

                                    /**
                                     * Static address doesn't exist
                                     */
                                    else {
                                        //myLat and myLong variables
//                                        Toast.makeText(GeoTracking.this, "delivery live("+myLat
//                                                +","+myLong+")", Toast.LENGTH_SHORT).show();

                                        /**
                                         * Track order movement
                                         */
                                        try {
                                            trackOrderLatLng = new LatLng(restaurantLat, restaurantLong);
                                            animateTracking(restaurantLat, restaurantLong,
                                                    trackOrderLatLng, loggedInUserLoc);
                                        } catch (Exception e){

                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            };
                            deliveryLocationRef.addValueEventListener(deliveryLocationListener);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };
                customerOrderRef.addValueEventListener(customerOrderListener);



                confirmOrd.setEnabled(true);
                callNduthi.setEnabled(true);
                //If person making delivery is within 500m radius, send notification
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                message = "Customer received Order?";
                callMsg = "Call Customer?";


            } catch (Exception e){
                Toast.makeText(GeoTracking.this, "err: " + e.toString(), Toast.LENGTH_SHORT).show();
                confirmOrd.setEnabled(false);
                callNduthi.setEnabled(false);
            }
        }

        if(accType.equals("3")) {//nduthi
            setTitle("Track Customer");

//            try {
//                loggedInUserLoc = new LatLng(myLat, myLong);
//                nduthiGuyLoc = new LatLng(nduthiLat, nduthiLng);
//
//                distance = distance(nduthiGuyLoc.latitude, nduthiGuyLoc.longitude, loggedInUserLoc.latitude, loggedInUserLoc.longitude, "K");
//                //Toast.makeText(GeoFireActivity.this, "Distance: " + distance, Toast.LENGTH_SHORT).show();
//                distance = distance * 1000; //Convert distance to meters
//            } catch(Exception e){
//
//            }
//            setTitle("Track Customer");
//            confirmOrd.setVisibility(View.INVISIBLE);
//            try {
//                confirmOrd.setEnabled(true);
//                callNduthi.setEnabled(true);
//                message = "Have you successfully made the delivery?";
//                callMsg = "Call customer?";
//
//                if (distance < 200 && notifSent == false) {
//                    //sendNotification("Customer is " + distance + "m away");
//                    notifSent = true;
//                }
//                myCurrent.remove();
//                providerCurrent.remove();
//                myArea.remove();
//
//                myCurrent = mMap.addMarker(new MarkerOptions().position(loggedInUserLoc).title("My Location")
//                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_delivery_scooter_24dp))
//                        .flat(true));
//
//                providerCurrent = mMap.addMarker(new MarkerOptions().position(nduthiGuyLoc).title("Customer Location")
//                        .snippet("Extra info"));
//
//                //Radius around customer's area
//                myArea = mMap.addCircle(new CircleOptions().center(nduthiGuyLoc)
//                        .radius(200)//in meters
//                        .strokeColor(Color.BLUE)
//                        .fillColor(0x220000FF)
//                        .strokeWidth(5.0f));
//                //track customer
//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nduthiLat, nduthiLng), zoomLevel));
//
//            } catch (Exception e){
//                confirmOrd.setEnabled(false);
//                callNduthi.setEnabled(false);
//                //Toast.makeText(GeoFireActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
//                Log.d("dish", "GeoFireActivity: " + e);
//                loggedInUserLoc = new LatLng(-1.281647, 36.822638); //Default Nairobi
//                myCurrent = mMap.addMarker(new MarkerOptions().position(loggedInUserLoc).title("Default Location").snippet("Error fetching your location"));
//                providerCurrent = mMap.addMarker(new MarkerOptions().position(loggedInUserLoc).title("Default Location").snippet("Error fetching your location"));
//
//                //Radius around my area
//                myArea = mMap.addCircle(new CircleOptions().center(loggedInUserLoc)
//                        .radius(500)//in meters
//                        .strokeColor(Color.BLUE)
//                        .fillColor(0x220000FF)
//                        .strokeWidth(5.0f));
//                //mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-1.281647, 36.822638), zoomLevel));
//            }
        }
    }

    private void animateTracking(Double riderLat, Double riderLong, LatLng from_, LatLng to_) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(riderLat, riderLong), zoomLevel));

        try {
            myCurrent.remove(); //Remove previous marker
            providerCurrent.remove();
            myArea.remove(); //Remove previous circle
        } catch (Exception e){

        }

        String provider_snippet = "";
        if(accType.equals("1")){
            provider_snippet = customerPhone;
        }

        if(accType.equals("2")){
            provider_snippet = restaurantPhone;
        }

        if(accType.equals("3")){

        }

        providerCurrent = mMap.addMarker(new MarkerOptions().position(from_).title("Rider")
                .snippet(provider_snippet)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_delivery_scooter_24dp))
                .flat(true));

        myCurrent = mMap.addMarker(new MarkerOptions().position(to_).title("Delivery Location"));

        //Radius around my area
        myArea = mMap.addCircle(new CircleOptions().center(to_)
                .radius(200)//in meters
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f));

        progressDialog.dismiss();
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }

        return round(dist, 2);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function converts decimal degrees to radians			:*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    public static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function converts radians to decimal degrees			:*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    public static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function rounds a double to N decimal places					 :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            customerOrderRef.removeEventListener(customerOrderListener);
            riderLocationRef.removeEventListener(riderLocationListener);
            deliveryLocationRef.removeEventListener(deliveryLocationListener);
            myRef.removeEventListener(myRefListener);
            restaurantLocationRef.removeEventListener(restaurantLocationRefListener);
            customerLocationRef.removeEventListener(customerLocationRefListener);
        } catch (Exception e){

        }
    }
}
