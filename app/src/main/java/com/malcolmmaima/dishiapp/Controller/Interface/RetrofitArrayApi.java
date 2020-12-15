package com.malcolmmaima.dishiapp.Controller.Interface;

import com.malcolmmaima.dishiapp.Model.WPPost;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RetrofitArrayApi {

    @GET("wp-json/wp/v2/posts?per_page=15")
    Call<List<WPPost>> getPostInfo();
    /// to make call to dynamic URL
    //  @GET
    //  Call<List<WPPost>> getPostInfo(@Url String url);
    //

}
