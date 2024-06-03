package android.app;

import android.os.Bundle;
import android.os.IBinder;

public interface IActivityTaskManager {
    // Shell has GET_TOP_ACTIVITY_INFO since A12
    boolean requestAssistContextExtras(int requestType, IAssistDataReceiver receiver,
                                       Bundle receiverExtras, IBinder activityToken,
                                       boolean focused, boolean newSessionId);

    boolean requestAssistDataForTask(IAssistDataReceiver receiver, int taskId,
                                     String callingPackageName, String callingAttributionTag);

    Bundle getAssistContextExtras(int requestType);

    class Stub {
        public static IActivityTaskManager asInterface(IBinder b) {
            throw new RuntimeException("");
        }
    }
}
