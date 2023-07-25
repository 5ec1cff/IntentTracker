// IIntentTrackerService.aidl
package io.github.a13e300.intenttracker.service;

import io.github.a13e300.intenttracker.service.ServiceInfo;
import io.github.a13e300.intenttracker.service.IIntentTrackerListener;

interface IIntentTrackerService {
    int getVersion() = 1;
    ServiceInfo getServiceInfo() = 2;
    void registerListener(IIntentTrackerListener Listener, int flags) = 3;
    void unregisterListener(IIntentTrackerListener Listener) = 4;
}