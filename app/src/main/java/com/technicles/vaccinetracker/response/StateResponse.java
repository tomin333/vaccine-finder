package com.technicles.vaccinetracker.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StateResponse {
    @SerializedName("responseCode")
    long responseCode;

    @SerializedName("responseCodeText")
    String responseCodeText;

    @SerializedName("states")
    List<StateModel> response;

    public long getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(long responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseCodeText() {
        return responseCodeText;
    }

    public void setResponseCodeText(String responseCodeText) {
        this.responseCodeText = responseCodeText;
    }

    public List<StateModel> getResponse() {
        return response;
    }

    public void setResponse(List<StateModel> response) {
        this.response = response;
    }
}
