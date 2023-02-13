package com.andreasmenzel.adds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

public class ResponseAnalyzer {

    private boolean executed = false;
    private final LinkedList<String> errors = new LinkedList<String>();
    private final LinkedList<String> warnings = new LinkedList<String>();

    private boolean somethingChanged = true;

    public void reset() {
        executed = false;
        errors.clear();
        warnings.clear();

        somethingChanged = true;
    }

    public void analyze(String response) {
        try {
            JSONObject json = new JSONObject(response);

            boolean response_executed = json.getBoolean("executed");
            JSONArray response_errors = json.getJSONArray("errors");
            JSONArray response_warnings = json.getJSONArray("warnings");

            setExecuted(response_executed);

            // Add errors
            for(int i = 0; i < response_errors.length(); ++i) {
                JSONObject err = response_errors.getJSONObject(i);
                addError(err.getInt("err_id"), err.getString("err_msg"));
            }

            // Add warnings
            for(int i = 0; i < response_warnings.length(); ++i) {
                JSONObject warn = response_warnings.getJSONObject(i);
                addWarning(warn.getInt("warn_id"), warn.getString("warn_msg"));
            }

            // TODO: Payload
        } catch (JSONException e) {
            addError(-1, "Invalid response from server.");
        }
    }

    public void addError(int err_id, String err_msg) {
        StringBuilder error = new StringBuilder();

        if(err_id >= 0) {
            error.append("[");
            error.append(err_id);
            error.append("] ");
        }
        error.append(err_msg);

        errors.add(error.toString());

        somethingChanged = true;
    }

    public void addWarning(int warn_id, String warn_msg) {
        StringBuilder warning = new StringBuilder();

        if(warn_id >= 0) {
            warning.append("[");
            warning.append(warn_id);
            warning.append("] ");
        }
        warning.append(warn_msg);

        warnings.add(warning.toString());

        somethingChanged = true;
    }

    private void setExecuted(boolean executed) {
        this.executed = executed;

        somethingChanged = true;
    }

    public boolean wasExecuted() {
        return executed;
    }

    public String getErrorsString() {
        if(errors.isEmpty()) return null;

        StringBuilder errors_string = new StringBuilder();

        for(int i = 0; i < errors.size(); ++i) {
            if(i > 0) errors_string.append("\n\n");
            errors_string.append(errors.get(i));
        }

        return errors_string.toString();
    }

    public String getWarningsString() {
        if(warnings.isEmpty()) return null;

        StringBuilder warnings_string = new StringBuilder();

        for(int i = 0; i < warnings.size(); ++i) {
            if(i > 0) warnings_string.append("\n\n");
            warnings_string.append(warnings.get(i));
        }

        return warnings_string.toString();
    }

    public boolean hasSomethingChanged() {
        boolean tmp_somethingChanged = somethingChanged;
        somethingChanged = false;
        return tmp_somethingChanged;
    }

}
