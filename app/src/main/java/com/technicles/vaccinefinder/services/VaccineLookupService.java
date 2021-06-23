package com.technicles.vaccinefinder.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.technicles.vaccinefinder.AppConstants;
import com.technicles.vaccinefinder.AvailabilityModel;
import com.technicles.vaccinefinder.MainActivity;
import com.technicles.vaccinefinder.R;
import com.technicles.vaccinefinder.RetrofitInstance;
import com.technicles.vaccinefinder.RetrofitInterface;
import com.technicles.vaccinefinder.VaccineFinderUtil;
import com.technicles.vaccinefinder.response.CenterModel;
import com.technicles.vaccinefinder.response.CenterResponse;
import com.technicles.vaccinefinder.response.SessionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VaccineLookupService extends Service {

    IBinder myBinder = new LocalBinder();
    NotificationManagerCompat notificationManager = null;

    android.os.Handler customHandler;
    public volatile AtomicInteger count;

    String districtId;
    String stateId;
    String district;
    String state;
    String pincode;
    Boolean isA45;
    Boolean isA18;
    Boolean a45dose1;
    Boolean a45dose2;
    Boolean a18dose1;
    Boolean a18dose2;


    List<AvailabilityModel> availabilityModels;
    List<CenterModel> unfilteredModels;
    long lastApiSuccessTime;

    List<String> notifiedSessionIds;


    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notifiedSessionIds = new ArrayList<>();
        if (intent != null) {
            district = intent.getStringExtra("district");
            state = intent.getStringExtra("state");
            districtId = intent.getStringExtra("districtId");
            stateId = intent.getStringExtra("stateId");
            pincode = intent.getStringExtra("pincode");
            isA45 = intent.getBooleanExtra("isA45", false);
            isA18 = intent.getBooleanExtra("isA18", false);
            a45dose1 = intent.getBooleanExtra("a45dose1", false);
            a45dose2 = intent.getBooleanExtra("a45dose2", false);
            a18dose1 = intent.getBooleanExtra("a18dose1", false);
            a18dose2 = intent.getBooleanExtra("a18dose2", false);

            customHandler = new android.os.Handler();
            count = new AtomicInteger();
            customHandler.postDelayed(updateTimerThread, 0);
            notification();
        } else {
            stopSelf();
            onDestroy();
        }

        return START_STICKY;

    }


    public void notification() {
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "com.technicles.vaccinefinder";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Vaccine",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Vaccine Finder")
                    .setContentText("Actively looking for vaccines")
                    .setSmallIcon(R.drawable.notif_icon)
                    .build();

            startForeground(1, notification);
        } else {
            Notification notification = new NotificationCompat.Builder(this, null)
                    .setContentTitle("Vaccine Finder")
                    .setContentText("Actively looking for vaccines")
                    .setSmallIcon(R.drawable.notif_icon)
                    .build();

            startForeground(1, notification);
        }
    }


    public void vaccineFoundNotify() {
        String CHANNEL_ID = "com.technicles.vaccinefinder";
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.notif_icon)
                        .setContentTitle("Vaccine slots found!!")
                        .setContentText("Slots available - tap to view details");

        Intent targetIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        NotificationManager nManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(AppConstants.NOTIFICATION_ID, builder.build());
    }


    protected BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(stopServiceReceiver);
            notificationManager.cancel(AppConstants.NOTIFICATION_ID);
            stopForeground(true);
        }
    };

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        }
        super.onDestroy();
    }


    public class LocalBinder extends Binder {
        public VaccineLookupService getServiceInstance() {
            return VaccineLookupService.this;
        }
    }

    public void apiCalls() {
        RetrofitInterface retrofitInterface =
                RetrofitInstance.getRetrofitInstance().create(RetrofitInterface.class);

        Call<CenterResponse> listCall =
                retrofitInterface.getCalendarByDistrict(getDistrictId(),
                        VaccineFinderUtil.getCurrentDate());
        listCall.enqueue(new Callback<CenterResponse>() {
            @Override
            public void onResponse(Call<CenterResponse> call, Response<CenterResponse> response) {
                lastApiSuccessTime = System.currentTimeMillis();
                count.incrementAndGet();
                parseData(response.body().getResponse());
            }

            @Override
            public void onFailure(Call<CenterResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void parseData(List<CenterModel> list) {
        availabilityModels = new ArrayList<>();
        unfilteredModels = list;
        for (CenterModel center : list) {
            for (SessionModel session : center.getSessions()) {
                if (session.getAvailableCapacity() == 0) {
                    continue;
                } else {
                    if (!((isA45 && session.getMinAgeLimit() == 45 && a45dose1 && session.getAvailableCapacityDose1() > 0)
                            || (isA45 && session.getMinAgeLimit() == 45 && a45dose2 && session.getAvailableCapacityDose2() > 0)
                            || (isA18 && session.getMinAgeLimit() == 18 && a18dose1 && session.getAvailableCapacityDose1() > 0)
                            || (isA18 && session.getMinAgeLimit() == 18 && a18dose2 && session.getAvailableCapacityDose2() > 0))) {
                        continue;
                    }
                }

                AvailabilityModel model = new AvailabilityModel();
                model.setCenter(center.getName());
                model.setAddress(center.getAddress());
                model.setPincode(center.getPincode());
                model.setFeeType(center.getFeeType());
                model.setTotalAvailable(session.getAvailableCapacity());
                model.setDose1(session.getAvailableCapacityDose1());
                model.setDose2(session.getAvailableCapacityDose2());
                model.setAgeGroup(session.getMinAgeLimit());
                model.setDate(session.getDate());
                model.setVaccine(session.getVaccine());
                model.setSessionId(session.getSessionId());

                if (model.getFeeType().equalsIgnoreCase("Free")) {
                    model.setCost("0");
                } else {
                    model.setCost(center.getVaccines().stream().filter(v -> v.getVaccine().equalsIgnoreCase(session.getVaccine())).findFirst().get().getFee());
                }

                availabilityModels.add(model);
            }
        }


        if (availabilityModels.size() > 0 && anyNewSessionIds(availabilityModels)) {
            notifiedSessionIds.clear();
            for (AvailabilityModel am : availabilityModels) {
                notifiedSessionIds.add(am.getSessionId());
            }
            vaccineFoundNotify();
        }
    }

    public boolean anyNewSessionIds(List<AvailabilityModel> list) {
        List<String> newList =
                list.stream().map(m -> m.getSessionId()).collect(Collectors.toList());

        if (VaccineFinderUtil.equalLists(notifiedSessionIds, newList)) {
            return false;
        }

        if (newList.size() < notifiedSessionIds.size()) {
            newList.removeAll(notifiedSessionIds);

            return !newList.isEmpty();
        }

        return true;
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            if (VaccineFinderUtil.haveNetworkConnection(getBaseContext())) {
                apiCalls();
            }

            customHandler.postDelayed(this, 60000);
        }
    };


    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Boolean getA45() {
        return isA45;
    }

    public void setA45(Boolean a45) {
        isA45 = a45;
    }

    public Boolean getA18() {
        return isA18;
    }

    public void setA18(Boolean a18) {
        isA18 = a18;
    }

    public Boolean getA45dose1() {
        return a45dose1;
    }

    public void setA45dose1(Boolean a45dose1) {
        this.a45dose1 = a45dose1;
    }

    public Boolean getA45dose2() {
        return a45dose2;
    }

    public void setA45dose2(Boolean a45dose2) {
        this.a45dose2 = a45dose2;
    }

    public Boolean getA18dose1() {
        return a18dose1;
    }

    public void setA18dose1(Boolean a18dose1) {
        this.a18dose1 = a18dose1;
    }

    public Boolean getA18dose2() {
        return a18dose2;
    }

    public void setA18dose2(Boolean a18dose2) {
        this.a18dose2 = a18dose2;
    }

    public List<AvailabilityModel> getAvailabilityModels() {
        return availabilityModels;
    }

    public void setAvailabilityModels(List<AvailabilityModel> availabilityModels) {
        this.availabilityModels = availabilityModels;
    }

    public AtomicInteger getCount() {
        return count;
    }

    public void setCount(AtomicInteger count) {
        this.count = count;
    }

    public long getLastApiSuccessTime() {
        return lastApiSuccessTime;
    }

    public void setLastApiSuccessTime(long lastApiSuccessTime) {
        this.lastApiSuccessTime = lastApiSuccessTime;
    }

    public List<CenterModel> getUnfilteredModels() {
        return unfilteredModels;
    }

    public void setUnfilteredModels(List<CenterModel> unfilteredModels) {
        this.unfilteredModels = unfilteredModels;
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
