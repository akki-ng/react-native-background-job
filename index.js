"use strict";
import { NativeModules, AppRegistry } from "react-native";
import AbstractTask from './abstractTask'
import TaskRegistry from './taskRegistry'
import JobScheduler from './jobScheduler'

const AppState = NativeModules.AppState;
const tag = "BackgroundJob:";
const jobModule = NativeModules.BackgroundJob;
const nativeJobs = jobModule.jobs;
var registeredJobs = {};
var alreadyScheduled = {};
var globalWarning = __DEV__;

const BackgroundJob = {
  NETWORK_TYPE_UNMETERED: jobModule.UNMETERED,
  NETWORK_TYPE_NONE: jobModule.NONE,
  NETWORK_TYPE_ANY: jobModule.ANY,
  /**
     * Registers jobs and the functions they should run.
     *
     * This has to run on each initialization of React Native. Only doing this will not start running the job. It has to be scheduled by `schedule` to start running.
     *
     * @param {Object} obj
     * @param {string} obj.jobKey A unique key for the job
     * @param {function} obj.job The JS-function that will be run
     *
     * @example
     * import BackgroundJob from 'react-native-background-job';
     *
     * const backgroundJob = {
     *  jobKey: "myJob",
     *  job: () => console.log("Running in background")
     * };
     * BackgroundJob.register(backgroundJob);
     *
     */
  register: function({ jobKey, job }) {
    const existingJob = registeredJobs[jobKey];

    if (!existingJob || !existingJob.registered) {
      var fn = async (data) => {
        console.warn(
          `${tag} Trying to execute the job ${data.jobKey}{${data.taskId}}`
        );
        job(data);
      };

      AppRegistry.registerHeadlessTask(jobKey, () => fn);

      if (existingJob) {
        registeredJobs[jobKey].registered = true;
      } else {
        const scheduledJob = nativeJobs.filter(nJob => nJob.jobKey == jobKey);
        const scheduled = scheduledJob[0] != undefined;
        registeredJobs[jobKey] = { registered: true, scheduled };
      }
    }
  },
  /**
     * Schedules a new job.
     *
     * This only has to be run once while `register` has to be run on each initialization of React Native.
     *
     * @param {Object} obj
     * @param {string} obj.jobKey A unique key for the job
     * @param {number} obj.timeout How long the JS job may run before being terminated by Android (in ms).
     * @param {number} [obj.period = 900000] - The frequency to run the job with (in ms). This number is not exact, Android may modify it to save batteries. Note: For Android > N, the minimum is 900 0000 (15 min).
     * @param {boolean} [obj.persist = true] If the job should persist over a device restart.
     * @param {boolean} [obj.warn = true] If a warning should be raised if overwriting a job that was already scheduled.
     * @param {number} [obj.networkType = BackgroundJob.NETWORK_TYPE_NONE] Only run for specific network requirements, (not respected by pre Android N devices) [docs](https://developer.android.com/reference/android/app/job/JobInfo.html#NETWORK_TYPE_ANY)
     * @param {boolean} [obj.requiresCharging = false] Only run job when device is charging, (not respected by pre Android N devices) [docs](https://developer.android.com/reference/android/app/job/JobInfo.Builder.html#setRequiresCharging(boolean))
     * @param {boolean} [obj.requiresDeviceIdle = false] Only run job when the device is idle, (not respected by pre Android N devices) [docs](https://developer.android.com/reference/android/app/job/JobInfo.Builder.html#setRequiresDeviceIdle(boolean))
     * @example
     * import BackgroundJob from 'react-native-background-job';
     * const backgroundJob = {
     *  jobKey: "myJob",
     *  job: () => console.log("Running in background")
     * };
     *
     * BackgroundJob.register(backgroundJob);
     *
     * var backgroundSchedule = {
     *   taskId: 182,
     *  jobKey: "myJob",
     *  timeout: 5000
     * }
     *
     * BackgroundJob.scheduleRepeatedJob(backgroundSchedule);
     */
  scheduleRepeatedJob: function(
    { taskId,
      jobKey,
      timeout,
      period = 900000,
      persist = true,
      warn = true,
      networkType = this.NETWORK_TYPE_NONE,
      requiresCharging = false,
      requiresDeviceIdle = false,
      payLoad = null,
      callback
    }
  ) {
    const registeredJob = registeredJobs[jobKey];


    if (!registeredJob) {
      console.error(
        `${tag} The job ${jobKey} has not been registered, you must register it before you can schedule it.`
      );
    } else {
      if(alreadyScheduled[taskId] && warn && globalWarning) {
        console.warn(
          `${tag} The job ${taskId} ${jobKey} is existing. Hence overwriting background job.`
        );
      }else{
        alreadyScheduled[taskId] = jobKey;
      }

      AppState.getCurrentAppState(
        ({ app_state }) => {
          const appActive = app_state == "active";
          jobModule.scheduleRepeatedJob(
            taskId,
            jobKey,
            timeout,
            period,
            persist,
            !appActive,
            networkType,
            requiresCharging,
            requiresDeviceIdle,
            payLoad,
            callback
          );
        },
        () => console.err(`${tag} Can't get Current App State`)
      );
    }
  },

  /*
  Schedule OneTimeJob
   */
  scheduleOneTimeJob: function(
    {
      taskId,
      jobKey,
      timeout,
      persist = true,
      warn = true,
      networkType = this.NETWORK_TYPE_NONE,
      requiresCharging = false,
      requiresDeviceIdle = false,
      payLoad = null,
      callback
    }
  ) {
    const registeredJob = registeredJobs[jobKey];

    if (!registeredJob) {
      console.error(
        `${tag} The job ${jobKey} has not been registered, you must register it before you can schedule it.`
      );
    } else {
      if(alreadyScheduled[taskId] && warn && globalWarning) {
        console.warn(
          `${tag} The job ${taskId} ${jobKey} is existing. Hence overwriting background job.`
        );
      }else{
        alreadyScheduled[taskId] = jobKey;
      }

      AppState.getCurrentAppState(
        ({ app_state }) => {
          const appActive = app_state == "active";
          jobModule.scheduleRepeatedJob(
            taskId,
            jobKey,
            timeout,
            100,
            persist,
            !appActive,
            networkType,
            requiresCharging,
            requiresDeviceIdle,
            payLoad,
            callback
          );
        },
        () => console.err(`${tag} Can't get Current App State`)
      );
    }
  },


  startNow: function({
                      taskId,
                      jobKey,
                      timeout = 0,
                      payLoad = null
                    }) {
    jobModule.startNow(
            taskId,
            jobKey,
            timeout,
            payLoad
          );
  },


    /**
     * Fetches all the currently scheduled jobs
     *
     * @param {Object} obj
     * @param {function(Array)} obj.callback A list of all the scheduled jobs will be passed to the callback
     *
     * @example
     * import BackgroundJob from 'react-native-background-job';
     *
     * BackgroundJob.getAll({callback: (jobs) => console.log("Jobs:",jobs)});
     *
     */
  getAll: function({ callback }) {
    jobModule.getAll(callback);
  },

  /**
     * Cancel a specific job
     *
     * @param {Object} obj
     * @param {string} obj.jobKey The unique key for the job
     * @param {int} obj.taskId The unique id for the task
     * @param {boolean} [obj.warn = true] If one tries to cancel a job that has not been scheduled it will warn
     *
     * @example
     * import BackgroundJob from 'react-native-background-job';
     *
     * BackgroundJob.cancel({jobKey: 'myJob', taskId: 182});
     */
  cancel: function({ jobKey, taskId, warn = true, callback }) {

    if (warn && globalWarning && !registeredJobs[jobKey]) {
      console.warn(
        `${tag} Trying to cancel the job ${jobKey} but it is not registered`
      );
    }
    if (warn && globalWarning && !alreadyScheduled[taskId]) {
      console.warn(
        `${tag} Trying to cancel the job ${jobKey}{${taskId}} but it is not scheduled`
      );
    }
    jobModule.cancel(jobKey, taskId, callback);
    delete alreadyScheduled[taskId];
    // jobs[jobKey] ? jobs[jobKey].scheduled = false : null;

  },

   /**
     * Hard cancel a specific job without worrying about jobkey
     *
     * @param {Object} obj
     * @param {int} obj.taskId The unique id for the task
     * @param {boolean} [obj.warn = true] If one tries to cancel a job that has not been scheduled it will warn
     *
     * @example
     * import BackgroundJob from 'react-native-background-job';
     *
     * BackgroundJob.cancel({taskId: 182});
     */
  hardCancel: function({ taskId, warn = true, callback }) {
    if (warn && globalWarning) {
      console.warn(
        `${tag} Trying to cancel the job with taskId ${taskId}`
      );
    }
    jobModule.hardCancel(taskId, callback);
    delete alreadyScheduled[taskId];
    // jobs[jobKey] ? jobs[jobKey].scheduled = false : null;
  },



  /**
     * Cancels all the scheduled jobs
     *
     * @example
     * import BackgroundJob from 'react-native-background-job';
     *
     * BackgroundJob.cancelAll();
     */
  cancelAll: function() {
    jobModule.cancelAll();
    const keys = Object.keys(alreadyScheduled);
    keys.map(key => {
      // jobs[key].scheduled = false;
      delete alreadyScheduled[key];
    });
  },
  /**
     * Sets the global warning level
     *
     * @param {boolean} warn
     *
     * @example
     * import BackgroundJob from 'react-native-background-job';
     *
     * BackgroundJob.setGlobalWarnings(false);
     *
     *
     * @param {boolean} warn
     *
     * @example
     * import BackgroundJob from 'react-native-background-job';
     *
     * BackgroundJob.setGlobalWarnings(false);
     *
     */
  setGlobalWarnings: function(warn) {
    globalWarning = warn;
  }
};

export {
  AbstractTask,
  TaskRegistry,
  JobScheduler,
  BackgroundJob
}
