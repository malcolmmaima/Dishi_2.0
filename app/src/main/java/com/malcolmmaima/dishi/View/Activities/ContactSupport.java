package com.malcolmmaima.dishi.View.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.R;

import io.fabric.sdk.android.services.common.SafeToast;

public class ContactSupport extends AppCompatActivity {
    String TAG = "ContactSupport";
    MyTextView_Roboto_Medium sendEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_support);

        sendEmail = findViewById(R.id.sendEmail);
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Contact Support");

        // https://stackoverflow.com/questions/8701634/send-email-intenthttps://stackoverflow.com/questions/8701634/send-email-intent
        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto", "dishifoodapp@gmail.com", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DishiFood - [Your Subject]");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi Dishi, ...");
                    startActivity(Intent.createChooser(emailIntent, "Send email..."));
                } catch (Exception e){
                    SafeToast.makeText(ContactSupport.this, "Something went wrong, email us at dishifoodapp@gmail.com", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "onClick: ", e);
                }
            }
        });

        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });
    }
}
