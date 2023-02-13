package com.andreasmenzel.adds;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

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
    }
}