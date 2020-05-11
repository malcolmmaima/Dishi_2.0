package com.malcolmmaima.dishi.Controller.Utils;

import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AppLifecycleObserver implements LifecycleObserver {

    FirebaseUser user;
    FirebaseAuth mAuth;
    DatabaseReference myRef;
    String myPhone;

    public static final String TAG = AppLifecycleObserver.class.getName();

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        //run the code we need
        //lockApp(false);
        Log.d(TAG, "onEnterForeground: true");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        //run the code we need
        lockApp(true);
        Log.d(TAG, "onEnterBackground: true");
    }


    void lockApp(Boolean lockedApp){
        //get auth state
        mAuth = FirebaseAuth.getInstance();
        //User is logged in
        if(mAuth.getInstance().getCurrentUser() != null) {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            myRef.child("appLocked").setValue(lockedApp);
        }
    }
}
