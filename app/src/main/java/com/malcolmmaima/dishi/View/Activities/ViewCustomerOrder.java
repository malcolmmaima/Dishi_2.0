package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.OnOrderChecked;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.CartAdapter;
import com.malcolmmaima.dishi.View.Adapter.ViewOrderAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewCustomerOrder extends AppCompatActivity implements OnOrderChecked {

    List<ProductDetails> list;
    String myPhone, phone, customerName;
    FirebaseUser user;
    DatabaseReference customerOrderItems;
    ValueEventListener customerOrderItemsListener;
    TextView subTotal, deliveryChargeAmount, payment, totalBill;
    Double deliveryCharge, totalAmount;
    RecyclerView recyclerview;
    AppCompatButton confirmOrder, declineOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_customer_order);

        //Initialize variables
        deliveryCharge = 0.0;
        totalAmount = 0.0;

        //Data passed from adapter
        phone = getIntent().getStringExtra("phone"); //From adapters
        customerName = getIntent().getStringExtra("name");

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
        declineOrder = findViewById(R.id.btn_decline);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+myPhone+"/"+phone);

        /**
         * Initialize cart items total and keep track of items
         */
        final int[] total = {0};
        customerOrderItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                total[0] = 0;
                list = new ArrayList<>();
                for(DataSnapshot items : dataSnapshot.child("items").getChildren()){

                    ProductDetails prod = items.getValue(ProductDetails.class);
                    list.add(prod);

                    int adapterTotal = prod.getQuantity() * Integer.parseInt(prod.getPrice());
                    total[0] = total[0] + adapterTotal;
                    totalAmount = total[0] + deliveryCharge;
                    payment.setText(prod.getPaymentMethod()); //We just need to capture the payment method from one of the items

                }
                subTotal.setText("Ksh "+total[0]);

                if(!list.isEmpty()){
                    Collections.reverse(list);
                    ViewOrderAdapter recycler = new ViewOrderAdapter(ViewCustomerOrder.this, list, ViewCustomerOrder.this);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewCustomerOrder.this);
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


        confirmOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        declineOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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

    @Override
    public void onItemChecked(Boolean value) {
        Toast.makeText(this, "checked: " + value, Toast.LENGTH_SHORT).show();
    }
}
