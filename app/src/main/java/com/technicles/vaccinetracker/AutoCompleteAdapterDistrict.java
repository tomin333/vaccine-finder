package com.technicles.vaccinetracker;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.technicles.vaccinetracker.response.DistrictModel;
import com.technicles.vaccinetracker.response.DistrictsResponse;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class AutoCompleteAdapterDistrict extends ArrayAdapter<String> implements Filterable {
    private ArrayList<String> data;

    public static List<DistrictModel> districts;
    public static List<DistrictModel> filteredDistricts;
    MainActivity activity;

    AutoCompleteAdapterDistrict(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
        this.districts = new ArrayList<>();
        this.data = new ArrayList<>();
        activity = (MainActivity) context;
        //fetchDistricts();
    }

    public boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public void fetchDistricts() {
        RetrofitInterface retrofitInterface = RetrofitInstance.getRetrofitInstance().create(RetrofitInterface.class);

        Call<DistrictsResponse> listCall = retrofitInterface.getDistrictsByState(isEmpty(activity.getSelectedStateId()) ? "17" : activity.getSelectedStateId());
        listCall.enqueue(new Callback<DistrictsResponse>() {
            @Override
            public void onResponse(Call<DistrictsResponse> call, Response<DistrictsResponse> response) {
                if (null == response.body())
                    return;

                districts = response.body().getResponse();
                notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<DistrictsResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });


    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return data.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null) {
                    String search = String.valueOf(constraint);
                    ArrayList<String> suggestions = new ArrayList<>();
                    filteredDistricts = new ArrayList<>();
                    for (int ind = 0; ind < districts.size(); ind++) {
                        String dName = districts.get(ind).getDistrictName();
                        if (dName.toLowerCase().contains(search.toLowerCase())) {
                            suggestions.add(districts.get(ind).getDistrictName());
                            filteredDistricts.add(districts.get(ind));
                        }
                    }
                    results.values = suggestions;
                    results.count = suggestions.size();
                    data = suggestions;
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else notifyDataSetInvalidated();
            }
        };
    }

}