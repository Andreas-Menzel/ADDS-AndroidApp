package com.andreasmenzel.adds;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds.Events.AccountActivationFailed;
import com.andreasmenzel.adds.Events.AccountActivationSucceededPartially;
import com.andreasmenzel.adds.Events.AccountActivationSucceeded;
import com.andreasmenzel.adds.Events.AccountAuthenticationFailed;
import com.andreasmenzel.adds.Events.AccountAuthenticationSucceeded;
import com.andreasmenzel.adds.Events.AccountAuthenticationSucceededPartially;
import com.andreasmenzel.adds.Events.AccountRegistrationFailed;
import com.andreasmenzel.adds.Events.AccountRegistrationSucceeded;
import com.andreasmenzel.adds.Events.AccountRegistrationSucceededPartially;
import com.andreasmenzel.adds.Events.ToastMessage;
import com.andreasmenzel.adds.Events.UpdateAccountActivationUI;
import com.andreasmenzel.adds.Events.UpdateAccountAuthenticationUI;
import com.andreasmenzel.adds.Events.UpdateAccountRegistrationUI;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Manages the communication to the User Management System and Booking System.
 */
public class CommunicationManager {

    private final EventBus bus;

    private final String userManagementSystemUrl = "http://adds-demo.an-men.de/";

    private String accountEmail;
    private String accountPwdHash;

    private final AtomicBoolean accountRegistrationInProgress;
    private final ResponseAnalyzer accountRegistrationResponseAnalyzer;

    private final AtomicBoolean accountAuthenticationInProgress;
    private final ResponseAnalyzer accountAuthenticationResponseAnalyzer;
    private String accountAuthenticationToken;
    private long accountAuthenticationTokenExpire;

    private final AtomicBoolean accountActivationInProgress;
    private final ResponseAnalyzer accountActivationResponseAnalyzer;


    /**
     * Sets up the event bus and all variables.
     */
    public CommunicationManager() {
        bus = EventBus.getDefault();

        accountEmail = "";
        accountPwdHash = "";

        accountRegistrationInProgress = new AtomicBoolean(false);
        accountRegistrationResponseAnalyzer = new ResponseAnalyzer();

        accountAuthenticationInProgress = new AtomicBoolean(false);
        accountAuthenticationResponseAnalyzer = new ResponseAnalyzer();
        accountAuthenticationToken = null;
        accountAuthenticationTokenExpire = 0;

        accountActivationInProgress = new AtomicBoolean(false);
        accountActivationResponseAnalyzer = new ResponseAnalyzer();
    }


    /**
     * Creates an account by sending a request to the User Management System.
     * Posts one of three events on the bus, depending on the success of the request:
     * AccountRegistrationSucceeded, AccountRegistrationSucceededPartially or
     * AccountRegistrationFailed.
     *
     * @param email The email of the user.
     * @param firstname The firstname of the user.
     * @param lastname The lastname of the user.
     * @param password The password of the user.
     */
    public void registerAccount(String email, String firstname, String lastname, String password) {
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


    /**
     * Executes the authenticateAccount function with autoAuthenticate=false.
     *
     * @param email The email of the user.
     * @param pwd_hash hash(password+hash).
     */
    public void authenticateAccount(String email, String pwd_hash) {
        authenticateAccount(email, pwd_hash, false);
    }

    // TODO: implement autoAuthenticate
    /**
     * Authenticates the account (email + hashed password) by requesting an authentication token
     * from the User Management System.
     * Posts one of three events on the bus, depending on the success of the request:
     * AccountAuthenticationSucceeded, AccountAuthenticationSucceededPartially or
     * AccountAuthenticationFailed.
     *
     * @param email The email of the user.
     * @param pwd_hash hash(password+salt).
     * @param autoAuthenticate Automatically start new authentication process before the current
     *                         authentication token gets invalid.
     */
    public void authenticateAccount(String email, String pwd_hash, boolean autoAuthenticate) {
        if(accountAuthenticationInProgress.compareAndSet(false, true)) {
            accountAuthenticationResponseAnalyzer.reset();
            bus.post(new UpdateAccountAuthenticationUI());

            Request request = new Request.Builder()
                    .url(userManagementSystemUrl + "authentication/authenticate?email=" + email + "&pwd_hash=" + pwd_hash)
                    .build();

            OkHttpClient client = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    accountAuthenticationResponseAnalyzer.addError(-1, "Authentication failed: Cannot reach server.");
                    accountAuthenticationInProgress.set(false);
                    bus.post(new AccountAuthenticationFailed());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        String myResponse = response.body().string();

                        accountAuthenticationResponseAnalyzer.analyze(myResponse);

                        if(accountAuthenticationResponseAnalyzer.wasExecuted()) {
                            JSONObject responsePayload = accountAuthenticationResponseAnalyzer.getPayload();
                            String authenticationToken = null;
                            long authenticationTokenExpire = 0;
                            if(responsePayload != null && responsePayload.has("auth_token") && responsePayload.has("exp")) {
                                try {
                                    authenticationToken = responsePayload.getString("auth_token");
                                    authenticationTokenExpire = responsePayload.getLong("exp");
                                } catch (JSONException e) {
                                    bus.post(new ToastMessage(e.getMessage()));
                                }
                            }

                            if(authenticationToken != null && authenticationTokenExpire > 0) {
                                accountEmail = email;
                                accountPwdHash = pwd_hash;
                                accountAuthenticationToken = authenticationToken;
                                accountAuthenticationTokenExpire = authenticationTokenExpire;

                                if(accountAuthenticationResponseAnalyzer.hasErrors() || accountAuthenticationResponseAnalyzer.hasWarnings()) {
                                    accountAuthenticationInProgress.set(false);
                                    bus.post(new AccountAuthenticationSucceededPartially());
                                } else {
                                    accountAuthenticationInProgress.set(false);
                                    bus.post(new AccountAuthenticationSucceeded());
                                }
                            } else {
                                accountAuthenticationInProgress.set(false);
                                accountAuthenticationResponseAnalyzer.addError(-1, "Authentication failed: authentication token or expire time missing");
                                bus.post(new AccountAuthenticationFailed());
                            }
                        } else {
                            accountAuthenticationInProgress.set(false);
                            bus.post(new AccountAuthenticationFailed());
                        }
                    } else {
                        accountAuthenticationInProgress.set(false);
                        accountAuthenticationResponseAnalyzer.addError(-1, "Authentication failed: unknown cause (notSuccessful)");
                        bus.post(new AccountAuthenticationFailed());
                    }
                }
            });
        } else {
            // TODO: Log?
        }
    }


    /**
     * Activates an account by sending the accountActivationCode to the User Management System.
     * Posts one of three events on the bus, depending on the success of the request:
     * AccountActivationSucceeded, AccountActivationSucceededPartially or AccountActivationFailed.
     *
     * @param accountActivationCode The account activation code.
     */
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


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GETTERS AND SETTERS                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns whether the account registration is currently in progress.
     *
     * @return accountRegistrationInProgress.
     */
    public AtomicBoolean getAccountRegistrationInProgress() {
        return accountRegistrationInProgress;
    }

    /**
     * Returns the accountRegistrationResponseAnalyzer. In this object the errors, warnings,
     * payload, ... from the last response for the account registration request are stored.
     *
     * @return accountActivationResponseAnalyzer.
     */
    public ResponseAnalyzer getAccountRegistrationResponseAnalyzer() {
        return accountRegistrationResponseAnalyzer;
    }

    /**
     * Returns whether the account authentication is currently in progress.
     *
     * @return accountAuthenticationInProgress.
     */
    public AtomicBoolean getAccountAuthenticationInProgress() {
        return accountAuthenticationInProgress;
    }

    /**
     * Returns the accountAuthenticationResponseAnalyzer. In this object the errors, warnings,
     * payload, ... from the last response for the account authentication request are stored.
     *
     * @return accountAuthenticationResponseAnalyzer.
     */
    public ResponseAnalyzer getAccountAuthenticationResponseAnalyzer() {
        return accountAuthenticationResponseAnalyzer;
    }

    /**
     * Returns whether the account activation is currently in progress.
     *
     * @return accountActivationInProgress.
     */
    public AtomicBoolean getAccountActivationInProgress() {
        return accountActivationInProgress;
    }

    /**
     * Returns the accountActivationResponseAnalyzer. In this object the errors, warnings,
     * payload, ... from the last response for the account activation request are stored.
     *
     * @return accountActivationResponseAnalyzer.
     */
    public ResponseAnalyzer getAccountActivationResponseAnalyzer() {
        return accountActivationResponseAnalyzer;
    }

}
