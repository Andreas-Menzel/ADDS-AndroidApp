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
import com.andreasmenzel.adds.Events.Event;
import com.andreasmenzel.adds.Events.ToastMessage;
import com.andreasmenzel.adds.Events.UpdateAccountActivationUI;
import com.andreasmenzel.adds.Events.UpdateAccountAuthenticationUI;
import com.andreasmenzel.adds.Events.UpdateAccountRegistrationUI;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Callable;
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

    enum RequestType {
        registerAccount {
            @NonNull
            @Override
            public String toString() {
                return "Account Registration";
            }
        },
        authenticateAccount {
            @NonNull
            @Override
            public String toString() {
                return "Account Authentication";
            }
        },
        activateAccount {
            @NonNull
            @Override
            public String toString() {
                return "Account Activation";
            }
        }
    }


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
     * Sends a request.
     *
     * @param requestType One of the RequestTypes: registerAccount | authenticateAccount |
     *                    activateAccount.
     * @param requestUrl The URL with parameters.
     * @param checkPayloadCallable Callable that checks the payload. Has to return a Boolean. Can be
     *                             null.
     */
    public void sendRequest(RequestType requestType, String requestUrl, Callable<Boolean> checkPayloadCallable) {
        Request request = new Request.Builder()
                .url(requestUrl)
                .build();

        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AtomicBoolean inProgress = null;
                ResponseAnalyzer responseAnalyzer = null;
                Event event = null;

                switch(requestType) {
                    case registerAccount:
                        inProgress = accountRegistrationInProgress;
                        responseAnalyzer = accountRegistrationResponseAnalyzer;
                        event = new AccountRegistrationFailed();
                        break;
                    case authenticateAccount:
                        inProgress = accountAuthenticationInProgress;
                        responseAnalyzer = accountAuthenticationResponseAnalyzer;
                        event = new AccountAuthenticationFailed();
                        break;
                    case activateAccount:
                        inProgress = accountActivationInProgress;
                        responseAnalyzer = accountActivationResponseAnalyzer;
                        event = new AccountActivationFailed();
                        break;
                }

                responseAnalyzer.addError(-1, requestType.toString() + " failed: Cannot reach server.");
                inProgress.set(false);
                bus.post(event);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                AtomicBoolean inProgress = null;
                ResponseAnalyzer responseAnalyzer = null;
                Event eventSucceeded = null;
                Event eventSucceededPartially = null;
                Event eventFailed = null;

                switch(requestType) {
                    case registerAccount:
                        inProgress = accountRegistrationInProgress;
                        responseAnalyzer = accountRegistrationResponseAnalyzer;
                        eventSucceeded = new AccountRegistrationSucceeded();
                        eventSucceededPartially = new AccountRegistrationSucceededPartially();
                        eventFailed = new AccountRegistrationFailed();
                        break;
                    case authenticateAccount:
                        inProgress = accountAuthenticationInProgress;
                        responseAnalyzer = accountAuthenticationResponseAnalyzer;
                        eventSucceeded = new AccountAuthenticationSucceeded();
                        eventSucceededPartially = new AccountAuthenticationSucceededPartially();
                        eventFailed = new AccountAuthenticationFailed();
                        break;
                }

                if(response.isSuccessful()) {
                    String myResponse = response.body().string();

                    responseAnalyzer.analyze(myResponse);

                    if(responseAnalyzer.wasExecuted()) {
                        boolean payloadOk = true;
                        if(checkPayloadCallable != null) {
                            try {
                                payloadOk = (boolean)checkPayloadCallable.call();
                            } catch (Exception e) {
                                payloadOk = false;
                            }
                        }

                        if(payloadOk) {
                            if(responseAnalyzer.hasErrors() || responseAnalyzer.hasWarnings()) {
                                inProgress.set(false);
                                bus.post(eventSucceededPartially);
                            } else {
                                inProgress.set(false);
                                bus.post(eventSucceeded);
                            }
                        } else {
                            inProgress.set(false);
                            bus.post(eventFailed);
                        }
                    } else {
                        inProgress.set(false);
                        bus.post(eventFailed);
                    }
                } else {
                    inProgress.set(false);
                    responseAnalyzer.addError(-1, requestType.toString() + " failed: unknown cause (notSuccessful)");
                    bus.post(eventFailed);
                }
            }
        });
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

            String url = userManagementSystemUrl + "account/create?email=" + email + "&firstname=" + firstname + "&lastname=" + lastname + "&pwd_salt=" + pwd_salt + "&pwd_hash=" + pwd_hash;
            sendRequest(RequestType.registerAccount, url, null);
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

            String url = userManagementSystemUrl + "authentication/authenticate?email=" + email + "&pwd_hash=" + pwd_hash;
            Callable<Boolean> checkPayloadCallable = new Callable() {
                @Override
                public Boolean call() throws Exception {
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

                        return true;
                    }

                    accountAuthenticationResponseAnalyzer.addError(-1, "Authentication failed: authentication token or expire time missing or invalid");
                    return false;
                }
            };
            sendRequest(RequestType.authenticateAccount, url, checkPayloadCallable);
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

            String url = userManagementSystemUrl + "account/activate?activation_code=" + accountActivationCode;
            sendRequest(RequestType.activateAccount, url, null);
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
     * Returns the accountAuthenticationToken.
     *
     * @return accountAuthenticationToken.
     */
    public String getAccountAuthenticationToken() {
        return accountAuthenticationToken;
    }

    /**
     * Returns the expire timestamp of the account authentication.
     *
     * @return accountAuthenticationTokenExpire.
     */
    public long getAccountAuthenticationTokenExpire() {
        return accountAuthenticationTokenExpire;
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
