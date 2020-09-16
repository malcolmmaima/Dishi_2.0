package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jsibbold.zoomage.ZoomageView;
import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;



public class ViewImage extends AppCompatActivity {

    ZoomageView viewImage;
    ProgressBar loading;
    FirebaseAuth mAuth;
    DatabaseReference myRef;
    FirebaseUser user;
    String myPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            mAuth = FirebaseAuth.getInstance();
            if(mAuth.getInstance().getCurrentUser() == null){
                finish();
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            } else {
                user = FirebaseAuth.getInstance().getCurrentUser();
                myPhone = user.getPhoneNumber();
                myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
                myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    try {
                                        Boolean locked = dataSnapshot.getValue(Boolean.class);

                                        if(locked == true){
                                            Intent slideactivity = new Intent(ViewImage.this, SecurityPin.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            slideactivity.putExtra("pinType", "resume");
                                            startActivity(slideactivity);
                                        }
                                    } catch(Exception e){ }
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

            viewImage = findViewById(R.id.viewImage);
            loading = findViewById(R.id.progressBar);
            loading.setVisibility(View.VISIBLE);

            final Toolbar topToolBar = findViewById(R.id.toolbar);
            topToolBar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            setSupportActionBar(topToolBar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            setTitle("");

            final String imageURL = getIntent().getStringExtra("imageURL");


            try {
                Picasso.with(this)
                        .load(imageURL)

                        .into(viewImage, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                loading.setVisibility(View.INVISIBLE);
                            }

                            @Override
                            public void onError() {
                                loading.setVisibility(View.INVISIBLE);
                                Snackbar snackbar = Snackbar.make(findViewById(R.id.parentLayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                snackbar.show();
                            }
                        });
            } catch (Exception e){}
            //Back button on toolbar
            topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); //Go back to previous activity
                }
            });
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
                            try {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if (locked == true) {
                                    Intent slideactivity = new Intent(ViewImage.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                }
                            } catch (Exception e){ }
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
}
