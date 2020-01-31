package com.malcolmmaima.dishi.View.Fragments;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.LocationSettings;

public class OrdersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;
    Switch liveStatus;
    ImageView liveStatusIcon;
    TextView liveTitle;
    CardView live;

    DatabaseReference dbRef;
    FirebaseDatabase db;
    FirebaseUser user;

    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;

    public static OrdersFragment newInstance() {
        OrdersFragment fragment = new OrdersFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_orders, container, false);
        progressDialog = new ProgressDialog(getContext());

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();

        dbRef = db.getReference("users/"+myPhone);

        //Our Live Status Switch
        liveTitle = v.findViewById(R.id.liveTitle);
        liveStatusIcon = v.findViewById(R.id.liveStatus);
        liveStatus = v.findViewById(R.id.switch1);
        live = v.findViewById(R.id.card_view);

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);

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
                // Fetching data from server

            }
        });

        liveTitle.setText("loading...");
        dbRef.child("liveStatus").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                try {
                    liveTitle.setText("Live");
                    Boolean live = dataSnapshot.getValue(Boolean.class);
                    if(live == true){
                        liveStatus.setChecked(true);
                        liveStatusIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                    }

                    if(live == false){
                        liveStatus.setChecked(false);
                        liveStatusIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
                    }

                    if(live == null){
                        dbRef.child("liveStatus").setValue(false);
                    }

                } catch (Exception e){
                    //Node doesn't exist
                    liveTitle.setText("Go live...");

                    /**
                     * This exception means that this is a fresh account which translates to the user not having set location settings
                     * thus lets set "live" as the default location type "users/locationType"
                     *
                    */
                    dbRef.child("locationType").setValue("live");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                liveTitle.setText("error...");
            }

        });

        liveStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true){
                    final AlertDialog goLive = new AlertDialog.Builder(getContext())
                            .setMessage("Restaurant Go Live?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dbRef.child("liveStatus").setValue(true);
                                    liveStatus.setChecked(true);
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dbRef.child("liveStatus").setValue(false);
                                    liveStatus.setChecked(false);
                                }
                            })

                            .create();
                    goLive.show();
                }

                else {
                    dbRef.child("liveStatus").setValue(isChecked);
                }

            }
        });

        live.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog settings = new AlertDialog.Builder(getContext())
                        .setMessage("Change location settings?")
                        //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                        .setCancelable(false)
                        //set three option buttons
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent slideactivity = new Intent(getContext(), LocationSettings.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Bundle bndlanimation =
                                        ActivityOptions.makeCustomAnimation(getContext(), R.anim.animation,R.anim.animation2).toBundle();
                                startActivity(slideactivity, bndlanimation);
                            }
                        })//setPositiveButton

                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Do nothing
                            }
                        })

                        .create();
                settings.show();

            }
        });

        return  v;
    }

    @Override
    public void onRefresh() {

    }
}