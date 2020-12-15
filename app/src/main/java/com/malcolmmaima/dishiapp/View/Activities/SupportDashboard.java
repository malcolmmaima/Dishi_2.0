package com.malcolmmaima.dishiapp.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.R;



public class SupportDashboard extends AppCompatActivity {
    FirebaseAuth mAuth;
    CardView card1, card2, card3, card4;
    TextView usersTxt, reportsText;
    DatabaseReference usersRef;
    ValueEventListener usersRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_dashboard);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            //Keep count of total users on platform
            usersRef = FirebaseDatabase.getInstance().getReference("users");
            usersRefListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        usersTxt.setText("Users (" + dataSnapshot.getChildrenCount() + ")");
                    } catch (Exception e){ }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            usersRef.addValueEventListener(usersRefListener);

            card1 = findViewById(R.id.card1);
            usersTxt = findViewById(R.id.usersTxt);

            card2 = findViewById(R.id.card2);
            reportsText = findViewById(R.id.reportsText);

            card3 = findViewById(R.id.card3);
            card4 = findViewById(R.id.card4);

            card1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent mainActivity = new Intent(SupportDashboard.this, UserManagement.class);
                    mainActivity.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP );
                    startActivity(mainActivity);
                }
            });
        }

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Support Dashboard");

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try { usersRef.removeEventListener(usersRefListener); } catch (Exception e){  }
    }
}
