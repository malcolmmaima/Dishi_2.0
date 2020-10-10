package com.malcolmmaima.dishi.View.Fragments;

import android.os.Bundle;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Interface.RetrofitArrayApi;
import com.malcolmmaima.dishi.Model.ExplorePostsModel;
import com.malcolmmaima.dishi.Model.WP.WPPost;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ExplorePostAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    String TAG = "ExploreFragment";
    View v;
    RecyclerView recyclerview;
    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinearLayoutManager layoutmanager;
    ArrayList<ExplorePostsModel> list;
    private String baseURL = "https://www.kaluhiskitchen.com/";
    public static List<WPPost> mListPost;
    private ExplorePostAdapter adapter;

    public ExploreFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ExploreFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ExploreFragment newInstance(String param1, String param2) {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_explore, container, false);

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);
        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                mSwipeRefreshLayout.setRefreshing(true);

                // Fetching data from server
                fetchPosts();

            }
        });

        return v;
    }

    private void fetchPosts() {
        list = new ArrayList<ExplorePostsModel>();
        /// call retrofill
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitArrayApi service = retrofit.create(RetrofitArrayApi.class);
        Call<List<WPPost>> call = service.getPostInfo();

        // to make call to dynamic URL

        // String yourURL = yourURL.replace(BaseURL,"");
        // Call<List<WPPost>>  call = service.getPostInfo( yourURL);

        /// to get only 6 post from your blog
        // http://domain/wp-json/wp/v2/posts?per_page=2

        // to get any specific blog post, use id of post
        //  http://domain/wp-json/wp/v2/posts/1179

        // to get only title and id of specific
        // http://domain/android/wp-json/wp/v2/posts/1179?fields=id,title

        call.enqueue(new Callback<List<WPPost>>() {
            @Override
            public void onResponse(Call<List<WPPost>> call, Response<List<WPPost>> response) {

                Log.d(TAG, " response "+ response.body());
                mListPost = response.body();

                for (int i = 0; i < response.body().size(); i++) {
                    Log.e("main ", " title " + response.body().get(i).getTitle().getRendered() + " " +
                            response.body().get(i).getId());

                    String tempdetails = response.body().get(i).getExcerpt().getRendered().toString();
                    tempdetails = tempdetails.replace("<p>", "");
                    tempdetails = tempdetails.replace("</p>", "");
                    tempdetails = tempdetails.replace("[&hellip;]", "");

                    try {
                        list.add(new ExplorePostsModel(ExplorePostsModel.IMAGE_TYPE, response.body().get(i).getTitle().getRendered(),
                                tempdetails,
                                "")); //response.body().get(i).getLinks().getWpFeaturedmedia().get(0).getHref()
                    } catch (Exception e){
                        Log.e(TAG, "onResponse: ", e);
                    }

                }
                if(!list.isEmpty()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    ExplorePostAdapter recycler = new ExplorePostAdapter(list, getContext());
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recycler.notifyDataSetChanged();
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.INVISIBLE);
                    icon.setVisibility(View.INVISIBLE);
                }

                else {

                    mSwipeRefreshLayout.setRefreshing(false);

                    ExplorePostAdapter recycler = new ExplorePostAdapter(list, getContext());
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);

                }

            }

            @Override
            public void onFailure(Call<List<WPPost>> call, Throwable t) {
                Toast.makeText(getContext(), "Something went wrong!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onFailure: "+t);

                mSwipeRefreshLayout.setRefreshing(false);
                ExplorePostAdapter recycler = new ExplorePostAdapter(list, getContext());
                recyclerview.setLayoutManager(layoutmanager);
                recyclerview.setItemAnimator( new DefaultItemAnimator());
                recyclerview.setAdapter(recycler);
                emptyTag.setVisibility(View.VISIBLE);
                icon.setVisibility(View.VISIBLE);
            }
        });
    }

    public static List<WPPost> getList(){
        return  mListPost;
    }

    @Override
    public void onRefresh() {
        fetchPosts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(v != null){
            v = null;
            recyclerview.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    // no-op
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    recyclerview.setAdapter(null);
                    layoutmanager = null;
                }
            });
        }
    }
}