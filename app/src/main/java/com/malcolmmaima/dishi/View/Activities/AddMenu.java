package com.malcolmmaima.dishi.View.Activities;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.fabric.sdk.android.services.common.SafeToast;


public class AddMenu extends AppCompatActivity {

    TextView productName,productPrice,productDescription;
    //CircleImageView foodPic;
    private ImageView foodPic;
    Button save;
    String myPhone;
    private String [] foodPicActions = {"Open Gallery","Open Camera", "View Photo"};

    private String [] foodImageOptions = {"Upload Food Photo","Use Default Photo"};


    // Creating URI.
    Uri FilePathUri;

    // Folder path for Firebase Storage.
    String Storage_Path = "Users";

    String key, phone;

    // Creating StorageReference and DatabaseReference object.
    StorageReference storageReference;
    DatabaseReference databaseReference, defaultsRef;

    ProgressDialog progressDialog ;
    StorageReference storageReference2nd;


    // Image request code for onActivityResult() .
    int Image_Request_Code = 7;
    private String name, description, imageLink, imageLocation, price, url;
    private String defaultFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_menu);

        defaultFood = "";
        // Assigning Id to ProgressDialog.
        progressDialog = new ProgressDialog(AddMenu.this);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Assign FirebaseStorage instance to storageReference.
        storageReference = FirebaseStorage.getInstance().getReference();

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });
        //topToolBar.setLogo(R.drawable.logo);
        //topToolBar.setLogoDescription(getResources().getString(R.string.logo_desc));

        phone = getIntent().getStringExtra("phone"); //From adapters
        key = getIntent().getStringExtra("key"); //From adapters, to allow for editing

        FirebaseUser user;
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        databaseReference = FirebaseDatabase.getInstance().getReference("menus/"+myPhone);
        defaultsRef = FirebaseDatabase.getInstance().getReference("defaults");

        foodPic = findViewById(R.id.foodpic);
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        productDescription = findViewById(R.id.productDescription);
        save = findViewById(R.id.save);

        //Fetch default food pic
        defaultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot defaults : dataSnapshot.getChildren()) {

                    try {
                        if (defaults.getKey().equals("foodPic")) {
                            defaultFood = defaults.getValue(String.class);

                        }
                    } catch (Exception e){

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //adding new item
        if(key == null){
            setTitle("Add New Item");

            foodPic.setOnClickListener(new View.OnClickListener() {
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

            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Checking whether FilePathUri Is not empty and passes validation check.
                    if (FilePathUri != null && CheckFieldValidation()) {
                        // Setting progressDialog Title.
                        progressDialog.setMessage("Adding...");

                        // Showing progressDialog.
                        progressDialog.show();
                        progressDialog.setCancelable(false);
                        uploadMenu();

                    }

                    // Checking whether FilePathUri Is empty and passes validation check.
                    else if(FilePathUri == null && CheckFieldValidation() && key != null){

                        progressDialog.setMessage("Saving...");
                        // Showing progressDialog.
                        progressDialog.show();
                        progressDialog.setCancelable(false);
                        uploadMenu();

                    }

                    //Handle other use cases
                    else {
                        if(FilePathUri != null){
                            if(CheckFieldValidation()){
                                progressDialog.setMessage("Saving...");
                                // Showing progressDialog.
                                progressDialog.show();
                                progressDialog.setCancelable(false);
                                uploadMenu();
                            }
                        }

                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(AddMenu.this);
                            builder.setItems(foodImageOptions, new DialogInterface.OnClickListener() {
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
                                        FilePathUri = null;

                                        if(CheckFieldValidation()){
                                            progressDialog.setMessage("Saving...");
                                            // Showing progressDialog.
                                            progressDialog.show();
                                            progressDialog.setCancelable(false);
                                            uploadMenu();
                                        }
                                    }
                                }
                            });
                            builder.setCancelable(false);
                            builder.create();
                            builder.show();
                        }

                    }
                }
            });
        } else { //Editing existing item (key != null)

            setTitle("Edit Item");
            //SafeToast.makeText(this, "key: " + key, Toast.LENGTH_SHORT).show();

            //Fetch item details from DB
            databaseReference.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    for (DataSnapshot menuDetails : dataSnapshot.getChildren()){
                        try {

                            if(menuDetails.getKey().equals("description")){
                                description = menuDetails.getValue(String.class);
                                productDescription.setText(description);
                            }
                            if(menuDetails.getKey().equals("imageURL")){
                                imageLink = menuDetails.getValue(String.class);

                                Picasso.with(AddMenu.this).load(imageLink).fit().centerCrop()
                                            .placeholder(R.drawable.menu)
                                            .error(R.drawable.menu)
                                            .into(foodPic);

                            }
                            if(menuDetails.getKey().equals("name")){
                                name = menuDetails.getValue(String.class);
                                productName.setText(name);
                            }
                            if(menuDetails.getKey().equals("price")){
                                price = menuDetails.getValue(String.class);
                                productPrice.setText(price);
                            }
                            if(menuDetails.getKey().equals("storageLocation")){
                                imageLocation = menuDetails.getValue(String.class);
                            }


                        } catch (Exception e){

                        }
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            /**
             * On image click, allow user to change or view current image
             */
            foodPic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddMenu.this);
                    builder.setItems(foodPicActions, new DialogInterface.OnClickListener() {
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
                                //View current photo
                                if(!imageLink.equals("")){
                                    Intent slideactivity = new Intent(AddMenu.this, ViewImage.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                    slideactivity.putExtra("imageURL", imageLink);
                                    startActivity(slideactivity);
                                }

                                else {
                                    Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout), "Something went wrong", Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                }
                            }
                        }
                    });
                    builder.create();
                    builder.show();
                }
            });

            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Checking whether FilePathUri Is not empty and passes validation check.
                    if (FilePathUri != null && CheckFieldValidation()) {
                        // Setting progressDialog Title.
                        progressDialog.setMessage("Adding...");

                        // Showing progressDialog.
                        progressDialog.show();
                        progressDialog.setCancelable(false);
                        uploadMenu();

                    }

                    // Checking whether FilePathUri Is empty and passes validation check.
                    else if(FilePathUri == null && CheckFieldValidation() && key != null){

                        progressDialog.setMessage("Saving...");
                        // Showing progressDialog.
                        progressDialog.show();
                        progressDialog.setCancelable(false);
                        uploadMenu();

                    }

                    //Handle other use cases
                    else {
                        if(FilePathUri != null){
                            if(CheckFieldValidation()){
                                progressDialog.setMessage("Saving...");
                                // Showing progressDialog.
                                progressDialog.show();
                                progressDialog.setCancelable(false);
                                uploadMenu();
                            }
                        }

                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(AddMenu.this);
                            builder.setItems(foodPicActions, new DialogInterface.OnClickListener() {
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
                                        //View current photo
                                        if(!imageLink.equals("")){
                                            Intent slideactivity = new Intent(AddMenu.this, ViewImage.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                            slideactivity.putExtra("imageURL", imageLink);
                                            startActivity(slideactivity);
                                        }

                                        else {
                                            Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout), "Something went wrong", Snackbar.LENGTH_LONG);
                                            snackbar.show();
                                        }
                                    }
                                }
                            });
                            builder.create();
                            builder.show();
                        }

                    }
                }
            });

        }

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
                foodPic.setImageBitmap(bitmap);

                // After selecting image change choose button above text.
                //ChooseButton.setText("Image Selected");

            }
            catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    // Creating Method to get the selected image file Extension from File Path URI.
    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }

    //checking if field are empty
    private boolean CheckFieldValidation() {

        boolean valid = true;

        if (productName.getText().toString().equals("")) {
            productName.setError("Can't be Empty");
            valid = false;
        }

        if (productPrice.getText().toString().equals("")) {
            productPrice.setError("Can't be Empty");
            valid = false;
        }

        if (productDescription.getText().toString().equals("")) {
            productDescription.setError("Can't be Empty");
            valid = false;
        }

        return valid;
    }

    public void uploadMenu(){

        final DatabaseReference menusRef;
        FirebaseDatabase db;

        db = FirebaseDatabase.getInstance();
        menusRef = db.getReference("menus/"+ myPhone); //Post to Menus node, phone number is reference ID

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        if(FilePathUri != null){
            // Creating second StorageReference.
            storageReference2nd = storageReference.child(Storage_Path + "/" + myPhone + "/" + System.currentTimeMillis() + "." + GetFileExtension(FilePathUri));

        }

        if(key != null){

            //has not changed image
            if(FilePathUri == null){

                String name_ = productName.getText().toString().trim();
                String price_ = productPrice.getText().toString().trim();
                String description_ = productDescription.getText().toString().trim();

                ProductDetails productDetails = new ProductDetails();
                productDetails.setName(name_);
                productDetails.setPrice(price_);
                productDetails.setDescription(description_);
                productDetails.setImageURL(imageLink);
                productDetails.setOwner(myPhone);
                productDetails.setStorageLocation(imageLocation);
                productDetails.setUploadDate(getDate());

                Log.d("myimage", "onSuccess: product image: " + productDetails.getImageURL());

                menusRef.child(key).setValue(productDetails).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Write was successful!
                        // Hiding the progressDialog after done uploading.
                        progressDialog.dismiss();

                        Snackbar snackbar = Snackbar
                                .make(findViewById(R.id.parentlayout), "Saved!", Snackbar.LENGTH_LONG);

                        snackbar.show();

                    }
                })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Write failed
                                progressDialog.dismiss();
                                Snackbar snackbar = Snackbar
                                        .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);

                                snackbar.show();
                                //SafeToast.makeText(AddMenu.this, "Failed: " + e.toString() + ". Try again!", Toast.LENGTH_LONG).show();
                            }
                        });
            }

            else { //has changed image
                storageReference2nd.putFile(FilePathUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                //Get image URL: //Here we get the image url from the firebase storage
                                storageReference2nd.getDownloadUrl().addOnSuccessListener(new OnSuccessListener() {

                                    @Override
                                    public void onSuccess(Object o) {

                                        ProductDetails productDetails = new ProductDetails();

                                        productDetails.setName(name);
                                        productDetails.setPrice(price);
                                        productDetails.setDescription(description);
                                        productDetails.setImageURL(o.toString());
                                        productDetails.setOwner(myPhone);
                                        productDetails.setStorageLocation(storageReference2nd.getPath());
                                        productDetails.setUploadDate(getDate());

                                        Log.d("myimage", "onSuccess: product image: " + productDetails.getImageURL());

                                        menusRef.child(key).setValue(productDetails).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Write was successful!
                                                // Hiding the progressDialog after done uploading.
                                                progressDialog.dismiss();

                                                Snackbar snackbar = Snackbar
                                                        .make((RelativeLayout) findViewById(R.id.parentlayout), "Saved!", Snackbar.LENGTH_LONG);

                                                snackbar.show();

                                            }
                                        })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Write failed
                                                        progressDialog.dismiss();

                                                        Snackbar snackbar = Snackbar
                                                                .make((RelativeLayout) findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);

                                                        snackbar.show();
                                                        //SafeToast.makeText(AddMenu.this, "Failed: " + e.toString() + ". Try again!", Toast.LENGTH_LONG).show();
                                                    }
                                                });


                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle any errors
                                        progressDialog.dismiss();
                                        SafeToast.makeText(AddMenu.this, "Error: " + exception, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        })
                        // If something goes wrong .
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {

                                // Hiding the progressDialog.
                                progressDialog.dismiss();

                                // Showing exception erro message.
                                //SafeToast.makeText(AddMenu.this, exception.getMessage(), Toast.LENGTH_LONG).show();

                                Snackbar snackbar = Snackbar
                                        .make((RelativeLayout) findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        })

                        // On progress change upload time.
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                                // will implement progress bar later on

                            }
                        });
            }



        }
        else { //New menu addition

            //Check if user has set food photo, if not use default
            if(FilePathUri == null){
                String name = productName.getText().toString().trim();
                String price = productPrice.getText().toString().trim();
                String description = productDescription.getText().toString().trim();

                String key = menusRef.push().getKey(); //The child node in mymenu for storing menu items
                ProductDetails productDetails = new ProductDetails();

                productDetails.setName(name);
                productDetails.setPrice(price);
                productDetails.setDescription(description);
                productDetails.setImageURL(defaultFood);
                productDetails.setOwner(myPhone);
                productDetails.setStorageLocation("default");
                productDetails.setUploadDate(getDate());

                Log.d("myimage", "onSuccess: product image: " + productDetails.getImageURL());

                menusRef.child(key).setValue(productDetails).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Write was successful!
                        // Hiding the progressDialog after done uploading.
                        progressDialog.dismiss();

                        FilePathUri = null;
                        Snackbar snackbar = Snackbar
                                .make((RelativeLayout) findViewById(R.id.parentlayout), "Added successfully!", Snackbar.LENGTH_LONG);

                        snackbar.show();

                    }
                })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Write failed
                                progressDialog.dismiss();
                                Snackbar snackbar = Snackbar
                                        .make((RelativeLayout) findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);

                                snackbar.show();
                                //SafeToast.makeText(AddMenu.this, "Failed: " + e.toString() + ". Try again!", Toast.LENGTH_LONG).show();
                            }
                        });

                productName.setText("");
                productPrice.setText("");
                productDescription.setText("");
                foodPic.setImageDrawable(getResources().getDrawable(R.drawable.menu));
            }

            //Upload the chosen photo
            else {

                // Adding addOnSuccessListener to second StorageReference.
                storageReference2nd.putFile(FilePathUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                //Get image URL: //Here we get the image url from the firebase storage
                                storageReference2nd.getDownloadUrl().addOnSuccessListener(new OnSuccessListener() {

                                    @Override
                                    public void onSuccess(Object o) {

                                        String name = productName.getText().toString().trim();
                                        String price = productPrice.getText().toString().trim();
                                        String description = productDescription.getText().toString().trim();

                                        String key = menusRef.push().getKey(); //The child node in mymenu for storing menu items
                                        ProductDetails productDetails = new ProductDetails();

                                        productDetails.setName(name);
                                        productDetails.setPrice(price);
                                        productDetails.setDescription(description);
                                        productDetails.setImageURL(o.toString());
                                        productDetails.setStorageLocation(storageReference2nd.getPath());
                                        productDetails.setOwner(myPhone);
                                        productDetails.setUploadDate(getDate());

                                        Log.d("myimage", "onSuccess: product image: " + productDetails.getImageURL());

                                        menusRef.child(key).setValue(productDetails).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Write was successful!
                                                // Hiding the progressDialog after done uploading.
                                                progressDialog.dismiss();
                                                FilePathUri = null;
                                                Snackbar snackbar = Snackbar
                                                        .make((RelativeLayout) findViewById(R.id.parentlayout), "Added successfully!", Snackbar.LENGTH_LONG);

                                                snackbar.show();

                                            }
                                        })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Write failed
                                                        progressDialog.dismiss();
                                                        Snackbar snackbar = Snackbar
                                                                .make((RelativeLayout) findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);

                                                        snackbar.show();
                                                        //SafeToast.makeText(AddMenu.this, "Failed: " + e.toString() + ". Try again!", Toast.LENGTH_LONG).show();
                                                    }
                                                });

                                        productName.setText("");
                                        productPrice.setText("");
                                        productDescription.setText("");
                                        foodPic.setImageDrawable(getResources().getDrawable(R.drawable.menu));
                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle any errors
                                        progressDialog.dismiss();
                                        Snackbar snackbar = Snackbar
                                                .make((RelativeLayout) findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);

                                        snackbar.show();
                                        //SafeToast.makeText(AddMenu.this, "Error: " + exception, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        })
                        // If something goes wrong .
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {

                                // Hiding the progressDialog.
                                progressDialog.dismiss();

                                // Showing exception erro message.
                                SafeToast.makeText(AddMenu.this, exception.getMessage(), Toast.LENGTH_LONG).show();

                                Snackbar snackbar = Snackbar
                                        .make((RelativeLayout) findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);

                                snackbar.show();
                            }
                        })

                        // On progress change upload time.
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                                // will implement progress bar later on

                            }
                        });
            }

        }

    }


    private String getDate() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        TimeZone timeZone = TimeZone.getDefault();
        Calendar calendar = Calendar.getInstance(timeZone);
        String time = date+ ":" +
                String.format("%02d" , calendar.get(Calendar.HOUR_OF_DAY))+":"+
                String.format("%02d" , calendar.get(Calendar.MINUTE))+":"+
                String.format("%02d" , calendar.get(Calendar.SECOND)) +":"+
                timeZone.getDisplayName(false, TimeZone.SHORT);

        return time;
    }
}
