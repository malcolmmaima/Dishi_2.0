package com.malcolmmaima.dishi.View.Activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.FollowerFollowingAdapter;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class SearchActivity extends AppCompatActivity {

    ProgressBar progressBar;
    EditText searchWord;
    TextView emptyTag;
    RecyclerView recyclerView;


    RadioButton usersRd, foodRd, restaurantsRd;
    RadioGroup searchPreference;

    String myPhone, selectedPreference;
    private FirebaseAuth mAuth;

    DatabaseReference databaseReference;

    List<UserModel> users;
    List <ProductDetails> foods;
    List <UserModel> restaurants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        searchWord = findViewById(R.id.edtSearch);
        searchWord.setEnabled(false);
        emptyTag = findViewById(R.id.empty_tag);
        recyclerView = findViewById(R.id.rview);

        searchPreference = findViewById(R.id.searchPreference);
        usersRd = findViewById(R.id.usersRd);
        foodRd = findViewById(R.id.foodRd);
        restaurantsRd = findViewById(R.id.restaurantsRd);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        databaseReference = FirebaseDatabase.getInstance().getReference();

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        searchPreference.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                //Toast.makeText(SearchActivity.this, "ID: " + i, Toast.LENGTH_SHORT).show();

                int searchPrf = searchPreference.getCheckedRadioButtonId();

                // find the radio button by returned id
                RadioButton radioButton = findViewById(searchPrf);

                selectedPreference = radioButton.getText().toString();
                //Toast.makeText(SearchActivity.this, "Selected: " + selectedPreference, Toast.LENGTH_SHORT).show();
                searchWord.setEnabled(true);
                recyclerView.setVisibility(View.GONE);
                if(selectedPreference.equals("Users")){
                    emptyTag.setText("Users");
                    emptyTag.setVisibility(VISIBLE);
                }

                if(selectedPreference.equals("Food")){
                    emptyTag.setText("Food");
                    emptyTag.setVisibility(VISIBLE);
                }

                if(selectedPreference.equals("Restaurants")){
                    emptyTag.setText("Restaurants");
                    emptyTag.setVisibility(VISIBLE);
                }

            }
        });

        searchWord.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {


            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Toast.makeText(SearchActivity.this, "typing...", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //Toast.makeText(SearchActivity.this, "done typing", Toast.LENGTH_SHORT).show();

                String word = editable.toString();

                if(isStringNullOrWhiteSpace(word)){
                    progressBar.setVisibility(View.GONE);
                    emptyTag.setText("Type something");
                    emptyTag.setVisibility(VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    //Toast.makeText(SearchActivity.this, "empty", Toast.LENGTH_SHORT).show();
                } else {
                    searchDB(word, selectedPreference);
                }

            }
        });
    }

    private void searchDB(final String word, String selectedPreference) {

        if(selectedPreference.equals("Users")){
            //Toast.makeText(this, "Search users...", Toast.LENGTH_SHORT).show();
            databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        users = new ArrayList<>();
                        //Loop through all phone numbers in the system and get user details
                        for(DataSnapshot phones : dataSnapshot.getChildren()){
                            //Log.d("UsersFound", "found: "+phones.getKey());
                            try {
                                UserModel dishiUser = phones.getValue(UserModel.class);
                                dishiUser.setPhone(phones.getKey());
                                //Toast.makeText(SearchActivity.this, "phone: " + phones.getKey(), Toast.LENGTH_SHORT).show();
                                //search if character is contained in returned results
                                String name = dishiUser.getFirstname()+" "+dishiUser.getLastname();
                                if(!myPhone.equals(phones.getKey())){
                                    if (name.toLowerCase().contains(word.toLowerCase())) {
                                        users.add(dishiUser);
                                    }
                                    else if(word.toLowerCase().contains(name.toLowerCase())){
                                        users.add(dishiUser);
                                    }
                                    //search if word is equal to user name object
                                    else if(word.toLowerCase() == name.toLowerCase()){
                                        users.add(dishiUser);
                                    }
                                    else if(name.toLowerCase().equals(word.toLowerCase())) {
                                        users.add(dishiUser);
                                    }
                                }

                            } catch (Exception e){
                                progressBar.setVisibility(View.GONE);
                                emptyTag.setVisibility(VISIBLE);
                                emptyTag.setText("TRY AGAIN");
                                recyclerView.setVisibility(View.GONE);
                            }

                            if (!users.isEmpty()) {
                                recyclerView.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                                recyclerView.setVisibility(VISIBLE);
                                FollowerFollowingAdapter recycler = new FollowerFollowingAdapter(SearchActivity.this, users);
                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(SearchActivity.this);
                                recyclerView.setLayoutManager(layoutmanager);
                                //recyclerView.setItemAnimator(new SlideInLeftAnimator());

                                recycler.notifyDataSetChanged();

//                                recyclerView.getItemAnimator().setAddDuration(1000);
//                                recyclerView.getItemAnimator().setRemoveDuration(1000);
//                                recyclerView.getItemAnimator().setMoveDuration(1000);
//                                recyclerView.getItemAnimator().setChangeDuration(1000);

                                recyclerView.setAdapter(recycler);
                                emptyTag.setVisibility(View.GONE);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                recyclerView.setVisibility(INVISIBLE);
                                emptyTag.setVisibility(VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                                emptyTag.setText("Nothing found");
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
        }

        /** if(selectedPreference.equals("Food")){

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                        foods = new ArrayList<>();

                        //So first we loop through the users in the firebase db
                        for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                            //afterwards we check if that user has a 'mymenu' child node, if so loop through it and show the products
                            //NOTE: only restaurant/provider accounts have the 'mymenu', so essentially we are fetching restaurant menus into our customers fragment via the adapter
                            for (DataSnapshot dataSnapshot2 : dataSnapshot1.child("mymenu").getChildren()) {

                                try {

                                    final ProductDetails orderDetails = dataSnapshot2.getValue(ProductDetails.class);
                                    //Toast.makeText(getContext(), "mymenu: " + dataSnapshot2.getKey(), Toast.LENGTH_SHORT).show();
//                                    orderDetails.providerNumber = dataSnapshot1.getKey();
//                                    orderDetails.providerName = dataSnapshot1.child("name").getValue().toString();
                                    orderDetails.key = dataSnapshot2.getKey(); //we'll use this to prevent duplicates

                                    if (orderDetails.getName().toLowerCase().contains(word.toLowerCase())) {
                                        foods.add(orderDetails);
                                    }

                                    else if(word.toLowerCase().contains(orderDetails.getName().toLowerCase())){
                                        foods.add(orderDetails);
                                    }

                                    //search if word is equal to user name object
                                    else if(word.toLowerCase() == orderDetails.getName().toLowerCase()){
                                        foods.add(orderDetails);
                                    }
                                    else if(orderDetails.getName().toLowerCase().equals(word.toLowerCase())) {
                                        foods.add(orderDetails);
                                    }

                            } catch (Exception e){
                                progressBar.setVisibility(View.GONE);
                                emptyTag.setVisibility(VISIBLE);
                                emptyTag.setText("TRY AGAIN");
                                recyclerView.setVisibility(View.GONE);
                            }

                                if (!foods.isEmpty()) {
                                    recyclerView.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.GONE);
                                    recyclerView.setVisibility(VISIBLE);
                                    ProductAdapter recycler = new ProductAdapter(SearchActivity.this, foods);
                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(SearchActivity.this);
                                    recyclerView.setLayoutManager(layoutmanager);
                                    //recyclerView.setItemAnimator(new SlideInLeftAnimator());

                                    recycler.notifyDataSetChanged();

//                                    recyclerView.getItemAnimator().setAddDuration(1000);
//                                    recyclerView.getItemAnimator().setRemoveDuration(1000);
//                                    recyclerView.getItemAnimator().setMoveDuration(1000);
//                                    recyclerView.getItemAnimator().setChangeDuration(1000);

                                    recyclerView.setAdapter(recycler);
                                    emptyTag.setVisibility(View.GONE);
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    recyclerView.setVisibility(INVISIBLE);
                                    emptyTag.setVisibility(VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                    emptyTag.setText("Nothing found");
                                }
                            }
                        }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } */

        /** if(selectedPreference.equals("Restaurants")){
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    restaurants = new ArrayList<>();
                    //Loop through all users
                    for (DataSnapshot users : dataSnapshot.getChildren()){

                        //loop through each user and find out if they're a restaurant
                        for(DataSnapshot restaurant : users.getChildren()){
                            if(restaurant.getKey().equals("account_type")){
                                String accType = restaurant.getValue(String.class);
                                if(accType.equals("2")){

                                    try {
                                        //Assign details to our Model
                                        final UserModel restaurantDetails = users.getValue(UserModel.class);
                                        //restaurantDetails.phone = users.getKey().toString();
                                        //Toast.makeText(SearchActivity.this,
                                        //        "details: " + restaurantDetails.getName(), Toast.LENGTH_SHORT).show();

                                        if (restaurantDetails.getName().toLowerCase().contains(word.toLowerCase())) {
                                            restaurants.add(restaurantDetails);
                                        }

                                        else if(word.toLowerCase().contains(restaurantDetails.getName().toLowerCase())){
                                            restaurants.add(restaurantDetails);
                                        }

                                        //search if word is equal to user name object
                                        else if(word.toLowerCase() == restaurantDetails.getName().toLowerCase()){
                                            restaurants.add(restaurantDetails);
                                        }
                                        else if(restaurantDetails.getName().toLowerCase().equals(word.toLowerCase())) {
                                            restaurants.add(restaurantDetails);
                                        }

                                    } catch (Exception e){
                                        progressBar.setVisibility(View.GONE);
                                        emptyTag.setVisibility(VISIBLE);
                                        emptyTag.setText("TRY AGAIN");
                                        recyclerView.setVisibility(View.GONE);
                                    }

                                    if (!restaurants.isEmpty()) {
                                        recyclerView.setVisibility(View.VISIBLE);
                                        progressBar.setVisibility(View.GONE);
                                        recyclerView.setVisibility(VISIBLE);
                                        RestaurantAdapter recycler = new RestaurantAdapter(SearchActivity.this, restaurants);
                                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(SearchActivity.this);
                                        recyclerView.setLayoutManager(layoutmanager);
                                        recyclerView.setItemAnimator(new SlideInLeftAnimator());

                                        recycler.notifyDataSetChanged();

                                        recyclerView.getItemAnimator().setAddDuration(1000);
                                        recyclerView.getItemAnimator().setRemoveDuration(1000);
                                        recyclerView.getItemAnimator().setMoveDuration(1000);
                                        recyclerView.getItemAnimator().setChangeDuration(1000);

                                        recyclerView.setAdapter(recycler);
                                        emptyTag.setVisibility(View.GONE);
                                    } else {
                                        progressBar.setVisibility(View.GONE);
                                        recyclerView.setVisibility(INVISIBLE);
                                        emptyTag.setVisibility(VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                        emptyTag.setText("Nothing found");
                                    }
                                    //filter duplicates from the list

                                }
                            }

                        }

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } */
    }

    public static boolean isStringNullOrWhiteSpace(String value) {
        if (value == null) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
