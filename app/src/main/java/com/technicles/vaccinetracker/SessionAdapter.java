package com.technicles.vaccinetracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

public class SessionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_ITEM = 0;
    private Activity activity;
    List<AvailabilityModel> availabilityModels;
    Integer updateCount = 0;

    public SessionAdapter(RecyclerView recyclerView, List<AvailabilityModel> availabilityModels,
                          Activity activity) {
        this.activity = activity;

        this.availabilityModels = availabilityModels;
    }


    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.session_item_recycler_row,
                parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserViewHolder) {
            AvailabilityModel model = availabilityModels.get(position);


            ((UserViewHolder) holder).date.setText("Date : " + model.getDate());
            ((UserViewHolder) holder).vaccineName.setText("Vaccine : " + model.getVaccine());

            String feeColor = model.getFeeType().equalsIgnoreCase("free") ? "green" : "yellow";
            ((UserViewHolder) holder).feeType.setText(HtmlCompat.fromHtml("<b>Type : <font " +
                            "color='" + feeColor + "'>" + model.getFeeType() + " </font></b> ",
                    HtmlCompat.FROM_HTML_MODE_COMPACT));

            ((UserViewHolder) holder).cost.setText("Fee : Rs. " + model.getCost());
            ((UserViewHolder) holder).name.setText("Center : " + model.getCenter());
            ((UserViewHolder) holder).address.setText("Address : " + model.getAddress());
            ((UserViewHolder) holder).pincode.setText("Pincode : " + model.getPincode());
            ((UserViewHolder) holder).minAge.setText("Age group : " + model.getAgeGroup() + "+");
            ((UserViewHolder) holder).dose1.setText(HtmlCompat.fromHtml("<b>Dose 1 : <font " +
                            "color='green'>" + model.getDose1() + " </font></b> ",
                    HtmlCompat.FROM_HTML_MODE_COMPACT));

            ((UserViewHolder) holder).dose2.setText(HtmlCompat.fromHtml("<b>Dose 2 : <font " +
                            "color='green'>" + model.getDose2() + " </font></b> ",
                    HtmlCompat.FROM_HTML_MODE_COMPACT));

            if (model.getPincode().equals(((MainActivity) activity).getSelectedPincode())) {
                holder.itemView.setBackground(ContextCompat.getDrawable(activity,
                        R.drawable.border_colored));
            } else {
                holder.itemView.setBackground(ContextCompat.getDrawable(activity,
                        R.drawable.border_transparent));
            }
        }
    }

    @Override
    public int getItemCount() {
        return availabilityModels == null ? 0 : availabilityModels.size();
    }


    private class UserViewHolder extends RecyclerView.ViewHolder {

        public TextView date;
        public TextView name;
        public TextView address;
        public TextView pincode;
        public TextView minAge;
        public TextView dose1;
        public TextView dose2;
        public TextView feeType;
        public TextView cost;
        public TextView vaccineName;

        public UserViewHolder(View view) {
            super(view);
            date = view.findViewById(R.id.date);
            name = view.findViewById(R.id.name);
            address = view.findViewById(R.id.address);
            pincode = view.findViewById(R.id.pincode);
            minAge = view.findViewById(R.id.minAge);
            dose1 = view.findViewById(R.id.dose1);
            dose2 = view.findViewById(R.id.dose2);
            feeType = view.findViewById(R.id.feeType);
            cost = view.findViewById(R.id.cost);
            vaccineName = view.findViewById(R.id.vaccineName);

            view.setOnClickListener((v)->{
                String url = "https://selfregistration.cowin.gov.in/";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                activity.startActivity(i);
            });
        }
    }


}
