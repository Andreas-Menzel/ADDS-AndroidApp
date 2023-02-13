package com.andreasmenzel.adds;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// TODO: setupCallbacks also onResume?
// TODO: onResume: response missed?
public class ActivateAccountActivity extends Activity {

    private final String activateAccountUrl = "http://adds-demo.an-men.de/";

    private final EventBus bus = new EventBus();

    private final AtomicBoolean activationInProgress = new AtomicBoolean(false);

    private final ResponseAnalyzer responseAnalyzer = new ResponseAnalyzer();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activate_account);
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
        Button btn_activateAccount = findViewById(R.id.btn_activateAccount);

        btn_activateAccount.setOnClickListener((View v) -> {
            closeKeyboard();
            activateAccount();
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUI(UpdateUI event) {
        TextView txtView_activateAccountErrors = findViewById(R.id.txtView_activateAccountErrors);
        TextView txtView_activateAccountWarnings = findViewById(R.id.txtView_activateAccountWarnings);

        String errors_string = responseAnalyzer.getErrorsString();
        if(errors_string != null) {
            txtView_activateAccountErrors.setText(errors_string);
            txtView_activateAccountErrors.setVisibility(View.VISIBLE);
        } else {
            txtView_activateAccountErrors.setVisibility(View.GONE);
            txtView_activateAccountErrors.setText("");
        }

        String warnings_string = responseAnalyzer.getWarningsString();
        if(warnings_string != null) {
            txtView_activateAccountWarnings.setText(warnings_string);
            txtView_activateAccountWarnings.setVisibility(View.VISIBLE);
        } else {
            txtView_activateAccountWarnings.setVisibility(View.GONE);
            txtView_activateAccountWarnings.setText("");
        }
    }

    
    public void activateAccount() {
        if(activationInProgress.compareAndSet(false, true)) {
            responseAnalyzer.reset();
            bus.post(new UpdateUI());

            EditText editText_accountActivationCode = findViewById(R.id.editText_accountActivationCode);

            String accountActivationCode = editText_accountActivationCode.getText().toString();

            if(TextUtils.isEmpty(accountActivationCode)) {
                editText_accountActivationCode.setError("Please enter a valid activation code.");

                activationInProgress.set(false);
                return;
            }

            Request request = new Request.Builder()
                    .url(activateAccountUrl + "account/activate?activation_code=" + accountActivationCode)
                    .build();

            OkHttpClient client = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    responseAnalyzer.addError(-1, "Activation failed: Cannot reach server.");
                    bus.post(new UpdateUI());

                    activationInProgress.set(false);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        String myResponse = response.body().string();

                        responseAnalyzer.analyze(myResponse);

                        if(responseAnalyzer.hasSomethingChanged()) {
                            bus.post(new UpdateUI());
                        }

                        if(responseAnalyzer.wasExecuted()) {
                            bus.post(new ToastMessage("Account successfully activated!"));
                            finish();
                        } else {
                            bus.post(new ToastMessage("Account not activated."));
                        }
                    } else {
                        responseAnalyzer.addError(-1, "Activation failed: unknown cause (notSuccessful)");
                        bus.post(new UpdateUI());
                    }

                    activationInProgress.set(false);
                }
            });
        } else {
            bus.post(new ToastMessage("Activation already in progress."));
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
