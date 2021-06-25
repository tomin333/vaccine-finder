package com.technicles.vaccinetracker.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CenterResponse {

    @SerializedName("responseCode")
    long responseCode;

    @SerializedName("responseCodeText")
    String responseCodeText;

    @SerializedName("centers")
    List<CenterModel> response;

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

    public List<CenterModel> getResponse() {
        return response;
    }

    public void setResponse(List<CenterModel> response) {
        this.response = response;
    }
}
