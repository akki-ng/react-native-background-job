
package com.pilloxa.backgroundjob;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.bridge.Arguments;

import android.content.Intent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundJobModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private String LOG_TAG = "backgroundjob";
    private static final String NETWORK_TYPE_UNMETERED = "UNMETERED";
    private static final String NETWORK_TYPE_NONE = "NONE";
    private static final String NETWORK_TYPE_ANY = "ANY";

    private final ReactApplicationContext reactContext;

    private JobScheduler jobScheduler;

    private boolean mInitialized = false;

    private Object lock1 = new Object();

    @Override
    public void initialize() {
        Log.d(LOG_TAG, "Initializing BackgroundJob");
        if (jobScheduler == null) {
            jobScheduler = (JobScheduler) getReactApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
            mInitialized = true;
            // jobScheduler.cancelAll();
        }
        super.initialize();
        getReactApplicationContext().addLifecycleEventListener(this);
    }

    public BackgroundJobModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    private JobInfo createJobObject(int taskId, String jobKey,
                         int timeout,
                         int period,
                         boolean persist,
                         int networkType,
                         boolean requiresCharging,
                         boolean requiresDeviceIdle,
                         String payLoad) {
        int persistInt = persist ? 1 : 0;

        ComponentName componentName = new ComponentName(getReactApplicationContext(), BackgroundJob.class.getName());
        PersistableBundle jobExtras = new PersistableBundle();
        JobInfo.Builder jobInfo = new JobInfo.Builder(taskId, componentName);

        if(period > 0) {
            jobExtras.putInt("period", period);
            jobInfo.setPeriodic(period);
        }

        jobExtras.putString("jobKey", jobKey);
        jobExtras.putInt("timeout", timeout);
        jobExtras.putInt("taskId", taskId);
        jobExtras.putInt("persist", persistInt);
        jobExtras.putInt("networkType", networkType);
        jobExtras.putInt("requiresCharging", requiresCharging ? 1 : 0);
        jobExtras.putInt("requiresDeviceIdle", requiresDeviceIdle ? 1 : 0);
        jobExtras.putString("payLoad", payLoad);


        jobInfo.setExtras(jobExtras);
        jobInfo.setRequiresDeviceIdle(requiresDeviceIdle);
        jobInfo.setRequiresCharging(requiresCharging);
        jobInfo.setPersisted(persist);

        jobInfo.setRequiredNetworkType(networkType);

        return jobInfo.build();
    }

    private synchronized void scheduleJob(int taskId, String jobKey,
                         int timeout,
                         int period,
                         boolean persist,
                         boolean appActive,
                         int networkType,
                         boolean requiresCharging,
                         boolean requiresDeviceIdle,
                         String payLoad, Callback callback) {
        JobInfo newJob = this.createJobObject(taskId, jobKey, timeout, period, persist, networkType, requiresCharging, requiresDeviceIdle, payLoad);
        Log.v(LOG_TAG, String.format("Create Job instance for JobId: %s", newJob.getId()));
        /*
        synchronized(lock1) {
            for (JobInfo iJobInfo : mJobs) {
                if (iJobInfo.getId() == taskId) {
                    mJobs.remove(iJobInfo);
                }
            }
            Log.v(LOG_TAG, "will Add in array");
            mJobs.add(newJob);
            Log.v(LOG_TAG, "Successfully Added in array");
        }
        */

        if (true || !appActive) {
            // scheduleJobs();
            scheduleAJob(newJob, callback);
        }
        // return -1; //newJob.getId();
    }

    @ReactMethod
    public void scheduleRepeatedJob(int taskId, String jobKey,
                         int timeout,
                         int period,
                         boolean persist,
                         boolean appActive,
                         int networkType,
                         boolean requiresCharging,
                         boolean requiresDeviceIdle,
                         String payLoad,
                         Callback callback) {

        Log.v(LOG_TAG, String.format("Scheduling Repeated Job: %s, JobId: %s, timeout: %s, period: %s, network type: %s, requiresCharging: %s, requiresDeviceIdle: %s, payLoad: %s", jobKey, taskId, timeout, period, networkType, requiresCharging, requiresDeviceIdle, String.valueOf(payLoad)));

        scheduleJob(taskId, jobKey, timeout, period, persist, appActive, networkType, requiresCharging, requiresDeviceIdle, payLoad, callback);
    }

    @ReactMethod
    public void scheduleOneTimeJob(int taskId, String jobKey,
                         int timeout,
                         boolean persist,
                         boolean appActive,
                         int networkType,
                         boolean requiresCharging,
                         boolean requiresDeviceIdle,
                         String payLoad,
                         Callback callback) {
        Log.v(LOG_TAG, String.format("Scheduling One Time Job: %s, JobId: %s, timeout: %s, network type: %s, requiresCharging: %s, requiresDeviceIdle: %s, payLoad: %s", jobKey, taskId, timeout, networkType, requiresCharging, requiresDeviceIdle, String.valueOf(payLoad)));

        scheduleJob(taskId, jobKey, timeout, -1, persist, appActive, networkType, requiresCharging, requiresDeviceIdle, payLoad, callback);
    }

    @ReactMethod
    public void startNow(int taskId, String jobKey, int timeout, String payLoad) {
        Log.v(LOG_TAG, String.format("Starting One Time Job: %s, JobId: %s, timeout: %s, payLoad: %s", jobKey, taskId, timeout, String.valueOf(payLoad)));
        Intent service = new Intent(reactContext, HeadlessService.class);

        Bundle bundle = new Bundle();

        bundle.putString("jobKey", jobKey);
        bundle.putInt("timeout", timeout);
        bundle.putInt("taskId", taskId);
        bundle.putString("payLoad", payLoad);

        service.putExtras(bundle);
        reactContext.startService(service);
    }

    @ReactMethod
    public synchronized void hardCancel(int taskId, Callback callback ) {
        List<JobInfo> mJobs = jobScheduler.getAllPendingJobs();
        //-1(TaskId not Found) , 0(TaskId found, jobKey doesnot match), 1(TaskId found, jobKey matched)
        boolean result = false;
        for (JobInfo iJobInfo : mJobs) {
            if (iJobInfo.getId() == taskId) {
                String storedJobKey = iJobInfo.getExtras().containsKey("jobKey") ? iJobInfo.getExtras().getString("jobKey") : null;

                Log.d(LOG_TAG, "Hard Cancelling job: " + String.valueOf(storedJobKey) + " (" + taskId + ")");
                jobScheduler.cancel(taskId);
                // mJobs = jobScheduler.getAllPendingJobs();
                result = true;
            }
        }
        Log.d(LOG_TAG, "Cancelling job:Failure: " + taskId + " does not exist");

        callback.invoke(result);
    }

    @ReactMethod
    public synchronized void cancel(String jobKey, int taskId, Callback callback ) {
        List<JobInfo> mJobs = jobScheduler.getAllPendingJobs();
        //-1(TaskId not Found) , 0(TaskId found, jobKey doesnot match), 1(TaskId found, jobKey matched)
        int isValid = -1;
        JobInfo iJobInfo = null;
        boolean result = false;
        for (JobInfo jobInfo : mJobs) {
            if (jobInfo.getId() == taskId) {
                PersistableBundle extras = jobInfo.getExtras();
                if(extras.containsKey("jobKey") && jobKey.equals(extras.getString("jobKey")) ) {
                    isValid = 1;
                }else {
                    isValid = 0;
                }
                iJobInfo = jobInfo;
                break;
            }
        }

        if(isValid == 1) {
            Log.d(LOG_TAG, "Cancelling job: " + jobKey + " (" + taskId + ")");
            jobScheduler.cancel(taskId);
            result = true;
        }else if(isValid == 0){
            String storedJobKey = iJobInfo != null && iJobInfo.getExtras().containsKey("jobKey") ? iJobInfo.getExtras().getString("jobKey") : null;
            Log.d(LOG_TAG, "Cancelling job:Failure: " + taskId + " existes but JobKey(" + String.valueOf(storedJobKey) + ") doesnot match to provided jobkey(" + jobKey + ")");
        }else {
            Log.d(LOG_TAG, "Cancelling job:Failure: " + taskId + " does not exist");
        }
        callback.invoke(result);
    }

    @ReactMethod
    public synchronized void cancelAll() {
        Log.d(LOG_TAG, "Cancelling all jobs");
        jobScheduler.cancelAll();
    }

    private WritableArray _getAll() {
        Log.d(LOG_TAG, "Getting all jobs");
        WritableArray jobs = Arguments.createArray();
        List<JobInfo> mJobs = jobScheduler.getAllPendingJobs();
        if (mJobs != null) {
            for (JobInfo job : mJobs) {
                Log.d(LOG_TAG, "Fetching job " + job.getId());
                Bundle extras = new Bundle(job.getExtras());
                WritableMap jobMap = Arguments.fromBundle(extras);
                jobMap.putBoolean("persist", extras.getInt("persist") == 1);
                jobMap.putBoolean("requiresCharging", extras.getInt("requiresCharging") == 1);
                jobMap.putBoolean("requiresDeviceIdle", extras.getInt("requiresDeviceIdle") == 1);
                jobs.pushMap(jobMap);
            }
        }

        return jobs;
    }

    @ReactMethod
    public void getAll(Callback callback) {
        WritableArray jobs = _getAll();
        callback.invoke(jobs);
    }

    @Override
    public String getName() {
        return "BackgroundJob";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        Log.d(LOG_TAG, "Getting constants");
        jobScheduler = (JobScheduler) getReactApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            // mJobs = jobScheduler.getAllPendingJobs();
            mInitialized = true;
        }
        HashMap<String, Object> constants = new HashMap<>();
        constants.put("jobs", _getAll());
        constants.put(NETWORK_TYPE_UNMETERED, JobInfo.NETWORK_TYPE_UNMETERED);
        constants.put(NETWORK_TYPE_ANY, JobInfo.NETWORK_TYPE_ANY);
        constants.put(NETWORK_TYPE_NONE, JobInfo.NETWORK_TYPE_NONE);
        return constants;
    }

    @Override
    public void onHostResume() {
        Log.d(LOG_TAG, "Woke up");
        // List<JobInfo> mJobs = jobScheduler.getAllPendingJobs();
        // jobScheduler.cancelAll();

    }

    private void scheduleAJob(JobInfo job, Callback callback) {
        jobScheduler.cancel(job.getId());
        int result = jobScheduler.schedule(job);
        if (result == JobScheduler.RESULT_SUCCESS) {
           callback.invoke(true);
           return;
        }
        callback.invoke(false);
    }

    private void scheduleJobs() {
        List<JobInfo> mJobs = jobScheduler.getAllPendingJobs();
        for (JobInfo job : mJobs) {
            Log.d(LOG_TAG, "Sceduling job " + job.getId());
            jobScheduler.cancel(job.getId());
            int result = jobScheduler.schedule(job);
            if (result == JobScheduler.RESULT_SUCCESS)
                Log.d(LOG_TAG, "Job (" + job.getId() + ") scheduled successfully!");
        }
    }

    @Override
    public void onHostPause() {
        Log.d(LOG_TAG, "Pausing");
        // scheduleJobs();
    }

    @Override
    public void onHostDestroy() {
        Log.d(LOG_TAG, "Destroyed");
        scheduleJobs();
    }
}
