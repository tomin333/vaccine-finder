package com.technicles.vaccinetracker.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SessionResponse {
    @SerializedName("responseCode")
    long responseCode;

    @SerializedName("responseCodeText")
    String responseCodeText;

    @SerializedName("sessions")
    List<SessionModel> response;

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

    public List<SessionModel> getResponse() {
        return response;
    }

    public void setResponse(List<SessionModel> response) {
        this.response = response;
    }
}