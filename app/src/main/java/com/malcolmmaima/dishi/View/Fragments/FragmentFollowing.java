package com.malcolmmaima.dishi.View.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.NewChat;
import com.malcolmmaima.dishi.View.Adapter.FollowerFollowingAdapter;
import com.malcolmmaima.dishi.View.Adapter.NewChatAdapter;

import java.util.ArrayList;
import java.util.List;

public class FragmentFollowing extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    String phone;
    DatabaseReference followingRef;
    RecyclerView recyclerview;
    MyTextView_Roboto_Regular emptyTag;
    SwipeRefreshLayout mSwipeRefreshLayout;
    View view;
    LinearLayoutManager layoutmanager;

    public FragmentFollowing() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_following, container, false);

        phone = getArguments().getString("phone");

        recyclerview = view.findViewById(R.id.rview);
        emptyTag = view.findViewById(R.id.empty_tag);

        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
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
                fetchFollowing();
            }
        });

        return view;
    }

    private void fetchFollowing() {
        followingRef = FirebaseDatabase.getInstance().getReference("following/"+phone);
        List<UserModel> usersFollowing = new ArrayList<>();
        followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    FollowerFollowingAdapter recycler = new FollowerFollowingAdapter(getContext(), usersFollowing);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                } else {
                    for(DataSnapshot user : dataSnapshot.getChildren()){
                        DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+user.getKey());
                        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserModel userFound = dataSnapshot.getValue(UserModel.class);
                                userFound.setPhone(user.getKey());
                                usersFollowing.add(userFound);


                                if (!usersFollowing.isEmpty()) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    //Collections.reverse(orders);
                                    FollowerFollowingAdapter recycler = new FollowerFollowingAdapter(getContext(), usersFollowing);
                                    recyclerview.setLayoutManager(layoutmanager);
                                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                                    recycler.notifyDataSetChanged();
                                    recyclerview.setAdapter(recycler);
                                    emptyTag.setVisibility(View.GONE);
                                } else {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    FollowerFollowingAdapter recycler = new FollowerFollowingAdapter(getContext(), usersFollowing);
                                    recyclerview.setLayoutManager(layoutmanager);
                                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                                    recyclerview.setAdapter(recycler);
                                    emptyTag.setVisibility(View.VISIBLE);
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
        fetchFollowing();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(view != null){
            view = null;

            recyclerview.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    // no-op
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    recyclerview.setAdapter(null);
                    layoutmanager = null;
                }
            });
        }
    }
}
