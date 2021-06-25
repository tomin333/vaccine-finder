package com.technicles.vaccinetracker;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.technicles.vaccinetracker.response.StateModel;
import com.technicles.vaccinetracker.response.StateResponse;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class AutoCompleteAdapterState extends ArrayAdapter<String> implements Filterable {
    private ArrayList<String> data;

    public static List<StateModel> states;
    public static List<StateModel> filteredStates;
    MainActivity activity;

    AutoCompleteAdapterState(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
        this.states = new ArrayList<>();
        this.data = new ArrayList<>();
        this.activity = (MainActivity) context;
        fetchDistricts();
    }

    public void fetchDistricts() {
        RetrofitInterface retrofitInterface = RetrofitInstance.getRetrofitInstance().create(RetrofitInterface.class);

        Call<StateResponse> listCall = retrofitInterface.getStates();
        listCall.enqueue(new Callback<StateResponse>() {
            @Override
            public void onResponse(Call<StateResponse> call, Response<StateResponse> response) {
                if (null == response.body())
                    return;

                states = response.body().getResponse();
                notifyDataSetChanged();
                activity.autoCompleteAdapter.fetchDistricts();
            }

            @Override
            public void onFailure(Call<StateResponse> call, Throwable t) {
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
                    filteredStates = new ArrayList<>();
                    for (int ind = 0; ind < states.size(); ind++) {
                        String dName = states.get(ind).getStateName();
                        if (dName.toLowerCase().contains(search.toLowerCase())) {
                            suggestions.add(states.get(ind).getStateName());
                            filteredStates.add(states.get(ind));
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