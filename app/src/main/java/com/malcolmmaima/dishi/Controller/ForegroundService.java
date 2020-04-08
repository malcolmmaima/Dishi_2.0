package com.malcolmmaima.dishi.Controller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.auth.User;
import com.malcolmmaima.dishi.Model.ProductDetails;
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
 * https://developer.android.com/training/notify-user/build-notification
 */

public class ForegroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    DatabaseReference databaseReference, myUserDetailsRef;
    ValueEventListener databaseListener, myOrdersListener, myUserDetailsListener;
    String myPhone;
    FirebaseUser user;
    UserModel myUserDetails;
    String restaurantName, lastName;
    String lastFourDigits = "";     //substring containing last 4 characters

    ArrayList<String> restaurants = new ArrayList<>(); //I want to show the "item confirmed notification" only once thus an arraylist that keeps trach
    //of restaurant notifications. One notification for each confirmed order item(s) since the listener that calls this function of type "orderConfirmed"
    //fires up everytime a single item's value is changed. might find a better way to do this in the future :-)


    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            databaseReference = FirebaseDatabase.getInstance().getReference();
            myUserDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        } catch(Exception e){}

        try {
            String title = intent.getStringExtra("title");
            String message = intent.getStringExtra("message");
        } catch (Exception e){}
        /**
         * Get logged in user details
         */
        myUserDetailsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    myUserDetails = dataSnapshot.getValue(UserModel.class);
                    if (myUserDetails.getAccount_type().equals("1")) {
                        startCustomerNotifications();
                    }

                    if(myUserDetails.getAccount_type().equals("2")){
                        startRestaurantNotifications();
                    }
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        try {
            myUserDetailsRef.addValueEventListener(myUserDetailsListener);
        } catch (Exception e){}

        //SafeToast.makeText(getApplicationContext(),"Notification started", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    private void startRestaurantNotifications() {

    }

    private void startCustomerNotifications() {
        //Check order status

        final ArrayList<String> activeRestaurantOrders = new ArrayList<>();
        myOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot providers : dataSnapshot.getChildren()){
                        final String provider = providers.getKey();
                        activeRestaurantOrders.add(provider);
                        DatabaseReference activeRestaurantRef_ = FirebaseDatabase.getInstance().getReference("orders/"+provider+ "/" + myPhone);
                        activeRestaurantRef_.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                try {
                                    Boolean complete = dataSnapshot.child("completed").getValue(Boolean.class);
                                    /**
                                     * Now the challenge i'm facing with notifications is duplication of notifications
                                     * from a single trigger. To curb that i've decided to use the last 4 digits of the
                                     * restaurant's phone number as the notification id that way no duplicates. Not sure how
                                     * effective this is in the long runs as the app scales but meeh, you'll figure it out
                                     */
                                    if (provider.length() > 4)
                                    {
                                        lastFourDigits = provider.substring(provider.length() - 4);
                                    }
                                    int notifId =  Integer.parseInt(lastFourDigits); //new Random().nextInt();

                                    if (complete == true) {
                                        //Lets get the restaurant's name that will be passed in the notification intent
                                        databaseReference.child("users").child(provider).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                restaurants.add(provider);
                                                restaurantName = dataSnapshot.child("firstname").getValue(String.class);
                                                lastName = dataSnapshot.child("lastname").getValue(String.class);
                                                String title = "Order Delivered";
                                                String message = restaurantName + " " + lastName + " order delivered!";
                                                sendOrderNotification(notifId, "orderDelivered", title, message, ViewMyOrders.class, provider, restaurantName + " " + lastName);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                    }

                                } catch (Exception e){}
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("orders/"+provider+ "/"+myPhone+"/items");
                        itemsRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (provider.length() > 4)
                                {
                                    lastFourDigits = provider.substring(provider.length() - 4);
                                }
                                int notifId =  Integer.parseInt(lastFourDigits); //new Random().nextInt();


                                for(DataSnapshot snap : dataSnapshot.getChildren()){
                                    DatabaseReference item = FirebaseDatabase.getInstance().getReference("orders/"+provider+ "/"+myPhone+"/items/"+snap.getKey());
                                    item.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            ProductDetails prod = snap.getValue(ProductDetails.class);

                                            if(prod.getConfirmed() == true){
                                                databaseReference.child("users").child(prod.getOwner()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        restaurantName = dataSnapshot.child("firstname").getValue(String.class);
                                                        lastName = dataSnapshot.child("lastname").getValue(String.class);

                                                        String title = "Order Confirmed";
                                                        String message = restaurantName + " " + lastName +" confirmed order items";
                                                        sendOrderNotification(notifId+1, "orderConfirmed", title, message, ViewMyOrders.class, provider, restaurantName + " " + lastName);
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
        };
        databaseReference.child("my_orders").child(myPhone).addValueEventListener(myOrdersListener);
    }

    private void sendOrderNotification(int notifId, String type, String title, String message, Class targetActivity, String restaurantPhone, String restaurantName){

        if(type.equals("orderConfirmed") && !restaurants.contains(restaurantPhone)){
            Notification.Builder builder = null;
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                builder = new Notification.Builder(this)
                        .setSmallIcon(R.drawable.dish)
                        .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        .setContentTitle(title)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                        .setSound(soundUri)
                        .setContentText(message);
            }

            NotificationManager manager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("phone", restaurantPhone);
            intent.putExtra("name", restaurantName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, intent, 0);
            builder.setContentIntent(contentIntent);
            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.icon |= Notification.BADGE_ICON_LARGE;
            manager.notify(notifId, notification);

            restaurants.add(restaurantPhone); //Add the restaurant's phone to this list that we track to control number of notifications of this type
        }


        else if(type.equals("orderDelivered") && restaurants.contains(restaurantPhone)){
            Notification.Builder builder = null;
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                builder = new Notification.Builder(this)
                        .setSmallIcon(R.drawable.logo_notification)
                        .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        .setContentTitle(title)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                        .setSound(soundUri)
                        .setContentText(message);
            }

            NotificationManager manager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("phone", restaurantPhone);
            intent.putExtra("name", restaurantName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, intent, 0);
            builder.setContentIntent(contentIntent);
            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.icon |= Notification.BADGE_ICON_LARGE;
            manager.notify(notifId, notification);

            restaurants.remove(restaurantPhone);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        restaurants.clear(); //Clear the tracker used in our send notification function
        try {
            databaseReference.removeEventListener(databaseListener);
            databaseReference.child("my_orders").child(myPhone).removeEventListener(myOrdersListener);
        } catch(Exception e){}
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
