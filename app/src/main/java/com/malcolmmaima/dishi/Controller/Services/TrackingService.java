package com.malcolmmaima.dishi.Controller.Services;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.malcolmmaima.dishi.R;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.content.Intent;
import android.content.IntentFilter;
import android.Manifest;
import android.location.Location;
import android.app.Notification;
import android.content.pm.PackageManager;
import android.app.PendingIntent;
import android.app.Service;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class TrackingService extends Service {

    private static final String TAG = TrackingService.class.getSimpleName();
    FirebaseAuth mAuth;
    Boolean stopTracking = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();
        mAuth = FirebaseAuth.getInstance();
        requestLocationUpdates();
    }

//Create the persistent notification//

    private void buildNotification() {
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);

// Create the persistent notification//
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.tracking_enabled_notif))

//Make this notification ongoing so it can’t be dismissed by the user//

                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.tracking_enabled);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            //startForeground(1, new Notification());
            startForeground(1, builder.build());

    }

    @SuppressLint("NewApi")
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "TrackingServiceChannel";
        String channelName = "Tracking";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));

        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.tracking_enabled_notif))
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.tracking_enabled)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1, notification);

        stopTracking = false;
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Unregister the BroadcastReceiver when the notification is tapped//

            unregisterReceiver(stopReceiver);

            //Stop the Service//

            onUnbind(intent);
            stopForeground(true);
            stopSelf();

            stopTracking = true;
        }
    };


//Initiate the request to track the device's location//

    private void requestLocationUpdates() {

            LocationRequest request = new LocationRequest();

//Specify how often your app should request the device’s location//

            request.setInterval(30000);

//Get the most accurate location data available//

            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

            int permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);

//If the app currently has access to the location permission...//

            if (permission == PackageManager.PERMISSION_GRANTED) {

//...then request location updates//

                client.requestLocationUpdates(request, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        Location location = locationResult.getLastLocation();
                        if (location != null && mAuth.getInstance().getCurrentUser() != null && stopTracking == false) {

                            //Get a reference to the database, so your app can perform read and write operations//
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            String myPhone = user.getPhoneNumber(); //Current logged in user phone number

                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
                            //Save the location data to the database//

                            ref.setValue(location);
//                            Toast.makeText(getApplicationContext(), "Location data: lat(" +
//                                    location.getLatitude() + ") long(" + location.getLongitude() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, null);
            }
        }
}