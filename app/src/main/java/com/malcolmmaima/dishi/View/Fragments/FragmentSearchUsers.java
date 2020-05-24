package com.malcolmmaima.dishi.View.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.NewSearchActivity;
import com.malcolmmaima.dishi.View.Adapter.FollowerFollowingAdapter;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.VISIBLE;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentSearchUsers extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    RecyclerView recyclerview;
    MyTextView_Roboto_Regular emptyTag;
    SwipeRefreshLayout mSwipeRefreshLayout;
    String searchValue, myPhone;
    FirebaseUser user;

    public static FragmentSearchUsers newInstance() {
        FragmentSearchUsers fragment = new FragmentSearchUsers();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_search_users, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber();
        //String searchString;
        //searchString = getArguments().getString("search");

        NewSearchActivity activity = (NewSearchActivity) getActivity();
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

                searchUsers(searchValue);
            }
        });

        return view;
    }

    private void searchUsers(String s) {
        if(isStringNullOrWhiteSpace(s)){
            emptyTag.setText("Type something");
            emptyTag.setVisibility(VISIBLE);
        } else {
            emptyTag.setText("NOT FOUND");
            mSwipeRefreshLayout.setRefreshing(true);
            DatabaseReference usersRef;
            usersRef = FirebaseDatabase.getInstance().getReference("users");
            List<UserModel> usersFound = new ArrayList<>();
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        mSwipeRefreshLayout.setRefreshing(false);
                        FollowerFollowingAdapter recycler = new FollowerFollowingAdapter(getContext(), usersFound);
                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                        recyclerview.setLayoutManager(layoutmanager);
                        recyclerview.setItemAnimator(new DefaultItemAnimator());
                        recyclerview.setAdapter(recycler);
                        emptyTag.setVisibility(View.VISIBLE);
                    } else {
                        for(DataSnapshot user_ : dataSnapshot.getChildren()){
                            UserModel userFound = user_.getValue(UserModel.class);
                            userFound.setPhone(user_.getKey());

                            String name = userFound.getFirstname()+" "+userFound.getLastname();
                            if(name.toLowerCase().contains(s.toLowerCase()) && !myPhone.equals(userFound.getPhone())){
                                usersFound.add(userFound);
                            }

                            if (!usersFound.isEmpty()) {
                                mSwipeRefreshLayout.setRefreshing(false);
                                //Collections.reverse(orders);
                                FollowerFollowingAdapter recycler = new FollowerFollowingAdapter(getContext(), usersFound);
                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                recyclerview.setLayoutManager(layoutmanager);
                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                recycler.notifyDataSetChanged();
                                recyclerview.setAdapter(recycler);
                                emptyTag.setVisibility(View.GONE);
                            } else {
                                mSwipeRefreshLayout.setRefreshing(false);
                                FollowerFollowingAdapter recycler = new FollowerFollowingAdapter(getContext(), usersFound);
                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                recyclerview.setLayoutManager(layoutmanager);
                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                recyclerview.setAdapter(recycler);
                                emptyTag.setVisibility(View.VISIBLE);
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
        searchUsers(searchValue);
    }
}
