package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ReceiptItemAdapter;

import java.util.ArrayList;

public class ReceiptActivity extends AppCompatActivity {

    String TAG = "ReceiptActivity";
    DatabaseReference receiptItemsRef;
    String key, myPhone;
    FirebaseUser user;
    private ArrayList<ProductDetailsModel> deliveredItems;
    private RecyclerView recyclerView;
    private ReceiptItemAdapter mAdapter;
    ImageView exitReceipt;
    MyTextView_Roboto_Regular totalTitle;
    MyTextView_Roboto_Medium totalBill;
    int totalAmount;
    Double vatCharge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        exitReceipt = findViewById(R.id.exitReceipt);
        recyclerView = findViewById(R.id.recyclerview);
        totalBill = findViewById(R.id.totalBill);
        totalTitle = findViewById(R.id.totalTitle);

        key = getIntent().getStringExtra("key");
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber();

        receiptItemsRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone+"/"+key+"/items");
        receiptItemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                deliveredItems = new ArrayList<>();
                totalAmount = 0;
                for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    ProductDetailsModel product = dataSnapshot1.getValue(ProductDetailsModel.class);
                    deliveredItems.add(product);

                    totalAmount = totalAmount + Integer.parseInt(product.getPrice());
                    totalBill.setText(""+totalAmount);
                    totalTitle.setText(""+totalAmount);
                }

                mAdapter = new ReceiptItemAdapter(ReceiptActivity.this,deliveredItems);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(ReceiptActivity.this);
                recyclerView.setLayoutManager(mLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setAdapter(mAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        exitReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

