
# openHAB Java Scripting

This openHAB add-on provides support for JSR 223 scripts written in Java.

It makes heavy use of Eric Obermühlner's Java JSR 223 ScriptEngine [java-scriptengine](https://github.com/eobermuhlner/java-scriptengine).

# Programming Hints

* all Java classes used as JSR 223 script have to inherit from [org.openhab.automation.javascripting.scriptsupport.Script](src/main/java/org/openhab/automation/javascripting/scriptsupport/Script.java)

* When the openHAB ScriptFileWatcher detects a new .java File in conf/automation/jsr223 
  it is loaded, compiled into memory, parsed for @Rule annotations and the rules are activated. Then the onLoad() method is executed.

* Java Rules do not see other rule classes. Each one has its own ClassLoader. 

* you can use libraries if you package them as [OSGI bundles](#library-code).

* you can use openHAB classes from the packages listed in [bnd.bnd](bnd.bnd).

* openHAB Java Scripting requires openHAB 3.3.0 or later

# Remote Debug

start openHAB with start_debug.sh and remote debug from Eclipse, stop at breakpoints.

![screenshot](src/doc/EclipseDebug.png?raw=true)
 

# Test

* Copy org.openhab.automation.javascripting-3.3.0.jar into the addons folder (download via the [Releases](https://github.com/weberjn/org.openhab.automation.javascripting/releases) link).

* Copy from the sample Java classes into conf/automation/jsr223/

(they are all in src/test/java)

A Java class is loaded, compiled into memory and its onLoad() method executed. A Python or JS Script is
evalated during load, this is simulated with the onLoad() method. So, rules can be defined programmatically
in onLoad().
Or, you can annotate public instance variables of type SimpleRule. See the CronRule sample.

# Project for Scripts

To have a script compile without errors in Eclipse, it should be in a Java project with openHAB dependencies and a dependency to javascripting.

* create a folder with a Maven project:
* use this [src/doc/pom.xml](src/doc/pom.xml) as template 
* adapt parent relativePath
* change groupId and artifactId
* import the folder as maven project into Eclipse
* create the Java scripts in src/main/java in the default package 
* if the source compiles without errors, copy it to conf/automation/jsr223

```sh
mvn  -DskipChecks clean install
```


# Library Code 

Java Rules has `DynamicImport-Package: *` so it can access code in other bundles. 

Bundle your code as OSGI bundle as in this sample: https://github.com/weberjn/org.openhab.automation.javascripting.ext 

# Building the Addon

Clone [java-scriptengine](https://github.com/eobermuhlner/java-scriptengine) and mvn install (symlink ch.obermuhlner.scriptengine.java/src to make the Maven
build work).

Clone Java Scripting under openhab-addons/bundles and run mvn install

# Sample Scripts

The samples are all in [src/test/java](src/test/java).

## Item change rules, annotation based

```java

import java.util.Map;

import org.openhab.automation.javascripting.annotations.ItemStateUpdateTrigger;
import org.openhab.automation.javascripting.annotations.Rule;
import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.OnOffType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MPDSilencer extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.mpdrules");

    private OnOffType mpd_previous_stop_state;

    @Rule(name = "PhoneRingingRule")
    @ItemStateUpdateTrigger(id = "PhoneRingingTrigger", item = "PhoneRinging")
    public SimpleRule phoneRingingRule = new SimpleRule() {

        @Override
        public Object execute(Action module, Map<String, ?> inputs) {

            logger.info("phone", "phone ringing {}", inputs.get("state"));

            Item mpd_stop = itemRegistry.get("mpd_music_player_pi_stop");

            OnOffType mpd_stop_state = (OnOffType) mpd_stop.getState();

            mpd_previous_stop_state = mpd_stop_state;

            if (mpd_stop_state == OnOffType.OFF) {
                events.sendCommand("mpd_music_player_pi_stop", "ON");
            }

            return "";
        }
    };

    @Rule(name = "PhoneIdleRule")
    @ItemStateUpdateTrigger(id = "PhoneIdleTrigger", item = "PhoneIdle")
    public SimpleRule phoneIdleRule = new SimpleRule() {

        @Override
        public Object execute(Action module, Map<String, ?> inputs) {

            logger.info("phone", "phone idle {}", inputs.get("state"));

            if (mpd_previous_stop_state == OnOffType.OFF) {
                events.sendCommand("mpd_music_player_pi_stop", "OFF");
            }

            return "";
        }
    };

    @Override
    protected void onLoad() {
        logger.info("phone", "rules loaded");
    };
}
```

## Changing Items

```java

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusExamples extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.eventbus");

    @Override
    protected void onLoad() {

        logger.info("Java onLoad()");

        events.sendCommand("Livingroom_Light", "OFF");

        Item item = itemRegistry.get("Morning_Temperature");

        ((NumberItem) item).setState(new DecimalType(0.0f));

        events.postUpdate(item, 37.2f);

        Number state = (Number) item.getState();
        logger.info("new State: {}", state.floatValue());

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));

        state = (Number) item.getState();
        logger.info("new State again: {}", state.floatValue());

        logger.info("eventbus done");
    }
}
```

## Cron Rule, annotation based

```java

import java.util.Map;

import org.openhab.automation.javascripting.annotations.CronTrigger;
import org.openhab.automation.javascripting.annotations.Rule;
import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CronRule extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.cronrule");

    private int counter = 1;

    @Rule(name = "CronRule")
    @CronTrigger(id = "CronTrigger", cronExpression = "0 * * * * ?")
    public SimpleRule cronrule = new SimpleRule() {

        @Override
        public Object execute(Action module, Map<String, ?> inputs) {

            logger.info("Java cronrule execute {}", counter++);

            return "";
        }
    };

    @Override
    protected void onLoad() {
        logger.info("Java onLoad()");
    };
}
```

## ItemChanged Rule

```java

import java.util.Map;

import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemChangedRule extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.itemrule");

    private int counter = 1;

    @Override
    protected void onLoad() {

        logger.info("Java onLoad()");

        SimpleRule sr = new SimpleRule() {

            @Override
            public Object execute(Action module, Map<String, ?> inputs) {

                logger.info("Java execute {}", counter++);

                return "";
            }
        };

        Trigger trigger = createItemStateChangeTrigger("BatteryLevelChangedTrigger", "BatteryLevel");

        ruleBuilder(sr).withName("BatteryLevelChanged").withTrigger(trigger).activate();

        logger.info("BatteryLevelChanged rule activated");
    };
}
```

## Addon Actions

```java

import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.thing.binding.ThingActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMail extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.mail");

    @Override
    protected void onLoad() {

        ThingActions thingActions = actions.get("mail", "mail:smtp:mailSender");
        actions.invoke(thingActions, "mail_at_receiver", "a subject", "mailcontent Java script onload()");

        logger.info("mail sent");
    }
}
```

## Static Actions

```java

import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.model.script.actions.Exec;
import org.openhab.core.model.script.actions.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticActions extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javarules.actions");

    @Override
    protected void onLoad() {

        String res = HTTP.sendHttpGetRequest("http://localhost/");

        String cmd = "termux-media-player play camera-shutter.mp3";
        Exec.executeCommandLine("ssh", "nexus9", cmd);

        logger.info("static actions done");
    }
}
```

## Transformations

```java

import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.transform.actions.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Transformations extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javarules.transform");

    @Override
    protected void onLoad() {

        String s = Transformation.transform("REGEX", ".*(hello).*", "hello, world");

        logger.info("transform done, got: " + s);
    }
}
```
## Persistence

```java

import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.items.Item;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistItems extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.persist");

    @Override
    protected void onLoad() {

        Item item = itemRegistry.get("Morning_Temperature");

        PersistenceExtensions.persist(item);

        logger.info("persist done");
    }
}
```

## Write to a File

```java

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.openhab.automation.javascripting.annotations.ItemStateUpdateTrigger;
import org.openhab.automation.javascripting.annotations.Rule;
import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.items.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWriteRule extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.writefile");

    @Rule(name = "TemperatureToFileRule")
    @ItemStateUpdateTrigger(id = "TemperatureTrigger", item = "Morning_Temperature")
    public SimpleRule temperatureToFileRule = new SimpleRule() {

        @Override
        public Object execute(Action module, Map<String, ?> inputs) {

            logger.info("@ItemStateUpdateTrigger Morning_Temperature");

            Item item = itemRegistry.get("Morning_Temperature");

            Number state = (Number) item.getState();

            Path path = Paths.get("/tmp/Morning_Temperature.txt");

            try {
                Files.writeString(path, String.format("%f%n", state.floatValue()), StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.error("", e);
                throw new RuntimeException(e);
            }

            return "";
        }
    };

    @Override
    protected void onLoad() {
        logger.info("Java onLoad()");
    }
}
```
Set a new temperature

```Shell
openhab> openhab:update Morning_Temperature 37.7
```

```Shell
$ cat /tmp/Morning_Temperature.txt
37.700001
```

## Json Rule

This rule is triggered bei either of two items, creates a Json String from their states and sends it to a third item 
(which should be linked to an MQTT command topic, on which a Python script could listen and feed an e-paper display).

```java

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.automation.javascripting.annotations.ItemStateUpdateTrigger;
import org.openhab.automation.javascripting.annotations.Rule;
import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class JsonRule extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.jsonrule");

    final String outsideTemperatureItem = "OutsideTemperature";
    final String salonTemperatureItem = "SalonTemperature";

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Rule(name = "JsonRule")
    @ItemStateUpdateTrigger(id = "OutsideTemperatureTrigger", item = outsideTemperatureItem)
    @ItemStateUpdateTrigger(id = "SalonTemperatureTrigger", item = salonTemperatureItem)
    public SimpleRule jsonrule = new SimpleRule() {

        @Override
        public Object execute(Action module, Map<String, ?> inputs) {

            logger.info("Java jsonrule execute: {}", inputs.toString());

            try {
                String json = createJson();

                logger.info("json: {}", json);

                Item item = itemRegistry.get("EPaper_Screen_Json");

                events.sendCommand(item, json);
            } catch (Exception e) {
                logger.error(inputs.toString(), e);
                throw e;
            }

            return "";
        }
    };

    @Override
    protected void onLoad() {
        logger.info("Java onLoad()");
    };

    private String createJson() {
        Map<String, List<Map<String, Object>>> screen = new HashMap<>();

        List<Map<String, Object>> ops = new ArrayList<>();

        int x = 10;
        int y = 0;

        LocalDateTime ldt = LocalDateTime.now();
        String s = ldt.format(dateTimeFormatter);

        Map<String, Object> text0 = new HashMap<>();
        text0.put("type", "text");
        text0.put("x", x);
        text0.put("y", y);
        text0.put("text", String.format("@ %s", s));

        ops.add(text0);

        y += 30;

        DecimalType dt = itemRegistry.get(outsideTemperatureItem).getStateAs(DecimalType.class);

        if (dt != null) {
            float f = dt.floatValue();

            Map<String, Object> text1 = new HashMap<>();
            text1.put("type", "text");
            text1.put("x", x);
            text1.put("y", y);
            text1.put("text", String.format("Temp Outside: %.1f", f));

            ops.add(text1);

            y += 30;
        }

        DecimalType dts = itemRegistry.get(salonTemperatureItem).getStateAs(DecimalType.class);

        if (dts != null) {
            float f = dts.floatValue();

            Map<String, Object> text1 = new HashMap<>();
            text1.put("type", "text");
            text1.put("x", x);
            text1.put("y", y);
            text1.put("text", String.format("Temp Salon: %.1f", f));

            ops.add(text1);

            y += 30;
        }

        screen.put("screenobjects", ops);

        Gson gson = new Gson();

        String output = gson.toJson(screen);

        return output;
    }
}
```

```Shell  
openhab> openhab:update  OutsideTemperature 27
Update has been sent successfully.
openhab> openhab:status EPaper_Screen_Json
{"screenobjects":[{"x":10,"y":0,"text":"@ 2023-06-04 20:07:42","type":"text"},{"x":10,"y":30,"text":"Temp Outside: 27.0","type":"text"}]}
```  
  
## Groovy Port

This class is ported from the [openHAB JSR 223 Groovy Sample](https://www.openhab.org/docs/configuration/jsr223.html#groovy).
It does not use syntactic sugar of the Script base class, only pure openHAB JSR 223.

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.automation.javascripting.scriptsupport.Script;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.automation.util.TriggerBuilder;
import org.openhab.core.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * @author weberjn
 */
public class GroovyPort extends Script {

    private Logger logger = LoggerFactory.getLogger("org.openhab.automation.javascripting.script");

    public int counter = 1;

    protected void onLoad() {

        SimpleRule sr = new SimpleRule() {

            @Override
            public Object execute(Action module, Map<String, ?> inputs) {

                logger.info("Java execute {},  inputs: {} ", counter++, inputs);

                return "";
            }
        };

        sr.setName("Java-One");

        List<Trigger> triggers = new ArrayList<Trigger>(1);

        Map<String, Object> triggerConf = new HashMap<String, Object>();
        triggerConf.put("cronExpression", "0 * * * * ?");

        Trigger trigger = TriggerBuilder.create().withId("aTimerTrigger").withTypeUID("timer.GenericCronTrigger")
                .withConfiguration(new Configuration(triggerConf)).build();

        triggers.add(trigger);

        sr.setTriggers(triggers);

        automationManager.addRule(sr);

        logger.info("onLoad() done");
    }
}
```

