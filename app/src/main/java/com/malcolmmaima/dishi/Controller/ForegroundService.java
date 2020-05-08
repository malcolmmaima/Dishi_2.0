package com.malcolmmaima.dishi.Controller;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.MessageModel;
import com.malcolmmaima.dishi.Model.NotificationModel;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.ReceiptModel;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.Chat;
import com.malcolmmaima.dishi.View.Activities.MyNotifications;
import com.malcolmmaima.dishi.View.Activities.ReceiptActivity;
import com.malcolmmaima.dishi.View.Activities.RestaurantActivity;
import com.malcolmmaima.dishi.View.Activities.RiderActivity;
import com.malcolmmaima.dishi.View.Activities.ViewCustomerOrder;
import com.malcolmmaima.dishi.View.Activities.ViewMyOrders;
import com.malcolmmaima.dishi.View.Activities.ViewProfile;
import com.malcolmmaima.dishi.View.Activities.ViewReview;
import com.malcolmmaima.dishi.View.Activities.ViewStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * https://androidwave.com/foreground-service-android-example/
 * https://developer.android.com/training/notify-user/build-notification
 */

public class ForegroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    String TAG = "ForeGroundService";
    DatabaseReference databaseReference, myUserDetailsRef, myOrdersRef, myRideRequests, notificationRef, myMessages;
    DatabaseReference myRideOrderRequests, receiptsRef;
    ValueEventListener databaseListener, myOrdersListener, myUserDetailsListener;
    ValueEventListener myRideOrderRequestsListener;
    ChildEventListener notificationsListener, receiptsListener, myMessagesListener, myRestaurantOrdersListener, myRideRequestsListener;
    String myPhone;
    FirebaseUser user;
    UserModel myUserDetails;
    String restaurantName, lastName;
    final int[] unreadCounter = {0};
    NotificationManager manager;

    //We use this two variables as our notification ID because they are unique and attached to a user phone number
    String lastFourDigits = "";     //substring containing last 4 characters
    String lastFiveDigits = "";     //substring containing last 5 characters

    ArrayList<String> restaurants = new ArrayList<>(); //I want to show the "item confirmed notification" only once
    // thus an arraylist that keeps trach of restaurant notifications. One notification for each confirmed order
    // item(s) since the listener that calls this function of type "orderConfirmed"
    //fires up everytime a single item's value is changed. might find a better way to do this in the future :-)

    NotificationChannel channel;


    @Override
    public void onCreate() {
        super.onCreate();
        //Bug fix for android 8.0+
        //https://stackoverflow.com/questions/44425584/context-startforegroundservice-did-not-then-call-service-startforeground
        if (Build.VERSION.SDK_INT >= 26) {
            channel = new NotificationChannel(CHANNEL_ID,
                    "Dishi",
                    NotificationManager.IMPORTANCE_HIGH);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Dishi")
                    .setContentText("Welcome to Dishi").build();

            startForeground(1, notification);
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Log.d("ForeGroundService", "ForegroundService: started");
        manager = ((NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }

        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            databaseReference = FirebaseDatabase.getInstance().getReference();
            myUserDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+myPhone);
            receiptsRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone);
        } catch(Exception e){}


        /**
         * Get logged in user details
         */
        myUserDetailsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    myUserDetails = dataSnapshot.getValue(UserModel.class);
                    if (myUserDetails.getAccount_type().equals("1") && myUserDetails.getVerified().equals("true")) {
                        //Check notification settings
                        if(myUserDetails.getOrderNotification() == true){
                            startCustomerNotifications();
                        } else {
                            try {
                                databaseReference.child("my_orders").child(myPhone).removeEventListener(myOrdersListener);
                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                    }


                    if(myUserDetails.getAccount_type().equals("2") && myUserDetails.getVerified().equals("true")){
                        if(myUserDetails.getOrderNotification() == true){
                            startRestaurantNotifications();
                        } else {
                            try {
                                myOrdersRef.removeEventListener(myRestaurantOrdersListener);
                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }
                    }



                    if(myUserDetails.getAccount_type().equals("3") && myUserDetails.getVerified().equals("true")){
                        if(myUserDetails.getOrderNotification() == true){
                            startRiderNotifications();
                        } else {
                            try {
                                myRideRequests.removeEventListener(myRideRequestsListener);
                            }catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                        /**
                         * We need to keep track of my active status and whether i (rider) have any orders in progress
                         */
                        myRideOrderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myPhone);
                        myRideOrderRequestsListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                //if node my_ride_requests/myPhone does not exist then it simply means i have no ride requests ata all
                                if(!dataSnapshot.exists()){
                                    Log.d("RiderRequestsService", myPhone+": no ride requests");
                                    DatabaseReference myrestaurants = FirebaseDatabase.getInstance().getReference("my_restaurants/"+myPhone);
                                    myrestaurants.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            for(DataSnapshot restaurants : dataSnapshot.getChildren()){
                                                //SafeToast.makeText(getContext(), "restaurants: " + restaurants.getKey(), Toast.LENGTH_SHORT).show();
                                                DatabaseReference restaurantRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+restaurants.getKey()+"/"+myPhone);
                                                restaurantRidersRef.setValue("inactive");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }

                                else {
                                    Log.d("RiderRequestsService", myPhone+": active ride requests");
                                    for(DataSnapshot restaurantRequest : dataSnapshot.getChildren()){
                                        //SafeToast.makeText(getContext(), "restaurant: " + restaurantRequest.getKey(), Toast.LENGTH_SHORT).show();
                                        for(DataSnapshot assignedCustomer : restaurantRequest.getChildren()){
                                            /**
                                             * we just need 1 'accepted' order request to keep rider status active otherwise if none then inactive
                                             */
                                            if(assignedCustomer.getValue().equals("accepted")){
                                                DatabaseReference myrestaurants = FirebaseDatabase.getInstance().getReference("my_restaurants/"+myPhone);
                                                myrestaurants.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        for(DataSnapshot restaurants : dataSnapshot.getChildren()){
                                                            //SafeToast.makeText(getContext(), "restaurants: " + restaurants.getKey(), Toast.LENGTH_SHORT).show();
                                                            DatabaseReference restaurantRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+restaurants.getKey()+"/"+myPhone);
                                                            restaurantRidersRef.setValue("active");
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });
                                            }

                                            else {
                                                DatabaseReference myrestaurants = FirebaseDatabase.getInstance().getReference("my_restaurants/"+myPhone);
                                                myrestaurants.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        for(DataSnapshot restaurants : dataSnapshot.getChildren()){
                                                            //SafeToast.makeText(getContext(), "restaurants: " + restaurants.getKey(), Toast.LENGTH_SHORT).show();
                                                            DatabaseReference restaurantRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+restaurants.getKey()+"/"+myPhone);
                                                            restaurantRidersRef.setValue("inactive");

                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });
                                            }

                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        };
                        myRideOrderRequests.addValueEventListener(myRideOrderRequestsListener);
                    }

                    try {
                        //start/stop social notifications depending on notification settings
                        if (myUserDetails.getSocialNotification() == true) {
                            //initialize social media notifications
                            startSocialNotifications();
                        }

                        if (myUserDetails.getSocialNotification() == false) {
                            try {
                                notificationRef.removeEventListener(notificationsListener);
                            } catch (Exception e) {
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                        //start/stop chat notifications depending on notification settings
                        if (myUserDetails.getChatNotification() == true) {
                            //initialize chat notifications listener
                            startChatNotifications();
                        }

                        if (myUserDetails.getChatNotification() == false) {
                            //initialize chat notifications listener
                            try {
                                myMessages.removeEventListener(myMessagesListener);
                            } catch (Exception e) {
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                    } catch (Exception err){
                        Log.e(TAG, "onDataChange: ", err);
                    }



                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        try {
            myUserDetailsRef.addListenerForSingleValueEvent(myUserDetailsListener);
        } catch (Exception e){}

        //SafeToast.makeText(getApplicationContext(),"Notification started", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    /**
     * Initialize Chat listener: this is a global listener for all account types
     */
    private void startChatNotifications() {
        myMessages = FirebaseDatabase.getInstance().getReference("messages/"+myPhone);
        myMessagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot incoming, @Nullable String s) {

                DatabaseReference incomingMessages = FirebaseDatabase.getInstance().getReference("messages/"+myPhone+"/"+incoming.getKey());
                incomingMessages.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot message, @Nullable String s) {

                        MessageModel messages = message.getValue(MessageModel.class);
                        try {
                            if (!messages.getSender().equals(myPhone) && messages.getRead() != true) {
                                //Fire up notification for the new chat messages
                                unreadCounter[0]++;
                                String incomingPhone = messages.getSender();
                                if (incomingPhone.length() > 5) {
                                    lastFiveDigits = incomingPhone.substring(incomingPhone.length() - 5); //We'll use this as the notification's unique ID
                                }
                                int notifId = Integer.parseInt(lastFiveDigits); //new Random().nextInt();
                                int rand = new Random().nextInt(10);
                                DatabaseReference incomingUserDetails = FirebaseDatabase.getInstance().getReference("users/"+messages.getSender());
                                int finalUnreadCounter = unreadCounter[0];
                                incomingUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        if(dataSnapshot.exists()){
                                            UserModel incomingUser = dataSnapshot.getValue(UserModel.class);

                                            //compose our notification and send
                                            String title = incomingUser.getFirstname()+" "+incomingUser.getLastname();
                                            String msg;
                                            if(finalUnreadCounter != 1){
                                                msg = finalUnreadCounter + " new messages";

                                            } else {
                                                msg = messages.getMessage();
                                            }

                                            //We want to check if user is currently in Chat activity, no need to send notification if
                                            //im actively in Chat refer to: https://stackoverflow.com/questions/3873659/android-how-can-i-get-the-current-foreground-activity-from-a-service
                                            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                                            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
                                            Log.d("topActivity", "CURRENT Activity ::" + taskInfo.get(0).topActivity.getClassName());
                                            ComponentName componentInfo = taskInfo.get(0).topActivity;
                                            componentInfo.getPackageName();
                                            Log.d("topActivity", "Component info ::" +componentInfo.getPackageName());

                                            if(!taskInfo.get(0).topActivity.getClassName().equals("com.malcolmmaima.dishi.View.Activities.Chat")){

                                                //TODO find a way to get data from Chat activity and compare to new notification data.
                                                //If i am actively in chat don't fire up notification
                                                sendChatNotification(notifId, "newUnreadMsg", title, msg, Chat.class, messages.getSender(), messages.getReciever());
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        } catch (Exception e){}
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        unreadCounter[0] = 0; // this onChildChanged() is triggered when user opens chat notification and child value of 'read' is changed to 'true'
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myMessages.addChildEventListener(myMessagesListener);

    }

    private void startSocialNotifications(){

        notificationsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                try {
                    NotificationModel newNotification = dataSnapshot.getValue(NotificationModel.class);
                    newNotification.key = dataSnapshot.getKey();
                    int notifId = new Random().nextInt();
                    sendSocialNotification(notifId, newNotification);
                }catch (Exception er){
                    Log.e(TAG, "onChildAdded: ", er);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        try {
            notificationRef.addChildEventListener(notificationsListener);
        } catch (Exception e){
            Log.e(TAG, "startSocialNotifications: ", e);
        }
    }

    /**
     * Initialize Notification listeners for different account types (3 total)
     */
    private void startRiderNotifications(){
        /**
         * Get the ride requests from the 'my_ride_requests' node (contains restaurants with customers as child nodes)
         */
        myRideRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myPhone);
        myRideRequestsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot restaurants, @Nullable String s) {
                /**
                 * For each restaurant, get the customers i have been assigned to and their user details
                 */
                DatabaseReference assignedCustomersRef = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myPhone+"/"+restaurants.getKey());
                assignedCustomersRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot customers, @Nullable String s) {
                        //Get user details
                        DatabaseReference customerDetails = FirebaseDatabase.getInstance().getReference("users/"+customers.getKey());
                        customerDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserModel customer = dataSnapshot.getValue(UserModel.class);

                                DatabaseReference restaurantDetails = FirebaseDatabase.getInstance().getReference("users/"+restaurants.getKey());
                                restaurantDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        UserModel restaurant = dataSnapshot.getValue(UserModel.class);

                                        //compose our notification and send
                                        String title = restaurant.getFirstname()+" "+restaurant.getLastname();
                                        String message = "Deliver to "+customer.getFirstname()+" "+customer.getLastname();
                                        String customerPhone = customers.getKey();
                                        if (customerPhone.length() > 4) {
                                            lastFourDigits = customerPhone.substring(customerPhone.length() - 4); //We'll use this as the notification's unique ID
                                        }
                                        int notifId = Integer.parseInt(lastFourDigits); //new Random().nextInt();
                                        sendRiderOrderNotification(notifId, "newRideRequest", title, message, RiderActivity.class, customerPhone, restaurants.getKey());
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRideRequests.addChildEventListener(myRideRequestsListener);
    }

    private void startRestaurantNotifications() {
        myOrdersRef = FirebaseDatabase.getInstance().getReference("orders/"+myPhone);
        myRestaurantOrdersListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot customerPhones, @Nullable String s) {
                int itemCount = (int) customerPhones.child("items").getChildrenCount();
                String orderID = customerPhones.child("orderID").getValue(String.class);
                //Get user details
                DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+customerPhones.getKey());
                userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            UserModel customer = dataSnapshot.getValue(UserModel.class);

                            //compose our notification and send
                            String title = customer.getFirstname() + " " + customer.getLastname();
                            String message = "New order request [#"+orderID+"]";
                            String customerPhone = customerPhones.getKey();
                            if (customerPhone.length() > 4) {
                                lastFourDigits = customerPhone.substring(customerPhone.length() - 4); //We'll use this as the notification's unique ID
                            }
                            int notifId = Integer.parseInt(lastFourDigits); //new Random().nextInt();

                            sendCustomerOrderNotification(notifId, "newOrderRequest", title, message, ViewCustomerOrder.class, customerPhone, title);
                        } catch (Exception e){}
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myOrdersRef.addChildEventListener(myRestaurantOrdersListener);
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
                                            ProductDetailsModel prod = snap.getValue(ProductDetailsModel.class);

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

        //check for new receipts generated
        receiptsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                try {
                    ReceiptModel newReceipt = dataSnapshot.getValue(ReceiptModel.class);
                    newReceipt.key = dataSnapshot.getKey();
                    if (newReceipt.getSeen() == false) {
                        //Log.d(TAG, "new receipt: "+newReceipt.getOrderID());
                        sendReceiptNotification(newReceipt);
                    }
                } catch (Exception e){
                    Log.e(TAG, "onChildAdded: ", e);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        receiptsRef.addChildEventListener(receiptsListener);

    }


    /**
     * End of Notification listeners
     */

    /**
     * Notification Builders for each account type
     */
    private void sendOrderNotification(int notifId, String type, String title, String message, Class targetActivity, String restaurantPhone, String restaurantName){

        if(type.equals("orderConfirmed") && !restaurants.contains(restaurantPhone)){
            Notification.Builder builder = null;
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder = new Notification.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.dish)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setContentText(message);
                } else {
                    builder = new Notification.Builder(this)
                            .setSmallIcon(R.drawable.dish)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setPriority(Notification.PRIORITY_MAX)
                            .setContentText(message);
                }
            }

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder = new Notification.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.logo_notification)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setContentText(message);
                } else {
                    builder = new Notification.Builder(this)
                            .setSmallIcon(R.drawable.logo_notification)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setContentText(message);
                }
            }

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

    private void sendCustomerOrderNotification(int notifId, String type, String title, String message, Class targetActivity, String customerPhone, String customerName){

        if(type.equals("newOrderRequest")){
            Notification.Builder builder = null;
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder = new Notification.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.dish)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setOnlyAlertOnce(true)
                            .setContentText(message);
                } else {
                    builder = new Notification.Builder(this)
                            .setSmallIcon(R.drawable.dish)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setOnlyAlertOnce(true)
                            .setPriority(Notification.PRIORITY_MAX)
                            .setContentText(message);
                }
            }

            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("phone", customerPhone);
            intent.putExtra("name", customerName);
            intent.putExtra("restaurantPhone", myPhone);
            intent.putExtra("restaurantName", myUserDetails.getFirstname()+" "+myUserDetails.getLastname());
            intent.putExtra("accountType", "2");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(contentIntent);
            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.icon |= Notification.BADGE_ICON_LARGE;
            manager.notify(notifId, notification);
        }

    }

    private void sendRiderOrderNotification(int notifId, String type, String title, String message, Class targetActivity, String customerPhone, String restaurantPhone){

        if(type.equals("newRideRequest")){
            Notification.Builder builder = null;
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder = new Notification.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.deliver_nduthi_48dp)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setContentText(message);
                } else {
                    builder = new Notification.Builder(this)
                            .setSmallIcon(R.drawable.deliver_nduthi_48dp)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setPriority(Notification.PRIORITY_MAX)
                            .setContentText(message);
                }
            }

            Intent intent = new Intent(this, targetActivity);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, intent, 0);
            builder.setContentIntent(contentIntent);
            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.icon |= Notification.BADGE_ICON_LARGE;
            manager.notify(notifId, notification);
        }

    }

    private void sendChatNotification(int notifId, String type, String title, String message, Class targetActivity, String incomingPhone, String myphone){

        if(type.equals("newUnreadMsg")){

            Notification.Builder builder = null;
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
                    builder = new Notification.Builder(this, CHANNEL_ID)
                            .setGroupSummary(true)
                            //.setOnlyAlertOnce(true)
                            .setGroup(String.valueOf(notifId))
                            .setSmallIcon(R.drawable.ic_send_black_24dp)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setContentText(message)
                            .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                    .bigText(message));
                } else {
                    builder = new Notification.Builder(this)
                            .setGroupSummary(true)
                            //.setOnlyAlertOnce(true)
                            .setGroup(String.valueOf(notifId))
                            .setSmallIcon(R.drawable.ic_send_black_24dp)
                            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setContentTitle(title)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                            .setSound(soundUri)
                            .setPriority(Notification.PRIORITY_MAX)
                            .setContentText(message)
                            .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                    .bigText(message));
                }
            }

            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("fromPhone", incomingPhone);
            intent.putExtra("toPhone", myphone);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, intent,PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(contentIntent);
            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.icon |= Notification.BADGE_ICON_LARGE;
            manager.notify(notifId, notification);
        }

    }

    private void sendSocialNotification(int notifId, NotificationModel newNotification) {

        if(newNotification.getType().equals("postedwall") && newNotification.getSeen() == false){
            Class targetActivity = ViewStatus.class;
            DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+newNotification.getFrom());
            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        //get the 'from' user details first
                        UserModel fromUser = dataSnapshot.getValue(UserModel.class);

                        DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("posts/"+myPhone+"/"+newNotification.getMessage());
                        postDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                try {
                                    StatusUpdateModel statusUpdate = dataSnapshot.getValue(StatusUpdateModel.class);

                                    Notification.Builder builder = null;
                                    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            //https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
                                            builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                                                    .setGroupSummary(true)
                                                    //.setOnlyAlertOnce(true)
                                                    .setGroup(String.valueOf(notifId))
                                                    .setSmallIcon(R.drawable.profile_64dp)
                                                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                                    .setContentTitle(fromUser.getFirstname()+" posted on your wall:")
                                                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                                    .setSound(soundUri)
                                                    .setTimeoutAfter(40000) //40s
                                                    .setOnlyAlertOnce(true)
                                                    .setContentText(statusUpdate.getStatus())
                                                    .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                            .bigText(statusUpdate.getStatus()));
                                        } else {
                                            builder = new Notification.Builder(getApplicationContext())
                                                    .setGroupSummary(true)
                                                    //.setOnlyAlertOnce(true)
                                                    .setGroup(String.valueOf(notifId))
                                                    .setSmallIcon(R.drawable.profile_64dp)
                                                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                                    .setContentTitle(fromUser.getFirstname()+" posted on your wall:")
                                                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                                    .setSound(soundUri)
                                                    .setAutoCancel(true)
                                                    .setOnlyAlertOnce(true)
                                                    .setPriority(Notification.PRIORITY_MAX)
                                                    .setContentText(statusUpdate.getStatus())
                                                    .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                            .bigText(statusUpdate.getStatus()));
                                        }


                                    }

                                    Intent intent = new Intent(getApplicationContext(), targetActivity);
                                    intent.putExtra("author", newNotification.getFrom());
                                    intent.putExtra("postedTo", myPhone);
                                    intent.putExtra("key", newNotification.getMessage());
                                    intent.putExtra("type", "notification");
                                    intent.putExtra("notifKey", newNotification.key);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), notifId, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                                    builder.setContentIntent(contentIntent);
                                    Notification notification = builder.build();
                                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                                    notification.defaults |= Notification.DEFAULT_SOUND;
                                    notification.icon |= Notification.BADGE_ICON_LARGE;
                                    manager.notify(notifId, notification);
                                }catch (Exception e){
                                    Log.e(TAG, "onDataChange: ",e);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    } catch (Exception er){
                        Log.e(TAG, "onDataChange: ", er);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        if(newNotification.getType().equals("followedwall") && newNotification.getSeen() == false){
            Class targetActivity = ViewProfile.class;
            DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+newNotification.getFrom());
            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        //get the 'from' user details first
                        UserModel fromUser = dataSnapshot.getValue(UserModel.class);

                        Notification.Builder builder = null;
                        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                //https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
                                builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                                        .setGroupSummary(true)
                                        //.setOnlyAlertOnce(true)
                                        .setGroup(String.valueOf(notifId))
                                        .setSmallIcon(R.drawable.logo_notification)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                        .setContentTitle(fromUser.getFirstname()+" "+fromUser.getLastname())
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                        .setSound(soundUri)
                                        .setOnlyAlertOnce(true)
                                        .setContentText(newNotification.getMessage())
                                        .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                .bigText(newNotification.getMessage()));
                            } else {
                                builder = new Notification.Builder(getApplicationContext())
                                        .setGroupSummary(true)
                                        //.setOnlyAlertOnce(true)
                                        .setGroup(String.valueOf(notifId))
                                        .setSmallIcon(R.drawable.logo_notification)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                        .setContentTitle(fromUser.getFirstname()+" "+fromUser.getLastname())
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                        .setSound(soundUri)
                                        .setAutoCancel(true)
                                        .setOnlyAlertOnce(true)
                                        .setPriority(Notification.PRIORITY_MAX)
                                        .setContentText(newNotification.getMessage())
                                        .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                .bigText(newNotification.getMessage()));
                            }


                        }

                        Intent intent = new Intent(getApplicationContext(), targetActivity);
                        intent.putExtra("phone", newNotification.getFrom());
                        intent.putExtra("type", "notification");
                        intent.putExtra("notifKey", newNotification.key);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), notifId, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                        builder.setContentIntent(contentIntent);
                        Notification notification = builder.build();
                        notification.flags |= Notification.FLAG_AUTO_CANCEL;
                        notification.defaults |= Notification.DEFAULT_SOUND;
                        notification.icon |= Notification.BADGE_ICON_LARGE;
                        manager.notify(notifId, notification);
                    } catch (Exception er){
                        Log.e(TAG, "onDataChange: ", er);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        if(newNotification.getType().equals("followrequest") && newNotification.getSeen() == false){
            Class targetActivity = MyNotifications.class;
            DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+newNotification.getFrom());
            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        //get the 'from' user details first
                        UserModel fromUser = dataSnapshot.getValue(UserModel.class);

                        Notification.Builder builder = null;
                        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                //https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
                                builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                                        .setGroupSummary(true)
                                        //.setOnlyAlertOnce(true)
                                        .setGroup(String.valueOf(notifId))
                                        .setSmallIcon(R.drawable.logo_notification)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                        .setContentTitle(fromUser.getFirstname()+" "+fromUser.getLastname())
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                        .setSound(soundUri)
                                        .setOnlyAlertOnce(true)
                                        .setContentText(newNotification.getMessage())
                                        .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                .bigText(newNotification.getMessage()));
                            } else {
                                builder = new Notification.Builder(getApplicationContext())
                                        .setGroupSummary(true)
                                        //.setOnlyAlertOnce(true)
                                        .setGroup(String.valueOf(notifId))
                                        .setSmallIcon(R.drawable.logo_notification)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                        .setContentTitle(fromUser.getFirstname()+" "+fromUser.getLastname())
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                        .setSound(soundUri)
                                        .setAutoCancel(true)
                                        .setOnlyAlertOnce(true)
                                        .setPriority(Notification.PRIORITY_MAX)
                                        .setContentText(newNotification.getMessage())
                                        .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                .bigText(newNotification.getMessage()));
                            }


                        }

                        Intent intent = new Intent(getApplicationContext(), targetActivity);
                        intent.putExtra("phone", newNotification.getFrom());
                        intent.putExtra("type", "notification");
                        intent.putExtra("notifKey", newNotification.key);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), notifId, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                        builder.setContentIntent(contentIntent);
                        Notification notification = builder.build();
                        notification.flags |= Notification.FLAG_AUTO_CANCEL;
                        notification.defaults |= Notification.DEFAULT_SOUND;
                        notification.icon |= Notification.BADGE_ICON_LARGE;
                        manager.notify(notifId, notification);
                    } catch (Exception er){
                        Log.e(TAG, "onDataChange: ", er);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        if(newNotification.getType().equals("likedstatus") && newNotification.getSeen() == false){
            Class targetActivity = ViewStatus.class;
            DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+newNotification.getFrom());
            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        //get the 'from' user details first
                        UserModel fromUser = dataSnapshot.getValue(UserModel.class);

                        DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("posts/"+newNotification.getPostedTo()+"/"+newNotification.getMessage());
                        postDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                try {
                                    StatusUpdateModel statusUpdate = dataSnapshot.getValue(StatusUpdateModel.class);

                                    Notification.Builder builder = null;
                                    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            //https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
                                            builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                                                    .setGroupSummary(true)
                                                    //.setOnlyAlertOnce(true)
                                                    .setGroup(String.valueOf(notifId))
                                                    .setSmallIcon(R.drawable.liked)
                                                    //.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                                    .setContentTitle(fromUser.getFirstname() + " " + fromUser.getLastname() + " liked:")
                                                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                                    .setSound(soundUri)
                                                    .setTimeoutAfter(40000) //40s
                                                    .setOnlyAlertOnce(true)
                                                    .setContentText(statusUpdate.getStatus())
                                                    .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                            .bigText(statusUpdate.getStatus()));
                                        } else {
                                            builder = new Notification.Builder(getApplicationContext())
                                                    .setGroupSummary(true)
                                                    //.setOnlyAlertOnce(true)
                                                    .setGroup(String.valueOf(notifId))
                                                    .setSmallIcon(R.drawable.liked)
                                                    //.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                                    .setContentTitle(fromUser.getFirstname() + " " + fromUser.getLastname() + " liked:")
                                                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                                    .setSound(soundUri)
                                                    .setAutoCancel(true)
                                                    .setOnlyAlertOnce(true)
                                                    .setPriority(Notification.PRIORITY_MAX)
                                                    .setContentText(statusUpdate.getStatus())
                                                    .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                            .bigText(statusUpdate.getStatus()));
                                        }


                                    }

                                    Intent intent = new Intent(getApplicationContext(), targetActivity);
                                    intent.putExtra("author", newNotification.getAuthor());
                                    intent.putExtra("postedTo", newNotification.getPostedTo());
                                    intent.putExtra("key", newNotification.getMessage());
                                    intent.putExtra("type", "notification");
                                    intent.putExtra("notifKey", newNotification.key);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                    builder.setContentIntent(contentIntent);
                                    Notification notification = builder.build();
                                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                                    notification.defaults |= Notification.DEFAULT_SOUND;
                                    notification.icon |= Notification.BADGE_ICON_LARGE;
                                    manager.notify(notifId, notification);
                                } catch (Exception e){
                                    Log.e(TAG, "onDataChange: ", e);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    } catch (Exception er){
                        Log.e(TAG, "onDataChange: ", er);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        if(newNotification.getType().equals("commentedstatus") && newNotification.getSeen() == false){
            Class targetActivity = ViewStatus.class;
            DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+newNotification.getFrom());
            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        //get the 'from' user details first
                        UserModel fromUser = dataSnapshot.getValue(UserModel.class);

                        DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("posts/"+newNotification.getPostedTo()+"/"+newNotification.getStatusKey()+"/comments/"+newNotification.getMessage());
                        postDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                try {
                                    StatusUpdateModel statusUpdate = dataSnapshot.getValue(StatusUpdateModel.class);
                                    Notification.Builder builder = null;
                                    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            //https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
                                            builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                                                    .setGroupSummary(true)
                                                    //.setOnlyAlertOnce(true)
                                                    .setGroup(String.valueOf(notifId))
                                                    .setSmallIcon(R.drawable.logo_notification)
                                                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                                    .setContentTitle(fromUser.getFirstname() + " " + fromUser.getLastname() + " commented:")
                                                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                                    .setSound(soundUri)
                                                    .setTimeoutAfter(40000) //40s
                                                    .setOnlyAlertOnce(true)
                                                    .setContentText(statusUpdate.getStatus())
                                                    .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                            .bigText(statusUpdate.getStatus()));
                                        } else {
                                            builder = new Notification.Builder(getApplicationContext())
                                                    .setGroupSummary(true)
                                                    //.setOnlyAlertOnce(true)
                                                    .setGroup(String.valueOf(notifId))
                                                    .setSmallIcon(R.drawable.logo_notification)
                                                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                                    .setContentTitle(fromUser.getFirstname() + " " + fromUser.getLastname() + " commented:")
                                                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                                    .setSound(soundUri)
                                                    .setAutoCancel(true)
                                                    .setOnlyAlertOnce(true)
                                                    .setPriority(Notification.PRIORITY_MAX)
                                                    .setContentText(statusUpdate.getStatus())
                                                    .setStyle(new Notification.BigTextStyle() //https://developer.android.com/training/notify-user/expanded
                                                            .bigText(statusUpdate.getStatus()));
                                        }

                                    }

                                    Intent intent = new Intent(getApplicationContext(), targetActivity);
                                    intent.putExtra("author", newNotification.getAuthor());
                                    intent.putExtra("postedTo", newNotification.getPostedTo());
                                    intent.putExtra("key", newNotification.getStatusKey());
                                    intent.putExtra("type", "notification");
                                    intent.putExtra("notifKey", newNotification.key);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), notifId, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                                    builder.setContentIntent(contentIntent);
                                    Notification notification = builder.build();
                                    notification.flags |= Notification.FLAG_AUTO_CANCEL;
                                    notification.defaults |= Notification.DEFAULT_SOUND;
                                    notification.icon |= Notification.BADGE_ICON_LARGE;
                                    manager.notify(notifId, notification);
                                } catch (Exception e){
                                    Log.e(TAG, "onDataChange: ", e);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    } catch (Exception er){
                        Log.e(TAG, "onDataChange: ", er);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        if(newNotification.getType().equals("postedreview") && newNotification.getSeen() == false){
            Class targetActivity = ViewReview.class;
            DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+newNotification.getFrom());
            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        //get the 'from' user details first
                        UserModel fromUser = dataSnapshot.getValue(UserModel.class);

                        Notification.Builder builder = null;
                        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                //https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
                                builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                                        .setGroupSummary(true)
                                        //.setOnlyAlertOnce(true)
                                        .setGroup(String.valueOf(notifId))
                                        .setSmallIcon(R.drawable.logo_notification)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                        .setContentTitle(fromUser.getFirstname()+" "+fromUser.getLastname())
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                        .setSound(soundUri)
                                        .setTimeoutAfter(40000) //40s
                                        .setOnlyAlertOnce(true)
                                        .setContentText("Just posted a review");
                            } else {
                                builder = new Notification.Builder(getApplicationContext())
                                        .setGroupSummary(true)
                                        //.setOnlyAlertOnce(true)
                                        .setGroup(String.valueOf(notifId))
                                        .setSmallIcon(R.drawable.logo_notification)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                        .setContentTitle(fromUser.getFirstname()+" "+fromUser.getLastname())
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                        .setSound(soundUri)
                                        .setAutoCancel(true)
                                        .setOnlyAlertOnce(true)
                                        .setPriority(Notification.PRIORITY_MAX)
                                        .setContentText("Just posted a review");
                            }
                        }

                        Intent intent = new Intent(getApplicationContext(), targetActivity);
                        intent.putExtra("author", newNotification.getFrom());
                        intent.putExtra("postedTo", myPhone);
                        intent.putExtra("key", newNotification.getMessage());
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), notifId, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                        builder.setContentIntent(contentIntent);
                        Notification notification = builder.build();
                        notification.flags |= Notification.FLAG_AUTO_CANCEL;
                        notification.defaults |= Notification.DEFAULT_SOUND;
                        notification.icon |= Notification.BADGE_ICON_LARGE;
                        manager.notify(notifId, notification);
                    } catch (Exception er){
                        Log.e(TAG, "onDataChange: ", er);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        if(newNotification.getType().equals("commentedreview") && newNotification.getSeen() == false){
            Class targetActivity = ViewReview.class;
            DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+newNotification.getFrom());
            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    try {
                        //get the 'from' user details first
                        UserModel fromUser = dataSnapshot.getValue(UserModel.class);

                        Notification.Builder builder = null;
                        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                //https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
                                builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                                        .setGroupSummary(true)
                                        //.setOnlyAlertOnce(true)
                                        .setGroup(String.valueOf(notifId))
                                        .setSmallIcon(R.drawable.review_64dp)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                        .setContentTitle(fromUser.getFirstname()+" "+fromUser.getLastname())
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                        .setSound(soundUri)
                                        .setTimeoutAfter(40000) //40s
                                        .setOnlyAlertOnce(true)
                                        .setContentText("Just commented on a review");
                            } else {
                                builder = new Notification.Builder(getApplicationContext())
                                        .setGroupSummary(true)
                                        //.setOnlyAlertOnce(true)
                                        .setGroup(String.valueOf(notifId))
                                        .setSmallIcon(R.drawable.review_64dp)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                                        .setContentTitle(fromUser.getFirstname()+" "+fromUser.getLastname())
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                        .setSound(soundUri)
                                        .setAutoCancel(true)
                                        .setOnlyAlertOnce(true)
                                        .setPriority(Notification.PRIORITY_MAX)
                                        .setContentText("Just commented on a review");
                            }
                        }

                        Intent intent = new Intent(getApplicationContext(), targetActivity);
                        intent.putExtra("author", newNotification.getFrom());
                        intent.putExtra("postedTo", myPhone);
                        intent.putExtra("key", newNotification.getMessage());
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), notifId, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                        builder.setContentIntent(contentIntent);
                        Notification notification = builder.build();
                        notification.flags |= Notification.FLAG_AUTO_CANCEL;
                        notification.defaults |= Notification.DEFAULT_SOUND;
                        notification.icon |= Notification.BADGE_ICON_LARGE;
                        manager.notify(notifId, notification);
                    } catch (Exception er){
                        Log.e(TAG, "onDataChange: ", er);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void sendReceiptNotification(ReceiptModel newReceipt) {
        int notifId = new Random().nextInt();
        Class targetActivity = ReceiptActivity.class;
        Notification.Builder builder = null;
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_receipt_white_48dp)
                        .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        .setContentTitle("Order #"+newReceipt.getOrderID())
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                        .setSound(soundUri)
                        .setContentText("Receipt has been generated");
            } else {
                builder = new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_receipt_white_48dp)
                        .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        .setContentTitle("Order #"+newReceipt.getOrderID())
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                        .setSound(soundUri)
                        .setContentText("Receipt has been generated");
            }
        }

        Intent intent = new Intent(this, targetActivity);
        intent.putExtra("orderOn", newReceipt.getInitiatedOn());
        intent.putExtra("deliveredOn", newReceipt.getDeliveredOn());
        intent.putExtra("restaurantName", "vendorName");
        intent.putExtra("orderID", newReceipt.getOrderID());
        intent.putExtra("restaurantPhone", newReceipt.getRestaurant());
        intent.putExtra("key", newReceipt.key);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.icon |= Notification.BADGE_ICON_LARGE;
        manager.notify(notifId, notification);
    }

    /**
     * End of notification builders
     */

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ForeGroundService", "ForegroundService: stopped");
        stopService(new Intent(ForegroundService.this, TrackingService.class));
        restaurants.clear(); //Clear the tracker used in our send notification function
        try {
            notificationRef.removeEventListener(notificationsListener);
            myMessages.removeEventListener(myMessagesListener);
            myOrdersRef.removeEventListener(myRestaurantOrdersListener);
            myRideRequests.removeEventListener(myRideRequestsListener);
            myRideOrderRequests.removeEventListener(myRideOrderRequestsListener);
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
