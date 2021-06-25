package com.technicles.vaccinetracker;

import com.technicles.vaccinetracker.response.CenterResponse;
import com.technicles.vaccinetracker.response.DistrictsResponse;
import com.technicles.vaccinetracker.response.SessionResponse;
import com.technicles.vaccinetracker.response.StateResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetrofitInterface {
    @GET("api/v2/appointment/sessions/public/findByDistrict")
    Call<SessionResponse> getSessionsByDistrict(@Query("district_id") String districtId, @Query("date") String date);

    @GET("api/v2/appointment/sessions/public/calendarByDistrict")
    Call<CenterResponse> getCalendarByDistrict(@Query("district_id") String districtId, @Query("date") String date);

    @Headers({"Cache-Control: max-age=640000", "User-Agent: Vaccine Finder"})
    @GET("api/v2/admin/location/districts/{stateId}")
    Call<DistrictsResponse> getDistrictsByState(@Path("stateId") String stateId);

    @Headers({"Cache-Control: max-age=640000", "User-Agent: Vaccine Finder"})
    @GET("api/v2/admin/location/states")
    Call<StateResponse> getStates();

}
