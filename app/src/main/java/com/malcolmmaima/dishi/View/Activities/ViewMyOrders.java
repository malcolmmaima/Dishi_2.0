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

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.auth.User;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.ReceiptModel;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ViewOrderItemsAdapter;
import com.malcolmmaima.dishi.View.Maps.GeoTracking;
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

public class ViewMyOrders extends AppCompatActivity {
    String TAG = "ViewMyOrder";
    List<ProductDetailsModel> list;
    String myPhone, phone, restaurantName, riderPhone, initiatedTime, address, orderID, paymentMethod;
    String nodeKey;
    FirebaseUser user;
    DatabaseReference customerOrderItems, myOrders, myOrdersHistory, riderStatus, myRef, myPostUpdates;
    ValueEventListener customerOrderItemsListener, currentRiderListener, riderStatusListener;
    MyTextView_Roboto_Medium myOrderID, totalBill;
    MyTextView_Roboto_Regular subTotal, deliveryChargeAmount, payment, myRemarks, riderName, timeStamp, trackOrderTxt;
    ImageView riderIcon;
    Integer deliveryCharge;
    Double totalAmount;
    RecyclerView recyclerview;
    AppCompatButton confirmOrder, declineOrder;
    CardView DeliveryAddress;
    Menu myMenu;
    String [] riderOptions = {"View","Message", "Call"};
    final int[] total = {0};
    FirebaseAuth mAuth;
    Timer timer;
    HashTagHelper mTextHashTagHelper;
    String tempRiderPhoneHolder;
    Integer paid;
    Boolean paymentDialogShown, receiptGenerated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            try {
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
                                        Intent slideactivity = new Intent(ViewMyOrders.this, SecurityPin.class)
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

                loadMyOrders();

            } catch (Exception e){
                Log.e(TAG, "onCreate: ", e);
                SafeToast.makeText(this, "Something went wrong!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadMyOrders() {
        setContentView(R.layout.activity_view_my_orders);

        //Initialize variables
        deliveryCharge = 0;
        totalAmount = 0.0;

        paymentDialogShown = false;
        receiptGenerated = false;

        //Data passed from adapter
        phone = getIntent().getStringExtra("phone"); //From adapters
        restaurantName = getIntent().getStringExtra("name");

        customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+phone+"/"+myPhone);
        myOrdersHistory = FirebaseDatabase.getInstance().getReference("orders_history/"+myPhone);
        myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone);
        myPostUpdates = FirebaseDatabase.getInstance().getReference("posts/"+myPhone);
        DatabaseReference myReceiptsRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone);
        //Generate a receipt key one time on each load
        nodeKey = myReceiptsRef.push().getKey();

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(restaurantName);

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
        myRemarks = findViewById(R.id.myOrderRemarks);
        riderName = findViewById(R.id.riderName);
        riderIcon = findViewById(R.id.riderIcon);
        timeStamp = findViewById(R.id.timeStamp);
        myOrderID = findViewById(R.id.myOrderID);
        trackOrderTxt = findViewById(R.id.trackOrderTxt);


        /**
         * Initialize cart items total and keep track of items
         */
        customerOrderItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    SafeToast.makeText(ViewMyOrders.this, "Order does not exist", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    try {

                        Boolean completed = dataSnapshot.child("completed").getValue(Boolean.class);
                        deliveryCharge = dataSnapshot.child("deliveryCharge").getValue(Integer.class);
                        String remarks = dataSnapshot.child("remarks").getValue(String.class);
                        paid = dataSnapshot.child("paid").getValue(Integer.class);
                        orderID = dataSnapshot.child("orderID").getValue(String.class);
                        paymentMethod = dataSnapshot.child("paymentMethod").getValue(String.class);
                        address = dataSnapshot.child("address").getValue(String.class);
                        myOrderID.setText("ORDER ID: #"+orderID);
                        payment.setText(paymentMethod);
                        deliveryChargeAmount.setText("Ksh "+deliveryCharge);
                        totalAmount = Double.valueOf(total[0] + deliveryCharge);
                        subTotal.setText("Ksh " + total[0]);
                        totalBill.setText("Ksh " + totalAmount);

                        if(address.equals("pick")){
                            trackOrderTxt.setText("Locate Vendor");
                            riderName.setVisibility(View.GONE);
                            riderIcon.setVisibility(View.GONE);
                        }

                        initiatedTime = dataSnapshot.child("initiatedOn").getValue(String.class);

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
                            Log.e(TAG, "onDataChange: ", e);
                        }

                        myRemarks.setText("Remarks: "+remarks);

                        try {
                            Linkify.addLinks(myRemarks, Linkify.ALL);
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
                                            Intent slideactivity = new Intent(ViewMyOrders.this, SearchActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                            slideactivity.putExtra("searchString", searchHashTag);
                                            startActivity(slideactivity);
                                        }
                                    });

                            mTextHashTagHelper.handle(myRemarks);
                        }

                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                    total[0] = 0;

                    list = new ArrayList<>();
                    for(DataSnapshot items : dataSnapshot.child("items").getChildren()){

                        try {
                            ProductDetailsModel prod = items.getValue(ProductDetailsModel.class);
                            prod.setKey(items.getKey());
                            list.add(prod);

                            if(prod.getConfirmed() == true){
                                int adapterTotal = prod.getQuantity() * Integer.parseInt(prod.getPrice());
                                total[0] = total[0] + adapterTotal;
                                totalAmount = Double.valueOf(total[0] + deliveryCharge);
                            }

                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }
                    subTotal.setText("Ksh "+total[0]);

                    //Mambo kienyeji hapa (^_^) basically want to enable order tracking by customer if atleast one item is confirmed
                    if(total[0] < 1.0){
                        DeliveryAddress.setVisibility(View.GONE);
                        confirmOrder.setVisibility(View.GONE);
                    } else {
                        DeliveryAddress.setVisibility(View.VISIBLE);
                        confirmOrder.setVisibility(View.VISIBLE);
                    }

                    if(!list.isEmpty()){
                        Collections.reverse(list);
                        ViewOrderItemsAdapter recycler = new ViewOrderItemsAdapter(ViewMyOrders.this, list);
                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewMyOrders.this);
                        recyclerview.setLayoutManager(layoutmanager);
                        recyclerview.setItemAnimator( new DefaultItemAnimator());
                        recycler.notifyDataSetChanged();
                        recyclerview.setAdapter(recycler);


                    }

                    totalBill.setText("ksh " + totalAmount);

                    try {
                        //1 is default.. order has been sent 2. is awaiting payment confirmation 3. payment confirmed, order complete 4. order complete
                        if (paid == 4) {
                            paymentDialog(findViewById(android.R.id.content).getRootView());
                        }

                        if(paid == 3){
                            paymentDialog(findViewById(android.R.id.content).getRootView());
                        }


                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        customerOrderItems.addValueEventListener(customerOrderItemsListener);

        /**
         * fetch current rider
         */
        currentRiderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    riderName.setText(restaurantName);
                }

                else {
                    final DatabaseReference riderUserInfo = FirebaseDatabase.getInstance().getReference("users/"+dataSnapshot.getValue());
                    riderUserInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                UserModel riderUser = dataSnapshot.getValue(UserModel.class);
                                riderName.setText(riderUser.getFirstname() + " " + riderUser.getLastname());
                            } catch (Exception e){}
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    /**
                     * Reference:
                     *
                     * dataSnapshot.getValue() = the rider's phone
                     * phone = the restaurant's phone
                     */

                    riderStatus = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+"/"+dataSnapshot.getValue()+"/"+phone+"/"+myPhone);
                    riderStatusListener = new ValueEventListener() {
                        @SuppressLint("RestrictedApi")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dtSnapshot) {
                            String riderOrderStatus = dtSnapshot.getValue(String.class);
                            if(riderOrderStatus == null && riderPhone == null){
                                Snackbar.make(findViewById(R.id.parentlayout), "Error, rider does not exist!", Snackbar.LENGTH_LONG).show();
                                riderName.setText(restaurantName);
                                //customerOrderItems.child("rider").removeValue(); //bug
                            }

                            try {
                                if(riderName.getText().toString().trim().equals(restaurantName.trim())){
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                    confirmOrder.setEnabled(true);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.colorPrimary));
                                }

                                else if (riderOrderStatus.equals("accepted")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                    confirmOrder.setEnabled(true);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.colorPrimary));
                                }

                                else if (riderOrderStatus.equals("assigned")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                    confirmOrder.setEnabled(false);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.grey));
                                }

                                else if (riderOrderStatus.equals("declined")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.GONE);
                                    confirmOrder.setEnabled(false);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.grey));
                                }


                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };
                    riderStatus.addValueEventListener(riderStatusListener);
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
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ViewMyOrders.this);
                    builder.setTitle(riderName.getText().toString());
                    builder.setItems(riderOptions, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int which) {
                            if(which == 0){
                                if(riderPhone != null){
                                    Snackbar.make(v.getRootView(), "In development", Snackbar.LENGTH_LONG).show();
                                }
                            }

                            if(which == 1){
                                if(riderPhone != null){
                                    Intent slideactivity = new Intent(ViewMyOrders.this, Chat.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                    slideactivity.putExtra("fromPhone", myPhone);
                                    slideactivity.putExtra("toPhone", riderPhone);
                                    Bundle bndlanimation =
                                            null;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        bndlanimation = ActivityOptions.makeCustomAnimation(ViewMyOrders.this, R.anim.animation,R.anim.animation2).toBundle();
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        startActivity(slideactivity, bndlanimation);
                                    }
                                }

                            }

                            if(which == 2){
                                if(riderPhone != null){
                                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", riderPhone, null));
                                    startActivity(intent);
                                }

                            }
                        }
                    });
                    builder.create();
                    builder.show();
                }

            }
        });

        DeliveryAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(ViewMyOrders.this, GeoTracking.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("restaurantPhone", phone);
                slideactivity.putExtra("customerPhone", myPhone);
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(ViewMyOrders.this, R.anim.animation,R.anim.animation2).toBundle();
                startActivity(slideactivity, bndlanimation);

            }
        });


        confirmOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                paymentDialog(v);
            }
        });

        declineOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog confirmorder = new AlertDialog.Builder(ViewMyOrders.this)
                        .setMessage("Cancel your order?")
                        //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                        .setCancelable(false)
                        //set three option buttons
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                customerOrderItems.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        myOrders.child(phone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                finish();
                                                SafeToast.makeText(ViewMyOrders.this, "Order Cancelled!", Toast.LENGTH_SHORT).show();
                                            }
                                        });

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
                confirmorder.show();
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            try {
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
                                        Intent slideactivity = new Intent(ViewMyOrders.this, SecurityPin.class)
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
                loadMyOrders();
            } catch (Exception e){
                SafeToast.makeText(this, "Something went wrong!", Toast.LENGTH_LONG).show();
            }
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
                                Intent slideactivity = new Intent(ViewMyOrders.this, SecurityPin.class)
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

        loadMyOrders();
    }

    public void paymentDialog(View view) {
        String message, action;
        message = "Have you paid for the order?";
        action = "YES";
        try {
            if (paid == 4) {
                action = "FINISH";
                message = "Vendor confirmed, finish order";
            } else {
                action = "YES";
                message = "Have you paid for the order?";
            }
        } catch (Exception e){
            Log.e(TAG, "paymentDialog: ", e);
        }
        if(paymentDialogShown == false){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogLayout = inflater.inflate(R.layout.alert_dialog_payment, null);
            builder.setCancelable(false);
            builder.setMessage(message);
            builder.setPositiveButton(action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    paymentDialogShown = false;
                    customerOrderItems.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                paid = dataSnapshot.child("paid").getValue(Integer.class);
                                if (paid == 4) {
                                    receiptGenerated = true;
                                    DatabaseReference riderReceiptRef = FirebaseDatabase.getInstance().getReference("receipts/" + riderPhone);
                                    DatabaseReference vendorReceiptsRef = FirebaseDatabase.getInstance().getReference("receipts/" + phone);
                                    DatabaseReference receiptsRef = FirebaseDatabase.getInstance().getReference("receipts/" + myPhone);
                                    ReceiptModel receipt = new ReceiptModel();
                                    GetCurrentDate currentDate = new GetCurrentDate();

                                    customerOrderItems.child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            for (DataSnapshot items : dataSnapshot.getChildren()) {
                                                ProductDetailsModel item = items.getValue(ProductDetailsModel.class);
                                                if (item.getConfirmed() == true) {

                                                    if (riderPhone != null) {
                                                        riderReceiptRef.child(nodeKey).child("items").child(items.getKey()).setValue(item);
                                                    }
                                                    vendorReceiptsRef.child(nodeKey).child("items").child(items.getKey()).setValue(item);
                                                    receiptsRef.child(nodeKey).child("items").child(items.getKey()).setValue(item);
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
                                    receipt.setRestaurant(phone);
                                    receipt.setCustomer(myPhone);
                                    receipt.setSeen(false);

                                    //Post status update if i've set shareOrders in settings to ON
                                    myRef.child("shareOrders").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {

                                                try {
                                                    Boolean sharedOrders = dataSnapshot.getValue(Boolean.class);

                                                    if (sharedOrders == true) {

                                                        DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/" + phone);
                                                        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot userVendor) {
                                                                receiptsRef.child(nodeKey).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                        if (dataSnapshot.exists()) {
                                                                            UserModel vendor = userVendor.getValue(UserModel.class);
                                                                            vendor.setPhone(phone);

                                                                            GetCurrentDate currentDate = new GetCurrentDate();
                                                                            String postDate = currentDate.getDate();

                                                                            String message = vendor.getFirstname() + " " + vendor.getLastname() + " successfully delivered my order :-) #DishiFoodie";
                                                                            StatusUpdateModel statusUpdate = new StatusUpdateModel();
                                                                            statusUpdate.setReceiptKey(nodeKey);
                                                                            statusUpdate.setStatus(message);
                                                                            statusUpdate.setAuthor(myPhone);
                                                                            statusUpdate.setVendorPhone(phone);
                                                                            statusUpdate.setPostedTo(myPhone);
                                                                            statusUpdate.setTimePosted(postDate);
                                                                            statusUpdate.setImageShare(null);
                                                                            String key = myPostUpdates.push().getKey();
                                                                            myPostUpdates.child(key).setValue(statusUpdate);
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                    }
                                                                });

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "onDataChange: ", e);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                                    if (riderPhone != null) {
                                        riderReceiptRef.child(nodeKey).setValue(receipt);
                                    }
                                    vendorReceiptsRef.child(nodeKey).setValue(receipt);
                                    receiptsRef.child(nodeKey).setValue(receipt).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            customerOrderItems.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for (final DataSnapshot items : dataSnapshot.child("items").getChildren()) {
                                                        try {
                                                            //We need to capture the rider phone before the node is removed. this will allow us to
                                                            //update rider status below on deletion. noticed the rider phone was being deleted with the order complete
                                                            //which would mean in turn we are unable to update the rider status
                                                            tempRiderPhoneHolder = dataSnapshot.child("rider").getValue(String.class);
                                                        } catch (Exception e) {
                                                        }
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

                                                                            //Log.d("SuccessOrder", "update rider status => del: \nmy_ride_requests/"+tempRiderPhoneHolder+"/"+phone+"/"+myPhone);
                                                                            myOrders.child(phone).removeValue();
                                                                            DatabaseReference rider = FirebaseDatabase.getInstance().getReference
                                                                                    ("my_ride_requests/" + tempRiderPhoneHolder + "/" + phone + "/" + myPhone);

                                                                            rider.removeValue();

                                                                        }
                                                                    });
                                                                }
                                                            });

                                                        } catch (Exception e) {

                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    });
                                } else {
                                    customerOrderItems.child("paid").setValue(2).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Snackbar.make(view.getRootView(), "Waiting for confirmation from vendor", Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            } catch (Exception e){
                                Log.e(TAG, "onClick: ", e);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    paymentDialogShown = false;
                    customerOrderItems.child("paid").setValue(1).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar.make(view.getRootView(), "Please pay to complete this order", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            });
            builder.setView(dialogLayout);
            builder.show();
            paymentDialogShown = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_my_order_menu, menu);
        myMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) { switch(item.getItemId()) {

        case R.id.callRestaurant:

            final AlertDialog callAlert = new AlertDialog.Builder(this)
                    //set message, title, and icon
                    .setMessage("Call " + restaurantName + "?")
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

        case R.id.sendMessage:
            Intent slideactivity = new Intent(ViewMyOrders.this, Chat.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            slideactivity.putExtra("fromPhone", myPhone);
            slideactivity.putExtra("toPhone", phone);
            Bundle bndlanimation =
                    null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                bndlanimation = ActivityOptions.makeCustomAnimation(ViewMyOrders.this, R.anim.animation,R.anim.animation2).toBundle();
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
        } catch (Exception e){

        }
    }
}
