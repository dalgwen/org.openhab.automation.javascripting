/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.automation.javascripting.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author Jürgen Weber - Initial contribution
 */

@Retention(RUNTIME)
@Repeatable(ThingStateChangeTriggers.class)
@Target(ElementType.FIELD)
public @interface ThingStateChangeTrigger {
    String id();

    String thingUID();

    String newState() default "";

    String previousState() default "";
}
