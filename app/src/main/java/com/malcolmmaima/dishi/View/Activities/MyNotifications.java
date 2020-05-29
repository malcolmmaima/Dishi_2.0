package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.NotificationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.NotificationAdapter;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class MyNotifications extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    String TAG = "MyNotifications";
    String myPhone;
    FirebaseUser user;
    DatabaseReference notificationsRef, myRef;
    ChildEventListener notificationListener;
    RecyclerView recyclerView;
    TextView emptyTag, loadMore;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    FirebaseAuth mAuth;
    List<NotificationModel> notifications;
    Query notifs;
    int defaultNotifCount;
    int mPosts;
    LinearLayoutManager layoutmanager;
    NotificationAdapter recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_notifications);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            notificationsRef = FirebaseDatabase.getInstance().getReference("notifications/"+myPhone);
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
                                    Intent slideactivity = new Intent(MyNotifications.this, SecurityPin.class)
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

            loadActivity();
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
                                Intent slideactivity = new Intent(MyNotifications.this, SecurityPin.class)
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

    private void loadActivity() {
        notifications = new ArrayList<>();
        defaultNotifCount = 10;
        mPosts = 10;

        icon = findViewById(R.id.menuIcon);
        recyclerView = findViewById(R.id.rview);
        emptyTag = findViewById(R.id.empty_tag);
        loadMore = findViewById(R.id.loadMore);
        loadMore.setVisibility(View.GONE);

        layoutmanager = new LinearLayoutManager(MyNotifications.this);
        recycler = new NotificationAdapter(MyNotifications.this, notifications);
        recyclerView.setLayoutManager(layoutmanager);
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Notifications");
        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(MyNotifications.this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                // Fetching data from server
                fetchNotifications(defaultNotifCount, false);
            }
        });

        loadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPosts = mPosts + 5;
                fetchNotifications(mPosts, true);
            }
        });
    }

    private void fetchNotifications(int notifCount, Boolean refresh) {
        mSwipeRefreshLayout.setRefreshing(true);
        notifications.clear();

        notifs = notificationsRef.limitToLast(notifCount);
        notificationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    recyclerView.setLayoutManager(layoutmanager);
                    recyclerView.setItemAnimator(new DefaultItemAnimator());
                    recyclerView.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                }

                else {
                    NotificationModel notification = dataSnapshot.getValue(NotificationModel.class);

                    if(!notifications.contains(notification)){
                        notifications.add(notification);
                        recyclerView.setHasFixedSize(true);
                    }

                    if (!notifications.isEmpty()) {

                        mSwipeRefreshLayout.setRefreshing(false);
                        //Collections.reverse(notifications);
                        try {
                            Collections.sort(notifications, (not1, not2) -> (not2.timeStamp.compareTo(not1.timeStamp)));
                        } catch (Exception e){}
                        recyclerView.setLayoutManager(layoutmanager);
                        recyclerView.setItemAnimator(new DefaultItemAnimator());
                        recycler.notifyDataSetChanged();
                        recyclerView.setAdapter(recycler);
                        emptyTag.setVisibility(View.INVISIBLE);
                        icon.setVisibility(View.INVISIBLE);

                        if(refresh == true){
                            try {
                                recyclerView.smoothScrollToPosition(notifCount);
                            } catch (Exception e){
                                Log.e(TAG, "onClick: ", e);
                            }
                        }



                    } else {

                        mSwipeRefreshLayout.setRefreshing(false);

                        recyclerView.setLayoutManager(layoutmanager);
                        recyclerView.setItemAnimator(new DefaultItemAnimator());
                        recyclerView.setAdapter(recycler);
                        emptyTag.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.VISIBLE);

                    }
                }

                //show/hide loadmore...
                notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        loadMore.setVisibility(View.GONE);
                        if(dataSnapshot.getChildrenCount() > notifications.size()){
                            loadMore.setVisibility(View.VISIBLE);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        notifs.addChildEventListener(notificationListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //update all notifications status to seen = true
        notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot notifs : dataSnapshot.getChildren()){
                    String notifKey = notifs.getKey();
                    notificationsRef.child(notifKey).child("seen").setValue(true);
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
            notifs.removeEventListener(notificationListener);
        } catch (Exception e){
            Log.e("MyNotifications", "onDestroy: ", e);
        }
    }

    @Override
    public void onRefresh() {
        fetchNotifications(defaultNotifCount, false);
    }
}
