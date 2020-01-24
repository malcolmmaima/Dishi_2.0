package com.malcolmmaima.dishi.View.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.jsibbold.zoomage.ZoomageView;
import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;

public class ViewImage extends AppCompatActivity {

    ZoomageView viewImage;
    ProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        viewImage = findViewById(R.id.viewImage);
        loading = findViewById(R.id.progressBar);
        loading.setVisibility(View.VISIBLE);

        final Toolbar topToolBar = findViewById(R.id.toolbar);
        topToolBar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("");

        final String imageURL = getIntent().getStringExtra("imageURL");


        Picasso.with(this)
                .load(imageURL)

                .into(viewImage, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        loading.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError() {
                        loading.setVisibility(View.INVISIBLE);
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.parentLayout), "Something went wrong", Snackbar.LENGTH_LONG);
                        snackbar.show();

                        if(snackbar.isShown()){
                            finish();
                        }
                    }
                });


        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });
    }


}
