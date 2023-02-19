package com.andreasmenzel.adds;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds.DataClasses.Product;
import com.andreasmenzel.adds.DataClasses.ProductList;
import com.andreasmenzel.adds.Events.FetchProductInfoFailed;
import com.andreasmenzel.adds.Events.FetchProductInfoSucceeded;
import com.andreasmenzel.adds.Events.FetchProductInfoSucceededPartially;
import com.andreasmenzel.adds.Events.FetchProductListFailed;
import com.andreasmenzel.adds.Events.FetchProductListSucceeded;
import com.andreasmenzel.adds.Events.FetchProductListSucceededPartially;
import com.andreasmenzel.adds.Events.ToastMessage;
import com.andreasmenzel.adds.Events.UpdateProductInfoUI;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.List;

public class ProductListActivity extends AppCompatActivity {

    private EventBus bus;

    private ProductList productList = new ProductList("demowarehouse");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

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

        productList.updateProductList();

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
            TextView txtView_tmp_productList = findViewById(R.id.txtView_tmp_productList);

            String text = "";

            LinkedList<Product> products = productList.getProducts();

            if(products != null) {
                for(Product p : products) {
                    text += ": " + p.getName() + " :";
                }
            }

            if(txtView_tmp_productList == null) {
                text = "?";
            }

            txtView_tmp_productList.setText(text);
        });
    }


    @Subscribe
    public void fetchProductListSucceeded(FetchProductListSucceeded event) {
        updateUI();
    }

    @Subscribe
    public void fetchProductListSucceededPartially(FetchProductListSucceededPartially event) {
        updateUI();
        bus.post(new ToastMessage("Fetching product list succeeded with errors and / or warnings."));
    }

    @Subscribe
    public void fetchProductListFailed(FetchProductListFailed event) {
        updateUI();
        bus.post(new ToastMessage("Fetching product list failed."));
    }

    @Subscribe
    public void updateProductInfoUI(UpdateProductInfoUI event) {
        updateUI();
    }


    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.getMessage(), Toast.LENGTH_LONG).show();
    }

}
