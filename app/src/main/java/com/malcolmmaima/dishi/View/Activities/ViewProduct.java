package com.malcolmmaima.dishi.View.Activities;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.Toast;

import com.malcolmmaima.dishi.R;

public class ViewProduct extends AppCompatActivity {

    String key, restaurant, product, description, price, imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_product);
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /**
         * Receive values from product adapter via intent
         */
        restaurant = getIntent().getStringExtra("restaurant"); //From adapters
        key = getIntent().getStringExtra("key"); //From adapters, to allow for editing
        product = getIntent().getStringExtra("product");
        price = getIntent().getStringExtra("price");
        description = getIntent().getStringExtra("description");
        imageUrl = getIntent().getStringExtra("imageUrl");

        setTitle(product);

//        Toast.makeText(this, "phone: " + restaurant
//                                                    + "\nkey: " + key
//                                                    + "\nproduct: " + product
//                                                    + "\nprice: " + price
//                                                    + "\ndescription: " + description
//                                                    + "\nimage: " + imageUrl, Toast.LENGTH_LONG).show();

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });
        //topToolBar.setLogo(R.drawable.logo);
        //topToolBar.setLogoDescription(getResources().getString(R.string.logo_desc));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Add to cart", Snackbar.LENGTH_LONG).show();
            }
        });
    }

}
