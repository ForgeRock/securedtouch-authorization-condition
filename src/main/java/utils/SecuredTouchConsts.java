package utils;

public class SecuredTouchConsts {

    public static final String securedTouchAuthenticationURL = "https://%s/SecuredTouch/rest/v3/%s/sessionRisk/?id=%s";
    public static final String AUTH_ADVICE_KEY = "auth_advice";
    public static final String SCORE_KEY = "score";

    public static final int DEFAULT_TIMEOUT = 5000;

    // JsonPath
    public static String SESSION_RISK_POLICY = "policy";
    public static String SESSION_RISK_POLICY_SCORE = "score";
    public static String SESSION_RISK_POLICY_THRESHOLD = "thresholds";
    public static String SESSION_RISK_POLICY_STATE = "state";

    // Advices
    public static String ADVICE_SCROE_RESULT = "Session %s, Policy '%s', Score %s - %s";
    public static String ADVICE_NO_SUCH_POLICY = "Session %s, Policy '%s' is missing on response";
    public static String ADVICE_NO_SCORE = "Session %s, Policy '%s', State '%s' - score is missing";
    public static String ADVICE_NO_THRESHOLDS = "Session %s, Policy '%s', thresholds are missing";

    // auth result
    public static String RESULT_AUTH = "Authenticated";
    public static String RESULT_NEUTRAL = "Neutral";
    public static String RESULT_RISK = "Risk";


}
