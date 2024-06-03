package android.app;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;

public interface IAssistDataReceiver {
    void onHandleAssistData(Bundle resultData);

    void onHandleAssistScreenshot(Bitmap screenshot);

    abstract class Stub extends Binder implements IAssistDataReceiver {

    }
}
