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
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// TODO: setupCallbacks also onResume?
// TODO: onResume: response missed?
public class RegisterActivity extends Activity {

    private final String activateAccountUrl = "http://adds-demo.an-men.de/";

    private final EventBus bus = new EventBus();

    private final AtomicBoolean registrationInProgress = new AtomicBoolean(false);

    private final ResponseAnalyzer responseAnalyzer = new ResponseAnalyzer();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
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

        btn_register.setOnClickListener((View v) -> {
            closeKeyboard();
            register();
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUI(UpdateUI event) {
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
    }


    public void register() {
        if(registrationInProgress.compareAndSet(false, true)) {
            responseAnalyzer.reset();
            bus.post(new UpdateUI());

            EditText editText_accountEmailRegister = findViewById(R.id.editText_accountEmailRegister);
            EditText editText_accountFirstnameRegister = findViewById(R.id.editText_accountFirstnameRegister);
            EditText editText_accountLastnameRegister = findViewById(R.id.editText_accountLastnameRegister);
            EditText editText_accountPasswordRegister = findViewById(R.id.editText_accountPasswordRegister);

            String accountEmail = editText_accountEmailRegister.getText().toString();
            String accountFirstname = editText_accountFirstnameRegister.getText().toString();
            String accountLastname = editText_accountLastnameRegister.getText().toString();
            String accountPassword = editText_accountPasswordRegister.getText().toString();

            // Check if all required editTexts were filled out.
            boolean error = false;
            if(TextUtils.isEmpty(accountEmail)) {
                editText_accountEmailRegister.setError("Please enter a valid email address.");
                error = true;
            }
            if(TextUtils.isEmpty(accountFirstname)) {
                editText_accountFirstnameRegister.setError("Please enter your firstname.");
                error = true;
            }
            if(TextUtils.isEmpty(accountLastname)) {
                editText_accountLastnameRegister.setError("Please enter your lastname.");
                error = true;
            }
            if(TextUtils.isEmpty(accountPassword)) {
                editText_accountPasswordRegister.setError("Please enter your password.");
                error = true;
            }
            if(error) {
                registrationInProgress.set(false);
                return;
            }

            // Generate password salt and hash
            String pwd_salt = "my_pwd_salt";
            String pwd_hash = "my_pws_hash";

            Request request = new Request.Builder()
                    .url(activateAccountUrl + "account/create?email=" + accountEmail + "&firstname=" + accountFirstname + "&lastname=" + accountLastname + "&pwd_salt=" + pwd_salt + "&pwd_hash=" + pwd_hash)
                    .build();

            OkHttpClient client = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    responseAnalyzer.addError(-1, "Registration failed: cannot reach server");
                    bus.post(new UpdateUI());

                    registrationInProgress.set(false);
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
                            bus.post(new ToastMessage("Welcome on board!"));
                            finish();
                        } else {
                            bus.post(new ToastMessage("Account not created."));
                        }
                    } else {
                        responseAnalyzer.addError(-1, "Registration failed: unknown cause (notSuccessful)");
                        bus.post(new UpdateUI());
                    }

                    registrationInProgress.set(false);
                }
            });
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
