package com.malcolmmaima.dishi.View.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.malcolmmaima.dishi.R;

public class FragmentFollowing extends Fragment {

    String phone;

    public FragmentFollowing() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_following, container, false);

        phone = getArguments().getString("phone");
        Toast.makeText(getContext(), "phone: " + phone, Toast.LENGTH_SHORT).show();

        return view;
    }
}
