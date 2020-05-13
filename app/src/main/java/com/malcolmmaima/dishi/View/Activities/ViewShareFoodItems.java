package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Utils.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class ViewShareFoodItems extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    String TAG = "ViewShareFoodItems";
    FirebaseAuth mAuth;
    String myPhone, receiptKey, accountType, authorPhone;
    DatabaseReference myRef, receiptRef, myLocationRef;
    List<ProductDetailsModel> list;
    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView recyclerview;
    LiveLocationModel liveLocationModel;
    ValueEventListener locationListener, accountTypeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_share_food_items);


        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Ordered Items");

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });
        recyclerview = findViewById(R.id.rview);
        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            receiptKey = getIntent().getStringExtra("receiptKey");
            authorPhone = getIntent().getStringExtra("author");

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
            receiptRef = FirebaseDatabase.getInstance().getReference("receipts/"+authorPhone+"/"+receiptKey);
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
                                    Intent slideactivity = new Intent(ViewShareFoodItems.this, SecurityPin.class)
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

            mSwipeRefreshLayout.post(new Runnable() {

                @Override
                public void run() {
                    loadItems();
                }
            });


        }
    }

    private void loadItems() {
        mSwipeRefreshLayout.setRefreshing(true);

        accountTypeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                accountType = dataSnapshot.getValue(String.class);
                Log.d(TAG, "accountType: "+ accountType);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRef.child("account_type").addValueEventListener(accountTypeListener);

        receiptRef.child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list = new ArrayList<>();
                if(!dataSnapshot.exists()){
                    finish();
                    SafeToast.makeText(ViewShareFoodItems.this, "Items no longer exist!", Toast.LENGTH_LONG).show();
                } else {
                    for(DataSnapshot items : dataSnapshot.getChildren()){
                        ProductDetailsModel product = items.getValue(ProductDetailsModel.class);
                        product.setKey(product.getOriginalKey());
                        product.setDistance(0.0);
                        product.accountType = accountType;

                        list.add(product);

                        if (!list.isEmpty()) {
                            /**
                             * https://howtodoinjava.com/sort/collections-sort/
                             * We want to sort from nearest to furthest location
                             */
                            Collections.sort(list, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                            mSwipeRefreshLayout.setRefreshing(false);
                            //Collections.reverse(list);
                            ProductAdapter recycler = new ProductAdapter(ViewShareFoodItems.this, list);
                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewShareFoodItems.this);
                            recyclerview.setLayoutManager(layoutmanager);
                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                            recycler.notifyDataSetChanged();
                            recyclerview.setAdapter(recycler);
                        } else {
                            //finish();
                            SafeToast.makeText(ViewShareFoodItems.this, "Items no longer exist!", Toast.LENGTH_LONG).show();
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRefresh() {
        loadItems();
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
                                Intent slideactivity = new Intent(ViewShareFoodItems.this, SecurityPin.class)
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            myLocationRef.removeEventListener(locationListener);
            myRef.child("account_type").removeEventListener(accountTypeListener);
        } catch (Exception e){
            Log.e(TAG, "onDestroy: ", e);
        }
    }
}
