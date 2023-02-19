package com.andreasmenzel.adds;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds.DataClasses.Product;
import com.andreasmenzel.adds.Events.FetchProductInfoFailed;
import com.andreasmenzel.adds.Events.FetchProductInfoSucceeded;
import com.andreasmenzel.adds.Events.FetchProductInfoSucceededPartially;
import com.andreasmenzel.adds.Events.ToastMessage;
import com.andreasmenzel.adds.Events.UpdateProductInfoUI;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ProductInfoActivity extends AppCompatActivity {

    private EventBus bus;

    private Product product = new Product("prinzregententorte", null, null);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_info);

        bus = EventBus.getDefault();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setupUICallbacks();
    }

    @Override
    protected void onResume() {
        super.onResume();

        product.updateProductInfo();

        bus.register(this);
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        bus.unregister(this);
    }


    private void setupUICallbacks() {

    }


    private void updateUI() {
        runOnUiThread(() -> {
            TextView txtView_productInfoName = findViewById(R.id.txtView_productInfoName);
            TextView txtView_productInfoID = findViewById(R.id.txtView_productInfoID);
            TextView txtView_productInfoDescription = findViewById(R.id.txtView_productInfoDescription);

            String productName = product.getName();
            String productId = product.getId();
            String productDescription = product.getDescription();

            if(productName == null) {
                productName = getString(R.string.product_info_default_name);
            }
            if(productDescription == null) {
                productDescription = getString(R.string.product_info_default_description);
            }

            txtView_productInfoName.setText(productName);
            txtView_productInfoID.setText(productId);
            txtView_productInfoDescription.setText(productDescription);
        });
    }


    @Subscribe
    public void fetchProductInfoSucceeded(FetchProductInfoSucceeded event) {
        updateUI();
    }

    @Subscribe
    public void fetchProductInfoSucceededPartially(FetchProductInfoSucceededPartially event) {
        updateUI();
        bus.post(new ToastMessage("Fetching product info succeeded with errors and / or warnings."));
    }

    @Subscribe
    public void fetchProductInfoFailed(FetchProductInfoFailed event) {
        updateUI();
        bus.post(new ToastMessage("Fetching product info failed."));
    }


    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.getMessage(), Toast.LENGTH_LONG).show();
    }

}
