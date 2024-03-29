package com.malcolmmaima.dishiapp.View.Activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishiapp.Model.ProductDetailsModel;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.squareup.picasso.Picasso;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.text.DecimalFormat;



public class ViewProduct extends AppCompatActivity {

    String TAG = "ViewProductActivity";
    String key, restaurant, restaurantName_,product, description, price, imageUrl, myPhone, accType;
    Double distance;
    ImageView addToFavourites;
    MyTextView_Roboto_Medium productName, productPrice, itemCount, subTotal;
    MyTextView_Roboto_Regular distanceAway, restaurantName, productDescription, favouritesTotal, outOfStockTxt;
    CircleImageView foodPic;
    FloatingActionButton add, minus;
    CheckBox checkBox;
    HashTagHelper mTextHashTagHelper;
    int count;
    Boolean outOfStock;
    Menu myMenu;
    DatabaseReference myCart, favouritesTotalRef, myFoodFavourites, menuExistRef, existingCartRef, myRef;
    ValueEventListener cartListener, favouritesTotalListener, myFoodFavouritesListener, existsListener;
    ValueEventListener cartexixtListner;
    FloatingActionButton fab;
    FirebaseUser user;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_product);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            mAuth = FirebaseAuth.getInstance();
            if(mAuth.getInstance().getCurrentUser() == null){
                finish();
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
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
                                            Intent slideactivity = new Intent(ViewProduct.this, SecurityPin.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            slideactivity.putExtra("pinType", "resume");
                                            startActivity(slideactivity);
                                        }
                                    } catch (Exception e){

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


            Toolbar topToolBar = findViewById(R.id.toolbar);
            setSupportActionBar(topToolBar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            count = 1; //item quantity

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
            fab = findViewById(R.id.fab);
            addToFavourites = findViewById(R.id.favourite);
            favouritesTotal = findViewById(R.id.favouritesTotal);
            checkBox = findViewById(R.id.checkBox);
            checkBox.setVisibility(View.GONE);
            checkBox.setEnabled(false);
            outOfStockTxt = findViewById(R.id.outOfStock);
            outOfStockTxt.setVisibility(View.GONE);

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
            accType = getIntent().getStringExtra("accType");
            outOfStock = getIntent().getBooleanExtra("outOfStock", false);


            favouritesTotalRef = FirebaseDatabase.getInstance().getReference("menus/"+restaurant+"/"+key+"/likes");
            myFoodFavourites = FirebaseDatabase.getInstance().getReference("my_food_favourites/"+myPhone);
            existingCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);
            cartexixtListner = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    for(DataSnapshot cartItem : dataSnapshot.getChildren()){
                        ProductDetailsModel cartProd = cartItem.getValue(ProductDetailsModel.class);

                        //Item already exists in cart, inititalize counter
                        if(cartProd.getOriginalKey().equals(key)){
                            int currItemQuantity = cartProd.getQuantity();
                            itemCount.setText(""+currItemQuantity);
                            count = currItemQuantity;
                            subTotal.setVisibility(View.VISIBLE);
                            subTotal.setText("(Already in cart) Subtotal: "+(currItemQuantity*Integer.parseInt(cartProd.getPrice())));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            existingCartRef.addValueEventListener(cartexixtListner);

            menuExistRef = FirebaseDatabase.getInstance().getReference("menus/"+restaurant+"/"+key);
            existsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){ //Does not exist!
                        finish();
                        Toast.makeText(ViewProduct.this, "Item no longer exists", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            menuExistRef.addValueEventListener(existsListener);

            myFoodFavouritesListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String productKey = dataSnapshot.getValue(String.class);
                    try {
                        if (productKey.equals("fav")) {
                            addToFavourites.setTag(R.drawable.ic_liked);
                            addToFavourites.setImageResource(R.drawable.ic_liked);
                        } else {
                            addToFavourites.setTag(R.drawable.ic_like);
                            addToFavourites.setImageResource(R.drawable.ic_like);
                        }
                    } catch (Exception e){

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

            //if the history item no longer exists in restaurant's menu, an exception is bound to be generated
            try {
                myFoodFavourites.child(restaurant).child(key).addValueEventListener(myFoodFavouritesListener);
            } catch (Exception e){
                finish();
                Toast.makeText(this, "Something went wrong!", Toast.LENGTH_LONG).show();
            }

            //Fetch total likes
            favouritesTotalListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        int total = (int) dataSnapshot.getChildrenCount();
                        Double totalLikes = Double.valueOf(total);

                        //below 1000
                        if(totalLikes < 1000){
                            DecimalFormat value = new DecimalFormat("#");
                            favouritesTotal.setText(""+value.format(totalLikes));
                        }

                        // 1000 to 999,999
                        else if(totalLikes >= 1000 && totalLikes <= 999999){
                            if(totalLikes % 1000 == 0){ //No remainder
                                DecimalFormat value = new DecimalFormat("#####");
                                favouritesTotal.setText(""+value.format(totalLikes/1000)+"K");
                            }

                            else { //Has remainder 999.9K
                                DecimalFormat value = new DecimalFormat("######.#");
                                Double divided = totalLikes/1000;
                                if(value.format(divided).equals("1000")){
                                    favouritesTotal.setText("1M"); //if rounded off
                                } else {
                                    favouritesTotal.setText(""+value.format(divided)+"K");
                                }
                            }
                        }

                        // 1,000,0000 to 999,999,999
                        else if(totalLikes >= 1000000 && totalLikes <= 999999999){
                            if(totalLikes % 1000000 == 0) { //No remainder
                                DecimalFormat value = new DecimalFormat("#");
                                favouritesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                            }

                            else { //Has remainder 9.9M, 999.9M etc
                                DecimalFormat value = new DecimalFormat("#.#");
                                if(value.format(totalLikes/1000000).equals("1000")){
                                    favouritesTotal.setText("1B"); //if rounded off
                                } else {
                                    favouritesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                                }
                            }
                        }
                    } catch (Exception e){}
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            favouritesTotalRef.addValueEventListener(favouritesTotalListener);

            addToFavourites.setTag(R.drawable.ic_like);
            addToFavourites.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //First lets check if the restaurant still has this particular product in their menu
                    DatabaseReference menuExistRef = FirebaseDatabase.getInstance().getReference("menus/"+restaurant+"/"+key);
                    menuExistRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.exists()){ //Does not exist!
                                Snackbar.make(v, "No longer exists!", Snackbar.LENGTH_LONG).show();
                            } else {
                                int id = (int)addToFavourites.getTag();
                                if( id == R.drawable.ic_like){
                                    //Add to my favourites
                                    myFoodFavourites.child(restaurant).child(key).setValue("fav").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //Add to global restaurant likes
                                            favouritesTotalRef.child(myPhone).setValue("fav").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //Add favourite to restaurant's node as well
                                                    addToFavourites.setTag(R.drawable.ic_liked);
                                                    addToFavourites.setImageResource(R.drawable.ic_liked);
                                                }
                                            });
                                            //Toast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                                        }
                                    });


                                } else{
                                    //Remove from my favourites
                                    myFoodFavourites.child(restaurant).child(key).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            favouritesTotalRef.child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //remove favourite from restaurant's node as well
                                                    addToFavourites.setTag(R.drawable.ic_like);
                                                    addToFavourites.setImageResource(R.drawable.ic_like);
                                                }
                                            });
                                            //Toast.makeText(context,restaurantDetails.getName()+" removed from favourites",Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            });

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

            productName.setText(product);
            productPrice.setText("Ksh "+price);
            productDescription.setText(description);
            restaurantName.setText(restaurantName_);
            itemCount.setText(""+count);

            try {
                Linkify.addLinks(productDescription, Linkify.ALL);
            } catch (Exception e){
                Log.e(TAG, "onDataChange: ", e);
            }

            //handle hashtags
            if(description.contains("#")){
                mTextHashTagHelper = HashTagHelper.Creator.create(getResources().getColor(R.color.colorPrimary),
                        new HashTagHelper.OnHashTagClickListener() {
                            @Override
                            public void onHashTagClicked(String hashTag) {
                                String searchHashTag = "#"+hashTag;
                                Intent slideactivity = new Intent(ViewProduct.this, SearchActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                slideactivity.putExtra("searchString", searchHashTag);
                                slideactivity.putExtra("goToFragment", 1);
                                startActivity(slideactivity);
                            }
                        });

                mTextHashTagHelper.handle(productDescription);
            }

            //Since this activity is receiving the restaurant name from adapter, in the case activity transitions to this without having fetched name, then fetch a fresh
            DatabaseReference restaurantDetails = FirebaseDatabase.getInstance().getInstance().getReference("users/"+restaurant);
            restaurantDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        UserModel restDetails = dataSnapshot.getValue(UserModel.class);
                        restaurantName.setText(restDetails.getFirstname()+" "+restDetails.getLastname());
                    } catch (Exception e){}
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            if(distance < 1.0){
                distanceAway.setText(distance*1000 + "m away"); //convert KM to meters
            } else {
                distanceAway.setText(distance + "km away");

            }

            if(!accType.equals("1")){
                add.setEnabled(false);
                minus.setEnabled(false);
                fab.setEnabled(false);
                addToFavourites.setEnabled(false);

                add.setSupportBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
                minus.setSupportBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
                fab.setSupportBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
            }

            if(outOfStock == true){
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(true);
                outOfStockTxt.setVisibility(View.VISIBLE);
                add.setEnabled(false);
                minus.setEnabled(false);
                fab.setEnabled(false);
                add.setSupportBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
                minus.setSupportBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
                fab.setSupportBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
            }

            if(myPhone.equals(restaurant)){
                restaurantName.setEnabled(false);
            }

            /**
             * View restaurant
             */

            restaurantName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatabaseReference profilePic = FirebaseDatabase.getInstance().getReference("users/"+restaurant+"/profilePic");
                    profilePic.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                String profilepicture = dataSnapshot.getValue(String.class);
                                //Slide to new activity
                                Intent slideactivity = new Intent(ViewProduct.this, ViewRestaurant.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                slideactivity.putExtra("restaurant_phone", restaurant);
                                slideactivity.putExtra("distance", distance);
                                slideactivity.putExtra("profilePic", profilepicture);
                                Bundle bndlanimation =
                                        null;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    bndlanimation = ActivityOptions.makeCustomAnimation(ViewProduct.this, R.anim.animation, R.anim.animation2).toBundle();
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    startActivity(slideactivity, bndlanimation);
                                }
                            } catch (Exception e){}
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            });

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

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {

                    Snackbar.make(view, "Adding...", Snackbar.LENGTH_LONG).show();

                    //Check to see if this item already exists in cart, if yes, increment quantity
                    DatabaseReference cartItemExistsRef = FirebaseDatabase
                            .getInstance().getReference("cart/"+myPhone);
                    cartItemExistsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.hasChildren()){
                                //cart is empty, add a fresh
                                String myPhone;
                                myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
                                DatabaseReference myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);

                                //Get current date
                                GetCurrentDate currentDate = new GetCurrentDate();
                                String cartDate = currentDate.getDate();

                                String newKey = myCartRef.push().getKey();
                                ProductDetailsModel cartProduct = new ProductDetailsModel();
                                cartProduct.setName(product);
                                cartProduct.setPrice(price);
                                cartProduct.setDescription(description);
                                cartProduct.setImageURL(imageUrl);
                                cartProduct.setOwner(restaurant);
                                cartProduct.setOriginalKey(key);
                                cartProduct.setQuantity(count);
                                cartProduct.setDistance(distance);
                                cartProduct.setUploadDate(cartDate);

                                myCartRef.child(newKey).setValue(cartProduct).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        try {
                                            Snackbar.make(view, "Added to cart", Snackbar.LENGTH_LONG).show();
                                        } catch (Exception e){

                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        try {
                                            Snackbar.make(view, "Something went wrong", Snackbar.LENGTH_LONG).show();
                                        } catch(Exception er){
                                            Log.e(TAG, "onFailure: ", er);
                                        }
                                    }
                                });
                            } else {

                                //Get current date
                                GetCurrentDate currentDate = new GetCurrentDate();
                                String cartDate = currentDate.getDate();

                                String newKey = cartItemExistsRef.push().getKey();
                                ProductDetailsModel cartProduct = new ProductDetailsModel();
                                cartProduct.setName(product);
                                cartProduct.setPrice(price);
                                cartProduct.setDescription(description);
                                cartProduct.setImageURL(imageUrl);
                                cartProduct.setOwner(restaurant);
                                cartProduct.setOriginalKey(key);
                                cartProduct.setQuantity(count);
                                cartProduct.setDistance(distance);
                                cartProduct.setUploadDate(cartDate);

                                cartItemExistsRef.child(newKey).setValue(cartProduct).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        try {
                                            Snackbar.make(view, "Added to cart", Snackbar.LENGTH_LONG).show();
                                        } catch (Exception e){

                                        }

                                        for(DataSnapshot cartItem : dataSnapshot.getChildren()){
                                            ProductDetailsModel cartProd = cartItem.getValue(ProductDetailsModel.class);

                                            //Item already exists in cart, set new quantity
                                            if(cartProd.getOriginalKey().equals(key)){
                                                cartProd.setQuantity(count);
                                                cartItemExistsRef.child(newKey).setValue(cartProd).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        cartItemExistsRef.child(cartItem.getKey()).removeValue();
                                                        try {
                                                            Snackbar.make(view, "Added to cart", Snackbar.LENGTH_LONG).show();
                                                            //Snackbar.make(v.getRootView(), cartProd.getName()+": "+ finalCurrItemQuantity, Snackbar.LENGTH_LONG).show();
                                                        } catch(Exception e){
                                                            Log.e(TAG, "onSuccess: ", e);
                                                        }
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        try {
                                                            Snackbar.make(view, "Something went wrong", Snackbar.LENGTH_LONG).show();
                                                        } catch(Exception er){
                                                            Log.e(TAG, "onFailure: ", er);
                                                        }
                                                    }
                                                });
                                                //Log.d(TAG, "old qty: "+cartProd.getQuantity()+" new qty "+ currItemQuantity);
                                            }
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        try {
                                            Snackbar.make(view, "Something went wrong", Snackbar.LENGTH_LONG).show();
                                        } catch(Exception er){
                                            Log.e(TAG, "onFailure: ", er);
                                        }
                                    }
                                });

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            });
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
                                Intent slideactivity = new Intent(ViewProduct.this, SecurityPin.class)
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
        try {
            item.setVisible(false);
        } catch (Exception e){}

        /**
         * Hide / show cart icon if has items or not
         */
        myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
        myCart = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);
        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    try {
                        myMenu.findItem(R.id.myCart).setVisible(true);
                        myMenu.findItem(R.id.myCart).setEnabled(true);
                    } catch (Exception e){

                    }
                }

                else {
                    try {
                        myMenu.findItem(R.id.myCart).setVisible(false);
                        myMenu.findItem(R.id.myCart).setEnabled(false);
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myCart.addValueEventListener(cartListener);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.myCart:
            finish();
            Intent cartActivity = new Intent(ViewProduct.this, MyCart.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(cartActivity);

            return(true);

            case R.id.reportProduct:
                Intent reportActivity = new Intent(ViewProduct.this, ReportAbuse.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                reportActivity.putExtra("type", "product");
                reportActivity.putExtra("owner", restaurant);
                reportActivity.putExtra("productKey", key);
                startActivity(reportActivity);
                return (true);
    }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            myFoodFavourites.child(restaurant).child(key).removeEventListener(myFoodFavouritesListener);
            favouritesTotalRef.removeEventListener(favouritesTotalListener);
            myCart.removeEventListener(cartListener);
            menuExistRef.removeEventListener(existsListener);
            existingCartRef.removeEventListener(cartexixtListner);
        } catch (Exception e){

        }
    }
}
