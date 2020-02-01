package com.malcolmmaima.dishi.View.Activities;

import android.content.Intent;
import android.os.Bundle;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.GetCurrentDate;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;

public class ViewProduct extends AppCompatActivity {

    String key, restaurant, restaurantName_,product, description, price, imageUrl;
    Double distance;
    AppCompatTextView productName, productPrice, itemCount;
    TextView distanceAway, restaurantName, productDescription, subTotal;
    CircleImageView foodPic;
    FloatingActionButton add, minus;
    int count;
    Menu myMenu;
    DatabaseReference myCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_product);
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        String myPhone;
        myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
        myCart = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);
        myCart.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    myMenu.findItem(R.id.myCart).setVisible(true);
                    myMenu.findItem(R.id.myCart).setEnabled(true);
                }

                else {
                    myMenu.findItem(R.id.myCart).setVisible(false);
                    myMenu.findItem(R.id.myCart).setEnabled(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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
        subTotal = findViewById(R.id.subTotal);

        /**
         * Receive values from product adapter via intent
         */
        restaurant = getIntent().getStringExtra("restaurant"); //restaurant phone, our primary key
        key = getIntent().getStringExtra("key"); //From adapters, to allow for indexing
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
            distanceAway.setText(distance*1000 + "m away"); //convert KM to meters
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

                //Show subtotal in realtime
                productSubTotal();
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(count>1){
                    count--;
                    itemCount.setText(""+count);
                }

                //Show subtotal in realtime
                productSubTotal();

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
            public void onClick(final View view) {

                Snackbar.make(view, "Adding...", Snackbar.LENGTH_LONG).show();

                String myPhone;
                myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
                DatabaseReference myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);

                //Get current date
                GetCurrentDate currentDate = new GetCurrentDate();
                String cartDate = currentDate.getDate();

                String key = myCartRef.push().getKey();
                ProductDetails cartProduct = new ProductDetails();
                cartProduct.setName(product);
                cartProduct.setPrice(price);
                cartProduct.setDescription(description);
                cartProduct.setImageURL(imageUrl);
                cartProduct.setOwner(restaurant);
                cartProduct.setQuantity(count);
                cartProduct.setUploadDate(cartDate);

                myCartRef.child(key).setValue(cartProduct).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Snackbar.make(view, "Added to cart", Snackbar.LENGTH_LONG).show();
                    }
                });

            }
        });
    }

    private void productSubTotal() {
        if(count != 1){
            int subtotal = Integer.parseInt(price) * count;
            subTotal.setVisibility(View.VISIBLE);
            subTotal.setText("Subtotal: " + subtotal);
        }

        else {
            subTotal.setVisibility(View.GONE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_product_menu, menu);
        myMenu = menu;
        MenuItem item = menu.findItem(R.id.myCart);
        if (item != null) {
            item.setVisible(true);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) { switch(item.getItemId()) {
        case R.id.myCart:
            Toast.makeText(this, "view cart", Toast.LENGTH_SHORT).show();
            return(true);
    }
        return(super.onOptionsItemSelected(item));
    }
}
