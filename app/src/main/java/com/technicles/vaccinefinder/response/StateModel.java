package com.technicles.vaccinefinder.response;

import com.google.gson.annotations.SerializedName;

public class StateModel {
    @SerializedName("state_id")
    private String stateId;
    @SerializedName("state_name")
    private String stateName;

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }
}
