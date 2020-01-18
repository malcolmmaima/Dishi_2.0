package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;

public class PersonalDetails extends AppCompatActivity {

    private static final String TAG = "PersonalDetails";
    private String myPhone, email, firstname, lastname, bio;
    private EditText mEmail, mFirstName, mLastName, mBio, mPhone;
    CircleImageView profilePic;
    private Button saveDetails;

    /**
     * Firebase
     */
    StorageReference storageReference;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;

    // Image request code for onActivityResult() .
    int Image_Request_Code = 7;

    // Folder path for Firebase Storage.
    String Storage_Path = "Users/";

    // Creating URI.
    Uri FilePathUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        intiWidgets();

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Personal Details");

        email = "";
        firstname = "";
        lastname = "";
        bio = "";

        //get auth state
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

        //User is logged in
        if(mAuth.getInstance().getCurrentUser() != null) {

            /**
             * Get logged in user details
             */
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        UserModel user = dataSnapshot.getValue(UserModel.class);

                        email = user.getEmail();
                        firstname = user.getFirstname();
                        lastname = user.getLastname();
                        bio = user.getBio();

                        //Set username on drawer header
                        mEmail.setText(""+user.getEmail());
                        mFirstName.setText(""+user.getFirstname());
                        mLastName.setText(""+user.getLastname());
                        mBio.setText(""+user.getBio());
                        mPhone.setText(myPhone);

                        Picasso.with(PersonalDetails.this).load(user.getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profilePic);

                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: " + e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

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

        /**
         * Realtime validation check
         */
        mEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //If data hasn't changed
                dataStatus();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //If data hasn't changed
                dataStatus();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //If data hasn't changed
                dataStatus();
            }
        });

        mFirstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //If data hasn't changed
                dataStatus();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //If data hasn't changed
                dataStatus();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //If data hasn't changed
                dataStatus();
            }
        });

        mLastName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //If data hasn't changed
                dataStatus();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //If data hasn't changed
                dataStatus();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //If data hasn't changed
                dataStatus();
            }
        });

        mBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //If data hasn't changed
                dataStatus();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //If data hasn't changed
                dataStatus();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //If data hasn't changed
                dataStatus();
            }
        });

        /**
         * Save details
         */
        saveDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //If data hasn't changed
                dataStatus();

                if(saveDetails.getTag().equals("edit")){
                    mEmail.setEnabled(true);
                    mFirstName.setEnabled(true);
                    mLastName.setEnabled(true);
                    mBio.setEnabled(true);
                }

                if(saveDetails.getTag().equals("save")){
                    Toast.makeText(PersonalDetails.this, "send data to DB", Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(PersonalDetails.this, "Tag: " + saveDetails.getTag().toString(), Toast.LENGTH_SHORT).show();

//                Toast.makeText(PersonalDetails.this, "Email: " + mEmail.getText().toString()
//                                                                + " FirstName: \n" +mFirstName.getText().toString()
//                                                                + " LastName: \n" + mLastName.getText().toString()
//                                                                + " Bio: \n" + mBio.getText().toString()
//                                                                + " Phone: \n" + mPhone.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dataStatus() {
        //If data hasn't changed
        if(mEmail.getText().toString().equals(email)
                && mFirstName.getText().toString().equals(firstname)
                && mLastName.getText().toString().equals(lastname)
                && mBio.getText().toString().equals(bio)){

            saveDetails.setTag("edit");
            saveDetails.setText("EDIT");
        }

        if(!mBio.getText().toString().equals(bio)){
            saveDetails.setTag("save");
            saveDetails.setText("SAVE");
        }

        if(!mLastName.getText().toString().equals(lastname)){
            saveDetails.setTag("save");
            saveDetails.setText("SAVE");
        }

        if(!mFirstName.getText().toString().equals(firstname)){
            saveDetails.setTag("save");
            saveDetails.setText("SAVE");
        }

        if(!mEmail.getText().toString().equals(email)){
            saveDetails.setTag("save");
            saveDetails.setText("SAVE");
        }

    }

    private void intiWidgets() {
        Log.d(TAG, "initWidgets: Initializing Widgets.");
        mEmail = findViewById(R.id.input_email);
        mFirstName = findViewById(R.id.input_first_name);
        mLastName = findViewById(R.id.input_surname);
        profilePic = findViewById(R.id.profilePic);
        mBio = findViewById(R.id.input_bio);
        mPhone = findViewById(R.id.numbered);
        saveDetails = findViewById(R.id.saveDetails);

        //Text fields
        mEmail.setEnabled(false);
        mFirstName.setEnabled(false);
        mLastName.setEnabled(false);
        mBio.setEnabled(false);

        //Phone Number
        mPhone.setFocusable(false);
        mPhone.setEnabled(false);

        //Button
        saveDetails.setEnabled(true);
        saveDetails.setTag("edit");
        saveDetails.setText("EDIT");
        //saveDetails.setBackgroundColor(Color.GRAY);
    }

}

