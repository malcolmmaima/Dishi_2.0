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
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.BlockedUserAdapter;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class MyBlockedAccounts extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    String myPhone;
    DatabaseReference myRef, myBlockedUsers;
    FirebaseUser user;
    FirebaseAuth mAuth;
    AppCompatImageView icon;
    MyTextView_Roboto_Medium emptyTag;
    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView recyclerview;
    LinearLayoutManager layoutmanager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_blocked_accounts);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Blocked Accounts");

        emptyTag = findViewById(R.id.empty_tag);
        emptyTag.setVisibility(View.GONE);
        recyclerview = findViewById(R.id.rview);

        layoutmanager = new LinearLayoutManager(MyBlockedAccounts.this);
        recyclerview.setLayoutManager(layoutmanager);

        icon = findViewById(R.id.blockedIcon);
        icon.setVisibility(View.GONE);
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
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber();
            myBlockedUsers = FirebaseDatabase.getInstance().getReference("blocked/"+myPhone);
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
                                    Intent slideactivity = new Intent(MyBlockedAccounts.this, SecurityPin.class)
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

            /**
             * Showing Swipe Refresh animation on activity create
             * As animation won't start on onCreate, post runnable is used
             */
            mSwipeRefreshLayout.post(new Runnable() {

                @Override
                public void run() {

                    mSwipeRefreshLayout.setRefreshing(true);
                    fetchBlocked();
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

    private void fetchBlocked() {
        mSwipeRefreshLayout.setRefreshing(true);
        List<UserModel> blockedUsers = new ArrayList<>();
        myBlockedUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    BlockedUserAdapter recycler = new BlockedUserAdapter(MyBlockedAccounts.this, blockedUsers);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                }

                else {
                    for(DataSnapshot blockedUser : dataSnapshot.getChildren()){
                        DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+blockedUser.getKey());
                        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserModel userFound = dataSnapshot.getValue(UserModel.class);
                                userFound.setPhone(blockedUser.getKey());
                                blockedUsers.add(userFound);


                                if (!blockedUsers.isEmpty()) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    //Collections.reverse(orders);
                                    BlockedUserAdapter recycler = new BlockedUserAdapter(MyBlockedAccounts.this, blockedUsers);
                                    recyclerview.setLayoutManager(layoutmanager);
                                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                                    recycler.notifyDataSetChanged();
                                    recyclerview.setAdapter(recycler);
                                    emptyTag.setVisibility(View.GONE);
                                    icon.setVisibility(View.GONE);
                                } else {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    BlockedUserAdapter recycler = new BlockedUserAdapter(MyBlockedAccounts.this, blockedUsers);
                                    recyclerview.setLayoutManager(layoutmanager);
                                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                                    recyclerview.setAdapter(recycler);
                                    emptyTag.setVisibility(View.GONE);
                                    icon.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

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
                                Intent slideactivity = new Intent(MyBlockedAccounts.this, SecurityPin.class)
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
        fetchBlocked();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        recyclerview.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                // no-op
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                recyclerview.setAdapter(null);
                layoutmanager = null;
            }
        });
    }
}
