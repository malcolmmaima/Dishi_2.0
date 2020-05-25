package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
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
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.CalculateDistance;
import com.malcolmmaima.dishi.Controller.Interface.OnOrderChecked;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ViewOrderAdapter;
import com.malcolmmaima.dishi.View.Maps.GeoTracking;
import com.squareup.picasso.Picasso;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.fabric.sdk.android.services.common.SafeToast;

public class ViewCustomerOrder extends AppCompatActivity implements OnOrderChecked {
    String TAG = "ViewCustomerOrder";
    List<ProductDetailsModel> list;
    String myPhone, phone, customerName, restaurantPhone, initiatedTime, address;
    FirebaseUser user;
    DatabaseReference riderRequests, customerOrderItems, myLocationRef, myRidersRef, riderStatus, myRef;
    ValueEventListener customerOrderItemsListener, myRidersListener, currentRiderListener, riderStatusListener;
    MyTextView_Roboto_Medium myOrderID, restaurantName, totalBill;
    MyTextView_Roboto_Regular subTotal, deliveryChargeAmount, payment, customerRemarks, riderName,
            timeStamp, trackOrderTxt;
    FloatingActionButton acceptOrd;
    ImageView riderIcon;
    Double deliveryCharge, totalAmount;
    RecyclerView recyclerview;
    AppCompatButton confirmOrder, declineOrder;
    CardView DeliveryAddress, OrderStatus;
    LiveLocationModel liveLocationModel;
    StaticLocationModel deliveryLocation;
    ValueEventListener locationListener;
    Menu myMenu;
    List<String> myRiders, ridersName;
    String accType, restaurantname, restaurantProfile;
    CircleImageView profilePic;
    UserModel riderUser;
    String riderPhone;
    String [] restaurantActions = {"View","Message", "Call"};
    String [] riderOptions = {"View","Message", "Call"};
    Timer timer;
    HashTagHelper mTextHashTagHelper;

    final int[] total = {0};

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
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
                                    Intent slideactivity = new Intent(ViewCustomerOrder.this, SecurityPin.class)
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

            loadViewCustomer();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
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
                                    Intent slideactivity = new Intent(ViewCustomerOrder.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                } else {
                                    loadViewCustomer();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    } else {
                        loadViewCustomer();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
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
                                Intent slideactivity = new Intent(ViewCustomerOrder.this, SecurityPin.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                slideactivity.putExtra("pinType", "resume");
                                startActivity(slideactivity);
                            } else {
                                loadViewCustomer();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } else {
                    loadViewCustomer();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadViewCustomer() {
        setContentView(R.layout.activity_view_customer_order);

        //Initialize variables
        deliveryCharge = 0.0;
        totalAmount = 0.0;

        //Data passed from adapter
        phone = getIntent().getStringExtra("phone"); //From adapters
        customerName = getIntent().getStringExtra("name");
        restaurantPhone = getIntent().getStringExtra("restaurantPhone");
        accType = getIntent().getStringExtra("accountType");
        restaurantname = getIntent().getStringExtra("restaurantName");
        restaurantProfile = getIntent().getStringExtra("restaurantProfile");

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(customerName);

        subTotal = findViewById(R.id.subTotal);
        deliveryChargeAmount = findViewById(R.id.deliveryChargeAmount);
        payment = findViewById(R.id.payment);
        totalBill = findViewById(R.id.totalBill);
        recyclerview = findViewById(R.id.rview);
        confirmOrder = findViewById(R.id.btn_confirm);
        confirmOrder.setTag("confirm");
        declineOrder = findViewById(R.id.btn_decline);
        declineOrder.setTag("active");
        DeliveryAddress = findViewById(R.id.DeliveryAddress);
        customerRemarks = findViewById(R.id.customerRemarks);
        riderName = findViewById(R.id.riderName);
        riderIcon = findViewById(R.id.riderIcon);
        profilePic = findViewById(R.id.profilePic);
        OrderStatus = findViewById(R.id.card_order_status);
        acceptOrd = findViewById(R.id.confirmOrd);
        timeStamp = findViewById(R.id.timeStamp);
        acceptOrd.setTag("accept");
        restaurantName = findViewById(R.id.restaurantName);
        restaurantName.setText(restaurantname);
        myOrderID = findViewById(R.id.myOrderID);
        trackOrderTxt = findViewById(R.id.trackOrderTxt);

        /**
         * Load image url onto imageview
         */
        try {
            //Load retaurant image
            Picasso.with(ViewCustomerOrder.this).load(restaurantProfile).fit().centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profilePic);
        } catch (Exception e){

        }

        restaurantName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ViewCustomerOrder.this);
                builder.setTitle(restaurantname);
                builder.setItems(restaurantActions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        if(which == 0){
                            Intent slideactivity = new Intent(ViewCustomerOrder.this, ViewRestaurant.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            slideactivity.putExtra("restaurant_phone", restaurantPhone);
                            slideactivity.putExtra("distance", 0.0);
                            slideactivity.putExtra("profilePic", restaurantProfile);
                            Bundle bndlanimation =
                                    null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                bndlanimation = ActivityOptions.makeCustomAnimation(ViewCustomerOrder.this, R.anim.animation,R.anim.animation2).toBundle();
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                startActivity(slideactivity, bndlanimation);
                            }
                        }
                        if(which == 1){
                            Intent slideactivity = new Intent(ViewCustomerOrder.this, Chat.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            slideactivity.putExtra("fromPhone", myPhone);
                            slideactivity.putExtra("toPhone", restaurantPhone);
                            Bundle bndlanimation =
                                    null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                bndlanimation = ActivityOptions.makeCustomAnimation(ViewCustomerOrder.this, R.anim.animation,R.anim.animation2).toBundle();
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                startActivity(slideactivity, bndlanimation);
                            }
                        }

                        if(which == 2){
                            final AlertDialog callAlert = new AlertDialog.Builder(v.getContext())
                                    //set message, title, and icon
                                    .setMessage("Call " + restaurantname + "?")
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
                            callAlert.show();
                        }

                    }
                });
                builder.create();
                builder.show();
            }
        });

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(ViewCustomerOrder.this, ViewImage.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("imageURL", restaurantProfile);
                startActivity(slideactivity);
            }
        });

        if(accType.equals("2")){
            OrderStatus.setVisibility(View.GONE);
            myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
            customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+myPhone+"/"+phone);
            myRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+myPhone);
        }

        if(accType.equals("3")){
            OrderStatus.setVisibility(View.VISIBLE);
            myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
            customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+phone);
            myRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+restaurantPhone);
        }

        /**
         * On create view fetch my location coordinates
         */

        try {
            liveLocationModel = null;
            locationListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        liveLocationModel = dataSnapshot.getValue(LiveLocationModel.class);
                        //SafeToast.makeText(ViewCustomerOrder.this, "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();

                        /**
                         * Compute distance between customer and restaurant
                         */

                        CalculateDistance calculateDistance = new CalculateDistance();
                        Double dist = calculateDistance.distance(liveLocationModel.getLatitude(),
                                liveLocationModel.getLongitude(), deliveryLocation.getLatitude(), deliveryLocation.getLongitude(), "K");

                        SafeToast.makeText(ViewCustomerOrder.this, "Distance: " + dist, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            myLocationRef.addListenerForSingleValueEvent(locationListener);
        } catch (Exception e){

        }


        /**
         * Initialize cart items total and keep track of items
         */
        customerOrderItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    finish();
                    SafeToast.makeText(ViewCustomerOrder.this, "Order Complete!", Toast.LENGTH_LONG).show();
                } else {
                    total[0] = 0;

                    list = new ArrayList<>();

                    String remarks = dataSnapshot.child("remarks").getValue(String.class);
                    String orderID = dataSnapshot.child("orderID").getValue(String.class);
                    String paymentMethod = dataSnapshot.child("paymentMethod").getValue(String.class);
                    address = dataSnapshot.child("address").getValue(String.class);
                    payment.setText(paymentMethod);
                    myOrderID.setText("ORDER ID: #"+orderID);
                    initiatedTime = dataSnapshot.child("initiatedOn").getValue(String.class);

                    try {
                        if(address.equals("pick")){
                            DeliveryAddress.setEnabled(false);
                            trackOrderTxt.setText("Customer will pick order");
                            riderName.setVisibility(View.GONE);
                            riderIcon.setVisibility(View.GONE);
                        }
                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }

                    timer = new Timer();
                    timer.schedule(new TimerTask() {

                        int second = 1800; //30minutes

                        @Override
                        public void run() {
                            if (second <= 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Orer id taking too long
                                        timer.cancel();
                                    }
                                });

                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        //Get today's date
                                        GetCurrentDate currentDate = new GetCurrentDate();
                                        String currDate = currentDate.getDate();

                                        //Get dates
                                        String dtEnd = currDate;
                                        String dtStart = initiatedTime;

                                        //https://stackoverflow.com/questions/8573250/android-how-can-i-convert-string-to-date
                                        //Format both current date and date status update was posted
                                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
                                        try {

                                            //Convert String date values to Date values
                                            Date dateStart;
                                            Date dateEnd;

                                            //Date dateStart = format.parse(dtStart);
                                            String[] timeS = Split(initiatedTime);
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

                                            timeStamp.setText("Ordered "+timeAgo.toRelative(dateStart, dateEnd, 2));

                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            Log.d(TAG, "timeStamp: "+ e.getMessage());
                                        }

                                        second--;
                                    }
                                });
                            }

                        }
                    }, 0, 1000);

                    try {
                        riderPhone = dataSnapshot.child("rider").getValue(String.class);
                    } catch (Exception e){

                    }

                    customerRemarks.setText("Remarks: "+remarks);

                    try {
                        Linkify.addLinks(customerRemarks, Linkify.ALL);
                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }

                    //handle hashtags
                    if(remarks.contains("#")){
                        mTextHashTagHelper = HashTagHelper.Creator.create(getResources().getColor(R.color.colorPrimary),
                                new HashTagHelper.OnHashTagClickListener() {
                                    @Override
                                    public void onHashTagClicked(String hashTag) {
                                        String searchHashTag = "#"+hashTag;
                                        Intent slideactivity = new Intent(ViewCustomerOrder.this, SearchActivity.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        slideactivity.putExtra("searchString", searchHashTag);
                                        startActivity(slideactivity);
                                    }
                                });

                        mTextHashTagHelper.handle(customerRemarks);
                    }
                    for(DataSnapshot items : dataSnapshot.child("items").getChildren()){

                        try {
                            ProductDetailsModel prod = items.getValue(ProductDetailsModel.class);
                            prod.setKey(items.getKey());
                            prod.accountType = accType;
                            list.add(prod);

                            if(prod.getConfirmed() == true){
                                int adapterTotal = prod.getQuantity() * Integer.parseInt(prod.getPrice());
                                total[0] = total[0] + adapterTotal;
                                totalAmount = total[0] + deliveryCharge;
                            }
                        } catch (Exception e){

                        }
                    }
                    subTotal.setText("Ksh "+total[0]);

                    if(!list.isEmpty()){
                        Collections.reverse(list);
                        ViewOrderAdapter recycler = new ViewOrderAdapter(ViewCustomerOrder.this, list, ViewCustomerOrder.this);
                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewCustomerOrder.this);
                        recyclerview.setLayoutManager(layoutmanager);
                        recyclerview.setItemAnimator( new DefaultItemAnimator());
                        recycler.notifyDataSetChanged();
                        recyclerview.setAdapter(recycler);


                    }

                    totalBill.setText("ksh " + totalAmount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        customerOrderItems.addValueEventListener(customerOrderItemsListener);

        /**
         * Fetch my riders list
         */
        myRidersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myRiders = new ArrayList<String>();
                ridersName = new ArrayList<String>();

                for(final DataSnapshot rider : dataSnapshot.getChildren()){
                    DatabaseReference riderUserInfo = FirebaseDatabase.getInstance().getReference("users/"+rider.getKey());
                    riderUserInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            try {
                                riderUser = dataSnapshot.getValue(UserModel.class);

                                myRiders.add(rider.getKey());
                                ridersName.add(riderUser.getFirstname() + " " + riderUser.getLastname());
                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                try {
                    Collections.reverse(myRiders);
                    Collections.reverse(ridersName);
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRidersRef.addValueEventListener(myRidersListener);

        /**
         * fetch current rider
         */
        currentRiderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    riderName.setText("You are the default rider for this order. Assign rider top right icon.");
                }

                else {
                    final DatabaseReference riderUserInfo = FirebaseDatabase.getInstance().getReference("users/"+dataSnapshot.getValue());
                    riderUserInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            riderUser = dataSnapshot.getValue(UserModel.class);
                            riderName.setText(riderUser.getFirstname()+" "+riderUser.getLastname());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    if(accType.equals("2")){
                        try {
                            //SafeToast.makeText(ViewCustomerOrder.this, "rider: "+ riderPhone, Toast.LENGTH_LONG).show();
                            riderStatus = FirebaseDatabase.getInstance().getReference("my_ride_requests/" + riderPhone + "/" + myPhone + "/" + phone);
                        } catch (Exception e){

                        }
                    }

                    if(accType.equals("3")){
                        riderStatus = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myPhone+"/"+restaurantPhone+"/"+phone);

                        /**
                         * Close this activity for rider if restaurant has assigned the order to a different rider
                         */
                        if(!myPhone.equals(dataSnapshot.getValue())){
                            finish();
                            SafeToast.makeText(ViewCustomerOrder.this, "Order assigned to different rider!", Toast.LENGTH_LONG).show();
                        }
                    }

                    riderStatusListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dtSnapshot) {
                            String riderOrderStatus = dtSnapshot.getValue(String.class);
                            //SafeToast.makeText(ViewCustomerOrder.this, "status: " + riderOrderStatus, Toast.LENGTH_SHORT).show();
                            if(riderOrderStatus == null && riderPhone == null){
                                Snackbar.make(findViewById(R.id.parentlayout), "Error, rider does not exist!", Snackbar.LENGTH_LONG).show();
                                riderName.setText("Rider does not exist");
                                //customerOrderItems.child("rider").removeValue(); //bug
                            }

                            try {
                                if (riderOrderStatus.equals("accepted")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewCustomerOrder.this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                                    acceptOrd.setTag("decline");
                                    acceptOrd.setImageResource(R.drawable.ic_clear_white_36dp);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                }

                                if (riderOrderStatus.equals("assigned")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewCustomerOrder.this, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
                                    acceptOrd.setTag("accept");
                                    acceptOrd.setImageResource(R.drawable.ic_action_save);
                                    confirmOrder.setVisibility(View.GONE);
                                }

                                if (riderOrderStatus.equals("declined")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewCustomerOrder.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
                                    acceptOrd.setTag("accept");
                                    acceptOrd.setImageResource(R.drawable.ic_action_save);
                                    confirmOrder.setVisibility(View.GONE);
                                }

                                if(accType.equals("2")){
                                    confirmOrder.setVisibility(View.VISIBLE);
                                }
                            } catch (Exception e){

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };
                    try {
                        riderStatus.addValueEventListener(riderStatusListener);
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        customerOrderItems.child("rider").addValueEventListener(currentRiderListener);

        riderName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                if(riderPhone != null && !riderPhone.equals(myPhone)){
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ViewCustomerOrder.this);
                    builder.setTitle(riderName.getText().toString());
                    builder.setItems(riderOptions, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int which) {
                            if(which == 0){
                                if(riderPhone != null){
                                    Snackbar.make(v.getRootView(), "In development", Snackbar.LENGTH_LONG).show();
                                }

                                else {
                                    Snackbar.make(v.getRootView(), "You're the default rider", Snackbar.LENGTH_LONG).show();
                                }
                            }

                            if(which == 1){
                                if(riderPhone != null){
                                    Intent slideactivity = new Intent(ViewCustomerOrder.this, Chat.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                    slideactivity.putExtra("fromPhone", myPhone);
                                    slideactivity.putExtra("toPhone", riderPhone);
                                    Bundle bndlanimation =
                                            null;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        bndlanimation = ActivityOptions.makeCustomAnimation(ViewCustomerOrder.this, R.anim.animation,R.anim.animation2).toBundle();
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        startActivity(slideactivity, bndlanimation);
                                    }
                                }

                                else {
                                    Snackbar.make(v.getRootView(), "You're the default rider", Snackbar.LENGTH_LONG).show();
                                }
                            }

                            if(which == 2){
                                if(riderPhone != null){
                                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", riderPhone, null));
                                    startActivity(intent);
                                }

                                else {
                                    Snackbar.make(v.getRootView(), "You're the default rider", Snackbar.LENGTH_LONG).show();
                                }

                            }
                        }
                    });
                    builder.create();
                    builder.show();
                }

            }
        });
        acceptOrd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(acceptOrd.getTag().equals("accept")){

                    final AlertDialog accept = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Accept ride order request?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    riderStatus.setValue("accepted");
                                    myRidersRef.child(myPhone).setValue("active").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            acceptOrd.setTag("decline");
                                            acceptOrd.setImageResource(R.drawable.ic_clear_white_36dp);
                                        }
                                    });

                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })

                            .create();
                    accept.show();

                }

                if(acceptOrd.getTag().equals("decline")){

                    final AlertDialog decline = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Decline ride order request?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    riderStatus.setValue("declined");
                                    myRidersRef.child(myPhone).setValue("inactive").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            acceptOrd.setTag("accept");
                                            acceptOrd.setImageResource(R.drawable.ic_action_save);
                                        }
                                    });

                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })

                            .create();
                    decline.show();

                }

            }
        });

        DeliveryAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(ViewCustomerOrder.this, GeoTracking.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if(accType.equals("2")){
                    slideactivity.putExtra("restaurantPhone", myPhone);
                    slideactivity.putExtra("customerPhone", phone);
                }

                if(accType.equals("3")){
                    slideactivity.putExtra("restaurantPhone", restaurantPhone);
                    slideactivity.putExtra("customerPhone", phone);
                }

                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(ViewCustomerOrder.this, R.anim.animation,R.anim.animation2).toBundle();
                startActivity(slideactivity, bndlanimation);

            }
        });

        customerOrderItems.child("items").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot items : dataSnapshot.getChildren()){
                    Boolean confirmed = items.child("confirmed").getValue(Boolean.class);
                    if(confirmed){
                        confirmOrder.setTag("end");
                        confirmOrder.setText("END");
                        declineOrder.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        confirmOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                String message = "Order Delivered?"; //default

                if(address.equals("pick")){
                    message = "Customer Picked Order?";
                } else {
                    message = "Order Delivered?";
                }
                /**
                 * Delivered order to address, notify customer
                 */
                if(confirmOrder.getTag().toString().equals("end")){
                    AlertDialog finish = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage(message)
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
                                    if(address.equals("pick")){
                                        AlertDialog cancelOrder = new AlertDialog.Builder(ViewCustomerOrder.this)
                                                .setMessage("Cancel anyway?")
                                                //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                                                .setCancelable(false)
                                                //set three option buttons
                                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        customerOrderItems.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                //
                                                            }
                                                        });
                                                    }
                                                })//setPositiveButton

                                                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        //Do nothing
                                                    }
                                                })

                                                .create();
                                        cancelOrder.show();
                                    }
                                }
                            })

                            .create();
                    finish.show();

                }
                /**
                 * Accept order
                 */
                else {
                    AlertDialog confirmOrd = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Accept " + customerName + "'s order?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    int j = 0;
                                    //set product item status to confirmed
                                    for(int i = 0; i<list.size(); i++){
                                        if(list.get(i).getConfirmed() == true){
                                            j++;
                                            customerOrderItems.child("items").child(list.get(i).getKey()).child("confirmed").setValue(true);
                                        }

                                        if(i==list.size()-1){ //loop has reached the end

                                            if(j==0){
                                                Snackbar.make(v.getRootView(), "You must select available order items", Snackbar.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                }
                            })

                            .create();
                    confirmOrd.show();
                }


            }
        });

        declineOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if(declineOrder.getTag().equals("declined")){
                    customerOrderItems.removeValue();
                }

                else {
                    AlertDialog confirmOrd = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Decline " + customerName + "'s order?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    int j = 0;
                                    //set product item status to confirmed
                                    for(int i = 0; i<list.size(); i++){
                                        customerOrderItems.child("items").child(list.get(i).getKey()).child("confirmed").setValue(false);

                                        if(i==list.size()-1){ //loop has reached the end
                                            confirmOrder.setVisibility(View.GONE);
                                            declineOrder.setTag("declined");
                                            declineOrder.setText("DELETE");
                                            Snackbar.make(v.getRootView(), "Order declined", Snackbar.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                }
                            })

                            .create();
                    confirmOrd.show();
                }

            }
        });

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });
        //topToolBar.setLogo(R.drawable.logo);
        //topToolBar.setLogoDescription(getResources().getString(R.string.logo_desc));
    }

    @Override
    public void onItemChecked(Boolean isChecked, int position, String price, int quantity) {

        list.get(position).setConfirmed(isChecked);

        /**
         * Compute adapter price per quantity
         */
        int adapterPrice = Integer.parseInt(price.trim());
        int adapterTotal = adapterPrice * quantity;

        /**
         * Add or subtract price of selected/unselected product
         */
        if(isChecked){
            total[0] = total[0] + adapterTotal;
            totalAmount = total[0] + deliveryCharge;
            subTotal.setText("Ksh " + total[0]);
            totalBill.setText("ksh " + totalAmount);

            if(confirmOrder.getTag().toString().equals("end")){
                confirmOrder.setTag("confirm");
                confirmOrder.setText("CONFIRM");
            }
        }

        else {
            total[0] = total[0] - adapterTotal;
            if(total[0] < 0) { total[0] = 0;}
            totalAmount = total[0] + deliveryCharge;
            subTotal.setText("Ksh " + total[0]);
            totalBill.setText("ksh " + totalAmount);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.assign_order_menu, menu);
        myMenu = menu;
        MenuItem item = menu.findItem(R.id.addRider);
        item.setVisible(false);

        try {
            if (address.equals("pick")) {
                item.setVisible(false);
            }

            else {
                if (accType.equals("2")) {
                    item.setVisible(true);
                }

                if (accType.equals("3")) {
                    item.setVisible(false);
                }
            }
        } catch (Exception e){
            Log.e(TAG, "onCreateOptionsMenu: ", e);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) { switch(item.getItemId()) {

        case R.id.callCustomer:

            final AlertDialog callAlert = new AlertDialog.Builder(this)
                    //set message, title, and icon
                    .setMessage("Call " + customerName + "?")
                    //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                    //set three option buttons
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String phone_ = phone;
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone_, null));
                            startActivity(intent);
                        }
                    }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //do nothing

                        }
                    })//setNegativeButton

                    .create();
            callAlert.show();
            return (true);

        case R.id.addRider:

            /**
             * Hide / show icon if atleast one order item confirmed
             */
            for(int i=0; i<list.size(); i++){
                //Loop through the order items, if atleast one order item is confirmed then allow assigning of order to rider
                if(list.get(i).getConfirmed() == true){
                    i = list.size(); //end loop
                    AlertDialog assignRider = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Assign order to rider?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, final int whichButton) {
                                    customerOrderItems.child("rider").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(!dataSnapshot.exists()){
                                                String [] riders = new String[ridersName.size()];
                                                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ViewCustomerOrder.this);
                                                builder.setItems(ridersName.toArray(riders), new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, final int which) {
                                                        customerOrderItems.child("rider").setValue(myRiders.get(which)).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {


                                                                myRidersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                        //Remove existing ride requests then set new request
                                                                        for(DataSnapshot riders : dataSnapshot.getChildren()){
                                                                            riderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+riders.getKey());
                                                                            riderRequests.child(myPhone).child(phone).removeValue();
                                                                        }

                                                                        riderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myRiders.get(which));
                                                                        riderRequests.child(myPhone).child(phone).setValue("assigned");
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                    }
                                                                });
                                                            }
                                                        });
                                                    }
                                                });
                                                builder.create();
                                                builder.show();

                                            }

                                            else {
                                                //Change rider
                                                AlertDialog changeRider = new AlertDialog.Builder(ViewCustomerOrder.this)
                                                        .setTitle("Already exists")
                                                        .setMessage("Change rider?")
                                                        //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                                                        .setCancelable(false)
                                                        //set three option buttons
                                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                                //Load riders list
                                                                String [] riders = new String[ridersName.size()];
                                                                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ViewCustomerOrder.this);
                                                                builder.setItems(ridersName.toArray(riders), new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, final int which) {
                                                                        /**
                                                                         * Logic here is to assign the rider number to the order, then send rider request to rider awaiting confirmation
                                                                         */
                                                                        customerOrderItems.child("rider").setValue(myRiders.get(which)).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid) {


                                                                                myRidersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                        //Remove existing ride requests then set new request
                                                                                        for(DataSnapshot riders : dataSnapshot.getChildren()){
                                                                                            /**
                                                                                             * Delete any existing rider request for this particular order by looping through my riders,
                                                                                             * getting their primary key (phone) then deleting from 'my_rider_requests'
                                                                                             */
                                                                                            riderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+riders.getKey());
                                                                                            riderRequests.child(myPhone).child(phone).removeValue();
                                                                                        }

                                                                                        /**
                                                                                         * then send fresh rider order request, this is to avoid assigning a single order to
                                                                                         * multiple riders. Preventing duplicates
                                                                                         */
                                                                                        riderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myRiders.get(which));
                                                                                        riderRequests.child(myPhone).child(phone).setValue("assigned");
                                                                                    }

                                                                                    @Override
                                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                    }
                                                                                });
                                                                            }
                                                                        });
                                                                    }
                                                                });
                                                                builder.create();
                                                                builder.show();
                                                            }
                                                        })//setPositiveButton

                                                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                //Do nothing
                                                            }
                                                        })

                                                        .create();
                                                changeRider.show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                    //Load riders list

                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                    //SafeToast.makeText(ViewCustomerOrder.this, "Default rider is your account", Toast.LENGTH_LONG).show();
                                }
                            })

                            .create();
                    assignRider.show();
                }

                else {
                    //
                    SafeToast.makeText(this, "You must confirm at-least one item", Toast.LENGTH_LONG).show();
                }
            }

            return(true);

        case R.id.sendMessage:
            Intent slideactivity = new Intent(ViewCustomerOrder.this, Chat.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            slideactivity.putExtra("fromPhone", myPhone);
            slideactivity.putExtra("toPhone", phone);
            Bundle bndlanimation =
                    null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                bndlanimation = ActivityOptions.makeCustomAnimation(ViewCustomerOrder.this, R.anim.animation,R.anim.animation2).toBundle();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                startActivity(slideactivity, bndlanimation);
            }
            return  (true);
    }
        return(super.onOptionsItemSelected(item));
    }

    public String[] Split(String timeStamp){
        String[] arrSplit = {};
        try {
            arrSplit = timeStamp.split(":");
        } catch (Exception e){
            Log.e(TAG, "Split: ", e);
        }

        return arrSplit;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            customerOrderItems.removeEventListener(customerOrderItemsListener);
            customerOrderItems.removeEventListener(currentRiderListener);
            myRidersRef.removeEventListener(myRidersListener);
            riderStatus.removeEventListener(riderStatusListener);
        } catch (Exception e){

        }
    }
}
