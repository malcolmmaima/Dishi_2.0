package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.ReceiptModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ReceiptItemAdapter;
import com.malcolmmaima.dishi.View.Maps.GeoTracking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import io.fabric.sdk.android.services.common.SafeToast;

public class ReceiptActivity extends AppCompatActivity {

    String TAG = "ReceiptActivity";
    DatabaseReference receiptItemsRef, receiptObjRef, myRef;
    String key, myPhone, orderid, orderedOn, deliveredOn,
            restaurantName, restaurantPhone, customerPhone;
    Boolean downloadRequest;
    FirebaseUser user;
    FirebaseAuth mAuth;
    private ArrayList<ProductDetailsModel> deliveredItems;
    private RecyclerView recyclerView;
    private ReceiptItemAdapter mAdapter;
    ImageView exitReceipt, receiptOptions;
    MyTextView_Roboto_Regular totalTitle, orderID, deliveryChargeAmount, subTotal;
    MyTextView_Roboto_Medium totalBill, dateOrdered, dateDelivered, vendorName, vendorPhone, nameTitle;
    int totalAmount;
    Integer deliveryCharge;
    ProgressBar progressBar;
    RelativeLayout myReceipt;
    private Bitmap bitmap;
    ProgressDialog progressDialog;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
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
                                        Intent slideactivity = new Intent(ReceiptActivity.this, SecurityPin.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        slideactivity.putExtra("pinType", "resume");
                                        startActivity(slideactivity);
                                    }
                                } catch (Exception e){
                                    Log.e(TAG, "onDataChange: ", e);
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

            loadReceipt();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
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
                                        Intent slideactivity = new Intent(ReceiptActivity.this, SecurityPin.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        slideactivity.putExtra("pinType", "resume");
                                        startActivity(slideactivity);
                                    }
                                } catch (Exception e){
                                    Log.e(TAG, "onDataChange: ", e);
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

            loadReceipt();
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
                                Intent slideactivity = new Intent(ReceiptActivity.this, SecurityPin.class)
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

    private void loadReceipt() {
        setContentView(R.layout.activity_receipt);

        progressDialog = new ProgressDialog(ReceiptActivity.this);
        myReceipt = findViewById(R.id.myReceipt);
        exitReceipt = findViewById(R.id.exitReceipt);
        recyclerView = findViewById(R.id.recyclerview);
        totalBill = findViewById(R.id.totalBill);
        totalTitle = findViewById(R.id.totalTitle);
        receiptOptions = findViewById(R.id.receiptOptions);
        orderID = findViewById(R.id.orderID);
        dateOrdered = findViewById(R.id.dateOrdered);
        dateDelivered = findViewById(R.id.dateDelivered);
        vendorName = findViewById(R.id.restaurantName);
        vendorPhone = findViewById(R.id.restaurantPhone);
        nameTitle = findViewById(R.id.nameTitle);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        deliveryChargeAmount = findViewById(R.id.deliveryChargeAmount);
        subTotal = findViewById(R.id.subTotal);

        downloadRequest = getIntent().getBooleanExtra("downloadRequest", false);
        deliveryCharge = getIntent().getIntExtra("deliveryCharge", 0);
        orderedOn = getIntent().getStringExtra("orderOn");
        deliveredOn = getIntent().getStringExtra("deliveredOn");
        key = getIntent().getStringExtra("key");

        customerPhone = getIntent().getStringExtra("customerPhone");
        restaurantPhone = getIntent().getStringExtra("restaurantPhone");

        restaurantName = getIntent().getStringExtra("restaurantName");
        vendorName.setText(restaurantName);

        orderid = getIntent().getStringExtra("orderID");
        orderID.setText("Order ID: #"+orderid);

        deliveryChargeAmount.setText("Ksh "+deliveryCharge);

        DatabaseReference myUserDetails = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        myUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    UserModel myDetails = dataSnapshot.getValue(UserModel.class);
                    if (myDetails.getAccount_type().equals("1")) {
                        nameTitle.setText("Vendor");
                        vendorPhone.setText(restaurantPhone);

                        vendorName.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    if (!myPhone.equals(restaurantPhone)) {
                                        Intent slideactivity = new Intent(ReceiptActivity.this, ViewProfile.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                        slideactivity.putExtra("phone", restaurantPhone);
                                        Bundle bndlanimation =
                                                ActivityOptions.makeCustomAnimation(ReceiptActivity.this, R.anim.animation, R.anim.animation2).toBundle();
                                        startActivity(slideactivity, bndlanimation);
                                    }
                                } catch (Exception e){
                                    Log.e(TAG, "onClick: ", e);
                                }
                            }
                        });
                    }

                    if (myDetails.getAccount_type().equals("2") || myDetails.getAccount_type().equals("3")) {
                        nameTitle.setText("Customer");
                        vendorPhone.setText(customerPhone);


                        vendorName.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    if (!myPhone.equals(customerPhone)) {
                                        Intent slideactivity = new Intent(ReceiptActivity.this, ViewProfile.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                        slideactivity.putExtra("phone", customerPhone);
                                        Bundle bndlanimation =
                                                ActivityOptions.makeCustomAnimation(ReceiptActivity.this, R.anim.animation, R.anim.animation2).toBundle();
                                        startActivity(slideactivity, bndlanimation);
                                    }
                                } catch (Exception e){
                                    Log.e(TAG, "onClick: ", e);
                                }
                            }
                        });
                    }

                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        receiptObjRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone+"/"+key);
        receiptObjRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    finish();
                    SafeToast.makeText(ReceiptActivity.this, "Receipt does not exist!", Toast.LENGTH_LONG).show();
                } else {

                    DatabaseReference myUserDetails = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
                    myUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            try {
                                UserModel myDetails = dataSnapshot.getValue(UserModel.class);
                                if (myDetails.getAccount_type().equals("1")) {
                                    DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+restaurantPhone);
                                    userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            try {
                                                UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                                vendorName.setText(userModel.getFirstname() + " " + userModel.getLastname());
                                            } catch (Exception e){
                                                Log.e(TAG, "onDataChange: ", e);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }

                                if (myDetails.getAccount_type().equals("2") || myDetails.getAccount_type().equals("3")) {
                                    DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+customerPhone);
                                    userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            try {
                                                UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                                vendorName.setText(userModel.getFirstname() + " " + userModel.getLastname());
                                            } catch (Exception e){
                                                Log.e(TAG, "onDataChange: ", e);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            } catch (Exception e){

                                if(downloadRequest == true){
                                    finish();
                                    Toast.makeText(ReceiptActivity.this, "Something went wrong! try again", Toast.LENGTH_SHORT).show();
                                }
                                Log.e(TAG, "onDataChange: ", e);
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                    vendorPhone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final AlertDialog callAlert = new AlertDialog.Builder(ReceiptActivity.this)
                                    //set message, title, and icon
                                    .setMessage("Call " + restaurantName + "?")
                                    //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                    //set three option buttons
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            DatabaseReference myUserDetails = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
                                            myUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                    try {
                                                        UserModel myDetails = dataSnapshot.getValue(UserModel.class);
                                                        if (myDetails.getAccount_type().equals("1")) {
                                                            String phone = restaurantPhone;
                                                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                                            startActivity(intent);
                                                        }

                                                        if (myDetails.getAccount_type().equals("2")) {
                                                            String phone = customerPhone;
                                                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                                            startActivity(intent);
                                                        }
                                                    } catch (Exception e){
                                                        Log.e(TAG, "onDataChange: ", e);
                                                    }

                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            //do nothing

                                        }
                                    })//setNegativeButton

                                    .create();
                            callAlert.show();
                        }
                    });


                    String dtEnd = deliveredOn;
                    String dtStart = orderedOn;

                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
                    try {
                        //Convert String date values to Date values
                        Date dateStart;
                        Date dateEnd;

                        dateEnd = format.parse(dtEnd);
                        dateStart = format.parse(dtStart);

                        dateDelivered.setText(""+dateEnd);
                        dateOrdered.setText(""+dateStart);

                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.d(TAG, "timeStamp: "+ e.getMessage());
                    }

                    //get the ordered items
                    receiptItemsRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone+"/"+key+"/items");
                    receiptItemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            deliveredItems = new ArrayList<>();
                            totalAmount = 0;
                            int subT = 0;
                            for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                                try {
                                    progressBar.setVisibility(View.GONE);
                                    ProductDetailsModel product = dataSnapshot1.getValue(ProductDetailsModel.class);
                                    deliveredItems.add(product);

                                    subT = totalAmount + (Integer.parseInt(product.getPrice())*product.getQuantity());
                                    subTotal.setText("Ksh "+subT);
                                    totalAmount = totalAmount + (Integer.parseInt(product.getPrice())*product.getQuantity());
                                    totalBill.setText("Ksh " + (totalAmount+deliveryCharge));
                                    totalTitle.setText("" + (totalAmount+deliveryCharge));
                                    deliveryChargeAmount.setText("Ksh "+deliveryCharge);
                                } catch (Exception e){
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ReceiptActivity.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "onDataChange: ", e);
                                }
                            }

                            mAdapter = new ReceiptItemAdapter(ReceiptActivity.this,deliveredItems);
                            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(ReceiptActivity.this);
                            recyclerView.setLayoutManager(mLayoutManager);
                            recyclerView.setItemAnimator(new DefaultItemAnimator());
                            recyclerView.setAdapter(mAdapter);

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    //mark as seen
                    receiptObjRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone+"/"+key);
                    receiptObjRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                ReceiptModel newReceipt = dataSnapshot.getValue(ReceiptModel.class);
                                if (newReceipt.getSeen() == false) {
                                    receiptObjRef.child("seen").setValue(true);
                                }
                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    //creating a popup menu
                    PopupMenu popup = new PopupMenu(ReceiptActivity.this, receiptOptions);
                    //inflating menu from xml resource
                    popup.inflate(R.menu.receipt_options_menu);

                    Menu myMenu = popup.getMenu();
                    MenuItem deleteOption = myMenu.findItem(R.id.delete);
                    MenuItem downloadOption = myMenu.findItem(R.id.download);
                    try {
                        deleteOption.setVisible(true);
                        downloadOption.setVisible(true);
                    } catch (Exception e){}

                    receiptOptions.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //adding click listener
                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    switch (item.getItemId()) {
                                        case R.id.delete:
                                            receiptObjRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    finish();
                                                    SafeToast.makeText(ReceiptActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                            //do something
                                            return (true);
                                        case R.id.download:
                                            //5 second delay to run a few checks
                                            progressDialog.setMessage("Generating...");
                                            progressDialog.setCancelable(false);
                                            progressDialog.show();

                                            timer = new Timer();
                                            timer.schedule(new TimerTask() {

                                                int second = 5;

                                                @Override
                                                public void run() {
                                                    if (second == 0) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                timer.cancel();
                                                                if(progressDialog.isShowing()){
                                                                    progressDialog.dismiss();
                                                                }

                                                                exitReceipt.setVisibility(View.GONE);
                                                                receiptOptions.setVisibility(View.GONE);
                                                                bitmap = loadBitmapFromView(myReceipt, myReceipt.getWidth(), myReceipt.getHeight());
                                                                createPdf();

                                                            }
                                                        });

                                                    } else {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                //seconds ticking
                                                                progressDialog.setMessage("Generating("+(second--)+")...");
                                                            }
                                                        });
                                                    }

                                                }
                                            }, 0, 1000);
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            });
                            //displaying the popup
                            popup.show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(downloadRequest == true){
            progressDialog.setMessage("Generating...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            timer = new Timer();
            timer.schedule(new TimerTask() {

                int second = 5;

                @Override
                public void run() {
                    if (second == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                timer.cancel();
                                //now create
                                exitReceipt.setVisibility(View.GONE);
                                receiptOptions.setVisibility(View.GONE);
                                bitmap = loadBitmapFromView(myReceipt, myReceipt.getWidth(), myReceipt.getHeight());

                                createPdf();

                            }
                        });

                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //seconds ticking
                                progressDialog.setMessage("Generating("+(second--)+")...");
                            }
                        });
                    }

                }
            }, 0, 1000);
        }

        exitReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    //https://demonuts.com/android-generate-pdf-view/
    private void createPdf(){
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        //  Display display = wm.getDefaultDisplay();
        DisplayMetrics displaymetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        float hight = displaymetrics.heightPixels ;
        float width = displaymetrics.widthPixels ;

        int convertHighet = (int) hight, convertWidth = (int) width;

//        Resources mResources = getResources();
//        Bitmap bitmap = BitmapFactory.decodeResource(mResources, R.drawable.screenshot);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(convertWidth, convertHighet, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        canvas.drawPaint(paint);

        bitmap = Bitmap.createScaledBitmap(bitmap, convertWidth, convertHighet, true);

        paint.setColor(Color.BLUE);
        canvas.drawBitmap(bitmap, 0, 0 , null);
        document.finishPage(page);

        // write the document content
        String targetPdf = Environment.getExternalStorageDirectory().getPath()+"/Download/Dishi_"+orderid+".pdf";
        File filePath;
        filePath = new File(targetPdf);
        try {
            document.writeTo(new FileOutputStream(filePath));

            // close the document
            document.close();
            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.myReceipt), "PDF is created", Snackbar.LENGTH_LONG);
            snackbar.show();

            try {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } catch (Exception e){
                Log.e(TAG, "createPdf: ", e);
            }

            if(downloadRequest == true){
                finish();
            }

            openGeneratedPDF();
            exitReceipt.setVisibility(View.VISIBLE);
            receiptOptions.setVisibility(View.VISIBLE);

        } catch (IOException e) {

            try {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } catch (Exception err){}
            Log.e(TAG, "createPdf: ", e);
            e.printStackTrace();
            exitReceipt.setVisibility(View.VISIBLE);
            receiptOptions.setVisibility(View.VISIBLE);
            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.myReceipt), "Something went wrong", Snackbar.LENGTH_LONG);
            snackbar.show();
        }

    }

    private void openGeneratedPDF(){
        File file = new File(Environment.getExternalStorageDirectory().getPath()+"/Download/Dishi_"+orderid+".pdf");
        if (file.exists())
        {
            Toast.makeText(this, "Opening...", Toast.LENGTH_SHORT).show();
            Intent intent=new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(file);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            try
            {
                startActivity(intent);
            }
            catch(ActivityNotFoundException e)
            {
                Toast.makeText(ReceiptActivity.this, "No Application available to view pdf", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static Bitmap loadBitmapFromView(View v, int width, int height) {
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);

        return b;
    }


}

