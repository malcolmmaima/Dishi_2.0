package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.malcolmmaima.dishi.R;

public class SetupAccountType extends AppCompatActivity {

    private String TAG;
    CardView customer, restaurant, rider;
    String accountType, myPhone;

    /**
     * Firebase
     */
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_account_type);

        TAG = "SetupAccountType";
        accountType = "0";

        initWidgets();

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users");

        //get auth state
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //User is logged in
        if(mAuth.getInstance().getCurrentUser() != null) {
            /**
             * Customer account
             */
            customer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Toast.makeText(SetupAccountType.this, "Customer account", Toast.LENGTH_SHORT).show();
                    accountType = "1";

                    //Set account type in database under logged in user's node
                    myRef.child(myPhone).child("account_type").setValue(accountType).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Load customer account
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //error
                        }
                    });
                }
            });


            /**
             * Restaurant account
             */
            restaurant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(SetupAccountType.this, "Restaurant account", Toast.LENGTH_SHORT).show();
                    accountType = "2";

                    //Set account type in database under logged in user's node
                    myRef.child(myPhone).child("account_type").setValue(accountType).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Load customer account
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //error
                        }
                    });
                }
            });

            /**
             * Rider Account
             */
            rider.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(SetupAccountType.this, "Rider account", Toast.LENGTH_SHORT).show();
                    accountType = "3";

                    //Set account type in database under logged in user's node
                    myRef.child(myPhone).child("account_type").setValue(accountType).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Load customer account
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //error
                        }
                    });
                }
            });
        }

        //You're not logged in
        else {
            Toast.makeText(this, "You're not logged in", Toast.LENGTH_LONG).show();
            //Slide to new activity
            Intent slideactivity = new Intent(SetupAccountType.this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Bundle bndlanimation =
                    null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                bndlanimation = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    startActivity(slideactivity, bndlanimation);
                } catch (Exception e){

                }
            }
        }


    }

    /**
     * Initialize the activity widgets
     */
    private void initWidgets(){
        Log.d(TAG, "initWidgets: Initializing Widgets.");
        customer = findViewById(R.id.customerAccountCard);
        restaurant = findViewById(R.id.restaurantAccountCard);
        rider = findViewById(R.id.riderCard);

    }
}
