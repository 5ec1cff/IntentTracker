package android.app;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(ActivityManager.class)
public class ActivityManagerHidden {
    /**
     * requestType for assist context: only basic information.
     */
    public static int ASSIST_CONTEXT_BASIC = 0;

    /**
     * requestType for assist context: generate full AssistStructure.
     */
    public static int ASSIST_CONTEXT_FULL = 1;

    /**
     * requestType for assist context: generate full AssistStructure for autofill.
     */
    public static int ASSIST_CONTEXT_AUTOFILL = 2;

    /**
     * requestType for assist context: generate AssistContent but not AssistStructure.
     */
    public static int ASSIST_CONTEXT_CONTENT = 3;
}
