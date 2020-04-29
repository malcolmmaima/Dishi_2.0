package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.NotificationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.NotificationAdapter;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyNotifications extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    String myPhone;
    FirebaseUser user;
    DatabaseReference notificationsRef;
    ValueEventListener notificationListener;
    RecyclerView recyclerView;
    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_notifications);

        icon = findViewById(R.id.menuIcon);
        recyclerView = findViewById(R.id.rview);
        emptyTag = findViewById(R.id.empty_tag);
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

                mSwipeRefreshLayout.setRefreshing(true);

                // Fetching data from server
                fetchNotifications();
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications/"+myPhone);

    }

    private void fetchNotifications() {
        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<NotificationModel> notifications = new ArrayList<>();
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);

                    NotificationAdapter recycler = new NotificationAdapter(MyNotifications.this, notifications);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(MyNotifications.this);
                    recyclerView.setLayoutManager(layoutmanager);
                    recyclerView.setItemAnimator(new DefaultItemAnimator());
                    recyclerView.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                }

                else {
                    for(DataSnapshot notif : dataSnapshot.getChildren()){
                        NotificationModel notification = notif.getValue(NotificationModel.class);
                        notifications.add(notification);

                        if (!notifications.isEmpty()) {

                            mSwipeRefreshLayout.setRefreshing(false);
                            //Collections.reverse(notifications);
                            try {
                                Collections.sort(notifications, (not1, not2) -> (not2.timeStamp.compareTo(not1.timeStamp)));
                            } catch (Exception e){}
                            NotificationAdapter recycler = new NotificationAdapter(MyNotifications.this, notifications);
                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(MyNotifications.this);
                            recyclerView.setLayoutManager(layoutmanager);
                            recyclerView.setItemAnimator(new DefaultItemAnimator());
                            recycler.notifyDataSetChanged();
                            recyclerView.setAdapter(recycler);
                            emptyTag.setVisibility(View.INVISIBLE);
                            icon.setVisibility(View.INVISIBLE);
                        } else {

                            mSwipeRefreshLayout.setRefreshing(false);

                            NotificationAdapter recycler = new NotificationAdapter(MyNotifications.this, notifications);
                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(MyNotifications.this);
                            recyclerView.setLayoutManager(layoutmanager);
                            recyclerView.setItemAnimator(new DefaultItemAnimator());
                            recyclerView.setAdapter(recycler);
                            emptyTag.setVisibility(View.VISIBLE);
                            icon.setVisibility(View.VISIBLE);

                        }
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        notificationsRef.addValueEventListener(notificationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationsRef.removeEventListener(notificationListener);
    }

    @Override
    public void onRefresh() {
        fetchNotifications();
    }
}
