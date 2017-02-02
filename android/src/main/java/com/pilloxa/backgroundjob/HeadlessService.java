package com.pilloxa.backgroundjob;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

/**
 * Created by viktor on 2016-12-13.
 */

public class HeadlessService  extends HeadlessJsTaskService {
    private String LOG_TAG = "backgroundjob";

    @Override
    protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        Log.d(LOG_TAG, "On Start of HeadlessService JOB");
        Bundle extras = intent.getExtras();
        String jobKey = extras.getString("jobKey");
        int timeout = extras.getInt("timeout");
        return new HeadlessJsTaskConfig( jobKey, Arguments.fromBundle(extras), timeout);
    }
}
