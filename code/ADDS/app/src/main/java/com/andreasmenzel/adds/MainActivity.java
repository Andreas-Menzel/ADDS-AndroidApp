package com.andreasmenzel.adds;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.andreasmenzel.adds.Events.ToastMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    private static final EventBus bus = EventBus.getDefault();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setupUICallbacks();
    }

    @Override
    protected void onResume() {
        super.onResume();

        bus.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        bus.unregister(this);
    }


    private void setupUICallbacks() {
        findViewById(R.id.btn_showActivateAccountActivity).setOnClickListener((View v) -> {
            Intent switchActivityIntent = new Intent(this, ActivateAccountActivity.class);
            startActivity(switchActivityIntent);
        });

        findViewById(R.id.btn_showRegisterActivity).setOnClickListener((View v) -> {
            Intent switchActivityIntent = new Intent(this, RegisterActivity.class);
            startActivity(switchActivityIntent);
        });

        findViewById(R.id.btn_showLoginActivity).setOnClickListener((View v) -> {
            Intent switchActivityIntent = new Intent(this, LoginActivity.class);
            startActivity(switchActivityIntent);
        });

        findViewById(R.id.btn_showProductInfoActivity).setOnClickListener((View v) -> {
            Intent switchActivityIntent = new Intent(this, ProductInfoActivity.class);
            startActivity(switchActivityIntent);
        });

        findViewById(R.id.btn_showProductListActivity).setOnClickListener((View v) -> {
            Intent switchActivityIntent = new Intent(this, ProductListActivity.class);
            startActivity(switchActivityIntent);
        });
    }


    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.getMessage(), Toast.LENGTH_LONG).show();
    }

}