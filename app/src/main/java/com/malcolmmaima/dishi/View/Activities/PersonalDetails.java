package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.icu.text.LocaleDisplayNames;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

public class PersonalDetails extends AppCompatActivity {

    private static final String TAG = "PersonalDetails";
    private String myPhone, email, firstname, lastname, bio;
    private EditText mEmail, mFirstName, mLastName, mBio, mPhone;
    CircleImageView profilePic;
    private Button saveDetails;
    private String [] profilePicActions = {"Open Gallery","Open Camera"};
    ProgressDialog progressDialog;

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

        progressDialog = new ProgressDialog(PersonalDetails.this);

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Initialize strings
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

        // Assign FirebaseStorage instance to storageReference.
        storageReference = FirebaseStorage.getInstance().getReference();

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

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

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

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

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

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

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

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //If data hasn't changed
                dataStatus();
            }
        });

        /**
         * Profile picture
         */

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PersonalDetails.this);
                builder.setItems(profilePicActions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            // Open Gallery.
                            Intent intent = new Intent();
                            // Setting intent type as image to select image from phone storage.
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent, "Please Select Image"), Image_Request_Code);
                        }
                        if(which == 1){
                            //Open Camera
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "In development", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }
                });
                builder.create();
                builder.show();
            }
        });

        /**
         * Save details
         */
        saveDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(saveDetails.getTag().equals("edit")){
                    mEmail.setEnabled(true);
                    mFirstName.setEnabled(true);
                    mLastName.setEnabled(true);
                    mBio.setEnabled(true);
                }

                if(saveDetails.getTag().equals("save")){

                    mEmail.setEnabled(false);
                    mFirstName.setEnabled(false);
                    mLastName.setEnabled(false);
                    mBio.setEnabled(false);

                    progressDialog.setMessage("Saving...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    saveprofilePhoto();

                    //Save data to specific nodes
                    myRef.child("email").setValue(mEmail.getText().toString());
                    myRef.child("firstname").setValue(mFirstName.getText().toString());
                    myRef.child("lastname").setValue(mLastName.getText().toString());
                    myRef.child("bio").setValue(mBio.getText().toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            saveDetails.setTag("edit");
                            saveDetails.setText("EDIT");

                            mEmail.setEnabled(false);
                            mFirstName.setEnabled(false);
                            mLastName.setEnabled(false);
                            mBio.setEnabled(false);

                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);

                            snackbar.show();

                            if(progressDialog.isShowing()){
                                progressDialog.dismiss();
                            }
                        }
                    });
                }
                //If data hasn't changed
                dataStatus();



            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Image_Request_Code && resultCode == RESULT_OK && data != null && data.getData() != null) {

            FilePathUri = data.getData();
            dataStatus();

            try {

                // Getting selected image into Bitmap.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), FilePathUri);

                // Setting up bitmap selected image into ImageView.
                profilePic.setImageBitmap(bitmap);

            }
            catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    public void saveprofilePhoto() {
        progressDialog.setTitle("Uploading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        //Check if user has selected new profile pic
        if (FilePathUri != null) {

            //user has selected new pic

            final StorageReference storageReference2nd = storageReference.child(Storage_Path + "/" + myPhone + "/" + System.currentTimeMillis() + "." + GetFileExtension(FilePathUri));

            // Adding addOnSuccessListener to second StorageReference.
            storageReference2nd.putFile(FilePathUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Get image URL: //Here we get the image url from the firebase storage
                            storageReference2nd.getDownloadUrl().addOnSuccessListener(new OnSuccessListener() {

                                @Override
                                public void onSuccess(Object o) {
                                    myRef.child("profilePic").setValue(o.toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            saveDetails.setTag("edit");
                                            saveDetails.setText("EDIT");
                                            if (progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                            }
                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    saveDetails.setTag("save");
                                    saveDetails.setText("SAVE");
                                    if (progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }

                                    Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                    snackbar.show();

                                }
                            });
                        }
                    });
        }
    }

    // Creating Method to get the selected image file Extension from File Path URI.
    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }

    private void dataStatus() {


        //If data hasn't changed
        if(mEmail.getText().toString().equals(email)
                && mFirstName.getText().toString().equals(firstname)
                && mLastName.getText().toString().equals(lastname)
                && mBio.getText().toString().equals(bio) && FilePathUri == null){

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

        //Check if new profile pic set
        if(FilePathUri != null){
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

