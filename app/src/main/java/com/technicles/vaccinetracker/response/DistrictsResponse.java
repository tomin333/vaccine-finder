package com.technicles.vaccinetracker.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DistrictsResponse {
    @SerializedName("responseCode")
    long responseCode;

    @SerializedName("responseCodeText")
    String responseCodeText;

    @SerializedName("districts")
    List<DistrictModel> response;

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

    public List<DistrictModel> getResponse() {
        return response;
    }

    public void setResponse(List<DistrictModel> response) {
        this.response = response;
    }
}
