package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.R;



public class DeliverCharges extends AppCompatActivity {

    EditText pricePerKm, deliveryWaiver;
    AppCompatButton saveDetails;
    String myPhone;
    int price, deliveryWaiverFee;
    DatabaseReference myRef;
    FirebaseAuth mAuth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliver_charges);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            pricePerKm = findViewById(R.id.pricePerKm);
            deliveryWaiver = findViewById(R.id.deliveryWaiver);
            saveDetails = findViewById(R.id.saveDetails);

            //Set fb database reference
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

                                    if (locked == true) {
                                        Intent slideactivity = new Intent(DeliverCharges.this, SecurityPin.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        slideactivity.putExtra("pinType", "resume");
                                        startActivity(slideactivity);
                                    }
                                } catch (Exception e){

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
            myRef.child("delivery_charge").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        myRef.child("delivery_charge").setValue(0);
                        pricePerKm.setText("0");
                    }

                    else {
                        price = dataSnapshot.getValue(Integer.class);
                        pricePerKm.setText(""+price);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            myRef.child("deliveryChargeLimit").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        myRef.child("deliveryChargeLimit").setValue(0);
                        deliveryWaiver.setText("0");
                    }

                    else {
                        deliveryWaiverFee = dataSnapshot.getValue(Integer.class);
                        deliveryWaiver.setText(""+deliveryWaiverFee);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            saveDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar snackbar = Snackbar
                            .make(findViewById(R.id.parentlayout), "Saving...", Snackbar.LENGTH_LONG);
                    snackbar.show();

                    price = Integer.parseInt(pricePerKm.getText().toString());
                    deliveryWaiverFee = Integer.parseInt(deliveryWaiver.getText().toString());

                    if(!pricePerKm.getText().equals("") && !deliveryWaiver.getText().equals("")){
                        myRef.child("delivery_charge").setValue(price).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                myRef.child("deliveryChargeLimit").setValue(deliveryWaiverFee).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(DeliverCharges.this, "Saved", Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                });

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar snackbar = Snackbar
                                        .make(findViewById(R.id.parentlayout), "Failed, try again", Snackbar.LENGTH_LONG);
                                snackbar.show();
                            }
                        });
                    }

                    else {
                        if(pricePerKm.getText().equals("")){
                            pricePerKm.setError("Can't be Empty");
                        }

                        if(deliveryWaiver.getText().equals("")){
                            deliveryWaiver.setError("Can't be Empty");
                        }
                    }
                }
            });

            Toolbar topToolBar = findViewById(R.id.toolbar);
            setSupportActionBar(topToolBar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            setTitle("Delivery Charges");

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
                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                            if(locked == true){
                                Intent slideactivity = new Intent(DeliverCharges.this, SecurityPin.class)
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
}
