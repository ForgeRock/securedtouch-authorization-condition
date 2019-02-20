package utils;

public class SecuredTouchConst {

    public static final String securedTouchAuthenticationURL = "https://%s/SecuredTouch/rest/v3/%s/sessionRisk/?id=%s";
    public static final String SECUREDTOUCH_ADVICE_KEY = "SECUREDTOUCH";

    public static final int DEFAULT_TIMEOUT = 5000;

    // JsonPath
    public static String SESSION_RISK_POLICY = "policy";
    public static String SESSION_RISK_POLICY_SCORE = "score";
    public static String SESSION_RISK_POLICY_THRESHOLD = "thresholds";
    public static String SESSION_RISK_POLICY_STATE = "state";

    // Advices
    public static String ADVICE_NO_SUCH_POLICY = "Session %s, Policy '%s' is missing on response";
    public static String ADVICE_NO_SCORE = "Session %s, Policy '%s', State '%s' - score is missing";
    public static String ADVICE_NO_THRESHOLDS = "Session %s, Policy '%s', thresholds are missing";

    // auth result
    public static String RESULT_AUTH = "STRONG_AUTH";
    public static String RESULT_NEUTRAL = "NO_THREAT";
    public static String RESULT_RISK = "FRAUD_ALERT";


}
