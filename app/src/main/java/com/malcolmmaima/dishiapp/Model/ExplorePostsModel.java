package com.malcolmmaima.dishiapp.Model;

public class ExplorePostsModel {
    public static final int IMAGE_TYPE =1;
    public String title, subtitle, Image, url, date;
    public int type;


    public ExplorePostsModel ( int mtype, String mtitle, String msubtitle, String image, String url, String date  ){

        this.title = mtitle;
        this.subtitle = msubtitle;
        this.type = mtype;
        this.Image = image;
        this.url = url;
        this.date = date;
    }
}
