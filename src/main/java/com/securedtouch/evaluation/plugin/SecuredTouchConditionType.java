package com.securedtouch.evaluation.plugin;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.debug.Debug;
import model.SessionRiskResponse;
import org.apache.commons.codec.binary.Base64;
import org.forgerock.openam.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import utils.RestTemplateUtil;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static utils.SecuredTouchConst.*;

public class SecuredTouchConditionType extends EntitlementConditionAdaptor {

    private final Debug debug = Debug.getInstance("Entitlement");


    public static final String APIDOMAIN_FIELD = "apiDomain";
    private String apiDomain;
    public static final String APPID_FIELD = "appId";
    private String appId;
    public static final String POLICY_FIELD = "policy";
    private String policy;
    public static final String TIMEOUT_FIELD = "timeout";
    // timeout in millis
    private int timeout = DEFAULT_TIMEOUT;

    public static final String USERNAME_FIELD = "username";
    private String username;
    public static final String PASSWORD_FIELD = "password";
    private String password;

    public void setState(String state) {
        try {
            JSONObject json = new JSONObject(state);

            if (json.has(APIDOMAIN_FIELD)) {
                setApiDomain(json.getString(APIDOMAIN_FIELD));
            }
            if (json.has(APPID_FIELD)) {
                setAppId(json.getString(APPID_FIELD));
            }
            if (json.has(POLICY_FIELD)) {
                setPolicy(json.getString(POLICY_FIELD));
            }
            if(json.has(TIMEOUT_FIELD)) {
                int timeout = json.getInt(TIMEOUT_FIELD);
                setTimeout(timeout == 0 ? DEFAULT_TIMEOUT : timeout);
            }
            if (json.has(USERNAME_FIELD)) {
                setUsername(json.getString(USERNAME_FIELD));
            }
            if (json.has(PASSWORD_FIELD)) {
                setPassword(json.getString(PASSWORD_FIELD));
            }
            this.validate();
        } catch (JSONException e) {
            this.debug.error("Failed to set state", e);
        } catch (EntitlementException e) {
            this.debug.error("Failed to validate state", e);
        }
    }

    public String getState() {
        try {
            JSONObject json = new JSONObject();
            json.put(APIDOMAIN_FIELD, getApiDomain());
            json.put(APPID_FIELD, getAppId());
            json.put(POLICY_FIELD, getPolicy());
            json.put(TIMEOUT_FIELD, getTimeout());
            json.put(USERNAME_FIELD, getUsername());
            json.put(PASSWORD_FIELD, getPassword());
            return json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void validate() throws EntitlementException {
        if(StringUtils.isBlank(this.apiDomain)) {
            throw new EntitlementException(711, new Object[]{APIDOMAIN_FIELD});
        }
        if(StringUtils.isBlank(this.appId)) {
            throw new EntitlementException(711, new Object[]{APPID_FIELD});
        }
        if(StringUtils.isBlank(this.policy)) {
            throw new EntitlementException(711, new Object[]{POLICY_FIELD});
        }
        if(this.timeout < 0) {
            throw new EntitlementException(400, new Object[]{TIMEOUT_FIELD});
        }
        if(StringUtils.isBlank(this.username)) {
            throw new EntitlementException(711, new Object[]{USERNAME_FIELD});
        }
        if(StringUtils.isBlank(this.password)) {
            throw new EntitlementException(711, new Object[]{PASSWORD_FIELD});
        }
    }

    public ConditionDecision evaluate(String s, Subject subject, String s1, Map<String, Set<String>> map) throws EntitlementException {

        // configure request URL
        String sessionId = getSessionId(subject);
        if(sessionId == null) {
            debug.warning("No sessionToken available, skipping session");
            return ConditionDecision.newFailureBuilder().build();
        }

        String requestURL = configureRequestURL(sessionId);

        // get response
        String response = getResponse(requestURL, sessionId);
        if(response == null) {
            return ConditionDecision.newFailureBuilder().build();
        }

        SessionRiskResponse sessionRiskScore = getSessionRiskScore(response, sessionId);
        this.debug.message(sessionRiskScore.getAdvice());

        // build conditionDecision and add to it securedtouch auth as advice
        ConditionDecision conditionDecision = ConditionDecision.newBuilder(sessionRiskScore.getAuthenticated())
                .setAdvice(Collections.singletonMap(SECUREDTOUCH_ADVICE_KEY, Collections.singleton(sessionRiskScore.getAdvice())))
                .build();

        return conditionDecision;
    }

    private String configureRequestURL(String sessionId) {
        return String.format(securedTouchAuthenticationURL, getApiDomain(), getAppId(), sessionId);
    }

    private String getResponse(String url, String sessionId) {
        RestTemplate restTemplate = RestTemplateUtil.getRestTemplate(getTimeout());
        debug.message("get response from: " + url);

        HttpEntity<String> request = new HttpEntity<String>(getBasicAuthHttpHeaders());
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        } catch (Exception e) {
            this.debug.error("Error while sending request", e);
        }
        if(response == null || response.getStatusCode().isError() || response.getBody() == null){
            this.debug.error("Failed to get response from SecuredTouch - Session " + sessionId);
            return null;
        }
        return response.getBody();
    }

    private HttpHeaders getBasicAuthHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        byte[] base64CredsBytes = Base64.encodeBase64((username + ":" + password).getBytes());
        String base64Creds = new String(base64CredsBytes);
        headers.add("Authorization", "Basic " + base64Creds);
        return headers;
    }

    private SessionRiskResponse getSessionRiskScore(String response, String sessionId) {
        String policyName = getPolicy();
        String info;

        JSONObject policy;
        try {
            policy = (JSONObject) new JSONArray(response).get(0);
            policy = policy.getJSONObject(SESSION_RISK_POLICY).getJSONObject(policyName);
        } catch (JSONException e) {
            // no such policy
            info = String.format(ADVICE_NO_SUCH_POLICY, sessionId, policyName);
            return new SessionRiskResponse(Boolean.FALSE, info);
        }

        int score;
        String state = null;
        try {
            state = policy.getString(SESSION_RISK_POLICY_STATE);
            score = policy.getInt(SESSION_RISK_POLICY_SCORE);
        } catch (JSONException e) {
            // no score for session
            info = String.format(ADVICE_NO_SCORE, sessionId, policyName, state);
            return new SessionRiskResponse(Boolean.FALSE, info);
        }

        double min, max;
        try {
            JSONArray thresholds = policy.getJSONArray(SESSION_RISK_POLICY_THRESHOLD);
            // thresholds will always be ascending
            min = thresholds.getInt(0);
            max = thresholds.getInt(thresholds.length()-1);
        } catch (JSONException e) {
            return new SessionRiskResponse(Boolean.FALSE, String.format(ADVICE_NO_THRESHOLDS, sessionId, policyName));
        }

        if (score > max) {
            return new SessionRiskResponse(Boolean.TRUE, RESULT_AUTH, score);
        } else if (score < max && score > min) {
            return new SessionRiskResponse(Boolean.FALSE, RESULT_NEUTRAL, score);
        } else {
            return new SessionRiskResponse(Boolean.FALSE, RESULT_RISK, score);
        }
    }

    private String getSessionId(Subject subject) {
        SSOToken ssoToken = SubjectUtils.getSSOToken(subject);
        if(ssoToken == null) {
            return null;
        }
        return ssoToken.getTokenID().toString();
    }

    public String getApiDomain() {
        return apiDomain;
    }

    public void setApiDomain(String apiDomain) {
        this.apiDomain = apiDomain;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
