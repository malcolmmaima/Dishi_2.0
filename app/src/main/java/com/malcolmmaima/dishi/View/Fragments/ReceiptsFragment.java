package com.malcolmmaima.dishi.View.Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
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
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.ReceiptModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.MenuAdapter;
import com.malcolmmaima.dishi.View.Adapter.ReceiptAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReceiptsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    String TAG = "ReceiptsFragment";
    List<ReceiptModel> list;
    RecyclerView recyclerview;
    String myPhone;
    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinearLayoutManager layoutmanager;

    DatabaseReference dbRef, receiptsRef;
    FirebaseDatabase db;
    FirebaseUser user;
    View v;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static ReceiptsFragment newInstance() {
        ReceiptsFragment fragment = new ReceiptsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_receipts, container, false);

        int permission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    getActivity(),
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference(myPhone);
        receiptsRef = db.getReference("receipts/"+ myPhone);


        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);
        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);

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
                fetchReceipts();

            }
        });

        return  v;
    }

    @Override
    public void onRefresh() {
        fetchReceipts();
    }

    private void fetchReceipts() {
        receiptsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                list = new ArrayList<>();

                // StringBuffer stringbuffer = new StringBuffer();
                for(DataSnapshot dataSnapshot1 :dataSnapshot.getChildren()){
                    try {
                        ReceiptModel receipt = dataSnapshot1.getValue(ReceiptModel.class);
                        receipt.key = dataSnapshot1.getKey();
                        list.add(receipt);
                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                    //progressDialog.dismiss();
                }

                if(!list.isEmpty()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    Collections.reverse(list);
                    ReceiptAdapter recycler = new ReceiptAdapter(getContext(),list);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recycler.notifyDataSetChanged();
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.INVISIBLE);
                    icon.setVisibility(View.INVISIBLE);
                }

                else {

                    mSwipeRefreshLayout.setRefreshing(false);

                    ReceiptAdapter recycler = new ReceiptAdapter(getContext(),list);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                //  Log.w(TAG, "Failed to read value.", error.toException());

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(v != null){
            v = null;
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