package com.malcolmmaima.dishi.Controller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.auth.User;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.SplashActivity;
import com.malcolmmaima.dishi.View.Activities.ViewMyOrders;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Random;

import io.fabric.sdk.android.services.common.SafeToast;

/**
 * https://androidwave.com/foreground-service-android-example/
 */

public class ForegroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    DatabaseReference databaseReference;
    ValueEventListener databaseListener, myOrdersListener;
    String myPhone;
    FirebaseUser user;
    UserModel myUserDetails;

    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        databaseReference = FirebaseDatabase.getInstance().getReference();
        try {
            String title = intent.getStringExtra("title");
            String message = intent.getStringExtra("message");
        } catch (Exception e){}
        /**
         * Get logged in user details
         */
        databaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myUserDetails = dataSnapshot.getValue(UserModel.class);

                if(myUserDetails.getAccount_type().equals("1")){
                    startCustomerNotifications();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("users").child(myPhone).addValueEventListener(databaseListener);

        SafeToast.makeText(getApplicationContext(),"Notification started", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    private void startCustomerNotifications() {
        //Check order status

        final ArrayList<String> activeRestaurantOrders = new ArrayList<>();
        myOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    DatabaseReference [] activeRestaurantRef = new DatabaseReference[(int) dataSnapshot.getChildrenCount()];
                    ValueEventListener [] activeRestaurantListener = new ValueEventListener[(int) dataSnapshot.getChildrenCount()];
                    for(DataSnapshot providers : dataSnapshot.getChildren()){
                        String provider = providers.getKey();
                        activeRestaurantOrders.add(provider);
                    }

                    //make sure array list contains the phones of the active restaurant orders
                    if(!activeRestaurantOrders.isEmpty()){

                        //loop through all my active restaurant orders
                        for(int i=0; i<activeRestaurantOrders.size(); i++){

                            try {
                                activeRestaurantRef[i] = FirebaseDatabase.getInstance().getReference("orders/" + activeRestaurantOrders.get(i) + "/" + myPhone);
                                final int finalI = i;
                                activeRestaurantListener[i] = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        try {
                                            Boolean complete = dataSnapshot.child("completed").getValue(Boolean.class);
                                            SafeToast.makeText(getApplicationContext(), "complete: " + complete, Toast.LENGTH_SHORT).show();
                                            if (complete == true) {
                                                //Lets get the restaurant's name that will be passed in the notification intent
                                                databaseReference.child("users").child(activeRestaurantOrders.get(finalI)).child("firstname").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        String restaurantName = dataSnapshot.getValue(String.class);
                                                        String title = "Order Delivered";
                                                        String message = "Hi " + myUserDetails.getFirstname() + ", your order has arrived!";
                                                        sendOrderNotification("orderDelivered", title, message, ViewMyOrders.class, activeRestaurantOrders.get(finalI), restaurantName);
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });

                                            }
                                        } catch (Exception e) {
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                };
                                activeRestaurantRef[i].addValueEventListener(activeRestaurantListener[i]);

                                Log.d("TAG", "activeRestaurants(" + activeRestaurantOrders.size() + "): " + activeRestaurantOrders.get(i));

                                //Loop has reached the end
                                if (i == activeRestaurantOrders.size() - 1) {
                                    Log.d("TAG", "end of loop: i (" + i + ") == list (" + activeRestaurantOrders.size() + ")");
                                }
                            } catch (Exception e){}
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("my_orders").child(myPhone).addValueEventListener(myOrdersListener);
    }


    private void sendOrderNotification(String type, String title, String message, Class targetActivity, String restaurantPhone, String restaurantName){

        if(type.equals("orderDelivered")){
            Notification.Builder builder = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.logo_notification)
                    .setContentTitle(title)
                    .setContentText(message);

            NotificationManager manager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            Intent intent = new Intent(getApplicationContext(), targetActivity);
            intent.putExtra("phone", restaurantPhone);
            intent.putExtra("name", restaurantName);
            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(contentIntent);
            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.icon |= Notification.BADGE_ICON_LARGE;

            manager.notify(new Random().nextInt(), notification);
        }

        ////////////////////////////////////////////////
        /**
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.logo_notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        // Issue the notification.
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, notification);

        //startForeground(1, notification);
        //do heavy work on a background thread
        //stopSelf(); */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SafeToast.makeText(getApplicationContext(),"Notification stopped", Toast.LENGTH_SHORT).show();
        databaseReference.removeEventListener(databaseListener);
        databaseReference.child("my_orders").child(myPhone).removeEventListener(myOrdersListener);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
