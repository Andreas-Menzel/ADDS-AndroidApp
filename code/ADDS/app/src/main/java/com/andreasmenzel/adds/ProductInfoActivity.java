package com.andreasmenzel.adds;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds.DataClasses.Product;

public class ProductInfoActivity extends AppCompatActivity {

    private Product product = new Product("product_id", "My Awesome Product", "This is my really awesome product. It is perfect in any way!");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_info);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setupUICallbacks();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //bus.register(this);
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //bus.unregister(this);
    }


    private void setupUICallbacks() {

    }


    private void updateUI() {
        runOnUiThread(() -> {
            TextView txtView_productInfoName = findViewById(R.id.txtView_productInfoName);
            TextView txtView_productInfoID = findViewById(R.id.txtView_productInfoID);
            TextView txtView_productInfoDescription = findViewById(R.id.txtView_productInfoDescription);

            txtView_productInfoName.setText(product.getName());
            txtView_productInfoID.setText(product.getId());
            txtView_productInfoDescription.setText(product.getDescription());
        });
    }

}
