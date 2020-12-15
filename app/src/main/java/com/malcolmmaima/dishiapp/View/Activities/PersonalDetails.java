package com.malcolmmaima.dishiapp.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.malcolmmaima.dishiapp.Controller.Utils.GenerateThumbnails;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;



public class PersonalDetails extends AppCompatActivity {

    private static final String TAG = "PersonalDetails";
    private String myPhone, email, firstname, lastname, bio, imageURL, imageURLSmall, imageURLBig;
    private EditText mEmail, mFirstName, mLastName, mBio, mPhone;
    CircleImageView profilePic;
    private Button saveDetails;
    private String [] profilePicActions = {"Open Gallery","Open Camera", "View Photo", "Remove Photo"};
    ProgressDialog progressDialog;
    UserModel myDetails;

    /**
     * Firebase
     */
    StorageReference storageReference;
    private DatabaseReference myRef;

    // Image request code for onActivityResult() .
    int Image_Request_Code = 7;

    // Folder path for Firebase Storage.
    String Storage_Path = "Users/";

    // Creating URI.
    Uri FilePathUri;
    FirebaseAuth mAuth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

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

                                    if (locked == true) {
                                        Intent slideactivity = new Intent(PersonalDetails.this, SecurityPin.class)
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
            imageURL = "";


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
                            myDetails = dataSnapshot.getValue(UserModel.class);

                            if(myDetails.getProfilePicBig() != null){
                                imageURL = myDetails.getProfilePicBig();
                            }
                            else {
                                imageURL = myDetails.getProfilePic();
                            }

                            email = myDetails.getEmail();
                            firstname = myDetails.getFirstname();
                            lastname = myDetails.getLastname();
                            bio = myDetails.getBio();

                            //Set username on drawer header
                            mEmail.setText(""+myDetails.getEmail());
                            mFirstName.setText(""+myDetails.getFirstname());
                            mLastName.setText(""+myDetails.getLastname());
                            mBio.setText(""+myDetails.getBio());
                            mPhone.setText(myPhone);

                            //set resized image
                            if(myDetails.getProfilePicSmall() != null){
                                Picasso.with(PersonalDetails.this).load(myDetails.getProfilePicSmall()).fit().centerCrop()
                                        .placeholder(R.drawable.default_profile)
                                        .error(R.drawable.default_profile)
                                        .into(profilePic);
                            }

                            else { //resized doesn't exist, set original unresized image
                                Picasso.with(PersonalDetails.this).load(myDetails.getProfilePic()).fit().centerCrop()
                                        .placeholder(R.drawable.default_profile)
                                        .error(R.drawable.default_profile)
                                        .into(profilePic);
                            }


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

                            if(which == 2){
                                try {
                                    //View current photo
                                    if (!imageURL.equals("")) {
                                        Intent slideactivity = new Intent(PersonalDetails.this, ViewImage.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                        slideactivity.putExtra("imageURL", imageURL);
                                        startActivity(slideactivity);
                                    } else {
                                        Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout), "Something went wrong", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                } catch (Exception e){
                                    Log.e(TAG, "onClick: ", e);
                                }
                            }

                            if(which == 3){
                                android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(PersonalDetails.this)
                                        .setMessage("Are you sure?")
                                        .setCancelable(false)
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                DatabaseReference defaultProfileRef = FirebaseDatabase.getInstance().getReference("defaults/profilePic");
                                                defaultProfileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        try {
                                                            String defaultProfilePic = dataSnapshot.getValue(String.class);

                                                            if (!myDetails.getProfilePic().equals(defaultProfilePic)) {
                                                                myRef.child("profilePic").setValue(defaultProfilePic).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        try {

                                                                            FirebaseStorage storage = FirebaseStorage.getInstance();
                                                                            StorageReference storageRefOriginal = storage.getReferenceFromUrl(myDetails.getProfilePic());
                                                                            StorageReference storageImgBig = storage.getReferenceFromUrl(myDetails.getProfilePicBig());
                                                                            StorageReference storageImgMedium = storage.getReferenceFromUrl(myDetails.getProfilePicMedium());
                                                                            StorageReference storageImgSmall = storage.getReferenceFromUrl(myDetails.getProfilePicSmall());


                                                                            //Delete previous images from storage
                                                                            storageRefOriginal.delete();
                                                                            storageImgBig.delete();
                                                                            storageImgMedium.delete();
                                                                            storageImgSmall.delete();

                                                                            myRef.child("profilePicBig").removeValue();
                                                                            myRef.child("profilePicMedium").removeValue();
                                                                            myRef.child("profilePicSmall").removeValue();

                                                                            try {
                                                                                Picasso.with(PersonalDetails.this).load(defaultProfilePic).fit().centerCrop()
                                                                                        .placeholder(R.drawable.default_profile)
                                                                                        .error(R.drawable.default_profile)
                                                                                        .into(profilePic);
                                                                            } catch (Exception e) {
                                                                            }

                                                                        } catch (Exception e) {

                                                                        }

                                                                        Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "Removed", Snackbar.LENGTH_LONG);
                                                                        snackbar.show();
                                                                    }
                                                                }).addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                                                        snackbar.show();
                                                                    }
                                                                });
                                                            } else {
                                                                Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "You do not have a profile picture set", Snackbar.LENGTH_LONG);
                                                                snackbar.show();
                                                            }
                                                        } catch (Exception e){}
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });
                                            }
                                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).create();
                                confirmDialog.show();
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
                    //If data hasn't changed
                    dataStatus();

                    if(saveDetails.getTag().equals("edit")){
                        mEmail.setEnabled(true);
                        mFirstName.setEnabled(true);
                        mLastName.setEnabled(true);
                        mBio.setEnabled(true);
                        profilePic.setEnabled(true);
                    }

                    if(saveDetails.getTag().equals("save")){

                        mEmail.setEnabled(false);
                        mFirstName.setEnabled(false);
                        mLastName.setEnabled(false);
                        mBio.setEnabled(false);
                        profilePic.setEnabled(false);

                        saveprofileDetails();
                    }


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
                                Intent slideactivity = new Intent(PersonalDetails.this, SecurityPin.class)
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

    public void saveprofileDetails() {
        progressDialog.setTitle("Saving...");
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
                                    //Delete previous images from storage
                                    try {
                                        DatabaseReference defaultProfileRef = FirebaseDatabase.getInstance().getReference("defaults/profilePic");
                                        defaultProfileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                try {
                                                    String defaultPic = dataSnapshot.getValue(String.class);
                                                    if (!defaultPic.equals(myDetails.getProfilePic())) {
                                                        FirebaseStorage storage = FirebaseStorage.getInstance();
                                                        StorageReference storageRefOriginal = storage.getReferenceFromUrl(myDetails.getProfilePic());
                                                        StorageReference storageImgBig = storage.getReferenceFromUrl(myDetails.getProfilePicBig());
                                                        StorageReference storageImgMedium = storage.getReferenceFromUrl(myDetails.getProfilePicMedium());
                                                        StorageReference storageImgSmall = storage.getReferenceFromUrl(myDetails.getProfilePicSmall());


                                                        //Delete images from storage
                                                        storageRefOriginal.delete();
                                                        storageImgBig.delete();
                                                        storageImgMedium.delete();
                                                        storageImgSmall.delete();
                                                    }
                                                } catch (Exception e){

                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    } catch (Exception e){

                                    }

                                    GenerateThumbnails thumbnails = new GenerateThumbnails();
                                    myRef.child("profilePicSmall").setValue(thumbnails.GenerateSmall(o.toString()));
                                    myRef.child("profilePicMedium").setValue(thumbnails.GenerateMedium(o.toString()));
                                    myRef.child("profilePicBig").setValue(thumbnails.GenerateBig(o.toString()));
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

                                    saveTextData();
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

                    })// On progress change upload time.
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {

                                // Progress Listener for loading
                                // percentage on the dialog box
                                @Override
                                public void onProgress(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    double progress
                                            = (100.0
                                            * taskSnapshot.getBytesTransferred()
                                            / taskSnapshot.getTotalByteCount());

                                    progressDialog.setMessage("Uploading ("+(int)progress+"%)");
                                }
                            });;
        }

        else {
            saveTextData();
        }
    }

    private void saveTextData() {
        //Save data to specific nodes
        myRef.child("email").setValue(mEmail.getText().toString().trim());
        myRef.child("firstname").setValue(mFirstName.getText().toString().trim());
        myRef.child("lastname").setValue(mLastName.getText().toString().trim());
        myRef.child("bio").setValue(mBio.getText().toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                saveDetails.setTag("edit");
                saveDetails.setText("EDIT");

                mEmail.setEnabled(false);
                mFirstName.setEnabled(false);
                mLastName.setEnabled(false);
                mBio.setEnabled(false);
                profilePic.setEnabled(false);

                Snackbar snackbar = Snackbar
                        .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);

                snackbar.show();

                if(progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
            }
        });
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
        //Log.d(TAG, "initWidgets: Initializing Widgets.");
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

        //profilepic
        profilePic.setEnabled(false);

        //Button
        saveDetails.setEnabled(true);
        saveDetails.setTag("edit");
        saveDetails.setText("EDIT");
        //saveDetails.setBackgroundColor(Color.GRAY);
    }

}

