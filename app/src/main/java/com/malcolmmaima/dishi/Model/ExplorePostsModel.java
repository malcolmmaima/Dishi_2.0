package com.malcolmmaima.dishi.Model;

public class ExplorePostsModel {
    public static final int IMAGE_TYPE =1;
    public String title, subtitle, Image;
    public int type;


    public ExplorePostsModel ( int mtype, String mtitle, String msubtitle, String image  ){

        this.title = mtitle;
        this.subtitle = msubtitle;
        this.type = mtype;
        this.Image = image;
    }
}
