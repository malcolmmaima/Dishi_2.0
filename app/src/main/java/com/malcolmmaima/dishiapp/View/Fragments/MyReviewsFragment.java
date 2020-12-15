package com.malcolmmaima.dishiapp.View.Fragments;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

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
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Model.StatusUpdateModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Adapter.ReviewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyReviewsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "MyReviewsFragment";
    List<StatusUpdateModel> statusUpdates;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference  myReviewsRef;
    ValueEventListener myListener;
    FirebaseUser user;
    View rootView;

    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    View v;
    LinearLayoutManager layoutmanager;

    public static MyReviewsFragment newInstance() {
        MyReviewsFragment fragment = new MyReviewsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_my_reviews, container, false);
        rootView = v.findViewById(R.id.activity_main);

        recyclerview = v.findViewById(R.id.rview);
        //recyclerview.setNestedScrollingEnabled(false);

        icon = v.findViewById(R.id.noPostsIcon);
        emptyTag = v.findViewById(R.id.empty_tag);
        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this::fetchReviews);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);



        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        myReviewsRef = FirebaseDatabase.getInstance().getReference("reviews/"+myPhone);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                mSwipeRefreshLayout.setRefreshing(true);
                fetchReviews();
            }
        });

        return  v;
    }

    private void fetchReviews() {
        //Fetch the updates from status_updates node
        myReviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mSwipeRefreshLayout.setRefreshing(true);
                statusUpdates = new ArrayList<>();
                for(DataSnapshot updates : dataSnapshot.getChildren()){
                    StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                    statusUpdateModel.key = updates.getKey();
                    statusUpdates.add(statusUpdateModel);
                }

                try {
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (!statusUpdates.isEmpty()) {
                        emptyTag.setVisibility(View.GONE);
                        icon.setVisibility(View.GONE);
                        recyclerview.setVisibility(View.VISIBLE);
                        Collections.reverse(statusUpdates);
                        recyclerview.setVisibility(View.VISIBLE);
                        ReviewAdapter recycler = new ReviewAdapter(getContext(), statusUpdates);
                        recyclerview.setLayoutManager(layoutmanager);

                        recycler.notifyDataSetChanged();
                        recyclerview.setAdapter(recycler);
                    } else {
                        emptyTag.setText("NO REVIEWS");
                        emptyTag.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.VISIBLE);
                        recyclerview.setVisibility(View.GONE);
                    }
                }

                catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                    mSwipeRefreshLayout.setRefreshing(false);
                    emptyTag.setVisibility(View.VISIBLE);
                    emptyTag.setText("ERROR");
                    recyclerview.setVisibility(View.VISIBLE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Creating Method to get the selected image file Extension from File Path URI.
    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContext().getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(v != null){
            v = null;

            rootView = null;
            recyclerview.setAdapter(null);
            layoutmanager = null;
        }
    }


    @Override
    public void onRefresh() {
        fetchReviews();
    }
}