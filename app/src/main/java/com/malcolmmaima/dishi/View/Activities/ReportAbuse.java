package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.core.Repo;
import com.malcolmmaima.dishi.Controller.CommentKeyBoardFix;
import com.malcolmmaima.dishi.Controller.GetCurrentDate;
import com.malcolmmaima.dishi.Model.ProductAbuseReportModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Maps.GeoTracking;

import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import io.fabric.sdk.android.services.common.SafeToast;

public class ReportAbuse extends AppCompatActivity {
    RelativeLayout option1, option2, option3, option4;
    EmojiconEditText describe;
    AppCompatButton reportBtn;
    AppCompatImageView option1Tick, option2Tick, option3Tick, option4Tick;
    int selectedOption;
    String reportType, productOwner, productKey, myPhone;
    FirebaseUser user;
    String [] reportOptions = {"It's suspicious or spam","It displays abusive content", "Copyright infringement", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_abuse);

        option1 = findViewById(R.id.option1);
        option1Tick = findViewById(R.id.option1Tick);

        option2 = findViewById(R.id.option2);
        option2Tick = findViewById(R.id.option2Tick);

        option3 = findViewById(R.id.option3);
        option3Tick = findViewById(R.id.option3Tick);

        option4 = findViewById(R.id.option4);
        option4Tick = findViewById(R.id.option4Tick);

        describe = findViewById(R.id.describe);
        reportBtn = findViewById(R.id.reportBtn);
        reportBtn.setVisibility(View.GONE);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("Report Abuse");

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        reportType = getIntent().getStringExtra("type");

        if(reportType.equals("product")){
            productOwner = getIntent().getStringExtra("owner");
            productKey = getIntent().getStringExtra("productKey");
        }

        //keep toolbar pinned at top. push edittext on keyboard load
        new CommentKeyBoardFix(this);

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });

        option1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedAbuse(1);
            }
        });

        option2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedAbuse(2);
            }
        });

        option3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedAbuse(3);
            }
        });

        option4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedAbuse(4);
            }
        });

        reportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ProgressDialog progressDialog = new ProgressDialog(ReportAbuse.this);
                progressDialog.setMessage("Sending...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                if(reportType.equals("product")){

                    DatabaseReference productReportsRef = FirebaseDatabase.getInstance().getReference("reports/products/"+productKey);

                    //Get today's date
                    GetCurrentDate currentDate = new GetCurrentDate();

                    //Bundle our report into a model (POJO)
                    ProductAbuseReportModel newReport = new ProductAbuseReportModel();
                    newReport.setDescription(describe.getText().toString().trim());
                    newReport.setComplaint(reportOptions[selectedOption-1]);
                    newReport.setReportedOn(currentDate.getDate());

                    //Make sure the contact of this product is capture as it acts as our primary key in referencing the particular product
                    productReportsRef.child("owner").setValue(productOwner);

                    //Now create a new unique reference and post the report POJO then exit
                    DatabaseReference userReportsRef = FirebaseDatabase.getInstance().getReference("reports/products/"+productKey+"/userReports/"+myPhone);
                    userReportsRef.setValue(newReport).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            try {
                                progressDialog.dismiss();
                                finish();
                                Toast.makeText(ReportAbuse.this, "Report sent!", Toast.LENGTH_LONG).show();
                            } catch (Exception e){}
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            try {
                                progressDialog.dismiss();
                                Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                snackbar.show();
                            } catch (Exception err){}
                        }
                    });
                }
            }
        });
    }

    private void selectedAbuse(int selected) {
        selectedOption = selected;
        reportBtn.setVisibility(View.VISIBLE);

        if(selected ==1){
            option1Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);

            option2Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
            option3Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
            option4Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        if(selected ==2){
            option2Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);

            option1Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
            option3Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
            option4Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        if(selected ==3){
            option3Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);

            option1Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
            option2Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
            option4Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        if(selected ==4){
            option4Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);

            option1Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
            option2Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
            option3Tick.setColorFilter(ContextCompat.getColor(ReportAbuse.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }
}
