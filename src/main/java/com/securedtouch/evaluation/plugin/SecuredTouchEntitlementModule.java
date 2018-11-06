package com.securedtouch.evaluation.plugin;

import org.forgerock.openam.entitlement.EntitlementModule;
import org.forgerock.openam.entitlement.EntitlementRegistry;

public class SecuredTouchEntitlementModule implements EntitlementModule {
    @Override
    public void registerCustomTypes(EntitlementRegistry entitlementRegistry) {
        entitlementRegistry.registerConditionType("SecuredTouchCondition", SecuredTouchConditionType.class);
    }
}