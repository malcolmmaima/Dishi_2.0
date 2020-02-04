package com.malcolmmaima.dishi.View.Activities;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.CartAdapter;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyCart extends AppCompatActivity {

    List<ProductDetails> list;
    RecyclerView recyclerview;
    String myPhone;
    TextView emptyTag;
    AppCompatImageView icon;

    DatabaseReference myCartRef;
    FirebaseDatabase db;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cart);

        icon = findViewById(R.id.menuIcon);
        recyclerview = findViewById(R.id.rview);
        emptyTag = findViewById(R.id.empty_tag);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");

        /**
         * Initialize firebase database
         */
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        myCartRef = db.getReference("cart/"+myPhone);

        fetchCart();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void fetchCart() {
        myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list = new ArrayList<>();

                for(DataSnapshot cart : dataSnapshot.getChildren()){
                    ProductDetails product = cart.getValue(ProductDetails.class);
                    product.setKey(cart.getKey());
                    list.add(product);
                    //Toast.makeText(MyCart.this, "key: " +product.getKey(), Toast.LENGTH_SHORT).show();
                }

                if(!list.isEmpty()){

                    //mSwipeRefreshLayout.setRefreshing(false);
                    Collections.reverse(list);
                    CartAdapter recycler = new CartAdapter(MyCart.this,list);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(MyCart.this);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());

                    recycler.notifyDataSetChanged();

                    recyclerview.getItemAnimator().setAddDuration(200);
                    recyclerview.getItemAnimator().setRemoveDuration(200);
                    recyclerview.getItemAnimator().setMoveDuration(200);
                    recyclerview.getItemAnimator().setChangeDuration(200);

                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.INVISIBLE);
                    icon.setVisibility(View.INVISIBLE);
                }

                else {

                    //mSwipeRefreshLayout.setRefreshing(false);

                    CartAdapter recycler = new CartAdapter(MyCart.this,list);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(MyCart.this);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
