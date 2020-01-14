package com.malcolmmaima.dishi.View.Activities;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.R;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.chaos.view.PinView;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    FirebaseAuth mAuth;
    EditText phoneed;
    PinView codeed;
    FloatingActionButton fabbutton;
    String mVerificationId;
    TextView timertext;
    Spinner countryCode;
    Timer timer;
    ImageView verifiedimg;
    Boolean mVerified = false;
    String phonenumber;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    String myPhone, countrycode;

    ProgressDialog progressDialog ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getSupportActionBar().setTitle("Dishi");

        // Assigning Id to ProgressDialog.
        progressDialog = new ProgressDialog(MainActivity.this);

        checkConnection();

        if(mAuth.getInstance().getCurrentUser() != null){

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference dbRef = db.getReference(myPhone);
        }

        phoneed = findViewById(R.id.numbered);
        codeed = findViewById(R.id.verificationed);
        fabbutton = findViewById(R.id.sendverifybt);
        timertext = findViewById(R.id.timertv);
        verifiedimg = findViewById(R.id.verifiedsign);

        countryCode = findViewById(R.id.countryCode);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.country, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        countryCode.setAdapter(adapter);
        countryCode.setOnItemSelectedListener(MainActivity.this);

        mAuth = FirebaseAuth.getInstance();

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                if(progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verificaiton without
                //     user action.
                Log.d("TAG", "onVerificationCompleted:" + credential);

                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w("TAG", "onVerificationFailed", e);
                fabbutton.setTag(getResources().getString(R.string.tag_send));

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    if(progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    Snackbar snackbar = Snackbar
                            .make((LinearLayout) findViewById(R.id.parentlayout), "Verification Failed !! Invalid verification Code", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }
                else if (e instanceof FirebaseTooManyRequestsException) {
                    if(progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    Snackbar snackbar = Snackbar
                            .make((LinearLayout) findViewById(R.id.parentlayout), "Verification Failed !! Too many request. Try after some time. ", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }

                else if(e instanceof FirebaseNetworkException){
                    if(progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    Snackbar snackbar = Snackbar
                            .make((LinearLayout) findViewById(R.id.parentlayout), "Failed! Network error, try again!. ", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }


            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d("TAG", "onCodeSent:" + verificationId);
                progressDialog.dismiss();
                starttimer();
                codeed.setVisibility(View.VISIBLE);

                Snackbar snackbar = Snackbar
                        .make((LinearLayout) findViewById(R.id.parentlayout), "Code Sent", Snackbar.LENGTH_LONG);

                snackbar.show();

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
            }
        };
        fabbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkConnection();
                if (fabbutton.getTag().equals(getResources().getString(R.string.tag_send))) {

                    if (!phoneed.getText().toString().trim().isEmpty() && phoneed.getText().toString().trim().length() >= 9) {
                        // Setting progressDialog Title.
                        progressDialog.setTitle("Verifying");
                        progressDialog.setMessage("Please wait...");
                        // Showing progressDialog.
                        progressDialog.show();
                        progressDialog.setCancelable(false);
                        startPhoneNumberVerification(countryCode.getSelectedItem().toString() + phoneed.getText().toString().trim());
                        mVerified = false;
                        fabbutton.setImageResource(R.drawable.ic_arrow_forward_white_24dp);
                        fabbutton.setTag(getResources().getString(R.string.tag_verify));
                        phonenumber = countryCode.getSelectedItem().toString() + phoneed.getText().toString();
                    }
                    else {
                        if(progressDialog.isShowing()){
                            progressDialog.dismiss();
                        }
                        phoneed.setError("Please enter valid mobile number");
                    }
                }

                if (fabbutton.getTag().equals(getResources().getString(R.string.tag_verify))) {
                    checkConnection();
                    if (!codeed.getText().toString().trim().isEmpty() && !mVerified) {
                        progressDialog.setMessage("Please wait...");
                        progressDialog.setTitle("Verifying");
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                        try {
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, codeed.getText().toString().trim());
                            signInWithPhoneAuthCredential(credential);
                        } catch (Exception e){ fabbutton.setTag(getResources().getString(R.string.tag_verify)); }
                    }
                    if (mVerified) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        myPhone = user.getPhoneNumber(); //Current logged in user phone number

                        FirebaseDatabase db = FirebaseDatabase.getInstance();
                        DatabaseReference dbRef = db.getReference("users/" + myPhone);

                        dbRef.child("verified").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Boolean verified = dataSnapshot.getValue(Boolean.class);

                                Toast.makeText(MainActivity.this, "Verified: " + verified, Toast.LENGTH_LONG).show();

                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                if(progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                            }
                        });
                    }

                }

            }
        });

        timertext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkConnection();
                if (!phoneed.getText().toString().trim().isEmpty() && phoneed.getText().toString().trim().length() >= 9) {
                    resendVerificationCode(countryCode.getSelectedItem().toString() + phoneed.getText().toString().trim(), mResendToken);
                    mVerified = false;
                    codeed.setVisibility(View.VISIBLE);
                    progressDialog.setTitle("Resending");
                    progressDialog.setMessage("Please wait...");
                    progressDialog.show();
                    fabbutton.setImageResource(R.drawable.ic_arrow_forward_white_24dp);
                    fabbutton.setTag(getResources().getString(R.string.tag_verify));
                    Snackbar snackbar = Snackbar
                            .make((LinearLayout) findViewById(R.id.parentlayout), "Resending verification code...", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnection();


    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressDialog.setMessage("Success...");
                            progressDialog.setCancelable(false);
                            progressDialog.show();
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            mVerified = true;
                            try {
                                timer.cancel();
                            } catch (Exception e){

                            }
                            verifiedimg.setVisibility(View.VISIBLE);
                            timertext.setVisibility(View.INVISIBLE);
                            phoneed.setEnabled(false);
                            codeed.setVisibility(View.INVISIBLE);
                            Snackbar snackbar = Snackbar
                                    .make((LinearLayout) findViewById(R.id.parentlayout), "Successfully Verified", Snackbar.LENGTH_LONG);

                            snackbar.show();

                            myPhone = user.getPhoneNumber(); //Current logged in user phone number

                            FirebaseDatabase db = FirebaseDatabase.getInstance();
                            final DatabaseReference dbRef = db.getReference("users/" + myPhone);

                            //Check whether user is verified, if true send them directly to MyAccount_(n)
                            dbRef.child("verified").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    String verified = dataSnapshot.getValue(String.class);

                                    if(verified == null) {
                                        verified = "false";

                                        dbRef.child("verified").setValue(verified).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //First time signup
                                            }
                                        })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Write failed
                                                        Toast.makeText(MainActivity.this, "error: " + e, Toast.LENGTH_SHORT).show();

                                                    }
                                                });
                                    }

                                    //Toast.makeText(MainActivity.this, "Verified: " + verified, Toast.LENGTH_LONG).show();
                                    if(verified.toString().equals("true")){
                                        //User is verified, so we need to check their account type and redirect accordingly
                                        dbRef.child("account_type").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                                                String account_type = dataSnapshot.getValue(String.class);

                                                //User has not finished setting up account
                                                if(account_type.equals("0")){
                                                    Intent slideactivity = new Intent(MainActivity.this, SetupAccountType.class)
                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    Bundle bndlanimation =
                                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
                                                    startActivity(slideactivity, bndlanimation);
                                                }

                                                if(account_type.equals("1")){ //Customer account
                                                    if(progressDialog.isShowing()){
                                                        progressDialog.dismiss();
                                                    }
                                                    //Toast.makeText(MainActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
//                                                    Intent slideactivity = new Intent(MainActivity.this, MyAccountCustomer.class)
//                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                                    Bundle bndlanimation =
//                                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
//                                                    startActivity(slideactivity, bndlanimation);
                                                }

                                                else if (account_type.equals("2")){ //Provider Restaurant account
                                                    if(progressDialog.isShowing()){
                                                        progressDialog.dismiss();
                                                    }
                                                    //Toast.makeText(MainActivity.this, "Provider Account", Toast.LENGTH_LONG).show();
//                                                    Intent slideactivity = new Intent(MainActivity.this, MyAccountRestaurant.class)
//                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                                    Bundle bndlanimation =
//                                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
//                                                    startActivity(slideactivity, bndlanimation);
                                                }

                                                else if (account_type.equals("3")){ //Nduthi account
                                                    if(progressDialog.isShowing()){
                                                        progressDialog.dismiss();
                                                    }
                                                    //Slide to new activity
                                                    //Toast.makeText(MainActivity.this, "Nduthi Account", Toast.LENGTH_LONG).show();
//                                                    Intent slideactivity = new Intent(MainActivity.this, MyAccountNduthi.class)
//                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                                    Bundle bndlanimation =
//                                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
//                                                    startActivity(slideactivity, bndlanimation);
                                                }

                                                else if (account_type.equals("X")){
                                                    Toast.makeText(MainActivity.this, "Your account has been disabled", Toast.LENGTH_LONG).show();

                                                }

                                                else { // Others
                                                    if(progressDialog.isShowing()){
                                                        progressDialog.dismiss();
                                                    }
                                                    Toast.makeText(MainActivity.this, "'Others' account still in development", Toast.LENGTH_LONG).show();
                                                }

                                                //Debugging purposes
                                                //Toast.makeText(SplashActivity.this, "Account type: " + account_type, Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                //DB error, try again...if fails login again
                                            }
                                        });
                                    } else {
                                        if(progressDialog.isShowing()){
                                            progressDialog.dismiss();
                                        }
                                        Intent slideactivity = new Intent(MainActivity.this, SetupProfile.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        Bundle bndlanimation =
                                                ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
                                        startActivity(slideactivity, bndlanimation);
                                    }
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });


                        } else {
                            // Sign in failed, display a message and update the UI
                            if(progressDialog.isShowing()){
                                progressDialog.dismiss();
                            }
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                fabbutton.setTag(getResources().getString(R.string.tag_send));
                                Snackbar snackbar = Snackbar
                                        .make((LinearLayout) findViewById(R.id.parentlayout), "Invalid Code ! Please enter correct Code", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        }
                    }
                });
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]

    }

    public void starttimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {

            int second = 60;

            @Override
            public void run() {
                if (second <= 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timertext.setText("RESEND CODE");
                            timer.cancel();
                        }
                    });

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timertext.setText("00:" + second--);
                        }
                    });
                }

            }
        }, 0, 1000);
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    public void checkConnection(){
        if(isOnline()){
            //Toast.makeText(SplashActivity.this, "You are connected to Internet", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(MainActivity.this, "You are not connected to the Internet", Toast.LENGTH_SHORT).show();
        }
    }
}
