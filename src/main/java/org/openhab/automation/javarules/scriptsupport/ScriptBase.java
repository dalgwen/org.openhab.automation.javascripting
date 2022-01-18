package org.openhab.automation.javarules.scriptsupport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.module.script.rulesupport.shared.ScriptedAutomationManager;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.automation.util.TriggerBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.binding.ThingActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScriptBase {

    private static Logger logger = LoggerFactory.getLogger("org.openhab.core.automation.javarules");

    protected Map<String, Object> bindings;

    protected ScriptedAutomationManager automationManager;

    protected ScriptThingActionsProxy actions;

    protected ScriptExtensionManagerWrapperProxy self;

    protected Map<String, Object> ruleSupport;

    // ScriptExtensionManagerWrapper is in the bundle private
    // org.openhab.core.automation.module.script.internal.ScriptExtensionManagerWrapper
    // which we can only access by reflection (Java is not Groovy)

    protected static class ScriptExtensionManagerWrapperProxy {

        Object scriptExtensionManagerRef;

        ScriptExtensionManagerWrapperProxy(Object scriptExtensionManagerRef) {
            this.scriptExtensionManagerRef = scriptExtensionManagerRef;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> importPreset(String preset) {

            Object o;
            Map<String, Object> presets = null;

            try {
                Method m = scriptExtensionManagerRef.getClass().getMethod("importPreset", String.class);
                o = m.invoke(scriptExtensionManagerRef, preset);
                presets = (Map<String, Object>) o;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return presets;
        }
    }

    protected static class ScriptThingActionsProxy {

        Object scriptThingActionsRef;

        ScriptThingActionsProxy(Object scriptThingActionsRef) {
            this.scriptThingActionsRef = scriptThingActionsRef;
        }

        public ThingActions get(String scope, String thingUid) {

            Object o;
            ThingActions actions = null;

            try {
                Method m = scriptThingActionsRef.getClass().getMethod("get", String.class, String.class);
                o = m.invoke(scriptThingActionsRef, scope, thingUid);
                actions = (ThingActions) o;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return actions;
        }

        public void invoke(ThingActions thingActions, String method, Object... params) {
            Object o;

            Class<?>[] paramClasses = new Class<?>[params.length];

            for (int i = 0; i < params.length; i++) {
                paramClasses[i] = params[i].getClass();
            }
            try {

                Method m = thingActions.getClass().getMethod(method, paramClasses);
                o = m.invoke(thingActions, params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /*
     * called by JavaRuleEngine on script load
     */

    public Map<String, Object> eval(Map<String, Object> bindings) {
        this.bindings = bindings;

        Object se = bindings.get("se");

        self = new ScriptExtensionManagerWrapperProxy(se);

        ruleSupport = self.importPreset("RuleSupport");

        Object actions = bindings.get("actions");

        this.actions = new ScriptThingActionsProxy(actions);

        automationManager = (ScriptedAutomationManager) ruleSupport.get("automationManager");

        try {

            onLoad();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }

        return this.bindings;
    }

    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    protected Map<String, Object> getBindings() {
        return bindings;
    }

    // to be implemented by the GroovyPort.java
    protected abstract void onLoad();

    // Utility methods
    // very inspired by pravussum's groovy rules
    // https://community.openhab.org/t/examples-for-groovy-scripts/131121/9

    protected Trigger createGenericCronTrigger(String triggerId, String cronExpression) {

        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("cronExpression", cronExpression);

        Trigger trigger = TriggerBuilder.create().withLabel(triggerId).withId(triggerId)
                .withTypeUID("timer.GenericCronTrigger").withConfiguration(new Configuration(configuration)).build();

        return trigger;
    }

    protected Trigger createItemStateChangeTrigger(String triggerId, String itemName) {

        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("itemName", itemName);

        Trigger trigger = TriggerBuilder.create().withLabel(triggerId).withId(triggerId)
                .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(new Configuration(configuration)).build();

        return trigger;
    }

    protected Trigger createItemStateChangeTrigger(String triggerId, String itemName, String newState) {

        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("itemName", itemName);
        configuration.put("state", newState);

        Trigger trigger = TriggerBuilder.create().withLabel(triggerId).withId(triggerId)
                .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(new Configuration(configuration)).build();

        return trigger;
    }

    protected Trigger createItemStateChangeTrigger(String triggerId, String itemName, String previousState,
            String newState) {

        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("itemName", itemName);
        configuration.put("previousState", previousState);
        configuration.put("state", newState);

        Trigger trigger = TriggerBuilder.create().withLabel(triggerId).withId(triggerId)
                .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(new Configuration(configuration)).build();

        return trigger;
    }

    protected Trigger createItemStateUpdateTrigger(String triggerId, String itemName) {

        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("itemName", itemName);

        Trigger trigger = TriggerBuilder.create().withLabel(triggerId).withId(triggerId)
                .withTypeUID("core.ItemStateUpdateTrigger").withConfiguration(new Configuration(configuration)).build();

        return trigger;
    }

    protected Trigger createItemStateUpdateTrigger(String triggerId, String itemName, String state) {

        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("itemName", itemName);
        configuration.put("command", state);

        Trigger trigger = TriggerBuilder.create().withLabel(triggerId).withId(triggerId)
                .withTypeUID("core.ItemStateUpdateTrigger").withConfiguration(new Configuration(configuration)).build();

        return trigger;
    }

    protected Trigger createItemCommandTrigger(String triggerId, String itemName, String command) {

        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("itemName", itemName);
        configuration.put("command", command);

        Trigger trigger = TriggerBuilder.create().withLabel(triggerId).withId(triggerId)
                .withTypeUID("core.ItemCommandTrigger").withConfiguration(new Configuration(configuration)).build();

        return trigger;
    }

    protected Trigger createChannelEventTrigger(String triggerId, String channelUID, String event) {

        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("channelUID", channelUID);
        configuration.put("event", event);

        Trigger trigger = TriggerBuilder.create().withLabel(triggerId).withId(triggerId)
                .withTypeUID("core.ChannelEventTrigger").withConfiguration(new Configuration(configuration)).build();

        return trigger;
    }

    protected static class RuleBuilder {

        private ScriptedAutomationManager automationManager;
        private SimpleRule sr = null;
        private List<Trigger> triggers = new ArrayList<Trigger>();
        private String name;

        private RuleBuilder(ScriptedAutomationManager automationManager, SimpleRule sr) {
            this.automationManager = automationManager;
            this.sr = sr;
        }

        public RuleBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public RuleBuilder withTrigger(Trigger trigger) {
            triggers.add(trigger);
            return this;
        }

        public void activate() {
            sr.setName(name);
            sr.setTriggers(triggers);
            automationManager.addRule(sr);
        }
    }

    protected RuleBuilder ruleBuilder(SimpleRule sr) {
        return new RuleBuilder(automationManager, sr);
    }
}
