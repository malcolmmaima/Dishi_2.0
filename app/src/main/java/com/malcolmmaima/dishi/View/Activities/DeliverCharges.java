package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.R;

import io.fabric.sdk.android.services.common.SafeToast;

public class DeliverCharges extends AppCompatActivity {

    EditText pricePerKm;
    AppCompatButton saveDetails;
    String myPhone;
    int price;
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliver_charges);

        pricePerKm = findViewById(R.id.pricePerKm);
        saveDetails = findViewById(R.id.saveDetails);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

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


        saveDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                price = Integer.parseInt(pricePerKm.getText().toString());
                if(!pricePerKm.getText().equals("")){
                    myRef.child("delivery_charge").setValue(price).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            SafeToast.makeText(DeliverCharges.this, "Saved", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }

                else {
                    pricePerKm.setError("Can't be Empty");
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
