package com.technicles.vaccinetracker.response;

import com.google.gson.annotations.SerializedName;

public class DistrictModel {
    @SerializedName("district_id")
    private String districtId;
    @SerializedName("district_name")
    private String districtName;

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }
}
