// IIntentTrackerListener.aidl
package io.github.a13e300.intenttracker.service;

oneway interface IServiceFetcher {
    void publishService(IBinder binder) = 1;
}