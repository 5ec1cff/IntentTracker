package android.app;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

public interface IActivityManager extends IInterface {
    @RequiresApi(31)
    int broadcastIntentWithFeature(IApplicationThread caller, String callingFeatureId,
                                   Intent intent, String resolvedType, IIntentReceiver resultTo,
                                   int resultCode, String resultData, Bundle resultExtras,
                                   String[] requiredPermissions, String[] excludedPermissions,
                                   String[] excludePackages, int appOp, Bundle bOptions,
                                   boolean serialized, boolean sticky, int userId) throws RemoteException;

    @RequiresApi(31)
    int broadcastIntentWithFeature(IApplicationThread caller, String callingFeatureId,
                                   Intent intent, String resolvedType, IIntentReceiver resultTo,
                                   int resultCode, String resultData, Bundle resultExtras,
                                   String[] requiredPermissions, String[] excludedPermissions,
                                   int appOp, Bundle bOptions,
                                   boolean serialized, boolean sticky, int userId) throws RemoteException;

    @RequiresApi(30)
    int broadcastIntentWithFeature(IApplicationThread caller, String callingFeatureId,
                                   Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode,
                                   String resultData, Bundle map, String[] requiredPermissions,
                                   int appOp, Bundle options, boolean serialized, boolean sticky, int userId) throws RemoteException;

    int broadcastIntent(IApplicationThread caller, Intent intent,
                        String resolvedType, IIntentReceiver resultTo, int resultCode,
                        String resultData, Bundle map, String[] requiredPermissions,
                        int appOp, Bundle options, boolean serialized, boolean sticky, int userId) throws RemoteException;

    abstract class Stub extends Binder implements IActivityManager {

        public static IActivityManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
