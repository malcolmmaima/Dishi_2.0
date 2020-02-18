package com.malcolmmaima.dishi.View.Activities;

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

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ViewPagerAdapter;
import com.malcolmmaima.dishi.View.Fragments.MenuFragment;
import com.malcolmmaima.dishi.View.Fragments.ReviewsFragment;
import com.malcolmmaima.dishi.View.Fragments.ViewRestaurantMenuFragment;
import com.squareup.picasso.Picasso;

public class ViewRestaurant extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference restaurantRef, myFavourites, providerFavs;
    ImageView coverImageView, favourite, callBtn, shareRest;
    TextView restaurantName, distAway, likes;
    String RestaurantName, phone;

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

        favourite = findViewById(R.id.likeImageView);
        callBtn = findViewById(R.id.callRestaurant);
        shareRest = findViewById(R.id.shareImageView);
        coverImageView = findViewById(R.id.coverImageView);
        restaurantName = findViewById(R.id.titleTextView);
        favourite.setTag(R.drawable.ic_like);

        distAway = findViewById(R.id.distanceAway);
        likes = findViewById(R.id.likesTotal);

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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String myPhone = user.getPhoneNumber(); //Current logged in user phone number

        /**
         * Receive values from Restaurant adapter
         */
        final String restaurantPhone = getIntent().getStringExtra("restaurant_phone");
        final Double distanceAway = getIntent().getDoubleExtra("distance", 0.0);

        String profilePic = getIntent().getStringExtra("profilePic");

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
            //Load food image
            Picasso.with(ViewRestaurant.this).load(profilePic).fit().centerCrop()
                    //.placeholder(R.drawable.shop)
                    .error(R.drawable.shop)
                    .into(coverImageView);
        } catch (Exception e){

        }

        restaurantRef = FirebaseDatabase.getInstance().getReference("users/"+restaurantPhone);
        myFavourites = FirebaseDatabase.getInstance().getReference("my_favourites/"+myPhone);
        providerFavs = FirebaseDatabase.getInstance().getReference("restaurant_favourites/"+ restaurantPhone);

        providerFavs.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int likesTotal = (int) dataSnapshot.getChildrenCount();
                    likes.setText(""+likesTotal);
                } catch (Exception e){

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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

        //Fetch the restauant basic info
        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserModel restaurantDetails = dataSnapshot.getValue(UserModel.class);
                //restaurantDetails.phone = restaurantPhone;

                try {
                    RestaurantName = restaurantDetails.getFirstname() + " " + restaurantDetails.getLastname();
                    restaurantName.setText(RestaurantName);
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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
                Toast.makeText(ViewRestaurant.this, "Share!", Toast.LENGTH_SHORT).show();
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
                            //Toast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
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
                            //Toast.makeText(context,restaurantDetails.getName()+" removed from favourites",Toast.LENGTH_SHORT).show();
                        }
                    });

                }

            }
        });


    }

    private void setupViewPager(ViewPager viewPager)
    {
        Double distance =  getIntent().getDoubleExtra("distance", 0.0);
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        restaurantMenu = new ViewRestaurantMenuFragment();
        restaurantReviews = new ReviewsFragment();

        phone = getIntent().getStringExtra("restaurant_phone");

        //Pass the phone number to the adapters
        Bundle data = new Bundle();//bundle instance
        data.putString("phone", phone);//string to pass with a key value
        data.putDouble("distance", distance);
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

    }
}
