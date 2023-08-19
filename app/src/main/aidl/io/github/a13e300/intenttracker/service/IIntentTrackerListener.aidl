// IIntentTrackerListener.aidl
package io.github.a13e300.intenttracker.service;

import io.github.a13e300.intenttracker.service.StartActivityInfo;
import io.github.a13e300.intenttracker.service.ActivityStartedInfo;

oneway interface IIntentTrackerListener {
    void onStartActivity(in StartActivityInfo info) = 1;
    void onActivityStarted(in ActivityStartedInfo info) = 2;
}