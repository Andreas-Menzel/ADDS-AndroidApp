package com.andreasmenzel.adds;

import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds.Events.AccountActivationFailed;
import com.andreasmenzel.adds.Events.AccountActivationSucceededPartially;
import com.andreasmenzel.adds.Events.AccountActivationSucceeded;
import com.andreasmenzel.adds.Events.AccountRegistrationFailed;
import com.andreasmenzel.adds.Events.AccountRegistrationSucceeded;
import com.andreasmenzel.adds.Events.AccountRegistrationSucceededPartially;
import com.andreasmenzel.adds.Events.UpdateAccountActivationUI;
import com.andreasmenzel.adds.Events.UpdateAccountRegistrationUI;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommunicationManager {

    private final EventBus bus;

    private final String userManagementSystemUrl = "http://adds-demo.an-men.de/";

    private String accountEmail;
    private String accountPwdHash;

    private final AtomicBoolean accountRegistrationInProgress;
    private final ResponseAnalyzer accountRegistrationResponseAnalyzer;

    private final AtomicBoolean authenticationInProgress;
    private final ResponseAnalyzer authenticationResponseAnalyzer;

    private final AtomicBoolean accountActivationInProgress;
    private final ResponseAnalyzer accountActivationResponseAnalyzer;


    public CommunicationManager() {
        bus = EventBus.getDefault();
        //bus.register(this); // TODO: unregister

        accountEmail = "";
        accountPwdHash = "";

        accountRegistrationInProgress = new AtomicBoolean(false);
        accountRegistrationResponseAnalyzer = new ResponseAnalyzer();

        authenticationInProgress = new AtomicBoolean(false);
        authenticationResponseAnalyzer = new ResponseAnalyzer();

        accountActivationInProgress = new AtomicBoolean(false);
        accountActivationResponseAnalyzer = new ResponseAnalyzer();
    }


    public void register_account(String email, String firstname, String lastname, String password) {
        if(accountRegistrationInProgress.compareAndSet(false, true)) {
            accountRegistrationResponseAnalyzer.reset();
            bus.post(new UpdateAccountRegistrationUI());

            // TODO
            // Generate password salt and hash
            String pwd_salt = "my_pwd_salt";
            String pwd_hash = "my_pws_hash";

            Request request = new Request.Builder()
                    .url(userManagementSystemUrl + "account/create?email=" + email + "&firstname=" + firstname + "&lastname=" + lastname + "&pwd_salt=" + pwd_salt + "&pwd_hash=" + pwd_hash)
                    .build();

            OkHttpClient client = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    accountRegistrationResponseAnalyzer.addError(-1, "Registration failed: Cannot reach server.");
                    accountRegistrationInProgress.set(false);
                    bus.post(new AccountRegistrationFailed());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        String myResponse = response.body().string();

                        accountRegistrationResponseAnalyzer.analyze(myResponse);

                        if(accountRegistrationResponseAnalyzer.wasExecuted()) {
                            if(accountRegistrationResponseAnalyzer.hasErrors() || accountRegistrationResponseAnalyzer.hasWarnings()) {
                                accountRegistrationInProgress.set(false);
                                bus.post(new AccountRegistrationSucceededPartially());
                            } else {
                                accountRegistrationInProgress.set(false);
                                bus.post(new AccountRegistrationSucceeded());
                            }
                        } else {
                            accountRegistrationInProgress.set(false);
                            bus.post(new AccountRegistrationFailed());
                        }
                    } else {
                        accountRegistrationInProgress.set(false);
                        accountRegistrationResponseAnalyzer.addError(-1, "Registration failed: unknown cause (notSuccessful)");
                        bus.post(new AccountRegistrationFailed());
                    }
                }
            });
        } else {
            // TODO: Log?
        }
    }


    /*private void authenticate() {
        if(authenticationInProgress.compareAndSet(false, true)) {
            authenticationResponseAnalyzer.reset();
            bus.post(new UpdateUI());

            EditText editText_accountActivationCode = findViewById(R.id.editText_accountActivationCode);

            String accountActivationCode = editText_accountActivationCode.getText().toString();

            if(TextUtils.isEmpty(accountActivationCode)) {
                editText_accountActivationCode.setError("Please enter a valid activation code.");

                authenticationInProgress.set(false);
                return;
            }

            Request request = new Request.Builder()
                    .url(activateAccountUrl + "account/activate?activation_code=" + accountActivationCode)
                    .build();

            OkHttpClient client = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    authenticationResponseAnalyzer.addError(-1, "Activation failed: Cannot reach server.");
                    bus.post(new UpdateUI());

                    authenticationInProgress.set(false);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        String myResponse = response.body().string();

                        authenticationResponseAnalyzer.analyze(myResponse);

                        if(authenticationResponseAnalyzer.hasSomethingChanged()) {
                            bus.post(new UpdateUI());
                        }

                        if(authenticationResponseAnalyzer.wasExecuted()) {
                            bus.post(new ToastMessage("Account successfully activated!"));
                            finish();
                        } else {
                            bus.post(new ToastMessage("Account not activated."));
                        }
                    } else {
                        authenticationResponseAnalyzer.addError(-1, "Activation failed: unknown cause (notSuccessful)");
                        bus.post(new UpdateUI());
                    }

                    authenticationInProgress.set(false);
                }
            });
        } else {
            // TODO: Log?
        }
    }*/


    public void activateAccount(String accountActivationCode) {
        if(accountActivationInProgress.compareAndSet(false, true)) {
            accountActivationResponseAnalyzer.reset();
            bus.post(new UpdateAccountActivationUI());

            Request request = new Request.Builder()
                    .url(userManagementSystemUrl + "account/activate?activation_code=" + accountActivationCode)
                    .build();

            OkHttpClient client = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    accountActivationResponseAnalyzer.addError(-1, "Activation failed: Cannot reach server.");
                    accountActivationInProgress.set(false);
                    bus.post(new AccountActivationFailed());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        String myResponse = response.body().string();

                        accountActivationResponseAnalyzer.analyze(myResponse);

                        if(accountActivationResponseAnalyzer.wasExecuted()) {
                            if(accountActivationResponseAnalyzer.hasErrors() || accountActivationResponseAnalyzer.hasWarnings()) {
                                accountActivationInProgress.set(false);
                                bus.post(new AccountActivationSucceededPartially());
                            } else {
                                accountActivationInProgress.set(false);
                                bus.post(new AccountActivationSucceeded());
                            }
                        } else {
                            accountActivationInProgress.set(false);
                            bus.post(new AccountActivationFailed());
                        }
                    } else {
                        accountActivationInProgress.set(false);
                        accountActivationResponseAnalyzer.addError(-1, "Activation failed: unknown cause (notSuccessful)");
                        bus.post(new AccountActivationFailed());
                    }
                }
            });
        } else {
            // TODO: Log?
        }
    }


    public AtomicBoolean getAccountRegistrationInProgress() {
        return accountRegistrationInProgress;
    }

    public ResponseAnalyzer getAccountRegistrationResponseAnalyzer() {
        return accountRegistrationResponseAnalyzer;
    }

    public AtomicBoolean getAuthenticationInProgress() {
        return authenticationInProgress;
    }

    public ResponseAnalyzer getAuthenticationResponseAnalyzer() {
        return authenticationResponseAnalyzer;
    }

    public AtomicBoolean getAccountActivationInProgress() {
        return accountActivationInProgress;
    }

    public ResponseAnalyzer getAccountActivationResponseAnalyzer() {
        return accountActivationResponseAnalyzer;
    }

}
