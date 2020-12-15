package com.malcolmmaima.dishiapp.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.R;

public class PrivacyPolicy extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    String TAG = "PrivacyPolicy";
    WebView privacyPolicy;
    SwipeRefreshLayout mSwipeRefreshLayout;
    DatabaseReference defaultsRef;
    String policyUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);
        privacyPolicy = findViewById(R.id.webView);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Privacy Policy");

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(PrivacyPolicy.this);
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
                loadPrivacyPolicy();
            }
        });

        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });
    }

    private void loadPrivacyPolicy() {
        mSwipeRefreshLayout.setRefreshing(true);
        defaultsRef = FirebaseDatabase.getInstance().getReference("admin");
        defaultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    mSwipeRefreshLayout.setRefreshing(false);
                    policyUrl = dataSnapshot.child("privacyPolicy").getValue(String.class);
                    privacyPolicy.loadUrl(policyUrl);
                } catch (Exception e){
                    mSwipeRefreshLayout.setRefreshing(false);
                    Log.e(TAG, "onDataChange: ", e);
                    Toast.makeText(PrivacyPolicy.this, "Something went wrong, try reloading!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onRefresh() {
        loadPrivacyPolicy();
    }
}
