package com.malcolmmaima.dishi.Controller;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GetCurrentDate {
    public String getDate() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        TimeZone timeZone = TimeZone.getDefault();
        Calendar calendar = Calendar.getInstance(timeZone);
        String time = date+ ":" +
                String.format("%02d" , calendar.get(Calendar.HOUR_OF_DAY))+":"+
                String.format("%02d" , calendar.get(Calendar.MINUTE))+":"+
                String.format("%02d" , calendar.get(Calendar.SECOND)) +":"+
                timeZone.getDisplayName(false, TimeZone.SHORT);

        return time;
    }
}
