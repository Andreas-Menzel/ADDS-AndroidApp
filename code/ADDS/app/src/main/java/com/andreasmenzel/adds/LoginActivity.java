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

import com.andreasmenzel.adds.Events.AccountAuthenticationFailed;
import com.andreasmenzel.adds.Events.AccountAuthenticationSucceeded;
import com.andreasmenzel.adds.Events.AccountAuthenticationSucceededPartially;
import com.andreasmenzel.adds.Events.ToastMessage;
import com.andreasmenzel.adds.Manager.CommunicationManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

// TODO: setupCallbacks also onResume?
public class LoginActivity extends Activity {

    private final EventBus bus = EventBus.getDefault();

    private CommunicationManager communicationManager;
    private ResponseAnalyzer responseAnalyzer;


    /**
     * Gets the communication manager and response analyzer.
     *
     * @param savedInstanceState savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        communicationManager = MyApplication.getCommunicationManagerAuthenticateAccountNotNull();
        responseAnalyzer = communicationManager.getResponseAnalyzer();
    }

    /**
     * Sets up the UI callbacks.
     */
    @Override
    protected void onStart() {
        super.onStart();

        setupUICallbacks();
    }

    /**
     * Registers to the event bus and updates the UI. Closes the activity if the user is already
     * logged in (has a valid authentication token).
     */
    @Override
    protected void onResume() {
        super.onResume();

        // TODO
        // Close this activity if the user is already logged in.
        /*if(communicationManager.getAccountAuthenticationTokenExpire() > (System.currentTimeMillis() / 1000)) {
            finish();
        }*/

        bus.register(this);
        updateUI();
    }

    /**
     * Unregisters from the event bus.
     */
    @Override
    protected void onPause() {
        super.onPause();

        bus.unregister(this);
    }


    /**
     * Sets up the callbacks for the UI elements.
     */
    private void setupUICallbacks() {
        Button btn_login = findViewById(R.id.btn_login);
        TextView txtView_dontHaveAnAccountYet = findViewById(R.id.txtView_dontHaveAnAccountYet);

        btn_login.setOnClickListener((View v) -> {
            closeKeyboard();
            authenticateAccount();
        });

        txtView_dontHaveAnAccountYet.setOnClickListener((View v) -> {
            Intent switchActivityIntent = new Intent(this, RegisterActivity.class);
            switchActivityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(switchActivityIntent);
        });

    }


    /**
     * Updates the UI elements (with the new information).
     */
    public void updateUI() {
        runOnUiThread(() -> {
            TextView txtView_LoginErrors = findViewById(R.id.txtView_LoginErrors);
            TextView txtView_LoginWarnings = findViewById(R.id.txtView_LoginWarnings);

            String errors_string = responseAnalyzer.getErrorsString();
            if(errors_string != null) {
                txtView_LoginErrors.setText(errors_string);
                txtView_LoginErrors.setVisibility(View.VISIBLE);
            } else {
                txtView_LoginErrors.setVisibility(View.GONE);
                txtView_LoginErrors.setText("");
            }

            String warnings_string = responseAnalyzer.getWarningsString();
            if(warnings_string != null) {
                txtView_LoginWarnings.setText(warnings_string);
                txtView_LoginWarnings.setVisibility(View.VISIBLE);
            } else {
                txtView_LoginWarnings.setVisibility(View.GONE);
                txtView_LoginWarnings.setText("");
            }
        });
    }


    /**
     * Closes this activity. This function is executed when the account authentication succeeded.
     *
     * @param event The AccountAuthenticationSucceeded event.
     */
    @Subscribe
    public void AccountAuthenticationSucceeded(AccountAuthenticationSucceeded event) {
        finish();
    }

    /**
     * Updates the UI and notifies the user. This function is executed when the account
     * authentication succeeded partially.
     *
     * @param event The AccountAuthenticationSucceededPartially event.
     */
    @Subscribe
    public void AccountAuthenticationSucceededPartially(AccountAuthenticationSucceededPartially event) {
        bus.post(new ToastMessage("Authenticated, thank you!"));
        updateUI();
    }

    /**
     * Updates the UI. This function is executed when the account authentication failed.
     *
     * @param event The AccountAuthenticationFailed event.
     */
    @Subscribe
    public void AccountAuthenticationFailed(AccountAuthenticationFailed event) {
        updateUI();
    }


    /**
     * Starts the account authentication process with the in the text fields provided information.
     */
    public void authenticateAccount() {
        if(!communicationManager.inProgress().get()) {
            EditText editText_accountEmailLogin = findViewById(R.id.editText_accountEmailLogin);
            EditText editText_accountPasswordLogin = findViewById(R.id.editText_accountPasswordLogin);

            String accountEmail = editText_accountEmailLogin.getText().toString();
            String accountPassword = editText_accountPasswordLogin.getText().toString();

            // Check if all required editTexts were filled out.
            boolean allRequiredFieldsFilled = true;
            if(TextUtils.isEmpty(accountEmail)) {
                editText_accountEmailLogin.setError("Please enter a valid email address.");
                allRequiredFieldsFilled = false;
            }
            if(TextUtils.isEmpty(accountPassword)) {
                editText_accountPasswordLogin.setError("Please enter your password.");
                allRequiredFieldsFilled = false;
            }
            if(!allRequiredFieldsFilled) {
                return;
            }

            communicationManager.authenticateAccount(accountEmail, accountPassword);
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
