package com.pilloxa.backgroundjob;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
/**
 * Created by viktor on 2016-12-13.
 */

public class BackgroundJob extends JobService {
    private String LOG_TAG = "backgroundjob";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(LOG_TAG, "On Start of JOB");
        Bundle bundle = new Bundle(params.getExtras());
        Context reactContext = getApplicationContext();
        Intent service = new Intent(reactContext, HeadlessService.class);
        service.putExtras(bundle);
        reactContext.startService(service);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(LOG_TAG, "On Stop of JOB");
        return false;
    }

}
