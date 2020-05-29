package com.malcolmmaima.dishi.View.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.SearchActivity;
import com.malcolmmaima.dishi.View.Adapter.NewsFeedAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class FragmentSearchPosts extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    String TAG = "FragmentSearchFood";
    RecyclerView recyclerview;
    MyTextView_Roboto_Regular emptyTag;
    SwipeRefreshLayout mSwipeRefreshLayout;
    String searchValue, myPhone;
    FirebaseUser user;
    DatabaseReference followingRef, postsRef;
    int searchCap;

    public static FragmentSearchPosts newInstance() {
        FragmentSearchPosts fragment = new FragmentSearchPosts();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_posts, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber();
        searchCap = 100;

        SearchActivity activity = (SearchActivity) getActivity();
        searchValue = activity.getSearchValue();

        recyclerview = view.findViewById(R.id.rview);
        emptyTag = view.findViewById(R.id.empty_tag);

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

                searchPosts(searchValue);
            }
        });

        return view;
    }

    private void searchPosts(String searchValue) {
        List<StatusUpdateModel> statusUpdates;

        if(isStringNullOrWhiteSpace(searchValue)){
            emptyTag.setText("Type something");
            emptyTag.setVisibility(VISIBLE);
        } else {
            statusUpdates = new ArrayList<>();
            mSwipeRefreshLayout.setRefreshing(true);
            followingRef = FirebaseDatabase.getInstance().getReference("following/"+myPhone);
            postsRef = FirebaseDatabase.getInstance().getReference("posts");

            postsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot users : dataSnapshot.getChildren()){

                        //Get user details
                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users/"+users.getKey());
                        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserModel userModel = dataSnapshot.getValue(UserModel.class);

                                //if account type is private check to see if i follow them
                                if(userModel.getAccountPrivacy().equals("private")){
                                    followingRef.child(users.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists()){
                                                //I do follow them so show post in search
                                                for(DataSnapshot updates : users.getChildren()){
                                                    try {
                                                        StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                                                        statusUpdateModel.key = updates.getKey();
                                                        statusUpdateModel.type = "searchPost";
                                                        if (statusUpdateModel.getStatus().toLowerCase().contains(searchValue.toLowerCase())) {
                                                            if(statusUpdates.size() < searchCap){
                                                                statusUpdates.add(statusUpdateModel);
                                                            }
                                                        }

                                                        try {
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            if (!statusUpdates.isEmpty()) {

                                                                //Sort by most recent (based on timeStamp)
                                                                Collections.reverse(statusUpdates);
                                                                try {
                                                                    Collections.sort(statusUpdates, (update1, update2) -> (update2.getTimePosted().compareTo(update1.getTimePosted())));
                                                                } catch (Exception e){
                                                                    Log.e(TAG, "onDataChange: ", e);
                                                                }

                                                                emptyTag.setVisibility(View.GONE);
                                                                recyclerview.setVisibility(View.VISIBLE);
                                                                recyclerview.setVisibility(View.VISIBLE);
                                                                NewsFeedAdapter recycler = new NewsFeedAdapter(getContext(), statusUpdates);
                                                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                                recyclerview.setLayoutManager(layoutmanager);

                                                                recycler.notifyDataSetChanged();
                                                                recyclerview.setAdapter(recycler);
                                                            } else {
                                                                mSwipeRefreshLayout.setRefreshing(false);
                                                                recyclerview.setVisibility(INVISIBLE);
                                                                emptyTag.setVisibility(VISIBLE);
                                                                recyclerview.setVisibility(View.GONE);
                                                                emptyTag.setText("Nothing found");
                                                            }
                                                        }

                                                        catch (Exception e){
                                                            Log.e(TAG, "onDataChange: ", e);
                                                        }

                                                    } catch (Exception e){
                                                        Log.e(TAG, "onDataChange: ", e);
                                                    }
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }

                                //account is public so show posts in search
                                else {
                                    for(DataSnapshot updates : users.getChildren()){
                                        try {
                                            StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                                            statusUpdateModel.key = updates.getKey();
                                            statusUpdateModel.type = "searchPost";

                                            if (statusUpdateModel.getStatus().toLowerCase().contains(searchValue.toLowerCase())) {
                                                if(statusUpdates.size() < searchCap){
                                                    statusUpdates.add(statusUpdateModel);
                                                }
                                            }

                                            try {
                                                mSwipeRefreshLayout.setRefreshing(false);
                                                if (!statusUpdates.isEmpty()) {

                                                    //Sort by most recent (based on timeStamp)
                                                    Collections.reverse(statusUpdates);
                                                    try {
                                                        Collections.sort(statusUpdates, (update1, update2) -> (update2.getTimePosted().compareTo(update1.getTimePosted())));
                                                    } catch (Exception e){
                                                        Log.e(TAG, "onDataChange: ", e);
                                                    }

                                                    emptyTag.setVisibility(View.GONE);
                                                    recyclerview.setVisibility(View.VISIBLE);
                                                    NewsFeedAdapter recycler = new NewsFeedAdapter(getContext(), statusUpdates);
                                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                    recyclerview.setLayoutManager(layoutmanager);

                                                    recycler.notifyDataSetChanged();
                                                    recyclerview.setAdapter(recycler);
                                                } else {
                                                    mSwipeRefreshLayout.setRefreshing(false);
                                                    recyclerview.setVisibility(INVISIBLE);
                                                    emptyTag.setVisibility(VISIBLE);
                                                    recyclerview.setVisibility(View.GONE);
                                                    emptyTag.setText("Nothing found");
                                                }
                                            }

                                            catch (Exception e){
                                                Log.e(TAG, "onDataChange: ", e);
                                            }

                                        } catch (Exception e){
                                            Log.e(TAG, "onDataChange: ", e);
                                        }
                                    }
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

    @Override
    public void onRefresh() {
        searchPosts(searchValue);
    }
}
