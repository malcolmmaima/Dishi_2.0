package com.malcolmmaima.dishiapp.View.Activities;

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
import android.widget.ProgressBar;
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
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Controller.Utils.CalculateDistance;
import com.malcolmmaima.dishiapp.Controller.Utils.GenerateRandomString;
import com.malcolmmaima.dishiapp.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishiapp.Controller.Services.TrackingService;
import com.malcolmmaima.dishiapp.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishiapp.Model.LiveLocationModel;
import com.malcolmmaima.dishiapp.Model.ProductDetailsModel;
import com.malcolmmaima.dishiapp.Model.StaticLocationModel;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Adapter.ReceiptItemAdapter;
import com.malcolmmaima.dishiapp.View.Maps.SearchLocation;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;



public class CheckOut extends AppCompatActivity {
    String TAG = "CheckOutActivity";

    List<ProductDetailsModel> list;
    AppCompatButton orderBtn;
    CardView PaymentMethod, DeliveryAddress;
    EditText remarks;
    MyTextView_Roboto_Medium totalBill;
    MyTextView_Roboto_Regular SubTotal, deliveryChargeAmount, VATamount, DeliveryCharge;
    Double deliveryAmount, totalBillAmount,VAT;
    String [] paymentMethods = {"M-Pesa","Cash on Delivery"};
    String [] deliveryAddress = {"Live Location","Select Location", "Pick My Order"};
    String selectedPaymentMethod, myPhone;
    Double lat, lng;
    String placeName, locationSet;
    AppCompatImageView paymentStatus, deliveryLocationStatus;
    DatabaseReference myRef, myCartRef, myLocationRef;
    ProgressDialog progressDialog;
    ArrayList<ProductDetailsModel> myCartItems;
    List<String> deliveryFeeBreakdown;
    private RecyclerView recyclerView;
    private ReceiptItemAdapter mAdapter;
    ArrayList<String> vendors;
    ArrayList<UserModel> vendorObj;
    Boolean stopDialogShown, pauseOrder;
    int prodCount;
    ProgressBar progressBar;
    LinearLayoutManager layoutmanager;
    int subTotalAmount;
    DecimalFormat df;
    Double orderDistance = 0.0;
    ValueEventListener mylocationListener;
    LiveLocationModel myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_out);

        myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);
        myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);

        myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if (locked == true) {
                                    Intent slideactivity = new Intent(CheckOut.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                }
                            } catch (Exception e){

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
        vendors = new ArrayList<String>();
        vendorObj = new ArrayList<UserModel>();
        progressDialog = new ProgressDialog(CheckOut.this);
        stopDialogShown = false;
        pauseOrder = false;

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        df = new DecimalFormat("#"); //#.##

        //Initialize some values
        subTotalAmount = getIntent().getIntExtra("subTotal", 0);

        totalBillAmount = Double.valueOf(subTotalAmount);
        SubTotal.setText("Ksh " + Double.valueOf(df.format(subTotalAmount)));
        deliveryChargeAmount.setText("Ksh " + deliveryAmount);

        VAT = 0.16 * totalBillAmount; //16% VAT : Kenya
        VAT = Double.valueOf(df.format(VAT));
        VATamount.setText("Ksh " + VAT);

        totalBillAmount = subTotalAmount + deliveryAmount; //+ VAT
        totalBill.setText("Ksh " + totalBillAmount);

        orderBtn.setVisibility(View.GONE);

        setTitle("Checkout");

        computeOrder(0);

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
                            computeOrder(0);
                            locationSet = "live";
                            startTrackerService();
                            deliveryLocationStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                        if(which == 1){
                            computeOrder(1);
                            requestLocation();
                        }

                        if(which == 2){
                            computeOrder(2);
                            locationSet = "pick";
                            deliveryLocationStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                        }

                        //Get my location coordinates
                        mylocationListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(which == 0){
                                    try {
                                        myLocation = dataSnapshot.getValue(LiveLocationModel.class);
                                    } catch (Exception e){

                                    }
                                }

                                if(which == 1 && lat != 0.0 && lng != 0.0){
                                    myLocation.setLatitude(lat);
                                    myLocation.setLongitude(lng);
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        };
                        myLocationRef.addValueEventListener(mylocationListener);
                    }
                });
                builder.create();
                builder.show();
            }
        });

        /**
         * Delivery charge breakdown
         */
        DeliveryCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deliveryCharges();
            }
        });

        deliveryChargeAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deliveryCharges();
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

    private void computeOrder(int deliveryAddressType) {
        deliveryAmount = 0.0; //reset to 0 then compute below
        myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                vendors.clear();
                myCartItems = new ArrayList<>();
                deliveryFeeBreakdown = new ArrayList<String>();
                for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    try {
                        ProductDetailsModel product = dataSnapshot1.getValue(ProductDetailsModel.class);

                        DatabaseReference vendorMenuItemRef = FirebaseDatabase.getInstance().getReference("menus/"+product.getOwner()+"/"+product.getOriginalKey());
                        vendorMenuItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    try {
                                        ProductDetailsModel vendorMenuItem = dataSnapshot.getValue(ProductDetailsModel.class);
                                        product.setOutOfStock(vendorMenuItem.getOutOfStock());

                                        if(product.getOutOfStock() == false){
                                            myCartItems.add(product);
                                        }
                                    } catch (Exception e){
                                        product.setOutOfStock(false);
                                        myCartItems.add(product);
                                    }
                                } else {
                                    product.setOutOfStock(false);
                                    myCartItems.add(product);
                                }

                                try {
                                    //allow checkout of items in stock only
                                    if (product.getOutOfStock() == false) {
                                        //myCartItems.add(product);
                                    }
                                } catch (Exception e){
                                    Log.e(TAG, "onDataChange: ", e);
                                }

                                if(!myCartItems.isEmpty()){
                                    progressBar.setVisibility(View.GONE);
                                    orderBtn.setVisibility(View.VISIBLE);
                                    mAdapter = new ReceiptItemAdapter(CheckOut.this,myCartItems);
                                    recyclerView.setLayoutManager(layoutmanager);
                                    recyclerView.setItemAnimator(new DefaultItemAnimator());
                                    recyclerView.setAdapter(mAdapter);
                                }

                                else {
                                    progressBar.setVisibility(View.GONE);
                                    orderBtn.setVisibility(View.GONE);
                                    if(stopDialogShown == false){
                                        android.app.AlertDialog notAllowed = new android.app.AlertDialog.Builder(CheckOut.this)
                                                .setTitle("Sorry :-(")
                                                .setMessage("You can't checkout items that are out of stock!")
                                                .setCancelable(false)
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        finish();
                                                        Intent slideactivity = new Intent(CheckOut.this, MyCart.class)
                                                                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                        startActivity(slideactivity);
                                                    }
                                                }).create();
                                        notAllowed.show();
                                        stopDialogShown = true;
                                    }
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        //filter the vendors and get their delivery charges then add
                        addVendors(product.getOwner());
                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                }

            }

            private void addVendors(String vendor) {

                if(!vendors.contains(vendor)){

                    //get vendor details incl delivery charge val
                    DatabaseReference vendorUserDetails = FirebaseDatabase.getInstance().getReference("users/"+vendor);
                    vendorUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                UserModel vendorUser = dataSnapshot.getValue(UserModel.class);
                                vendorUser.setPhone(vendor);

                                vendorObj.add(vendorUser);

                                //Count the number of items from each individual vendor, if above waiver limit then add delivery fee
                                myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        prodCount = 0;
                                        for(DataSnapshot items : dataSnapshot.getChildren()){
                                            try {
                                                ProductDetailsModel product = items.getValue(ProductDetailsModel.class);

                                                DatabaseReference vendorMenuItemRef = FirebaseDatabase.getInstance().getReference("menus/"+product.getOwner()+"/"+product.getOriginalKey());
                                                vendorMenuItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        try {
                                                            if (dataSnapshot.exists()) {
                                                                ProductDetailsModel vendorMenuItem = dataSnapshot.getValue(ProductDetailsModel.class);
                                                                product.setOutOfStock(vendorMenuItem.getOutOfStock());

                                                                computeDeliveryTotal();
                                                            } else {
                                                                product.setOutOfStock(false);
                                                                computeDeliveryTotal();
                                                            }


                                                        } catch (Exception e){
                                                            Log.e(TAG, "onDataChange: ", e);
                                                        }

                                                    }

                                                    public void computeDeliveryTotal() {
                                                        //increment product count only if it's in stock.
                                                        //product count helps us determine the delivery charge
                                                        if (product.getOwner().equals(vendor) && product.getOutOfStock() == false) {
                                                            prodCount = prodCount + product.getQuantity();

                                                            if (prodCount > vendorUser.getDeliveryChargeLimit() && !vendors.contains(product.getOwner())) {
                                                                //if customer selects option (2) to pick their order, remove delivery fee
                                                                if(deliveryAddressType == 0 || deliveryAddressType == 1){
                                                                    deliveryAmount = deliveryAmount + vendorUser.getDelivery_charge();
                                                                } else {
                                                                    deliveryAmount = 0.0;
                                                                }

                                                                deliveryChargeAmount.setText("Ksh " + Double.valueOf(df.format(deliveryAmount)));
                                                                deliveryFeeBreakdown.add(vendorUser.getFirstname()+" "+vendorUser.getLastname()+" (Ksh " + vendorUser.getDelivery_charge()+")");
                                                                //Log.d(TAG, vendorUser.getFirstname()+"("+prodCount+") => " + vendorUser.getDelivery_charge()+" (waive="+vendorUser.getDeliveryChargeLimit()+")");
                                                                vendors.add(vendor);
                                                                prodCount = 0;

//                                                                Log.d(TAG, "computeDeliveryTotal: dist("+vendorUser.getPhone()+") => "+computeOrderDistance(vendorUser.getPhone())
//                                                                        + " * chrg: "+vendorUser.getDelivery_charge());
                                                            }

                                                            totalBillAmount = subTotalAmount + deliveryAmount; //+ VAT
                                                            totalBill.setText("Ksh " + Double.valueOf(df.format(totalBillAmount)));

                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });

                                            } catch (Exception e){
                                                Log.e(TAG, "onDataChange: ", e);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });


                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
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

    private double computeOrderDistance(String vendorPhone){

        DatabaseReference userData = FirebaseDatabase.getInstance().getReference("users/"+ vendorPhone);
        userData.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    final UserModel user = dataSnapshot.getValue(UserModel.class);
                    user.setPhone(vendorPhone);

                    /**
                     * Now check "locationType" so as to decide which location node to fetch, live or static
                     */
                    if (user.getLocationType().equals("default")) {
                        //if location type is default then fetch static location
                        DatabaseReference defaultLocation = FirebaseDatabase.getInstance().getReference("users/" + vendorPhone + "/my_location");

                        defaultLocation.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                try {
                                    StaticLocationModel staticLocationModel = dataSnapshot.getValue(StaticLocationModel.class);

                                    /**
                                     * Now lets compute distance of each restaurant with customer location
                                     */
                                    CalculateDistance calculateDistance = new CalculateDistance();
                                    Double dist = calculateDistance.distance(myLocation.getLatitude(),
                                            myLocation.getLongitude(), staticLocationModel.getLatitude(), staticLocationModel.getLongitude(), "K");

                                    orderDistance = dist;

                                } catch (Exception e) {
                                    Log.e(TAG, "onDataChange: ", e);
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                    /**
                     * If location type is live then track restaurant live location instead of static location
                     */
                    else if (user.getLocationType().equals("live")) {
                        DatabaseReference restliveLocation = FirebaseDatabase.getInstance().getReference("location/" + vendorPhone);

                        restliveLocation.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                try {

                                    LiveLocationModel restLiveLoc = dataSnapshot.getValue(LiveLocationModel.class);

                                    /**
                                     * Now lets compute distance of each restaurant with customer location
                                     */

                                    CalculateDistance calculateDistance = new CalculateDistance();
                                    Double dist = calculateDistance.distance(myLocation.getLatitude(),
                                            myLocation.getLongitude(), restLiveLoc.getLatitude(), restLiveLoc.getLongitude(), "K");

                                    orderDistance = dist;
                                } catch (Exception e) {
                                    Log.e(TAG, "onDataChange: ", e);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    /**
                     * available track options are "default" which tracks the restaurant's static location under "users/phone/my_location"
                     * and "live" which tracks the restaurant's live location under "location/phone"
                     */
                    else {
                        //Toast.makeText(getContext(), "Something went wrong, contact support!", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return orderDistance;
    }

    private void deliveryCharges() {
        if(deliveryAmount > 0.0){
            try {
                if(deliveryFeeBreakdown.size() > 1){
                    String[] vendors = new String[deliveryFeeBreakdown.size()];
                    AlertDialog.Builder builder = new AlertDialog.Builder(CheckOut.this);
                    builder.setTitle("Delivery Charge Breakdown");
                    builder.setItems(deliveryFeeBreakdown.toArray(vendors), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.create();
                    builder.show();
                }
            } catch (Exception e){
                Log.e(TAG, "deliveryCharges: ",e );
            }
        }
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

                                //Log.d(TAG, "Order exists: " + dataSnapshot.getKey());
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
                                    if(!timeS[4].equals("GMT+03:00")){ //Noticed some devices post timezone like so ... i'm going to optimize for EA first
                                        timeS[4] = "GMT+03:00";

                                        //2020-04-27:20:37:32:GMT+03:00
                                        dtStart = timeS[0]+":"+timeS[1]+":"+timeS[2]+":"+timeS[3]+":"+timeS[4];
                                        dateStart = format.parse(dtStart);
                                    } else {
                                        dateStart = format.parse(dtStart);
                                    }

                                    //my device current date
                                    if(!timeT[4].equals("GMT+03:00")){ //Noticed some devices post timezone like so ... i'm going to optimize for EA first
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

                                    //Log.d(TAG, "initiated: "+ timeAgo.toRelative(dateStart, dateEnd, 1));

                                    long timestamp1 = dateStart.getTime();
                                    long timestamp2 = dateEnd.getTime();


                                    if (Math.abs(timestamp2 - timestamp1) > TimeUnit.MINUTES.toMillis(10)) {
                                        DatabaseReference restaurantDetails = FirebaseDatabase.getInstance().getReference("users/"+product.getOwner());
                                        restaurantDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                try {
                                                    UserModel restaurant_ = dataSnapshot.getValue(UserModel.class);

                                                    pauseOrder = true;
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
                                                                    finish();
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

                                        //Log.d(TAG, "10 minutes has passed cannot change order");
                                        //Log.d(TAG, "x: "+ Math.abs(timestamp2 - timestamp1)+" y: "+TimeUnit.MINUTES.toMillis(10));
                                    } else {
                                        //Below 10 minutes so we'll allow changing of order

                                        //Log.d(TAG, "10 minutes not up yet");
                                        GenerateRandomString randomString = new GenerateRandomString();
                                        String orderID_1 = randomString.getAlphaNumericString(3);

                                        //Generate random integer
                                        int orderID_2 = new Random().nextInt(1000);
                                        String orderID = orderID_1.toUpperCase()+""+orderID_2;

                                        //post delivery charge with order as well
                                        DatabaseReference vendorDetails = FirebaseDatabase.getInstance().getReference("users/"+product.getOwner());
                                        vendorDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                try {
                                                    UserModel vendorDet = dataSnapshot.getValue(UserModel.class);
                                                    ordersRef.child(myPhone).child("deliveryCharge").setValue(vendorDet.getDelivery_charge());
                                                } catch (Exception e){
                                                    Log.e(TAG, "onDataChange: ", e);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                        ordersRef.child(myPhone).child("initiatedOn").setValue(orderDate);
                                        ordersRef.child(myPhone).child("paymentMethod").setValue(selectedPaymentMethod);
                                        ordersRef.child(myPhone).child("address").setValue(locationSet);
                                        ordersRef.child(myPhone).child("orderID").setValue(orderID);
                                        ordersRef.child(myPhone).child("items").child(cart.getKey()).setValue(product);
                                        ordersRef.child(myPhone).child("completed").setValue(false);
                                        ordersRef.child(myPhone).child("remarks").setValue(finalMyRemarks);
                                        ordersRef.child(myPhone).child("paid").setValue(1);


                                        if(locationSet.equals("static")){
                                            staticLocationModel.setLatitude(lat);
                                            staticLocationModel.setLongitude(lng);
                                            staticLocationModel.setPlace(placeName);

                                            ordersRef.child(myPhone).child("static_address").setValue(staticLocationModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //Log.d(TAG, "Order changed");
                                                    ordersRef.child(myPhone).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            for(DataSnapshot items : dataSnapshot.getChildren()){
                                                                //Log.d(TAG, "Remove cart item: " + items.getKey());
                                                                myCartRef.child(items.getKey()).removeValue();
                                                                try {
                                                                    progressDialog.dismiss();
                                                                } catch (Exception er){

                                                                }

                                                                //Allow the customer to read the pop up message above about their existing order
                                                                if(pauseOrder == false){
                                                                    finish();
                                                                }
                                                                Toast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();

                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                                }
                                            });
                                        } else {
                                            //Log.d(TAG, "Order changed");
                                            ordersRef.child(myPhone).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for(DataSnapshot items : dataSnapshot.getChildren()){
                                                        //Log.d(TAG, "Remove cart item: " + items.getKey());
                                                        myCartRef.child(items.getKey()).removeValue();
                                                        try {
                                                            progressDialog.dismiss();
                                                        } catch (Exception e){

                                                        }
                                                        if(pauseOrder == false){
                                                            finish();
                                                        }
                                                        Toast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
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
                                                            if(pauseOrder == false){
                                                                finish();
                                                            }
                                                            Toast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
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
                                    //Log.d(TAG, "timeStamp: "+ e.getMessage());
                                }
                            } else {
                                //Log.d(TAG, "Order "+dataSnapshot.getKey()+" at "+product.getOwner()+" doesn't exist, add new");
                                //No active order with said restaurant
                                //Generate a random String
                                GenerateRandomString randomString = new GenerateRandomString();
                                String orderID_1 = randomString.getAlphaNumericString(3);

                                //Generate random integer
                                int orderID_2 = new Random().nextInt(1000);
                                String orderID = orderID_1.toUpperCase()+""+orderID_2;

                                //post delivery charge with order as well
                                DatabaseReference vendorDetails = FirebaseDatabase.getInstance().getReference("users/"+product.getOwner());
                                vendorDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        try {
                                            UserModel vendorDet = dataSnapshot.getValue(UserModel.class);
                                            ordersRef.child(myPhone).child("deliveryCharge").setValue(vendorDet.getDelivery_charge());
                                        } catch (Exception e){
                                            Log.e(TAG, "onDataChange: ", e);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                ordersRef.child(myPhone).child("initiatedOn").setValue(orderDate);
                                ordersRef.child(myPhone).child("paymentMethod").setValue(selectedPaymentMethod);
                                ordersRef.child(myPhone).child("address").setValue(locationSet);
                                ordersRef.child(myPhone).child("orderID").setValue(orderID);
                                ordersRef.child(myPhone).child("items").child(cart.getKey()).setValue(product);
                                ordersRef.child(myPhone).child("completed").setValue(false);
                                ordersRef.child(myPhone).child("remarks").setValue(finalMyRemarks);
                                ordersRef.child(myPhone).child("paid").setValue(1);

                                if(locationSet.equals("static")){
                                    staticLocationModel.setLatitude(lat);
                                    staticLocationModel.setLongitude(lng);
                                    staticLocationModel.setPlace(placeName);

                                    ordersRef.child(myPhone).child("static_address").setValue(staticLocationModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //Log.d(TAG, "New Order added!");
                                            ordersRef.child(myPhone).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for(DataSnapshot items : dataSnapshot.getChildren()){
                                                        //Log.d(TAG, "Remove cart item "+items.getKey());
                                                        myCartRef.child(items.getKey()).removeValue();
                                                        //Log.d(TAG, "Order complete");
                                                        try {
                                                            progressDialog.dismiss();
                                                        } catch (Exception er){

                                                        }
                                                        if(pauseOrder == false){
                                                            finish();
                                                        }
                                                        Toast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
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
                                            //Log.d(TAG, "Remove all cart items");
                                            //We need to have a node that keeps track of our active orders to the different restaurants
                                            DatabaseReference myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone);

                                            //Post restaurant phone numbers which act as our primary key, keep track of our active orders
                                            for(int i = 0; i<list.size(); i++){
                                                //Log.d(TAG, "New order updated my orders active");
                                                myOrders.child(list.get(i).getOwner()).setValue("active");

                                                if(i == list.size()-1){
                                                    try {
                                                        progressDialog.dismiss();
                                                    } catch (Exception er){

                                                    }
                                                    if(pauseOrder == false){
                                                        finish();
                                                    }
                                                    Toast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
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

        //Toast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show();

        //////////////////////////////////
    }

    private void initWidgets() {
        orderBtn = findViewById(R.id.btn_order);
        PaymentMethod = findViewById(R.id.PaymentMethod);
        DeliveryAddress = findViewById(R.id.DeliveryAddress);
        remarks = findViewById(R.id.remarks);

        SubTotal = findViewById(R.id.subTotal);
        deliveryChargeAmount = findViewById(R.id.deliveryChargeAmount);
        DeliveryCharge = findViewById(R.id.DeliveryCharge);
        VATamount = findViewById(R.id.VATamount);
        totalBill = findViewById(R.id.totalBill);

        paymentStatus = findViewById(R.id.paymentStatus);
        deliveryLocationStatus = findViewById(R.id.deliveryLocationStatus);
        recyclerView = findViewById(R.id.recyclerview);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        layoutmanager = new LinearLayoutManager(CheckOut.this);
        recyclerView.setLayoutManager(layoutmanager);
    }

    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");

        return arrSplit;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        recyclerView.setAdapter(null);
        layoutmanager = null;
    }
}