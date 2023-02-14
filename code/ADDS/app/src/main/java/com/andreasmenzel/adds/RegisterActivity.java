package com.andreasmenzel.adds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.andreasmenzel.adds.Events.AccountRegistrationFailed;
import com.andreasmenzel.adds.Events.AccountRegistrationSucceeded;
import com.andreasmenzel.adds.Events.AccountRegistrationSucceededPartially;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

// TODO: setupCallbacks also onResume?
public class RegisterActivity extends Activity {

    private final EventBus bus = EventBus.getDefault();

    private CommunicationManager communicationManager;
    private ResponseAnalyzer responseAnalyzer;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        communicationManager = MyApplication.getCommunicationManager();
        responseAnalyzer = communicationManager.getAccountRegistrationResponseAnalyzer();
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
        Button btn_register = findViewById(R.id.btn_register);
        TextView txtView_alreadyHaveAnAccount = findViewById(R.id.txtView_alreadyHaveAnAccount);

        btn_register.setOnClickListener((View v) -> {
            closeKeyboard();
            registerAccount();
        });

        txtView_alreadyHaveAnAccount.setOnClickListener((View v) -> {
            Intent switchActivityIntent = new Intent(this, LoginActivity.class);
            startActivity(switchActivityIntent);
        });

    }


    @Subscribe
    public void updateUI(UpdateUI event) {
        runOnUiThread(() -> {
            TextView txtView_registerErrors = findViewById(R.id.txtView_registerErrors);
            TextView txtView_registerWarnings = findViewById(R.id.txtView_registerWarnings);

            String errors_string = responseAnalyzer.getErrorsString();
            if(errors_string != null) {
                txtView_registerErrors.setText(errors_string);
                txtView_registerErrors.setVisibility(View.VISIBLE);
            } else {
                txtView_registerErrors.setVisibility(View.GONE);
                txtView_registerErrors.setText("");
            }

            String warnings_string = responseAnalyzer.getWarningsString();
            if(warnings_string != null) {
                txtView_registerWarnings.setText(warnings_string);
                txtView_registerWarnings.setVisibility(View.VISIBLE);
            } else {
                txtView_registerWarnings.setVisibility(View.GONE);
                txtView_registerWarnings.setText("");
            }
        });
    }


    @Subscribe
    public void AccountRegistrationSucceeded(AccountRegistrationSucceeded event) {
        finish();
    }

    @Subscribe
    public void AccountRegistrationSucceededPartially(AccountRegistrationSucceededPartially event) {
        bus.post(new ToastMessage("Account created!"));
        updateUI(null);
    }

    @Subscribe
    public void AccountRegistrationFailed(AccountRegistrationFailed event) {
        updateUI(null);
    }


    public void registerAccount() {
        if(!communicationManager.getAccountRegistrationInProgress().get()) {
            EditText editText_accountEmailRegister = findViewById(R.id.editText_accountEmailRegister);
            EditText editText_accountFirstnameRegister = findViewById(R.id.editText_accountFirstnameRegister);
            EditText editText_accountLastnameRegister = findViewById(R.id.editText_accountLastnameRegister);
            EditText editText_accountPasswordRegister = findViewById(R.id.editText_accountPasswordRegister);

            String accountEmail = editText_accountEmailRegister.getText().toString();
            String accountFirstname = editText_accountFirstnameRegister.getText().toString();
            String accountLastname = editText_accountLastnameRegister.getText().toString();
            String accountPassword = editText_accountPasswordRegister.getText().toString();

            // Check if all required editTexts were filled out.
            boolean allRequiredFieldsFilled = true;
            if(TextUtils.isEmpty(accountEmail)) {
                editText_accountEmailRegister.setError("Please enter a valid email address.");
                allRequiredFieldsFilled = false;
            }
            if(TextUtils.isEmpty(accountFirstname)) {
                editText_accountFirstnameRegister.setError("Please enter your firstname.");
                allRequiredFieldsFilled = false;
            }
            if(TextUtils.isEmpty(accountLastname)) {
                editText_accountLastnameRegister.setError("Please enter your lastname.");
                allRequiredFieldsFilled = false;
            }
            if(TextUtils.isEmpty(accountPassword)) {
                editText_accountPasswordRegister.setError("Please enter your password.");
                allRequiredFieldsFilled = false;
            }
            if(!allRequiredFieldsFilled) {
                return;
            }

            communicationManager.registerAccount(accountEmail, accountFirstname, accountLastname, accountPassword);
        } else {
            bus.post(new ToastMessage("Registration already in progress."));
        }
    }


    /**
     * Closes the keyboard.
     */
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.getMessage(), Toast.LENGTH_LONG).show();
    }

}
