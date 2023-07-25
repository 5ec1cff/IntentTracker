// IIntentTrackerListener.aidl
package io.github.a13e300.intenttracker.service;

import io.github.a13e300.intenttracker.service.StartActivityInfo;

oneway interface IIntentTrackerListener {
    void onStartActivity(in StartActivityInfo info) = 1;
}