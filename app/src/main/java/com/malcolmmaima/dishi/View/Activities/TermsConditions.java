package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.R;



public class TermsConditions extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    String TAG = "TermsConditions";
    WebView termsConditions;
    SwipeRefreshLayout mSwipeRefreshLayout;
    DatabaseReference defaultsRef;
    FirebaseAuth mAuth;
    String termsUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);
        termsConditions = findViewById(R.id.webView);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Terms & Conditions");

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(TermsConditions.this);
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
                loadTermsConditions();
            }
        });

        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });
    }

    private void loadTermsConditions() {
        mSwipeRefreshLayout.setRefreshing(true);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            try {
                String manualLink = "https://firebasestorage.googleapis.com/v0/b/dishi-food.appspot.com/o/defaults%2FTerms%20and%20Conditions.html?alt=media&token=4c60b0e6-8ec7-405e-b47a-688739f1cdb1";
                mSwipeRefreshLayout.setRefreshing(false);
                termsConditions.loadUrl(manualLink);
            } catch (Exception e){
                mSwipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "onDataChange: ", e);
                Toast.makeText(TermsConditions.this, "Something went wrong, try reloading!", Toast.LENGTH_LONG).show();
            }
        } else {
            defaultsRef = FirebaseDatabase.getInstance().getReference("admin");
            defaultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        mSwipeRefreshLayout.setRefreshing(false);
                        termsUrl = dataSnapshot.child("termsConditions").getValue(String.class);
                        termsConditions.loadUrl(termsUrl);
                    } catch (Exception e){
                        mSwipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "onDataChange: ", e);
                        Toast.makeText(TermsConditions.this, "Something went wrong, try reloading!", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }


    }

    @Override
    public void onRefresh() {
        loadTermsConditions();
    }
}
