package com.technicles.vaccinefinder;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.technicles.vaccinefinder.response.CenterModel;
import com.technicles.vaccinefinder.response.DistrictModel;
import com.technicles.vaccinefinder.services.VaccineLookupService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends Activity {
    Button searchBtn;
    boolean searchInProgress;

    AutoCompleteAdapter autoCompleteAdapter;


    public static AutoCompleteTextView districtU;
    public static EditText pincodeU;
    public static CheckBox isA45U;
    public static CheckBox isA18U;
    public static CheckBox a45dose1U;
    public static CheckBox a45dose2U;
    public static CheckBox a18dose1U;
    public static CheckBox a18dose2U;

    public static String district;
    public static String pincode;
    public static boolean isA45;
    public static boolean isA18;
    public static boolean a45dose1;
    public static boolean a45dose2;
    public static boolean a18dose1;
    public static boolean a18dose2;

    private String selectedPincode;
    private String selectedDistrictId;

    private SessionAdapter adapter;
    private RecyclerView recyclerView;

    private long lastApiSuccessTime;
    private RelativeLayout requestDetailsLayout;

    ServiceConnection serviceConnection = null;
    VaccineLookupService runningService;
    Intent serviceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUIFields();

        if (isServiceRunning(VaccineLookupService.class)) {
            bindToService();
        } else {
            initDefaultUI();
        }

        districtU.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DistrictModel item = AutoCompleteAdapter.filteredDistricts.get(position);
                selectedDistrictId = item.getDistrictId();
            }
        });

        searchBtn.setOnClickListener((view) -> {
            if (!searchInProgress) {
                this.startLookup();
            } else {
                stopService();
                enableInputs();
                searchInProgress = false;
                searchBtn.setText("START SEARCHING");
            }

        });
    }


    public void startLookup() {
        readInputs();
        if (validateInputs()) {
            startService();
            bindToService();
        }

    }

    public void readInputs() {
        district = districtU.getText().toString();
        pincode = pincodeU.getText().toString();
        isA45 = isA45U.isChecked();
        isA18 = isA18U.isChecked();
        a45dose1 = a45dose1U.isChecked() && a45dose1U.isEnabled();
        a45dose2 = a45dose2U.isChecked() && a45dose2U.isEnabled();
        a18dose1 = a18dose1U.isChecked() && a18dose1U.isEnabled();
        a18dose2 = a18dose2U.isChecked() && a18dose2U.isEnabled();
    }

    public boolean validateInputs() {
        DistrictModel selectedDistrictModel =
                AutoCompleteAdapter.districts.stream().filter(d -> (d.getDistrictId().equals(selectedDistrictId) && d.getDistrictName().equals(district))).findAny().orElse(null);

        if (selectedDistrictModel == null) {
            Toast.makeText(this, "Invalid district", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!VaccineFinderUtil.isValidPinCode(pincode)) {
            Toast.makeText(this, "Invalid pincode", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!isA45 && !isA18) {
            Toast.makeText(this, "Select atleast 1 age group", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isA45 && !a45dose1 && !a45dose2) {
            Toast.makeText(this, "Select dose for 45+", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isA18 && !a18dose1 && !a18dose2) {
            Toast.makeText(this, "Select dose for 18+", Toast.LENGTH_SHORT).show();
            return false;
        }

        this.selectedPincode = pincode;


        SharedPreferences.Editor pref = VaccineFinderUtil.getPref(getBaseContext()).edit();
        pref.putString("district", district);
        pref.putString("pincode", pincode);
        pref.putBoolean("isA45", isA45);
        pref.putBoolean("isA18", isA18);
        pref.putBoolean("a45dose1", a45dose1);
        pref.putBoolean("a45dose2", a45dose2);
        pref.putBoolean("a18dose1", a18dose1);
        pref.putBoolean("a18dose2", a18dose2);

        pref.apply();

        return true;
    }


    public void startService() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
                return;
            }
        }

        serviceIntent = new Intent(this, VaccineLookupService.class);
        serviceIntent.putExtra("district", district);
        serviceIntent.putExtra("pincode", pincode);
        serviceIntent.putExtra("isA45", isA45);
        serviceIntent.putExtra("isA18", isA18);
        serviceIntent.putExtra("a45dose1", a45dose1);
        serviceIntent.putExtra("a45dose2", a45dose2);
        serviceIntent.putExtra("a18dose1", a18dose1);
        serviceIntent.putExtra("a18dose2", a18dose2);
        serviceIntent.putExtra("districtId", selectedDistrictId);
        ContextCompat.startForegroundService(this, serviceIntent);

    }

    public void stopService() {
        stopService(serviceIntent);
        unbindService(serviceConnection);
        servicePollHandler.removeCallbacksAndMessages(null);
        requestDetailsLayout.setVisibility(View.GONE);

    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    Handler servicePollHandler;

    public void bindToService() {
        serviceIntent = new Intent(this, VaccineLookupService.class);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                VaccineLookupService.LocalBinder binder =
                        (VaccineLookupService.LocalBinder) service;
                runningService = binder.getServiceInstance();
                initServiceStateInUI(runningService);
                adapter = new SessionAdapter(recyclerView,
                        new ArrayList<>(), MainActivity.this);
                servicePollHandler = new Handler();
                servicePollHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<AvailabilityModel> models = runningService.getAvailabilityModels();
                        if (null != models && models.size() > 0 && runningService.getCount().get() > adapter.updateCount) {
                            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                            adapter.availabilityModels = models;
                            adapter.notifyDataSetChanged();
                            adapter.updateCount = runningService.getCount().get();
                            MainActivity.this.recyclerView.setAdapter(adapter);
                            requestDetailsLayout.setVisibility(View.GONE);
                        } else if (null != models && models.size() == 0) {
                            if (adapter != null) {
                                adapter.availabilityModels = new ArrayList<>();
                                adapter.notifyDataSetChanged();
                            }
                            updateRequestDetails(runningService.getUnfilteredModels(),
                                    runningService.getLastApiSuccessTime(),
                                    runningService.getCount());
                        }
                        servicePollHandler.postDelayed(this, 1000);
                    }
                }, 1000);


            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i("name", "fev");
            }
        };

        bindService(serviceIntent, serviceConnection, 0);
    }

    public void initServiceStateInUI(VaccineLookupService service) {
        this.selectedDistrictId = service.getDistrictId();
        this.districtU.setText(service.getDistrict());
        this.pincodeU.setText(service.getPincode());
        this.isA45U.setChecked(service.getA45());
        this.isA18U.setChecked(service.getA18());
        this.a45dose1U.setChecked(service.getA45dose1());
        this.a45dose2U.setChecked(service.getA45dose2());
        this.a18dose1U.setChecked(service.getA18dose1());
        this.a18dose2U.setChecked(service.getA18dose2());
        readInputs();
        searchBtn.setText("STOP SEARCHING");
        disableInputs();
        searchInProgress = true;
    }

    public void disableInputs() {
        districtU.setEnabled(false);
        pincodeU.setEnabled(false);
        isA45U.setEnabled(false);
        isA18U.setEnabled(false);
        a45dose1U.setEnabled(false);
        a45dose2U.setEnabled(false);
        a18dose1U.setEnabled(false);
        a18dose2U.setEnabled(false);
    }

    public void enableInputs() {
        districtU.setEnabled(true);
        pincodeU.setEnabled(true);
        isA45U.setEnabled(true);
        isA18U.setEnabled(true);
        a45dose1U.setEnabled(true);
        a45dose2U.setEnabled(true);
        a18dose1U.setEnabled(true);
        a18dose2U.setEnabled(true);
        if (!isA45U.isChecked()) {
            a45dose1U.setEnabled(false);
            a45dose2U.setEnabled(false);
        }

        if (!isA18U.isChecked()) {
            a18dose1U.setEnabled(false);
            a18dose2U.setEnabled(false);
        }
    }

    public void updateRequestDetails(List<CenterModel> list, long lastApiSuccessTime,
                                     AtomicInteger count) {
        requestDetailsLayout.setVisibility(View.VISIBLE);
        TextView requestDetails = findViewById(R.id.requestDetails);

        DateFormat f1 = new SimpleDateFormat("HH:mm:ss");
        Date d = null;
        try {
            d = f1.parse(String.format("%1$tH:%1$tM:%1$tS",
                    lastApiSuccessTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        DateFormat f2 = new SimpleDateFormat("h:mm:ss a");

        int slotSize = list == null ? 0 : list.size();

        String html =
                "<b>Last successful request: " + f2.format(d) + "<br>Total requests : " + count.get() + "<br>Sessions with no available slots : " + slotSize + "</b>";
        requestDetails.setText(HtmlCompat.fromHtml(html,
                HtmlCompat.FROM_HTML_MODE_COMPACT));
    }


    public void initUIFields() {
        requestDetailsLayout = findViewById(R.id.requestDetailsLayout);

        autoCompleteAdapter = new AutoCompleteAdapter(this,
                android.R.layout.simple_dropdown_item_1line);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        searchBtn = findViewById(R.id.searchBtn);


        districtU = findViewById(R.id.district);
        pincodeU = findViewById(R.id.pincode);
        isA45U = findViewById(R.id.isA45U);
        isA18U = findViewById(R.id.isA18U);
        a45dose1U = findViewById(R.id.a45dose1U);
        a45dose2U = findViewById(R.id.a45dose2U);
        a18dose1U = findViewById(R.id.a18dose1U);
        a18dose2U = findViewById(R.id.a18dose2U);

        districtU.setAdapter(autoCompleteAdapter);
    }

    public void initDefaultUI() {
        a45dose1U.setEnabled(false);
        a45dose2U.setEnabled(false);
        a18dose1U.setEnabled(false);
        a18dose2U.setEnabled(false);

        isA45U.setOnCheckedChangeListener((view, ischecked) -> {
            if (ischecked) {
                a45dose1U.setEnabled(true);
                a45dose2U.setEnabled(true);
            } else {
                a45dose1U.setEnabled(false);
                a45dose2U.setEnabled(false);
            }
        });

        isA18U.setOnCheckedChangeListener((view, ischecked) -> {
            if (ischecked) {
                a18dose1U.setEnabled(true);
                a18dose2U.setEnabled(true);
            } else {
                a18dose1U.setEnabled(false);
                a18dose2U.setEnabled(false);
            }
        });
    }


    public String getSelectedPincode() {
        return selectedPincode;
    }

    public void setSelectedPincode(String selectedPincode) {
        this.selectedPincode = selectedPincode;
    }

    public String getSelectedDistrictId() {
        return selectedDistrictId;
    }

    public void setSelectedDistrictId(String selectedDistrictId) {
        this.selectedDistrictId = selectedDistrictId;
    }
}