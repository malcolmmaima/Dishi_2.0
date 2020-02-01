package com.malcolmmaima.dishi.View.Activities;

import android.content.Intent;
import android.os.Bundle;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.TextView;

import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;

public class ViewProduct extends AppCompatActivity {

    String key, restaurant, restaurantName_,product, description, price, imageUrl;
    Double distance;
    AppCompatTextView productName, productPrice, itemCount;
    TextView distanceAway, restaurantName, productDescription;
    CircleImageView foodPic;
    FloatingActionButton add, minus;
    int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_product);
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /**
         * UI Widgets
         */

        foodPic = findViewById(R.id.foodpic);
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        add = findViewById(R.id.addItem);
        minus = findViewById(R.id.minusItem);
        itemCount = findViewById(R.id.itemCount);
        distanceAway = findViewById(R.id.distanceAway);
        restaurantName = findViewById(R.id.restaurantName);
        productDescription = findViewById(R.id.productDescription);

        /**
         * Receive values from product adapter via intent
         */
        restaurant = getIntent().getStringExtra("restaurant"); //From adapters
        key = getIntent().getStringExtra("key"); //From adapters, to allow for editing
        product = getIntent().getStringExtra("product");
        price = getIntent().getStringExtra("price");
        description = getIntent().getStringExtra("description");
        imageUrl = getIntent().getStringExtra("imageUrl");
        distance = getIntent().getDoubleExtra("distance", 0.0);
        restaurantName_ = getIntent().getStringExtra("restaurantName");

        setTitle("Order Now"); //Set title

        /**
         * Set product image
         */
        Picasso.with(ViewProduct.this).load(imageUrl).fit().centerCrop()
                .placeholder(R.drawable.menu)
                .error(R.drawable.menu)
                .into(foodPic);

        /**
         * Set product details
         */
        count = 1;
        productName.setText(product);
        productPrice.setText("Ksh "+price);
        productDescription.setText(description);
        restaurantName.setText(restaurantName_);
        itemCount.setText(""+count);

        if(distance < 1.0){
            distanceAway.setText(distance*1000 + "m away");
        } else {
            distanceAway.setText(distance + "km away");

        }

        /**
         * Set product quantity
         */
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                itemCount.setText(""+count);
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(count>1){
                    count--;
                    itemCount.setText(""+count);
                }

            }
        });

        /**
         * View image click listener
         */
        foodPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(ViewProduct.this, ViewImage.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("imageURL", imageUrl);
                startActivity(slideactivity);
            }
        });
        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });
        //topToolBar.setLogo(R.drawable.dish);
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
