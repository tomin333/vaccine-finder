package com.technicles.vaccinefinder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
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

import com.judemanutd.autostarter.AutoStartPermissionHelper;
import com.technicles.vaccinefinder.response.CenterModel;
import com.technicles.vaccinefinder.response.DistrictModel;
import com.technicles.vaccinefinder.response.StateModel;
import com.technicles.vaccinefinder.services.VaccineLookupService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends Activity {
    Button searchBtn;
    boolean searchInProgress;

    AutoCompleteAdapterDistrict autoCompleteAdapter;
    AutoCompleteAdapterState autoCompleteAdapterState;


    public AutoCompleteTextView districtU;
    public AutoCompleteTextView stateU;
    public EditText pincodeU;
    public CheckBox isA45U;
    public CheckBox isA18U;
    public CheckBox a45dose1U;
    public CheckBox a45dose2U;
    public CheckBox a18dose1U;
    public CheckBox a18dose2U;

    public static String district;
    public static String state;
    public static String pincode;
    public static boolean isA45;
    public static boolean isA18;
    public static boolean a45dose1;
    public static boolean a45dose2;
    public static boolean a18dose1;
    public static boolean a18dose2;

    private String selectedPincode;
    private String selectedDistrictId;
    private String selectedStateId;

    private SessionAdapter adapter;
    private RecyclerView recyclerView;

    private RelativeLayout requestDetailsLayout;

    ServiceConnection serviceConnection = null;
    VaccineLookupService runningService;
    Intent serviceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!VaccineFinderUtil.haveNetworkConnection(this)) {
            final AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("No internet connection")
                    .setMessage("Please make sure you have an active data connection.")
                    .setCancelable(false)
                    .setPositiveButton("RETRY", null).setNegativeButton("EXIT", (d, w) -> {
                        d.dismiss();
                        finishAndRemoveTask();
                    }).show();

            Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(view -> {
                if (VaccineFinderUtil.haveNetworkConnection(MainActivity.this)) {
                    alertDialog.dismiss();
                    recreate();
                }

            });
        }


        initUIFields();

        if (isServiceRunning(VaccineLookupService.class)) {
            bindToService();
        } else {
            initDefaultUI();
        }

        districtU.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DistrictModel item = AutoCompleteAdapterDistrict.filteredDistricts.get(position);
                selectedDistrictId = item.getDistrictId();
            }
        });

        stateU.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                StateModel item = AutoCompleteAdapterState.filteredStates.get(position);
                if (!item.getStateId().equals(selectedStateId)) {
                    selectedDistrictId = null;
                    districtU.setText("");
                }
                selectedStateId = item.getStateId();
                autoCompleteAdapter.fetchDistricts();

            }
        });

        searchBtn.setOnClickListener((view) -> {
            if (!searchInProgress) {
                if (VaccineFinderUtil.haveNetworkConnection(this)) {
                    this.startLookup();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("No internet connection")
                            .setCancelable(false)
                            .setMessage("Please make sure you have an active data connection.")
                            .setPositiveButton(android.R.string.ok, (d, w) -> {
                                d.dismiss();
                            })
                            .show();
                }

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
        state = stateU.getText().toString();
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
                AutoCompleteAdapterDistrict.districts.stream().filter(d -> (d.getDistrictId().equals(selectedDistrictId) && d.getDistrictName().equals(district))).findAny().orElse(null);
        StateModel selectedStateModel = AutoCompleteAdapterState.states.stream().filter(d -> (d.getStateId().equals(selectedStateId) && d.getStateName().equals(state))).findAny().orElse(null);

        if (selectedStateModel == null) {
            Toast.makeText(this, "Invalid state", Toast.LENGTH_SHORT).show();
            return false;
        } else if (selectedDistrictModel == null) {
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

        return true;
    }


    public void startService() {
        handleDozeMode();

        serviceIntent = new Intent(this, VaccineLookupService.class);
        serviceIntent.putExtra("district", district);
        serviceIntent.putExtra("state", state);
        serviceIntent.putExtra("pincode", pincode);
        serviceIntent.putExtra("isA45", isA45);
        serviceIntent.putExtra("isA18", isA18);
        serviceIntent.putExtra("a45dose1", a45dose1);
        serviceIntent.putExtra("a45dose2", a45dose2);
        serviceIntent.putExtra("a18dose1", a18dose1);
        serviceIntent.putExtra("a18dose2", a18dose2);
        serviceIntent.putExtra("districtId", selectedDistrictId);
        serviceIntent.putExtra("stateId", selectedStateId);
        ContextCompat.startForegroundService(this, serviceIntent);

        if (adapter != null) {
            adapter.availabilityModels = new ArrayList<>();
        }

    }

    public void handleDozeMode() {

        /* Is autostarup permission needed? */
        if (AutoStartPermissionHelper.getInstance().isAutoStartPermissionAvailable(this)) {
            AutoStartPermissionHelper.getInstance().getAutoStartPermission(this);
        }
        /* I hate the doze mode */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission required")
                        .setCancelable(false)
                        .setMessage("In order for the app to run in background, please allow VaccineFinder to run unrestrictedly in the background.")
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                        startActivity(intent);
                                    }
                                })
                        .show();
            }

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            // Checks if the device is on a metered network
            if (connMgr.isActiveNetworkMetered()) {
                // Checks user’s Data Saver settings.
                switch (connMgr.getRestrictBackgroundStatus()) {
                    case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED:
                    case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED:


                        new AlertDialog.Builder(this)
                                .setTitle("Permission required")
                                .setCancelable(false)
                                .setMessage("In order for the app to run in background, please allow Vaccine Finder to connect to the COWIN API in the background")
                                .setPositiveButton(android.R.string.yes,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent();
                                                intent.setAction(android.provider.Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                intent.setData(Uri.parse("package:" + getPackageName()));

                                                startActivity(intent);
                                            }
                                        })
                                .show();
                }
            }
        }
    }

    public void stopService() {
        stopService(serviceIntent);
        unbindService(serviceConnection);
        servicePollHandler.removeCallbacksAndMessages(null);
        requestDetailsLayout.setVisibility(View.GONE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null)
            unbindService(serviceConnection);
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

                            List<AvailabilityModel> pinnedModels =
                                    models.stream().filter(m -> m.getPincode().equals(getSelectedPincode())).collect(Collectors.toList());
                            models.removeAll(pinnedModels);
                            models.addAll(0, pinnedModels);


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
        this.selectedStateId = service.getStateId();
        this.stateU.setText(service.getState());
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
        stateU.setEnabled(false);
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
        initDefaultUI();
        stateU.setEnabled(true);
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

        autoCompleteAdapter = new AutoCompleteAdapterDistrict(this,
                android.R.layout.simple_dropdown_item_1line);

        autoCompleteAdapterState = new AutoCompleteAdapterState(this,
                android.R.layout.simple_dropdown_item_1line);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        searchBtn = findViewById(R.id.searchBtn);

        districtU = findViewById(R.id.district);
        stateU = findViewById(R.id.state);
        pincodeU = findViewById(R.id.pincode);
        isA45U = findViewById(R.id.isA45U);
        isA18U = findViewById(R.id.isA18U);
        a45dose1U = findViewById(R.id.a45dose1U);
        a45dose2U = findViewById(R.id.a45dose2U);
        a18dose1U = findViewById(R.id.a18dose1U);
        a18dose2U = findViewById(R.id.a18dose2U);

        districtU.setAdapter(autoCompleteAdapter);
        stateU.setAdapter(autoCompleteAdapterState);
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

    public String getSelectedStateId() {
        return selectedStateId;
    }

    public void setSelectedStateId(String selectedStateId) {
        this.selectedStateId = selectedStateId;
    }
}
