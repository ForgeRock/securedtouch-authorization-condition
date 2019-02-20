package com.securedtouch.evaluation.plugin;

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.security.auth.Subject;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SecuredTouchConditionType.class)
public class SecuredTouchConditionTypeTest {

    private static SecuredTouchConditionType securedTouchCondition;

    private static final String API_DOMAIN_FIELD = "apiDomain";
    private static final String APP_ID_FIELD = "appId";
    private static final String POLICY_FIELD = "policy";
    private static final String TIMEOUT_FIELD = "timeout";

    public static final String AUTH_ADVICE_KEY = "SECUREDTOUCH";

    private static final String DEFAULT_POLICY = "MIN_FRAUD_MODULE";
    private static final String UNAUTH_MID_ADVICE = "NO_THREAT";
    private static final String UNAUTH_LOW_ADVICE = "FRAUD_ALERT";

    @BeforeClass
    public static void init() {
        securedTouchCondition = new SecuredTouchConditionType();
    }

    @Test
    public void setStateTest() {
        String json = loadJsonFromResource("goodState.json");
        securedTouchCondition.setState(json);
        try {
            JSONObject jsonObject = new JSONObject(json);
            Assert.assertEquals(jsonObject.getString(API_DOMAIN_FIELD), securedTouchCondition.getApiDomain());
            Assert.assertEquals(jsonObject.getString(APP_ID_FIELD), securedTouchCondition.getAppId());
            Assert.assertEquals(jsonObject.getString(POLICY_FIELD), securedTouchCondition.getPolicy());
            Assert.assertEquals(jsonObject.getInt(TIMEOUT_FIELD), securedTouchCondition.getTimeout());
        } catch (JSONException e) {
            e.printStackTrace();
            Assert.fail("Something went wrong...");
        }
    }

    @Test
    public void getStateTest() {
        String expectedState = loadJsonFromResource("goodState.json");
        securedTouchCondition.setState(expectedState);
        String state = securedTouchCondition.getState();
        try {
            JSONObject expectedStateJson = new JSONObject(expectedState);
            JSONObject actualSteteJson = new JSONObject(state);

            Assert.assertEquals(expectedStateJson.getString(API_DOMAIN_FIELD), actualSteteJson.getString(API_DOMAIN_FIELD));
            Assert.assertEquals(expectedStateJson.getString(APP_ID_FIELD), actualSteteJson.getString(APP_ID_FIELD));
            Assert.assertEquals(expectedStateJson.getString(POLICY_FIELD), actualSteteJson.getString(POLICY_FIELD));
            Assert.assertEquals(expectedStateJson.getString(TIMEOUT_FIELD), actualSteteJson.getString(TIMEOUT_FIELD));
        } catch (JSONException e) {
            e.printStackTrace();
            Assert.fail("Something went wrong...");
        }

    }

    @Test(expected = EntitlementException.class)
    public void validateTest_fail() throws EntitlementException {
        String state = loadJsonFromResource("goodState.json");
        securedTouchCondition.setState(state);
        securedTouchCondition.setTimeout(-5);
        securedTouchCondition.validate();
    }

    @Test
    public void evaluateTest() throws Exception {
        securedTouchCondition = PowerMockito.spy(new SecuredTouchConditionType());
        PowerMockito.doReturn("test-session-id").when(securedTouchCondition, "getSessionId", ArgumentMatchers.any());
        PowerMockito.doReturn(loadJsonFromResource("authSessionResponse.json")).when(securedTouchCondition, "getResponse", ArgumentMatchers.any(), ArgumentMatchers.any());
        securedTouchCondition.setPolicy(DEFAULT_POLICY);

        // Auth
        ConditionDecision evaluated = testEvaluate();
        Assert.assertEquals(Boolean.TRUE, evaluated.isSatisfied());

        // Unauth - in mid threshold
        PowerMockito.doReturn(loadJsonFromResource("unAuthMidSessionResponse.json")).when(securedTouchCondition, "getResponse", ArgumentMatchers.any(), ArgumentMatchers.any());
        evaluated = testEvaluate();
        Assert.assertEquals(Boolean.FALSE, evaluated.isSatisfied());
        Assert.assertTrue(evaluated.getAdvice().get(AUTH_ADVICE_KEY).toString().contains(UNAUTH_MID_ADVICE));

        // Unauth - under low threshold
        PowerMockito.doReturn(loadJsonFromResource("unAuthLowSessionResponse.json")).when(securedTouchCondition, "getResponse", ArgumentMatchers.any(), ArgumentMatchers.any());
        evaluated = testEvaluate();
        Assert.assertEquals(Boolean.FALSE, evaluated.isSatisfied());
        Assert.assertTrue(evaluated.getAdvice().get(AUTH_ADVICE_KEY).toString().contains(UNAUTH_LOW_ADVICE));
    }

    @Test
    public void evaluateTest_fail() throws Exception {
        // No session token
        ConditionDecision evaluated = testEvaluate();
        Assert.assertEquals(Boolean.FALSE, evaluated.isSatisfied());

        // Missing policy
        securedTouchCondition = PowerMockito.spy(new SecuredTouchConditionType());
        String noPolicy = "no-such-policy";
        securedTouchCondition.setPolicy(noPolicy);
        PowerMockito.doReturn("test-session-id").when(securedTouchCondition, "getSessionId", ArgumentMatchers.any());
        PowerMockito.doReturn(loadJsonFromResource("authSessionResponse.json")).when(securedTouchCondition, "getResponse", ArgumentMatchers.any(), ArgumentMatchers.any());

        evaluated = testEvaluate();
        Assert.assertEquals(Boolean.FALSE, evaluated.isSatisfied());
        Assert.assertTrue(evaluated.getAdvice().get(AUTH_ADVICE_KEY).toString().contains(String.format("Policy '%s' is missing on response", noPolicy)));
    }

    private ConditionDecision testEvaluate() throws EntitlementException {
        return securedTouchCondition.evaluate("", new Subject(), null, null);
    }

    private String loadJsonFromResource(String path) {
        InputStream inputStream = this.getClass().getResourceAsStream("/" + path);
        if(inputStream == null) {
            return "";
        }
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                sb.append(inputStr);
            }
            streamReader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}