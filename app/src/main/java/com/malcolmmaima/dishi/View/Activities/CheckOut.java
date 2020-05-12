package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.GenerateRandomString;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Controller.Services.TrackingService;
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ReceiptItemAdapter;
import com.malcolmmaima.dishi.View.Maps.SearchLocation;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.services.common.SafeToast;

public class CheckOut extends AppCompatActivity {
    String TAG = "CheckOutActivity";

    List<ProductDetailsModel> list;
    AppCompatButton orderBtn;
    CardView PaymentMethod, DeliveryAddress;
    EditText remarks;
    MyTextView_Roboto_Medium totalBill;
    MyTextView_Roboto_Regular SubTotal, deliveryChargeAmount, VATamount;
    Double deliveryAmount, totalBillAmount,VAT;
    String [] paymentMethods = {"M-Pesa","Cash on Delivery"};
    String [] deliveryAddress = {"Live Location","Select Location", "Pick My Order"};
    String selectedPaymentMethod, myPhone;
    Double lat, lng;
    String placeName, locationSet;
    AppCompatImageView paymentStatus, deliveryLocationStatus;
    DatabaseReference myRef, myCartRef;
    ProgressDialog progressDialog;
    ArrayList<ProductDetailsModel> myCartItems;
    private RecyclerView recyclerView;
    private ReceiptItemAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_out);

        myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);

        myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                            if(locked == true){
                                Intent slideactivity = new Intent(CheckOut.this, SecurityPin.class)
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

        initWidgets();
        selectedPaymentMethod = "";
        deliveryAmount = 0.0;
        totalBillAmount = 0.0;
        lat = 0.0;
        lng = 0.0;
        placeName = "";
        locationSet = "";
        progressDialog = new ProgressDialog(CheckOut.this);

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //Initialize some values
        int subTotalAmount = getIntent().getIntExtra("subTotal", 0);

        totalBillAmount = Double.valueOf(subTotalAmount);
        SubTotal.setText("Ksh " + subTotalAmount);
        deliveryChargeAmount.setText("Ksh " + deliveryAmount);

        DecimalFormat df = new DecimalFormat("#"); //#.##
        VAT = 0.16 * totalBillAmount; //16% VAT : Kenya
        VAT = Double.valueOf(df.format(VAT));
        VATamount.setText("Ksh " + VAT);

        totalBillAmount = subTotalAmount + deliveryAmount; //+ VAT
        totalBill.setText("Ksh " + totalBillAmount);

        setTitle("Checkout");

        myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myCartItems = new ArrayList<>();
                for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    try {
                        ProductDetailsModel product = dataSnapshot1.getValue(ProductDetailsModel.class);
                        myCartItems.add(product);

                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                }

                mAdapter = new ReceiptItemAdapter(CheckOut.this,myCartItems);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(CheckOut.this);
                recyclerView.setLayoutManager(mLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setAdapter(mAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        PaymentMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(CheckOut.this);
                builder.setItems(paymentMethods, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            selectedPaymentMethod = "mpesa";
                            paymentStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);

//                            selectedPaymentMethod = ""; //will set to "mpesa" once implemented Mpesa
//                            Snackbar.make(v.getRootView(), "In development", Snackbar.LENGTH_LONG).show();
//
//                            //Lets set to grey for now since we have not yet implemented Mpesa
//                            paymentStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                        if(which == 1){
                            selectedPaymentMethod = "cash";
                            paymentStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                    }
                });
                builder.create();
                builder.show();
            }
        });

        DeliveryAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(CheckOut.this);
                builder.setItems(deliveryAddress, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            locationSet = "live";
                            startTrackerService();
                            deliveryLocationStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                        if(which == 1){
                            requestLocation();
                        }

                        if(which == 2){
                            locationSet = "pick";
                            deliveryLocationStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                    }
                });
                builder.create();
                builder.show();
            }
        });

        /**
         * Complete order
         */
        orderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //We need to perform a validation check before sending the order
                if(!selectedPaymentMethod.equals("") && !locationSet.equals("")){
                    sendOrder();
                }

                else{
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout),
                            "Set Address and Payment method", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        });
    }

    private void sendOrder() {

        progressDialog.setMessage("Sending...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        //Get current date
        GetCurrentDate currentDate = new GetCurrentDate();
        final String orderDate = currentDate.getDate();

        //for static location preference
        final StaticLocationModel staticLocationModel = new StaticLocationModel();

        /**
         * Loop through my cart items and add to list Array before passing to adapter
         */
        myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list = new ArrayList<>();
                String myRemarks = "";
                if(remarks.getText().toString().equals("")){
                    myRemarks = "none";
                }

                else {
                    myRemarks = remarks.getText().toString().trim();
                }

                for(DataSnapshot cart : dataSnapshot.getChildren()){
                    ProductDetailsModel product = cart.getValue(ProductDetailsModel.class);
                    product.setDistance(null);
                    product.setUploadDate(orderDate);
                    product.setConfirmed(false);
                    list.add(product);

                    DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders/"+product.getOwner());

                    //Check to see if i have an active order with the said restaurant
                    String finalMyRemarks = myRemarks;
                    ordersRef.child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            /**
                             * I do have an active order, now check if time has passed 10 minutes
                             * (our minimum order response time from restaurant) after which we cant change our order
                             */
                            if(dataSnapshot.exists()){

                                Log.d(TAG, "Order exists: " + dataSnapshot.getKey());
                                //Get today's date
                                GetCurrentDate currentDate = new GetCurrentDate();
                                String currDate = currentDate.getDate();

                                //Get date status update was posted
                                String dtEnd = currDate;
                                String dtStart = dataSnapshot.child("initiatedOn").getValue(String.class);

                                //https://stackoverflow.com/questions/8573250/android-how-can-i-convert-string-to-date
                                //Format both current date and date status update was posted
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
                                try {

                                    //Convert String date values to Date values
                                    Date dateStart;
                                    Date dateEnd;

                                    //Date dateStart = format.parse(dtStart);
                                    String[] timeS = Split(dtStart);
                                    String[] timeT = Split(currDate);

                                    /**
                                     * timeS[0] = date
                                     * timeS[1] = hr
                                     * timeS[2] = min
                                     * timeS[3] = seconds
                                     * timeS[4] = timezone
                                     */

                                    //post timeStamp
                                    if(timeS[4].equals("EAT")){ //Noticed some devices post timezone like so ... i'm going to optimize for EA first
                                        timeS[4] = "GMT+03:00";

                                        //2020-04-27:20:37:32:GMT+03:00
                                        dtStart = timeS[0]+":"+timeS[1]+":"+timeS[2]+":"+timeS[3]+":"+timeS[4];
                                        dateStart = format.parse(dtStart);
                                    } else {
                                        dateStart = format.parse(dtStart);
                                    }

                                    //my device current date
                                    if(timeT[4].equals("EAT")){ //Noticed some devices post timezone like so ... i'm going to optimize for EA first
                                        timeT[4] = "GMT+03:00";

                                        //2020-04-27:20:37:32:GMT+03:00
                                        dtEnd = timeT[0]+":"+timeT[1]+":"+timeT[2]+":"+timeT[3]+":"+timeT[4];
                                        dateEnd = format.parse(dtEnd);
                                    } else {
                                        dateEnd = format.parse(dtEnd);
                                    }

                                    //https://memorynotfound.com/calculate-relative-time-time-ago-java/
                                    //Now compute timeAgo duration
                                    TimeAgo timeAgo = new TimeAgo();

                                    Log.d(TAG, "initiated: "+ timeAgo.toRelative(dateStart, dateEnd, 1));

                                    long timestamp1 = dateStart.getTime();
                                    long timestamp2 = dateEnd.getTime();


                                    if (Math.abs(timestamp2 - timestamp1) > TimeUnit.MINUTES.toMillis(10)) {
                                        DatabaseReference restaurantDetails = FirebaseDatabase.getInstance().getReference("users/"+product.getOwner());
                                        restaurantDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                try {
                                                    UserModel restaurant_ = dataSnapshot.getValue(UserModel.class);

                                                    final AlertDialog alertUser = new AlertDialog.Builder(CheckOut.this)
                                                            //set message, title, and icon
                                                            .setCancelable(false)
                                                            .setMessage("You have an active order at "
                                                                    + restaurant_.getFirstname() + " " + restaurant_.getLastname()
                                                                    + "'s. You can only change order within 10 minutes of placing it otherwise cancel whole order and resend.")
                                                            //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                                            //set three option buttons
                                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                                    try {
                                                                        progressDialog.dismiss();
                                                                    }catch(Exception er){

                                                                    }
//                                                        finish();
//                                                        Intent backToCart = new Intent(CheckOut.this, MyCart.class)
//                                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                                        startActivity(backToCart);
                                                                }
                                                            }).create();
                                                    alertUser.show();
                                                } catch (Exception e){
                                                    finish();
                                                    Toast.makeText(CheckOut.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                                                    Log.e(TAG, "onDataChange: ", e);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                        Log.d(TAG, "10 minutes has passed cannot change order");
                                        //Log.d(TAG, "x: "+ Math.abs(timestamp2 - timestamp1)+" y: "+TimeUnit.MINUTES.toMillis(10));
                                    } else {
                                        //Below 10 minutes so we'll allow changing of order

                                        Log.d(TAG, "10 minutes not up yet");
                                        GenerateRandomString randomString = new GenerateRandomString();
                                        String orderID_1 = randomString.getAlphaNumericString(3);

                                        //Generate random integer
                                        int orderID_2 = new Random().nextInt(1000);
                                        String orderID = orderID_1.toUpperCase()+""+orderID_2;

                                        ordersRef.child(myPhone).child("initiatedOn").setValue(orderDate);
                                        ordersRef.child(myPhone).child("paymentMethod").setValue(selectedPaymentMethod);
                                        ordersRef.child(myPhone).child("address").setValue(locationSet);
                                        ordersRef.child(myPhone).child("orderID").setValue(orderID);
                                        ordersRef.child(myPhone).child("items").child(cart.getKey()).setValue(product);
                                        ordersRef.child(myPhone).child("completed").setValue(false);
                                        ordersRef.child(myPhone).child("remarks").setValue(finalMyRemarks);

                                        if(locationSet.equals("static")){
                                            staticLocationModel.setLatitude(lat);
                                            staticLocationModel.setLongitude(lng);
                                            staticLocationModel.setPlace(placeName);

                                            ordersRef.child(myPhone).child("static_address").setValue(staticLocationModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d(TAG, "Order changed");
                                                    ordersRef.child(myPhone).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            for(DataSnapshot items : dataSnapshot.getChildren()){
                                                                Log.d(TAG, "Remove cart item: " + items.getKey());
                                                                myCartRef.child(items.getKey()).removeValue();
                                                                try {
                                                                    progressDialog.dismiss();
                                                                } catch (Exception er){

                                                                }
                                                                finish();
                                                                SafeToast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();

                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });
                                        } else {
                                            Log.d(TAG, "Order changed");
                                            ordersRef.child(myPhone).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for(DataSnapshot items : dataSnapshot.getChildren()){
                                                        Log.d(TAG, "Remove cart item: " + items.getKey());
                                                        myCartRef.child(items.getKey()).removeValue();
                                                        try {
                                                            progressDialog.dismiss();
                                                        } catch (Exception e){

                                                        }
                                                        finish();
                                                        SafeToast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }

                                        //Loop has reached the end
                                        if(dataSnapshot.getChildrenCount() == list.size()){
                                            //Clear my cart then exit
                                            myCartRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    //We need to have a node that keeps track of our active orders to the different restaurants
                                                    DatabaseReference myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone);

                                                    //Post restaurant phone numbers which act as our primary key, keep track of our active orders
                                                    for(int i = 0; i<list.size(); i++){
                                                        myOrders.child(list.get(i).getOwner()).setValue("active");

                                                        if(i == list.size()-1){

                                                            try {
                                                                progressDialog.dismiss();
                                                            } catch (Exception e){

                                                            }
                                                            finish();
                                                            SafeToast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    try {
                                                        progressDialog.dismiss();
                                                    } catch (Exception er){

                                                    }
                                                    Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout),
                                                            "Something went wrong", Snackbar.LENGTH_LONG);
                                                    snackbar.show();

                                                    if(snackbar.getDuration() == 3000){
                                                        finish();
                                                    }
                                                }
                                            });

                                        }

                                    }

                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    Log.d(TAG, "timeStamp: "+ e.getMessage());
                                }
                            } else {
                                Log.d(TAG, "Order "+dataSnapshot.getKey()+" at "+product.getOwner()+" doesn't exist, add new");
                                //No active order with said restaurant
                                //Generate a random String
                                GenerateRandomString randomString = new GenerateRandomString();
                                String orderID_1 = randomString.getAlphaNumericString(3);

                                //Generate random integer
                                int orderID_2 = new Random().nextInt(1000);
                                String orderID = orderID_1.toUpperCase()+""+orderID_2;

                                ordersRef.child(myPhone).child("initiatedOn").setValue(orderDate);
                                ordersRef.child(myPhone).child("paymentMethod").setValue(selectedPaymentMethod);
                                ordersRef.child(myPhone).child("address").setValue(locationSet);
                                ordersRef.child(myPhone).child("orderID").setValue(orderID);
                                ordersRef.child(myPhone).child("items").child(cart.getKey()).setValue(product);
                                ordersRef.child(myPhone).child("completed").setValue(false);
                                ordersRef.child(myPhone).child("remarks").setValue(finalMyRemarks);

                                if(locationSet.equals("static")){
                                    staticLocationModel.setLatitude(lat);
                                    staticLocationModel.setLongitude(lng);
                                    staticLocationModel.setPlace(placeName);

                                    ordersRef.child(myPhone).child("static_address").setValue(staticLocationModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "New Order added!");
                                            ordersRef.child(myPhone).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for(DataSnapshot items : dataSnapshot.getChildren()){
                                                        Log.d(TAG, "Remove cart item "+items.getKey());
                                                        myCartRef.child(items.getKey()).removeValue();
                                                        Log.d(TAG, "Order complete");
                                                        try {
                                                            progressDialog.dismiss();
                                                        } catch (Exception er){

                                                        }
                                                        finish();
                                                        SafeToast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    });
                                }

                                //Loop has reached the end
                                if(dataSnapshot.getChildrenCount() != list.size()){
                                    //Clear my cart then exit
                                    myCartRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "Remove all cart items");
                                            //We need to have a node that keeps track of our active orders to the different restaurants
                                            DatabaseReference myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone);

                                            //Post restaurant phone numbers which act as our primary key, keep track of our active orders
                                            for(int i = 0; i<list.size(); i++){
                                                Log.d(TAG, "New order updated my orders active");
                                                myOrders.child(list.get(i).getOwner()).setValue("active");

                                                if(i == list.size()-1){
                                                    try {
                                                        progressDialog.dismiss();
                                                    } catch (Exception er){

                                                    }
                                                    finish();
                                                    SafeToast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception er){

                                            }
                                            Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout),
                                                    "Something went wrong", Snackbar.LENGTH_LONG);
                                            snackbar.show();

                                            if(snackbar.getDuration() == 3000){
                                                finish();
                                            }
                                        }
                                    });

                                }
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

    @Override
    protected void onResume() {
        super.onResume();
        myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                            if(locked == true){
                                Intent slideactivity = new Intent(CheckOut.this, SecurityPin.class)
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

    /**
     * Listen to the SearchLocation activity for LatLng values sent back
     */
    private static final int REQUEST_GET_MAP_LOCATION = 0;
    void requestLocation() {
        startActivityForResult(new Intent(CheckOut.this, SearchLocation.class), REQUEST_GET_MAP_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GET_MAP_LOCATION && resultCode == Activity.RESULT_OK) {
            Double latitude = data.getDoubleExtra("latitude", 0.0);
            Double longitude = data.getDoubleExtra("longitude", 0.0);

            lat = latitude;
            lng = longitude;
            placeName = data.getStringExtra("place");

            deliveryLocationStatus
                    .setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary),
                            android.graphics.PorterDuff.Mode.SRC_IN);

            locationSet = "static"; //after location coordinates have been returned by the searchlocation module, set this value to static

        }
    }

    private void startTrackerService() {
        startService(new Intent(this, TrackingService.class));
        //Notify the user that tracking has been enabled//

        //SafeToast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show();

        //////////////////////////////////
    }

    private void initWidgets() {
        orderBtn = findViewById(R.id.btn_order);
        PaymentMethod = findViewById(R.id.PaymentMethod);
        DeliveryAddress = findViewById(R.id.DeliveryAddress);
        remarks = findViewById(R.id.remarks);

        SubTotal = findViewById(R.id.subTotal);
        deliveryChargeAmount = findViewById(R.id.deliveryChargeAmount);
        VATamount = findViewById(R.id.VATamount);
        totalBill = findViewById(R.id.totalBill);

        paymentStatus = findViewById(R.id.paymentStatus);
        deliveryLocationStatus = findViewById(R.id.deliveryLocationStatus);
        recyclerView = findViewById(R.id.recyclerview);
    }

    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");

        return arrSplit;
    }
}
