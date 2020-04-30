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
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ViewOrderItemsAdapter;
import com.malcolmmaima.dishi.View.Maps.GeoTracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class ViewMyOrders extends AppCompatActivity {

    List<ProductDetailsModel> list;
    String myPhone, phone, restaurantName, riderPhone;
    FirebaseUser user;
    DatabaseReference customerOrderItems, myOrders, myOrdersHistory, riderStatus;
    ValueEventListener customerOrderItemsListener, currentRiderListener, riderStatusListener;
    TextView subTotal, deliveryChargeAmount, payment, totalBill, myRemarks, riderName;
    ImageView riderIcon;
    Double deliveryCharge, totalAmount;
    RecyclerView recyclerview;
    AppCompatButton confirmOrder, declineOrder;
    CardView DeliveryAddress;
    Menu myMenu;
    String [] riderOptions = {"View","Message", "Call"};
    final int[] total = {0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_my_orders);

        final String[] tempRiderPhoneHolder = new String[1];

        //Initialize variables
        deliveryCharge = 0.0;
        totalAmount = 0.0;

        //Data passed from adapter
        phone = getIntent().getStringExtra("phone"); //From adapters
        restaurantName = getIntent().getStringExtra("name");

        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
        } catch (Exception e){
            SafeToast.makeText(this, "Something went wrong!", Toast.LENGTH_LONG).show();
        }

        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+phone+"/"+myPhone);
        myOrdersHistory = FirebaseDatabase.getInstance().getReference("orders_history/"+myPhone);
        myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone);

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

        /**
         * On loading this module check to see if it exists. this is a one time check
         */
        customerOrderItems.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    SafeToast.makeText(ViewMyOrders.this, "Order does not exist", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        /**
         * Initialize cart items total and keep track of items
         */
        customerOrderItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    finish();
                }

                try {
                    Boolean completed = dataSnapshot.child("completed").getValue(Boolean.class);
                    String remarks = dataSnapshot.child("remarks").getValue(String.class);

                    try {
                        riderPhone = dataSnapshot.child("rider").getValue(String.class);

                    } catch (Exception e){

                    }

                    myRemarks.setText("Remarks: "+remarks);
                    if (completed == true) {
                        final AlertDialog finish = new AlertDialog.Builder(ViewMyOrders.this)
                                .setMessage("Order Delivered?")
                                //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                                .setCancelable(false)
                                //set three option buttons
                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        customerOrderItems.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                for (final DataSnapshot items : dataSnapshot.child("items").getChildren()) {

                                                    try {
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

                                                                        myOrders.child(phone).removeValue();
                                                                        DatabaseReference rider = FirebaseDatabase.getInstance().getReference
                                                                                ("my_ride_requests/"+tempRiderPhoneHolder[0]+"/"+phone+"/"+myPhone);

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
                } catch (Exception e){

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
                            totalAmount = total[0] + deliveryCharge;
                            payment.setText(prod.getPaymentMethod()); //We just need to capture the payment method from one of the items
                        }

                    } catch (Exception e){

                    }
                }
                subTotal.setText("Ksh "+total[0]);

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
                                if (riderOrderStatus.equals("accepted")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                    confirmOrder.setEnabled(true);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.colorPrimary));
                                }

                                if (riderOrderStatus.equals("assigned")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                    confirmOrder.setEnabled(false);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.grey));
                                }

                                if (riderOrderStatus.equals("declined")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.GONE);
                                    confirmOrder.setEnabled(false);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.grey));
                                }

                            } catch (Exception e){

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
                final AlertDialog confirmorder = new AlertDialog.Builder(ViewMyOrders.this)
                        .setMessage("Order has been delivered?")
                        //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                        .setCancelable(false)
                        //set three option buttons
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

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

                                                                //Log.d("SuccessOrder", "update rider status => del: \nmy_ride_requests/"+tempRiderPhoneHolder+"/"+phone+"/"+myPhone);
                                                                myOrders.child(phone).removeValue();
                                                                DatabaseReference rider = FirebaseDatabase.getInstance().getReference
                                                                        ("my_ride_requests/"+tempRiderPhoneHolder[0]+"/"+phone+"/"+myPhone);

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
                        })//setPositiveButton

                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Do nothing
                                Snackbar.make(v.getRootView(), "Confirm once order has been delivered", Snackbar.LENGTH_LONG).show();
                            }
                        })
                        .create();
                confirmorder.show();
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

        //Data passed from adapter
        final String phone_ = intent.getStringExtra("phone"); //From adapters
        final String restaurantName_ = intent.getStringExtra("name");
        setTitle(restaurantName_);

        phone = phone_;
        restaurantName = restaurantName_;

        //SafeToast.makeText(this, "Phone: " + phone_, Toast.LENGTH_LONG).show();

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+phone_+"/"+myPhone);
        myOrdersHistory = FirebaseDatabase.getInstance().getReference("orders_history/"+myPhone);
        myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone);

        /**
         * Initialize cart items total and keep track of items
         */
        customerOrderItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    finish();
                }

                try {
                    Boolean completed = dataSnapshot.child("completed").getValue(Boolean.class);
                    String remarks = dataSnapshot.child("remarks").getValue(String.class);

                    try {
                        riderPhone = dataSnapshot.child("rider").getValue(String.class);
                    } catch (Exception e){

                    }

                    myRemarks.setText("Remarks: "+remarks);
                    if (completed == true) {
                        final AlertDialog finish = new AlertDialog.Builder(ViewMyOrders.this)
                                .setMessage("Order Delivered?")
                                //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                                .setCancelable(false)
                                //set three option buttons
                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        customerOrderItems.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                for (final DataSnapshot items : dataSnapshot.child("items").getChildren()) {

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

                                                                        myOrders.child(phone_).removeValue();
                                                                        DatabaseReference rider = FirebaseDatabase.getInstance().getReference
                                                                                ("my_ride_requests/"+riderPhone+"/"+phone_+"/"+myPhone);

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
                } catch (Exception e){

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
                            totalAmount = total[0] + deliveryCharge;
                            payment.setText(prod.getPaymentMethod()); //We just need to capture the payment method from one of the items
                        }

                    } catch (Exception e){

                    }
                }
                subTotal.setText("Ksh "+total[0]);

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
                    riderName.setText(restaurantName_);
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

                    riderStatus = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+"/"+dataSnapshot.getValue()+"/"+phone_+"/"+myPhone);
                    riderStatusListener = new ValueEventListener() {
                        @SuppressLint("RestrictedApi")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dtSnapshot) {
                            String riderOrderStatus = dtSnapshot.getValue(String.class);
                            if(riderOrderStatus == null && riderPhone == null){
                                Snackbar.make(findViewById(R.id.parentlayout), "Error, rider does not exist!", Snackbar.LENGTH_LONG).show();
                                riderName.setText(restaurantName_);
                                //customerOrderItems.child("rider").removeValue(); //bug
                            }

                            try {
                                if (riderOrderStatus.equals("accepted")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                    confirmOrder.setEnabled(true);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.colorPrimary));
                                }

                                if (riderOrderStatus.equals("assigned")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                    confirmOrder.setEnabled(false);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.grey));
                                }

                                if (riderOrderStatus.equals("declined")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewMyOrders.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
                                    confirmOrder.setVisibility(View.GONE);
                                    confirmOrder.setEnabled(false);
                                    confirmOrder.setSupportBackgroundTintList(ContextCompat.getColorStateList(ViewMyOrders.this, R.color.grey));
                                }

                            } catch (Exception e){

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        customerOrderItems.removeEventListener(customerOrderItemsListener);
    }

}
