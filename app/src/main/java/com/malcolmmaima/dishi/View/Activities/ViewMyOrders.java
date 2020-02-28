package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
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
import com.malcolmmaima.dishi.Controller.CalculateDistance;
import com.malcolmmaima.dishi.Controller.OnOrderChecked;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ViewOrderAdapter;
import com.malcolmmaima.dishi.View.Adapter.ViewOrderItemsAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewMyOrders extends AppCompatActivity {

    List<ProductDetails> list;
    String myPhone, phone, customerName;
    FirebaseUser user;
    DatabaseReference customerOrderItems, myOrders;
    ValueEventListener customerOrderItemsListener;
    TextView subTotal, deliveryChargeAmount, payment, totalBill;
    Double deliveryCharge, totalAmount;
    RecyclerView recyclerview;
    AppCompatButton confirmOrder, declineOrder;
    CardView DeliveryAddress;

    final int[] total = {0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_my_orders);

        //Initialize variables
        deliveryCharge = 0.0;
        totalAmount = 0.0;

        //Data passed from adapter
        phone = getIntent().getStringExtra("phone"); //From adapters
        customerName = getIntent().getStringExtra("name");

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+phone+"/"+myPhone);
        myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone);

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

        /**
         * Initialize cart items total and keep track of items
         */
        customerOrderItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    finish();
                    Toast.makeText(ViewMyOrders.this, "Does not exist!", Toast.LENGTH_SHORT).show();
                }
                total[0] = 0;

                list = new ArrayList<>();
                for(DataSnapshot items : dataSnapshot.child("items").getChildren()){

                    try {
                        ProductDetails prod = items.getValue(ProductDetails.class);
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

                    recyclerview.getItemAnimator().setAddDuration(200);
                    recyclerview.getItemAnimator().setRemoveDuration(200);
                    recyclerview.getItemAnimator().setMoveDuration(200);
                    recyclerview.getItemAnimator().setChangeDuration(200);
                    recyclerview.setAdapter(recycler);


                }

                totalBill.setText("ksh " + totalAmount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        customerOrderItems.addValueEventListener(customerOrderItemsListener);

        DeliveryAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ViewMyOrders.this, "Clicked", Toast.LENGTH_SHORT).show();

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

                                customerOrderItems.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        myOrders.child(phone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                finish();
                                                Toast.makeText(ViewMyOrders.this, "Order Complete", Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(ViewMyOrders.this, "Order Cancelled!", Toast.LENGTH_SHORT).show();
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
    protected void onDestroy() {
        super.onDestroy();

        customerOrderItems.removeEventListener(customerOrderItemsListener);
    }

}
