package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
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
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.MyDeviceModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.MyDeviceAdapter;
import com.malcolmmaima.dishi.View.Adapter.ReceiptAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class DeviceLoginActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    String TAG = "LoginActivity";
    String myPhone;
    DatabaseReference myRef, loggedDevicesRef;
    FirebaseAuth mAuth;
    FirebaseUser user;

    List<MyDeviceModel> list;
    RecyclerView recyclerview;
    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinearLayoutManager layoutmanager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_activity);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Login Activity");

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber();
            loggedDevicesRef = FirebaseDatabase.getInstance().getReference("mydevices/"+myPhone);
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
                                    Intent slideactivity = new Intent(DeviceLoginActivity.this, SecurityPin.class)
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

            icon = findViewById(R.id.phoneIcon);
            recyclerview = findViewById(R.id.rview);
            emptyTag = findViewById(R.id.empty_tag);

            layoutmanager = new LinearLayoutManager(DeviceLoginActivity.this);
            recyclerview.setLayoutManager(layoutmanager);

            // SwipeRefreshLayout
            mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            mSwipeRefreshLayout.setOnRefreshListener(this);
            mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                    android.R.color.holo_green_dark,
                    android.R.color.holo_orange_dark,
                    android.R.color.holo_blue_dark);

            /**
             * Showing Swipe Refresh animation on activity create
             * As animation won't start on onCreate, post runnable is used
             */
            mSwipeRefreshLayout.post(new Runnable() {

                @Override
                public void run() {
                    // Fetching data from server
                    fetchLoggedDevices();

                }
            });
        }

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

    }

    private void fetchLoggedDevices() {
        mSwipeRefreshLayout.setRefreshing(true);
        loggedDevicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list = new ArrayList<>();
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    MyDeviceAdapter recycler = new MyDeviceAdapter(DeviceLoginActivity.this,list);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                }

                else {
                    for(DataSnapshot device : dataSnapshot.getChildren()){
                        try {
                            MyDeviceModel myDevice = device.getValue(MyDeviceModel.class);
                            myDevice.setDeviceID(device.getKey());
                            list.add(myDevice);
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    if(!list.isEmpty()){
                        mSwipeRefreshLayout.setRefreshing(false);
                        Collections.reverse(list);
                        MyDeviceAdapter recycler = new MyDeviceAdapter(DeviceLoginActivity.this,list);
                        recyclerview.setLayoutManager(layoutmanager);
                        recyclerview.setItemAnimator( new DefaultItemAnimator());
                        recycler.notifyDataSetChanged();
                        recyclerview.setAdapter(recycler);
                        emptyTag.setVisibility(View.INVISIBLE);
                        icon.setVisibility(View.INVISIBLE);
                    }

                    else {

                        mSwipeRefreshLayout.setRefreshing(false);
                        MyDeviceAdapter recycler = new MyDeviceAdapter(DeviceLoginActivity.this,list);
                        recyclerview.setLayoutManager(layoutmanager);
                        recyclerview.setItemAnimator( new DefaultItemAnimator());
                        recyclerview.setAdapter(recycler);
                        emptyTag.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.VISIBLE);
                    }
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
                                Intent slideactivity = new Intent(DeviceLoginActivity.this, SecurityPin.class)
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
    public void onRefresh() {
        fetchLoggedDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        recyclerview.setAdapter(null);
        layoutmanager = null;
    }
}
