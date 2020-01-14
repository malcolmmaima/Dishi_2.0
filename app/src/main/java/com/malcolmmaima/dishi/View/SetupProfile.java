package com.malcolmmaima.dishi.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;

import java.io.IOException;

public class SetupProfile extends AppCompatActivity {

    String TAG = "SetupAccount";

    private Context mContext;
    private String email, firstname, lastname, bio, gender, myPhone, defaultProfile;
    private EditText mEmail, mFirstName, mLastName, mBio;
    private Button btnRegister;
    private ProgressBar mProgressBar;
    RadioButton maleRd, femaleRd;
    RadioGroup mGender;
    private ImageView profile_pic;

    /**
     * Firebase
     */
    // Creating StorageReference and DatabaseReference object.
    StorageReference storageReference;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;

    // Image request code for onActivityResult() .
    int Image_Request_Code = 7;

    // Folder path for Firebase Storage.
    String Storage_Path = "Users/";

    // Creating URI.
    Uri FilePathUri;

    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        initWidgets();

        // Assigning Id to ProgressDialog.
        progressDialog = new ProgressDialog(SetupProfile.this);
        defaultProfile = "https://firebasestorage.googleapis.com/v0/b/dishi-food.appspot.com/o/defaults%2Fdefault_profile.png?alt=media&token=af43783d-4432-4218-8dea-9f66b66c8f9e";

        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference("users");
        // Assign FirebaseStorage instance to storageReference.
        storageReference = FirebaseStorage.getInstance().getReference();

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

                /**
                 *  If inputs passes the validation then post details to firebase
                 */
                if(CheckFieldValidation()){
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
                     * Check if user is logged in then fetch phone number
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
                                        UploadImage();
                                        //Check if profile picture has been set
                                        myRef.child(myPhone).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                //UploadImage();
                                                for(DataSnapshot data : dataSnapshot.getChildren()){
                                                    if(data.getKey().equals("profilePic")){
                                                        //Slide to new activity
                                                        Intent slideactivity = new Intent(SetupProfile.this, SetupAccountType.class)
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
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                    }
                                });

                            }
                        });
                    }
                }
            }
        });

        profile_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Creating intent.
                Intent intent = new Intent();

                // Setting intent type as image to select image from phone storage.
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Please Select Image"), Image_Request_Code);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Image_Request_Code && resultCode == RESULT_OK && data != null && data.getData() != null) {

            FilePathUri = data.getData();

            try {

                // Getting selected image into Bitmap.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), FilePathUri);

                // Setting up bitmap selected image into ImageView.
                profile_pic.setImageBitmap(bitmap);

            }
            catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    /**
     * Creating Method to get the selected image file Extension from File Path URI.
      */

    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }

    /**
     * Creating UploadImageFileToFirebaseStorage method to upload image on storage.
     */
    public void UploadImage() {

        // Checking whether FilePathUri Is empty or not and text fields are empty
        if (FilePathUri != null) {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            try {
                // Creating second StorageReference.
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
                                        myRef.child(myPhone).child("profilePic").setValue(o.toString());
                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle any errors
                                    }
                                });

                            }
                        })
                        // If something goes wrong .
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {

                                // Showing exception erro message.
                                Toast.makeText(mContext, exception.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        })

                        // On progress change upload time.
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                try {
                                    progressDialog.setTitle("Uploading...");
                                    progressDialog.setCancelable(false);
                                    progressDialog.show();
                                } catch (Exception e){

                                }

                            }
                        });
            } catch (Exception e){

            }
        }

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

    private boolean CheckFieldValidation(){
        Log.d(TAG, "checkInputs: checking inputs for null values.");
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        final boolean[] valid = {true};

        if (FilePathUri == null) {
            final AlertDialog profile = new AlertDialog.Builder(mContext)
                    //set message, title, and icon
                    .setMessage("Use default profile picture?")
                    //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                    //set three option buttons
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            valid[0] = true;
                            //Set default profile picture in database
                            try {
                                myRef.child(myPhone).child("profilePic").setValue(defaultProfile);
                            } catch (Exception e){

                            }
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Toast.makeText(mContext, "Upload a profile picture!", Toast.LENGTH_SHORT).show();
                            valid[0] = false;

                        }
                    })//setNegativeButton
                    .setCancelable(false)
                    .create();
            profile.show();
        }


        if(mFirstName.getText().toString().equals("")){
            mFirstName.setError("Can't be Empty");
            valid[0] =false;
        }

        if(mEmail.getText().toString().equals("")){
            mEmail.setError("Cant't be empty");
            valid[0] =false;
        }

        if(mEmail.equals(emailPattern)){
            mEmail.setError("Invalid Email");
            valid[0] =false;
        }

        if (mGender.getCheckedRadioButtonId() == -1){

            Toast.makeText(SetupProfile.this, "You must select gender", Toast.LENGTH_SHORT).show();
            valid[0] =false;
        }

        if(mLastName.getText().toString().equals("")){
            mLastName.setError("Can't be Empty");
            valid[0] =false;
        }


        return valid[0];
    }


}
