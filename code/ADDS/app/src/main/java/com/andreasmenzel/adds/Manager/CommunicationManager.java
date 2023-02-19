package com.andreasmenzel.adds.Manager;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds.DataClasses.Product;
import com.andreasmenzel.adds.Events.AccountActivationFailed;
import com.andreasmenzel.adds.Events.AccountActivationSucceeded;
import com.andreasmenzel.adds.Events.AccountActivationSucceededPartially;
import com.andreasmenzel.adds.Events.AccountAuthenticationFailed;
import com.andreasmenzel.adds.Events.AccountAuthenticationSucceeded;
import com.andreasmenzel.adds.Events.AccountAuthenticationSucceededPartially;
import com.andreasmenzel.adds.Events.AccountRegistrationFailed;
import com.andreasmenzel.adds.Events.AccountRegistrationSucceeded;
import com.andreasmenzel.adds.Events.AccountRegistrationSucceededPartially;
import com.andreasmenzel.adds.Events.Event;
import com.andreasmenzel.adds.Events.FetchProductInfoFailed;
import com.andreasmenzel.adds.Events.FetchProductInfoSucceeded;
import com.andreasmenzel.adds.Events.FetchProductInfoSucceededPartially;
import com.andreasmenzel.adds.Events.ToastMessage;
import com.andreasmenzel.adds.Events.UpdateAccountActivationUI;
import com.andreasmenzel.adds.Events.UpdateAccountAuthenticationUI;
import com.andreasmenzel.adds.Events.UpdateAccountRegistrationUI;
import com.andreasmenzel.adds.Events.UpdateProductInfoUI;
import com.andreasmenzel.adds.ResponseAnalyzer;

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

    private String UserManagementSystemUrl = "http://adds-demo.an-men.de/";
    private String BookingSystemUrl = "http://adds-demo.an-men.de/";

    private final AtomicBoolean inProgress;
    private final ResponseAnalyzer responseAnalyzer;

    private final Callable<Boolean> accountAuthenticationProcessPayload = new Callable() {
        @Override
        public Boolean call() throws Exception {
            JSONObject responsePayload = responseAnalyzer.getPayload();
            String authenticationToken = null;
            long authenticationTokenExpire = 0;
            if(responsePayload != null && responsePayload.has("auth_token") && responsePayload.has("exp")) {
                try {
                    authenticationToken = responsePayload.getString("auth_token");
                    authenticationTokenExpire = responsePayload.getLong("exp");
                } catch (JSONException e) {
                    // TODO: Error / Log?
                    bus.post(new ToastMessage(e.getMessage()));
                }
            }

            if(authenticationToken != null && authenticationTokenExpire > 0) {
                // TODO: save data
                /*accountEmail = email;
                accountPwdHash = pwd_hash;
                accountAuthenticationToken = authenticationToken;
                accountAuthenticationTokenExpire = authenticationTokenExpire;*/

                return true;
            }

            responseAnalyzer.addError(-1, "Authentication failed: authentication token or expire time missing or invalid");
            return false;
        }
    };
    private final Callable<Boolean> fetchProductProcessPayload = new Callable() {
        @Override
        public Boolean call() throws Exception {
            JSONObject responsePayload = responseAnalyzer.getPayload();

            String productName = null;
            String productDescription = null;
            if(responsePayload != null && responsePayload.has("name") && responsePayload.has("description")) {
                try {
                    productName = responsePayload.getString("name");
                    productDescription = responsePayload.getString("description");
                } catch (JSONException e) {
                    // TODO: Error / Log?
                    bus.post(new ToastMessage(e.getMessage()));
                }
            }

            if(productName != null && productDescription != null) {
                if(product != null) {
                    product.setName(productName);
                    product.setDescription(productDescription);

                    bus.post(new UpdateProductInfoUI());
                }

                return true;
            }

            responseAnalyzer.addError(-1, "Fetching product failed: response incomplete or invalid");
            return false;
        }
    };

    public enum RequestTypes {
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
        },
        fetchProduct {
            @NonNull
            @Override
            public String toString() {
                return "Product";
            }
        }
    }

    private RequestTypes requestType;


    Product product;


    public CommunicationManager(Product product) {
        this.product = product;
        requestType = RequestTypes.fetchProduct;

        bus = EventBus.getDefault();

        inProgress = new AtomicBoolean(false);
        responseAnalyzer = new ResponseAnalyzer();
    }


    /**
     * Sets up the event bus and all variables.
     */
    public CommunicationManager(RequestTypes requestType) {
        this.requestType = requestType;

        bus = EventBus.getDefault();

        inProgress = new AtomicBoolean(false);
        responseAnalyzer = new ResponseAnalyzer();
    }


    /**
     * Sends a request.
     *
     * @param requestUrl The URL with parameters.
     */
    public void sendRequest(String requestUrl) {
        Request request = new Request.Builder()
                .url(requestUrl)
                .build();

        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Event eventFailed = null;

                switch(requestType) {
                    case registerAccount:
                        eventFailed = new AccountRegistrationFailed();
                        break;
                    case authenticateAccount:
                        eventFailed = new AccountAuthenticationFailed();
                        break;
                    case activateAccount:
                        eventFailed = new AccountActivationFailed();
                        break;
                    case fetchProduct:
                        eventFailed = new FetchProductInfoFailed();
                        break;
                }

                responseAnalyzer.addError(-1, requestType.toString() + " failed: Cannot reach server.");
                inProgress.set(false);
                bus.post(eventFailed);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Event eventSucceeded = null;
                Event eventSucceededPartially = null;
                Event eventFailed = null;
                Callable<Boolean> checkPayloadCallable = null;

                switch(requestType) {
                    case registerAccount:
                        eventSucceeded = new AccountRegistrationSucceeded();
                        eventSucceededPartially = new AccountRegistrationSucceededPartially();
                        eventFailed = new AccountRegistrationFailed();
                        break;
                    case authenticateAccount:
                        eventSucceeded = new AccountAuthenticationSucceeded();
                        eventSucceededPartially = new AccountAuthenticationSucceededPartially();
                        eventFailed = new AccountAuthenticationFailed();
                        checkPayloadCallable = accountAuthenticationProcessPayload;
                        break;
                    case activateAccount:
                        eventSucceeded = new AccountActivationSucceeded();
                        eventSucceededPartially = new AccountActivationSucceededPartially();
                        eventFailed = new AccountActivationFailed();
                        break;
                    case fetchProduct:
                        eventSucceeded = new FetchProductInfoSucceeded();
                        eventSucceededPartially = new FetchProductInfoSucceededPartially();
                        eventFailed = new FetchProductInfoFailed();
                        checkPayloadCallable = fetchProductProcessPayload;
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
        if(inProgress.compareAndSet(false, true)) {
            responseAnalyzer.reset();
            bus.post(new UpdateAccountRegistrationUI());

            // TODO
            // Generate password salt and hash
            String pwd_salt = "my_pwd_salt";
            String pwd_hash = "my_pws_hash";

            String requestUrl = UserManagementSystemUrl + "account/create?email=" + email + "&firstname=" + firstname + "&lastname=" + lastname + "&pwd_salt=" + pwd_salt + "&pwd_hash=" + pwd_hash;
            sendRequest(requestUrl);
        } else {
            // TODO: Log?
        }
    }


    /**
     * Authenticates the account (email + hashed password) by requesting an authentication token
     * from the User Management System.
     * Posts one of three events on the bus, depending on the success of the request:
     * AccountAuthenticationSucceeded, AccountAuthenticationSucceededPartially or
     * AccountAuthenticationFailed.
     *
     * @param email The email of the user.
     * @param pwd_hash hash(password+salt).
     */
    public void authenticateAccount(String email, String pwd_hash) {
        if(inProgress.compareAndSet(false, true)) {
            responseAnalyzer.reset();
            bus.post(new UpdateAccountAuthenticationUI());

            String responseUrl = UserManagementSystemUrl + "authentication/authenticate?email=" + email + "&pwd_hash=" + pwd_hash;
            sendRequest(responseUrl);
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
        if(inProgress.compareAndSet(false, true)) {
            responseAnalyzer.reset();
            bus.post(new UpdateAccountActivationUI());

            String requestUrl = UserManagementSystemUrl + "account/activate?activation_code=" + accountActivationCode;
            sendRequest(requestUrl);
        } else {
            // TODO: Log?
        }
    }


    /**
     * Updates the information of a product.
     *
     * @param productId The id of the product.
     */
    public void updateProductInfo(String productId) {
        if(inProgress.compareAndSet(false, true)) {
            responseAnalyzer.reset();
            bus.post(new UpdateProductInfoUI());

            String requestUrl = BookingSystemUrl + "api/product_info?id=" + productId;
            sendRequest(requestUrl);
        } else {
            // TODO: Log?
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GETTERS AND SETTERS                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns whether the communication is currently in progress.
     *
     * @return inProgress.
     */
    public AtomicBoolean inProgress() {
        return inProgress;
    }

    /**
     * Returns the responseAnalyzer. In this object the errors, warnings, payload, ... from the last
     * response for the account registration request are stored.
     *
     * @return accountActivationResponseAnalyzer.
     */
    public ResponseAnalyzer getResponseAnalyzer() {
        return responseAnalyzer;
    }

}
