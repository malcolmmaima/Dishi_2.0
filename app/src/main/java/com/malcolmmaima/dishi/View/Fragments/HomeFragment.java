package com.malcolmmaima.dishi.View.Fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.NewsFeedAdapter;
import com.malcolmmaima.dishi.View.Adapter.StatusUpdateAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    String TAG = "HomeFragment";
    List<StatusUpdateModel> statusUpdates;
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference followingRef;
    FirebaseUser user;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;


    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_home, container, false);
        progressDialog = new ProgressDialog(getContext());

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        followingRef = FirebaseDatabase.getInstance().getReference("following/"+myPhone);

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);

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
                loadNewsFeed();

            }
        });


        return  v;
    }

    private void loadNewsFeed() {
        mSwipeRefreshLayout.setRefreshing(true);
        //loop through my 'following' node and get users, vendors that i follow
        followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                statusUpdates = new ArrayList<>();
                for(DataSnapshot following : dataSnapshot.getChildren()){
                    //for each 'following' user ... go to their posts node and fetch status updates
                    DatabaseReference newsFeedPosts = FirebaseDatabase.getInstance().getReference("posts/"+following.getKey());
                    newsFeedPosts.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            mSwipeRefreshLayout.setRefreshing(true);
                            for(DataSnapshot updates : dataSnapshot.getChildren()){
                                StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                                statusUpdateModel.key = updates.getKey();
                                statusUpdates.add(statusUpdateModel);
                            }

                            try {
                                mSwipeRefreshLayout.setRefreshing(false);
                                if (!statusUpdates.isEmpty()) {

                                    //Sort by most recent (based on timeStamp)
                                    try {
                                        Collections.sort(statusUpdates, (update1, update2) -> (update2.getTimePosted().compareTo(update1.getTimePosted())));
                                    } catch (Exception e){}

                                    icon.setVisibility(View.GONE);
                                    recyclerview.setVisibility(View.VISIBLE);
                                    recyclerview.setVisibility(View.VISIBLE);
                                    NewsFeedAdapter recycler = new NewsFeedAdapter(getContext(), statusUpdates);
                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                    recyclerview.setLayoutManager(layoutmanager);

                                    recycler.notifyDataSetChanged();
                                    recyclerview.setAdapter(recycler);
                                } else {
                                    icon.setVisibility(View.VISIBLE);
                                    recyclerview.setVisibility(View.GONE);
                                }
                            }

                            catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                                recyclerview.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "onCancelled: ", databaseError.toException());
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
    public void onRefresh() {
        loadNewsFeed();
    }
}