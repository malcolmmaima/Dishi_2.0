package com.malcolmmaima.dishi.View;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;

public class SetupProfile extends AppCompatActivity {

    String TAG = "SetupAccount";

    private Context mContext;
    private String email, firstname, lastname, bio, gender, myPhone;
    private EditText mEmail, mFirstName, mLastName, mBio;
    private Button btnRegister;
    private ProgressBar mProgressBar;
    RadioButton maleRd, femaleRd;
    RadioGroup mGender;
    private ImageView profile_pic;

    /**
     * Firebase
     */
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        initWidgets();

        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference("users");

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = mEmail.getText().toString();
                firstname = mFirstName.getText().toString();
                lastname = mLastName.getText().toString();
                bio = mBio.getText().toString();

                int userGender = mGender.getCheckedRadioButtonId();
                // find the radio button by returned id
                RadioButton radioButton = findViewById(userGender);

                try {
                    gender = radioButton.getText().toString();
                } catch(Exception e){
                    gender = "";
                }

                /* If inputs passes the validation then post details to firebase */
                if(checkInputs(email, firstname, lastname, bio, gender)){
                    mProgressBar.setVisibility(View.VISIBLE);

                    /**
                     * Store user details in UserModel object before posting to Firebase DB
                     */
                    UserModel userModel = new UserModel();
                    userModel.setEmail(email);
                    userModel.setFirstname(firstname);
                    userModel.setLastname(lastname);
                    userModel.setVerified("true");
                    userModel.setBio(bio);
                    userModel.setGender(gender);

                    /**
                     * Check if user is logged in the fetch phone number
                     */
                    if(mAuth.getInstance().getCurrentUser() != null) {

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        myPhone = user.getPhoneNumber(); //Current logged in user phone number

                        myRef.child(myPhone).setValue(userModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                //Set account type to 0 if setting up no complete
                                myRef.child(myPhone).child("account_type").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        mProgressBar.setVisibility(View.INVISIBLE);
                                        //Toast.makeText(mContext, "Details saved!", Toast.LENGTH_LONG).show();
                                        //Slide to new activity
                                        Intent slideactivity = new Intent(SetupProfile.this, SetupAccountType.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        Bundle bndlanimation =
                                                null;
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                            bndlanimation = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                            startActivity(slideactivity, bndlanimation);
                                        }
                                    }
                                });

                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * Initialize the activity widgets
     */
    private void initWidgets(){
        Log.d(TAG, "initWidgets: Initializing Widgets.");
        mEmail = findViewById(R.id.input_email);
        mFirstName = findViewById(R.id.input_first_name);
        mLastName = findViewById(R.id.input_surname);
        btnRegister = findViewById(R.id.btn_register);
        mProgressBar = findViewById(R.id.progressBar);
        profile_pic = findViewById(R.id.profilePic);
        maleRd = findViewById(R.id.maleRd);
        femaleRd = findViewById(R.id.femaleRd);
        mGender = findViewById(R.id.gender);
        mBio = findViewById(R.id.input_bio);
        mContext = SetupProfile.this;
        mProgressBar.setVisibility(View.GONE);

    }

    /**
     * Form validation
     */
    private boolean checkInputs(String email, String firstname, String lastname, String bio, String gender){
        Log.d(TAG, "checkInputs: checking inputs for null values.");
        if(email.isEmpty() || firstname.isEmpty() || lastname.isEmpty() || bio.isEmpty() || gender.isEmpty()){
            Toast.makeText(mContext, "All fields must be filled out.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
