package com.malcolmmaima.dishi.View.Activities;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ViewPagerAdapter;
import com.malcolmmaima.dishi.View.Fragments.ReviewsFragment;
import com.malcolmmaima.dishi.View.Fragments.ViewRestaurantMenuFragment;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

import io.fabric.sdk.android.services.common.SafeToast;

public class ViewRestaurant extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference restaurantRef, myFavourites, providerFavs, myRef;
    ImageView coverImageView, favourite, callBtn, shareRest;
    MyTextView_Roboto_Medium restaurantName;
    MyTextView_Roboto_Regular distAway, likes;
    String RestaurantName, phone;
    ValueEventListener providerFavsListener;
    Menu myMenu;
    DatabaseReference myLocationRef, restaurantLocationRef, myCartRef;
    ValueEventListener mylocationListener, restaurantLocationListener, cartListener;
    LiveLocationModel myLocation, restaurantLocation;
    Double dist;
    String myPhone;
    FirebaseUser user;
    FloatingActionButton fab;

    //This is our tablayout
    private TabLayout tabLayout;

    //This is our viewPager
    private ViewPager viewPager;

    ViewPagerAdapter adapter;

    //Fragments
    ViewRestaurantMenuFragment restaurantMenu;
    ReviewsFragment restaurantReviews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_restaurant);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if(locked == true){
                                    Intent slideactivity = new Intent(ViewRestaurant.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                } else {
                                    loadActivity();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    } else {
                        loadActivity();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void loadActivity() {
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("Details");

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        /**
         * Receive values from Restaurant adapter
         */
        final String restaurantPhone = getIntent().getStringExtra("restaurant_phone");
        final Double distanceAway = getIntent().getDoubleExtra("distance", 0.0);

        String profilePic = getIntent().getStringExtra("profilePic");

        favourite = findViewById(R.id.likeImageView);
        callBtn = findViewById(R.id.callRestaurant);
        shareRest = findViewById(R.id.shareImageView);
        coverImageView = findViewById(R.id.coverImageView);
        restaurantName = findViewById(R.id.titleTextView);
        favourite.setTag(R.drawable.ic_like);

        distAway = findViewById(R.id.distanceAway);
        likes = findViewById(R.id.likesTotal);

        fab = findViewById(R.id.fab); //cart icon

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String myPhone = user.getPhoneNumber(); //Current logged in user phone number

        myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
        restaurantLocationRef = FirebaseDatabase.getInstance().getReference("location/"+restaurantPhone);

        restaurantRef = FirebaseDatabase.getInstance().getReference("users/"+restaurantPhone);
        myFavourites = FirebaseDatabase.getInstance().getReference("my_restaurant_favourites/"+myPhone);
        providerFavs = FirebaseDatabase.getInstance().getReference("restaurant_favourites/"+ restaurantPhone);

        myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);
        cartListener = new ValueEventListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    try {
                        fab.setVisibility(View.VISIBLE);
                    } catch (Exception e){

                    }
                }

                else {
                    try {
                        fab.setVisibility(View.GONE);
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(ViewRestaurant.this, MyCart.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(slideactivity);
            }
        });

        myCartRef.addValueEventListener(cartListener);

        //Fetch the restauant basic info
        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    finish();
                    SafeToast.makeText(ViewRestaurant.this, "Restaurant no longer exists!", Toast.LENGTH_LONG).show();
                } else {
                    UserModel restaurantDetails = dataSnapshot.getValue(UserModel.class);
                    //restaurantDetails.phone = restaurantPhone;

                    try {
                        RestaurantName = restaurantDetails.getFirstname() + " " + restaurantDetails.getLastname();
                        restaurantName.setText(RestaurantName);
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Get my location coordinates
        mylocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    myLocation = dataSnapshot.getValue(LiveLocationModel.class);
                    computeDistance(myLocation.getLatitude(), myLocation.getLongitude(), restaurantLocation.getLatitude(), restaurantLocation.getLongitude(), "K");
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myLocationRef.addValueEventListener(mylocationListener);

        //get restaurant location coordinates
        restaurantLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    restaurantLocation = dataSnapshot.getValue(LiveLocationModel.class);
                    computeDistance(myLocation.getLatitude(), myLocation.getLongitude(), restaurantLocation.getLatitude(), restaurantLocation.getLongitude(), "K");
                }catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        restaurantLocationRef.addValueEventListener(restaurantLocationListener);

        //Initializing viewPager
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        //Initializing the tablayout
        tabLayout = findViewById(R.id.tablayout);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition(),false);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                tabLayout.getTabAt(position).select();

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        distAway.setText(distanceAway + "");

        if(distanceAway < 1.0){
            distAway.setText(distanceAway*1000 + "m away");
        } else {
            distAway.setText(distanceAway + "km away");
        }

        /**
         * Load image url onto imageview
         */
        try {
            //Load image
            Picasso.with(ViewRestaurant.this).load(profilePic).fit().centerCrop()
                    .placeholder(R.drawable.shop)
                    .error(R.drawable.shop)
                    .into(coverImageView);
        } catch (Exception e){ }

        providerFavsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int total = (int) dataSnapshot.getChildrenCount();
                    Double totalLikes = Double.valueOf(total);

                    //below 1000
                    if(totalLikes < 1000){
                        DecimalFormat value = new DecimalFormat("#");
                        likes.setText(""+value.format(totalLikes));
                    }

                    // 1000 to 999,999
                    else if(totalLikes >= 1000 && totalLikes <= 999999){
                        if(totalLikes % 1000 == 0){ //No remainder
                            DecimalFormat value = new DecimalFormat("#####");
                            likes.setText(""+value.format(total/1000)+"K");
                        }

                        else { //Has remainder 999.9K
                            DecimalFormat value = new DecimalFormat("######.#");
                            Double divided = totalLikes/1000;
                            if(value.format(divided).equals("1000")){
                                likes.setText("1M"); //if rounded off
                            } else {
                                likes.setText(""+value.format(divided)+"K");
                            }
                        }
                    }

                    // 1,000,0000 to 999,999,999
                    else if(totalLikes >= 1000000 && totalLikes <= 999999999){
                        if(totalLikes % 1000000 == 0) { //No remainder
                            DecimalFormat value = new DecimalFormat("#");
                            likes.setText(""+value.format(totalLikes/1000000)+"M");
                        }

                        else { //Has remainder 9.9M, 999.9M etc
                            DecimalFormat value = new DecimalFormat("#.#");
                            if(value.format(totalLikes/1000000).equals("1000")){
                                likes.setText("1B"); //if rounded off
                            } else {
                                likes.setText(""+value.format(totalLikes/1000000)+"M");
                            }
                        }
                    }
                } catch (Exception e){

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        providerFavs.addValueEventListener(providerFavsListener);

        //Initialize on load
        myFavourites.child(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String phone = dataSnapshot.getValue(String.class);
                try {
                    if (phone.equals("fav")) {
                        favourite.setTag(R.drawable.ic_liked);
                        favourite.setImageResource(R.drawable.ic_liked);
                    } else {
                        favourite.setTag(R.drawable.ic_like);
                        favourite.setImageResource(R.drawable.ic_like);
                    }
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        coverImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!myPhone.equals(restaurantPhone)){
                    finish();
                    Intent slideactivity = new Intent(ViewRestaurant.this, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    slideactivity.putExtra("phone", restaurantPhone);
                    Bundle bndlanimation =
                            null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        bndlanimation = ActivityOptions.makeCustomAnimation(ViewRestaurant.this, R.anim.animation,R.anim.animation2).toBundle();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        startActivity(slideactivity, bndlanimation);
                    }
                }
            }
        });

        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog callAlert = new AlertDialog.Builder(v.getContext())
                        //set message, title, and icon
                        .setMessage("Call " + RestaurantName + "?")
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String phone_ = phone;
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone_, null));
                                startActivity(intent);
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

        shareRest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SafeToast.makeText(ViewRestaurant.this, "Share!", Toast.LENGTH_SHORT).show();
            }
        });

        favourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int id = (int) favourite.getTag();
                if( id == R.drawable.ic_like){
                    //Add to my favourites
                    myFavourites.child(phone).setValue("fav").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            favourite.setTag(R.drawable.ic_liked);
                            favourite.setImageResource(R.drawable.ic_liked);

                            //Add to global restaurant likes
                            providerFavs.child(myPhone).setValue("fav").addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //Add favourite to restaurant's node as well
                                }
                            });
                            //SafeToast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                        }
                    });


                } else{
                    //Remove from my favourites
                    myFavourites.child(phone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            favourite.setTag(R.drawable.ic_like);
                            favourite.setImageResource(R.drawable.ic_like);

                            providerFavs.child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //remove favourite from restaurant's node as well
                                }
                            });
                            //SafeToast.makeText(context,restaurantDetails.getName()+" removed from favourites",Toast.LENGTH_SHORT).show();
                        }
                    });

                }

            }
        });
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
                                Intent slideactivity = new Intent(ViewRestaurant.this, SecurityPin.class)
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_view_restaurants, menu);
        myMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) { switch(item.getItemId()) {

        case R.id.sendMessage:
            Intent slideactivity = new Intent(ViewRestaurant.this, Chat.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            slideactivity.putExtra("fromPhone", myPhone);
            slideactivity.putExtra("toPhone", phone);
            Bundle bndlanimation =
                    null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                bndlanimation = ActivityOptions.makeCustomAnimation(ViewRestaurant.this, R.anim.animation,R.anim.animation2).toBundle();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                startActivity(slideactivity, bndlanimation);
            }
            return  (true);

    }
        return(super.onOptionsItemSelected(item));
    }

    private void computeDistance(Double latitude, Double longitude, Double latitude1, Double longitude1, String k) {
        CalculateDistance calculateDistance = new CalculateDistance();
        dist = calculateDistance.distance(latitude, longitude, latitude1, longitude1, "K");
        try {
            if (dist < 1.0) {
                distAway.setText(dist * 1000 + "m away");
            } else {
                distAway.setText(dist + "km away");

            }
        } catch (Exception e){

        }
    }

    private void setupViewPager(ViewPager viewPager)
    {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        restaurantMenu = new ViewRestaurantMenuFragment();
        restaurantReviews = new ReviewsFragment();

        phone = getIntent().getStringExtra("restaurant_phone");

        //Pass the phone number to the adapters
        Bundle data = new Bundle();//bundle instance
        data.putString("phone", phone);//string to pass with a key value
        data.putString("fullName", RestaurantName);
        restaurantMenu.setArguments(data);//Set bundle data to fragment
        restaurantReviews.setArguments(data);

        adapter.addFragment(restaurantMenu,"Menu");
        adapter.addFragment(restaurantReviews,"Reviews");
        viewPager.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            providerFavs.removeEventListener(providerFavsListener);
            myLocationRef.removeEventListener(mylocationListener);
            restaurantLocationRef.removeEventListener(restaurantLocationListener);
        } catch (Exception e){

        }
    }
}
