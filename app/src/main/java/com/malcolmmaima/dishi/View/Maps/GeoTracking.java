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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.malcolmmaima.dishi.Controller.Services.TrackingService;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.ReceiptModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.R;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.fabric.sdk.android.services.common.SafeToast;

public class GeoTracking extends AppCompatActivity implements OnMapReadyCallback {

    String TAG = "GeoTrackingActivity";
    private GoogleMap mMap;
    FloatingActionButton callNduthi, confirmOrd;
    double myLat, myLong;
    LatLng loggedInUserLoc, trackOrderLatLng;
    Marker myCurrent, providerCurrent;
    Circle myArea;
    Double distance;
    int zoomLevel;
    String paymentMethod, address, orderID, initiatedTime;
    Double restaurantLat, restaurantLong;
    VerticalSeekBar zoomMap;
    DatabaseReference myRef, myOrders, myOrdersHistory, customerOrderItems,customerOrderRef, riderLocationRef, deliveryLocationRef, restaurantLocationRef, customerLocationRef;
    ValueEventListener myRefListener, customerOrderListener, riderLocationListener, deliveryLocationListener, restaurantLocationRefListener, customerLocationRefListener;
    String myPhone, accType, message, callMsg, restaurantPhone, riderPhone, customerPhone;
    ProgressDialog progressDialog;
    LiveLocationModel riderLocation;
    StaticLocationModel staticDeliveryLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final String[] tempRiderPhoneHolder = new String[1];

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
        customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+customerPhone);
        myOrdersHistory = FirebaseDatabase.getInstance().getReference("orders_history/"+customerPhone);
        myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+customerPhone);

        message = "Order delivered?";
        callMsg = "Call?";

        callNduthi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog callAlert = new AlertDialog.Builder(v.getContext())
                        //set message, title, and icon
                        .setMessage(callMsg)
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if(accType.equals("1")){

                                    //Rider has been assigned
                                    if(riderPhone != null){
                                        String phone = riderPhone;
                                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                        startActivity(intent);
                                    }

                                    //Rider not assigned, call restaurant
                                    else {
                                        String phone = restaurantPhone;
                                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                        startActivity(intent);
                                    }
                                }

                                if(accType.equals("2")){
                                    String phone = customerPhone;
                                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                    startActivity(intent);
                                }

                                if(accType.equals("3")){
                                    String phone = customerPhone;
                                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                    startActivity(intent);
                                }

                            }
                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do nothing

                            }
                        })//setNegativeButton

                        .create();
                callAlert.show();
            }
        });

        confirmOrd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if(accType.equals("1")){

                    customerOrderItems.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            paymentMethod = dataSnapshot.child("paymentMethod").getValue(String.class);
                            address = dataSnapshot.child("address").getValue(String.class);
                            initiatedTime = dataSnapshot.child("initiatedOn").getValue(String.class);
                            orderID = dataSnapshot.child("orderID").getValue(String.class);

                            String message = "Order has been delivered?";
                            if(address.equals("pick")){
                                message = "Have you picked your order?";
                            } else {
                                message = "Order has been delivered?";
                            }
                            final AlertDialog finish = new AlertDialog.Builder(GeoTracking.this)
                                    .setMessage(message)
                                    //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                                    .setCancelable(false)
                                    //set three option buttons
                                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            DatabaseReference vendorReceiptsRef = FirebaseDatabase.getInstance().getReference("receipts/"+restaurantPhone);
                                            DatabaseReference receiptsRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone);
                                            ReceiptModel receipt = new ReceiptModel();
                                            String nodeKey = receiptsRef.push().getKey();
                                            GetCurrentDate currentDate = new GetCurrentDate();
                                            customerOrderItems.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for (DataSnapshot items : dataSnapshot.child("items").getChildren()) {
                                                        ProductDetailsModel prod = items.getValue(ProductDetailsModel.class);
                                                        //prod.setKey(items.getKey());

                                                        if(prod.getConfirmed() == true){
                                                            receiptsRef.child(nodeKey).child("items").child(items.getKey()).setValue(prod);
                                                            vendorReceiptsRef.child(nodeKey).child("items").child(items.getKey()).setValue(prod);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });

                                            receipt.setDeliveredOn(currentDate.getDate());
                                            receipt.setInitiatedOn(initiatedTime);
                                            receipt.setOrderID(orderID);
                                            receipt.setPaymentMethod(paymentMethod);
                                            receipt.setRestaurant(restaurantPhone);
                                            receipt.setCustomer(myPhone);
                                            receipt.setSeen(false);

                                            vendorReceiptsRef.child(nodeKey).setValue(receipt);
                                            receiptsRef.child(nodeKey).setValue(receipt).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    customerOrderItems.addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            for(final DataSnapshot items : dataSnapshot.child("items").getChildren()){
                                                                try {
                                                                    //We need to capture the rider phone before the node is removed. this will allow us to
                                                                    //update rider status below on deletion. noticed the rider phone was being deleted with the order complete
                                                                    //which would mean in turn we are unable to update the rider status
                                                                    tempRiderPhoneHolder[0] = dataSnapshot.child("rider").getValue(String.class);
                                                                } catch (Exception e){}
                                                                try {
                                                                    ProductDetailsModel prod = items.getValue(ProductDetailsModel.class);
                                                                    prod.setKey(items.getKey());

                                                                    /**
                                                                     * Move order items to history node
                                                                     */
                                                                    myOrdersHistory.child(items.getKey()).setValue(prod).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void aVoid) {
                                                                            customerOrderItems.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void aVoid) {

                                                                                    myOrders.child(restaurantPhone).removeValue();
                                                                                    DatabaseReference rider = FirebaseDatabase.getInstance().getReference
                                                                                            ("my_ride_requests/"+riderPhone+"/"+restaurantPhone+"/"+myPhone);

                                                                                    rider.removeValue();

                                                                                }
                                                                            });
                                                                        }
                                                                    });
                                                                } catch (Exception e){

                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });

                                        }
                                    })//setPositiveButton

                                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            customerOrderItems.child("completed").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //SafeToast.makeText(ViewMyOrders.this, "", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    })

                                    .create();
                            finish.show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                if(accType.equals("2")){
                    AlertDialog finish = new AlertDialog.Builder(GeoTracking.this)
                            .setMessage("Order Delivered?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    customerOrderItems.child("completed").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Snackbar.make(v.getRootView(), "Awaiting customer confirmation", Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    customerOrderItems.child("completed").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //Do nothing
                                        }
                                    });
                                }
                            })

                            .create();
                    finish.show();
                }

                if(accType.equals("3")){
                    AlertDialog finish = new AlertDialog.Builder(GeoTracking.this)
                            .setMessage("Order Delivered?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    customerOrderItems.child("completed").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Snackbar.make(v.getRootView(), "Awaiting customer confirmation", Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    customerOrderItems.child("completed").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //Do nothing
                                        }
                                    });
                                }
                            })

                            .create();
                    finish.show();
                }

            }
        });

        //Get logged in user account type
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

        myRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                accType = dataSnapshot.getValue(String.class);
                //SafeToast.makeText(GeoFireActivity.this, "accType: " + accType, Toast.LENGTH_SHORT).show();
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
                //SafeToast.makeText(GeoFireActivity.this, "nduthiLat: " + nduthiLat, Toast.LENGTH_LONG).show();
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
                //SafeToast.makeText(GeoFireActivity.this, "nduthiLng: " + nduthiLat, Toast.LENGTH_LONG).show();
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

                //SafeToast.makeText(GeoFireActivity.this, "lat: "+ myLat + " long: " + myLong, Toast.LENGTH_SHORT).show();
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
                                SafeToast.makeText(GeoTracking.this, "Error: " + e, Toast.LENGTH_SHORT).show();
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
            });
        } catch (Exception e){
            Log.d("dishi", "GeoFireActivity: "+ e);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        confirmOrd.setEnabled(true);
        callNduthi.setEnabled(true);

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

                customerOrderRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+customerPhone);
                customerOrderListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists()){
                            finish();
                        }
                        /**
                         * Rider set
                         */
                        if(dataSnapshot.child("rider").exists()){
                            riderPhone = dataSnapshot.child("rider").getValue(String.class);
                           //SafeToast.makeText(GeoTracking.this, "Rider assigned: " + riderPhone, Toast.LENGTH_LONG).show();

                            /**
                             * Rider node exists, get rider location co-ordinates
                             */
                            riderLocationRef = FirebaseDatabase.getInstance().getReference("location/"+riderPhone);
                            riderLocationListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot location) {
                                    riderLocation = location.getValue(LiveLocationModel.class);
//                                    SafeToast.makeText(GeoTracking.this, "rider live: ("+riderLocation.getLatitude()
//                                            + ","+riderLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

                                    /**
                                     * Delivery location (Live or static)
                                     */

                                    deliveryLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+customerPhone);
                                    deliveryLocationListener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.child("static_address").exists()){
                                                staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocationModel.class);
//                                                SafeToast.makeText(GeoTracking.this, "delivery static("+ staticDeliveryLocation.getLatitude()
//                                                        + ","+staticDeliveryLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

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
//                                                SafeToast.makeText(GeoTracking.this, "delivery live("+myLat
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
                            //SafeToast.makeText(GeoTracking.this, "rider: false", Toast.LENGTH_SHORT).show();
                            /**
                             * Delivery location (Live or static)
                             */

//                            SafeToast.makeText(GeoTracking.this, "rest: " + restaurantPhone, Toast.LENGTH_SHORT).show();
                            deliveryLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+customerPhone);
                            deliveryLocationListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    /**
                                     * Static address exists
                                     */
                                    if(dataSnapshot.child("static_address").exists()){
                                        staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocationModel.class);
                                        //SafeToast.makeText(GeoTracking.this, "Static address: true", Toast.LENGTH_SHORT).show();

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
//                                        SafeToast.makeText(GeoTracking.this, "delivery live("+myLat
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
                SafeToast.makeText(GeoTracking.this, "err: " + e.toString(), Toast.LENGTH_SHORT).show();
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
                        if(!dataSnapshot.exists()){
                            finish();
                        }
                        /**
                         * Rider set
                         */
                        if(dataSnapshot.child("rider").exists()){
                            riderPhone = dataSnapshot.child("rider").getValue(String.class);
//                            SafeToast.makeText(GeoTracking.this, "Rider assigned: " + riderPhone, Toast.LENGTH_LONG).show();

                            /**
                             * Rider node exists, get rider location co-ordinates
                             */
                            riderLocationRef = FirebaseDatabase.getInstance().getReference("location/"+riderPhone);
                            riderLocationListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot location) {
                                    riderLocation = location.getValue(LiveLocationModel.class);
//                                    SafeToast.makeText(GeoTracking.this, "rider live: ("+riderLocation.getLatitude()
//                                            + ","+riderLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

                                    /**
                                     * Delivery location (Live or static)
                                     */

                                    deliveryLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+customerPhone);
                                    deliveryLocationListener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.child("static_address").exists()){
                                                staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocationModel.class);
//                                                SafeToast.makeText(GeoTracking.this, "delivery static("+ staticDeliveryLocation.getLatitude()
//                                                        + ","+staticDeliveryLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

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
//                                                SafeToast.makeText(GeoTracking.this, "delivery live("+myLat
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
                            //SafeToast.makeText(GeoTracking.this, "rider: false", Toast.LENGTH_SHORT).show();
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
                                        staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocationModel.class);
                                        //SafeToast.makeText(GeoTracking.this, "Static address: true", Toast.LENGTH_SHORT).show();

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
//                                        SafeToast.makeText(GeoTracking.this, "delivery live("+myLat
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
                message = "Have you made the delivery?";
                callMsg = "Call customer?";


            } catch (Exception e){
                SafeToast.makeText(GeoTracking.this, "err: " + e.toString(), Toast.LENGTH_SHORT).show();
                confirmOrd.setEnabled(false);
                callNduthi.setEnabled(false);
            }
        }

        if(accType.equals("3")) {//nduthi
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
                        if(!dataSnapshot.exists()){
                            finish();
                        }
                        /**
                         * Rider set
                         */
                        if(dataSnapshot.child("rider").exists()){
                            riderPhone = dataSnapshot.child("rider").getValue(String.class);
//                            SafeToast.makeText(GeoTracking.this, "Rider assigned: " + riderPhone, Toast.LENGTH_LONG).show();

                            if(!riderPhone.equals(myPhone)){
                                finish();
                                SafeToast.makeText(GeoTracking.this, "Order assigned to different rider!", Toast.LENGTH_LONG).show();
                            }
                            /**
                             * Rider node exists, get rider location co-ordinates
                             */
                            riderLocationRef = FirebaseDatabase.getInstance().getReference("location/"+riderPhone);
                            riderLocationListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot location) {
                                    riderLocation = location.getValue(LiveLocationModel.class);
//                                    SafeToast.makeText(GeoTracking.this, "rider live: ("+riderLocation.getLatitude()
//                                            + ","+riderLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

                                    /**
                                     * Delivery location (Live or static)
                                     */

                                    deliveryLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+customerPhone);
                                    deliveryLocationListener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.child("static_address").exists()){
                                                staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocationModel.class);
//                                                SafeToast.makeText(GeoTracking.this, "delivery static("+ staticDeliveryLocation.getLatitude()
//                                                        + ","+staticDeliveryLocation.getLongitude()+")", Toast.LENGTH_SHORT).show();

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
//                                                SafeToast.makeText(GeoTracking.this, "delivery live("+myLat
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
                            //SafeToast.makeText(GeoTracking.this, "rider: false", Toast.LENGTH_SHORT).show();
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
                                        staticDeliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocationModel.class);
                                        //SafeToast.makeText(GeoTracking.this, "Static address: true", Toast.LENGTH_SHORT).show();

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
//                                        SafeToast.makeText(GeoTracking.this, "delivery live("+myLat
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
                message = "Have you made the delivery?";
                callMsg = "Call customer?";


            } catch (Exception e){
                SafeToast.makeText(GeoTracking.this, "err: " + e.toString(), Toast.LENGTH_SHORT).show();
                confirmOrd.setEnabled(false);
                callNduthi.setEnabled(false);
            }

        }
    }

    private void animateTracking(Double riderLat, Double riderLong, LatLng from_, LatLng to_) {


        try {
            myCurrent.remove(); //Remove previous marker
            providerCurrent.remove();
            myArea.remove(); //Remove previous circle
        } catch (Exception e){

        }

        if(accType.equals("1")){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(riderLat, riderLong), zoomLevel));
        }

        if(accType.equals("2")){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(to_, zoomLevel));
        }

        if(accType.equals("3")){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(to_, zoomLevel));
        }

        providerCurrent = mMap.addMarker(new MarkerOptions().position(from_).title("Rider")
                .snippet("")
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
