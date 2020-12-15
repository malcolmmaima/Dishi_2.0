package com.malcolmmaima.dishiapp.View.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.R;

public class DishiFaq extends AppCompatActivity {

    String TAG = "DishiFaq";
    MyTextView_Roboto_Medium askFaq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dishi_faq);
        askFaq = findViewById(R.id.askFAQ);
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("FAQ");

        // https://stackoverflow.com/questions/8701634/send-email-intenthttps://stackoverflow.com/questions/8701634/send-email-intent
        askFaq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto", "dishifoodapp@gmail.com", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DishiFood - [Your Subject]");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi Dishi, ...");
                    startActivity(Intent.createChooser(emailIntent, "Send email..."));
                } catch (Exception e){
                    Toast.makeText(DishiFaq.this, "Something went wrong, email us at dishifoodapp@gmail.com", Toast.LENGTH_LONG).show();
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
