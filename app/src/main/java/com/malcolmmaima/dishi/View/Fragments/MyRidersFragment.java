package com.malcolmmaima.dishi.View.Fragments;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.AddRider;
import com.malcolmmaima.dishi.View.Activities.LocationSettings;
import com.malcolmmaima.dishi.View.Adapter.MyOrdersAdapter;
import com.malcolmmaima.dishi.View.Adapter.MyRidersAdapter;
import com.malcolmmaima.dishi.View.Adapter.OrdersAdapter;

import java.util.ArrayList;
import java.util.List;

public class MyRidersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    List<UserModel> riders = new ArrayList<>();
    RecyclerView recyclerview;
    String myPhone;
    ImageButton addRider;

    DatabaseReference myRiders, riderDetailsRef;
    FirebaseDatabase db;
    FirebaseUser user;

    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;


    public static RiderFragment newInstance() {
        RiderFragment fragment = new RiderFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_my_riders, container, false);

        riders.clear();

        addRider = v.findViewById(R.id.addRider);
        icon = v.findViewById(R.id.riderIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        myRiders = db.getReference("my_riders/"+myPhone);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                mSwipeRefreshLayout.setRefreshing(true);
                fetchRiders();
            }
        });



        addRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(getContext(), AddRider.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(getContext(), R.anim.animation,R.anim.animation2).toBundle();
                startActivity(slideactivity, bndlanimation);
            }
        });



        return  v;
    }

    private void fetchRiders() {
        riders.clear();
        myRiders.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                /**
                 * Check to see if my rider node exists for my account
                 */
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    riders = new ArrayList<>();
                    MyRidersAdapter recycler = new MyRidersAdapter(getContext(), riders);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                }

                else {
                    for(final DataSnapshot rider : dataSnapshot.getChildren()){

                        riderDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+rider.getKey());

                        riderDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                mSwipeRefreshLayout.setRefreshing(false);
                                UserModel user = dataSnapshot.getValue(UserModel.class);
                                user.setPhone(rider.getKey());
                                riders.add(user);

                                if (!riders.isEmpty()) {
                                    //Collections.reverse(orders);
                                    MyRidersAdapter recycler = new MyRidersAdapter(getContext(), riders);
                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                    recyclerview.setLayoutManager(layoutmanager);
                                    recyclerview.setItemAnimator(new DefaultItemAnimator());

                                    recycler.notifyDataSetChanged();

                                    recyclerview.getItemAnimator().setAddDuration(200);
                                    recyclerview.getItemAnimator().setRemoveDuration(200);
                                    recyclerview.getItemAnimator().setMoveDuration(200);
                                    recyclerview.getItemAnimator().setChangeDuration(200);

                                    recyclerview.setAdapter(recycler);
                                    emptyTag.setVisibility(View.INVISIBLE);
                                    icon.setVisibility(View.INVISIBLE);
                                } else {
//                                        progressDialog.dismiss();
                                    MyRidersAdapter recycler = new MyRidersAdapter(getContext(), riders);
                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                    recyclerview.setLayoutManager(layoutmanager);
                                    recyclerview.setItemAnimator(new DefaultItemAnimator());
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

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRefresh() {
        fetchRiders();
    }

}
