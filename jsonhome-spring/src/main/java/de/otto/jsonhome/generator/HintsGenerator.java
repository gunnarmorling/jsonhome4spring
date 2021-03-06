/**
 Copyright 2012 Guido Steinacker

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package de.otto.jsonhome.generator;

import de.otto.jsonhome.model.Allow;
import de.otto.jsonhome.model.Hints;
import de.otto.jsonhome.model.HintsBuilder;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.*;

import static de.otto.jsonhome.model.Allow.*;
import static de.otto.jsonhome.model.HintsBuilder.hints;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.EnumSet.of;

public class HintsGenerator {

    private HintsGenerator() {
    }

    public static Hints hintsOf(final Method method) {
        final Set<Allow> allows = allowedHttpMethodsOf(method);
        final HintsBuilder hintsBuilder = hints().allowing(allows);
        final List<String> supportedRepresentations = supportedRepresentationsOf(method);
        if (allows.contains(PUT)) {
            hintsBuilder.acceptingForPut(supportedRepresentations);
        }
        if (allows.contains(POST)) {
            hintsBuilder.acceptingForPost(supportedRepresentations);
        }
        if (allows.contains(GET) || allows.contains(HEAD)) {
            hintsBuilder.representedAs(supportedRepresentations);
        }
        // TODO: PATCH

        return hintsBuilder.build();
    }

    /**
     * Analyses the method with a RequestMapping and returns a list of allowed http methods (GET, PUT, etc.).
     * <p/>
     * If the RequestMapping does not specify the allowed HTTP methods, "GET" is returned in a singleton list.
     *
     * @return list of allowed HTTP methods.
     * @throws NullPointerException if method is not annotated with @RequestMapping.
     */
    protected static Set<Allow> allowedHttpMethodsOf(final Method method) {
        final RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
        final Set<Allow> allows = listOfStringsFrom(methodRequestMapping.method());
        if (allows.isEmpty()) {
            return of(GET);
        } else {
            return allows;
        }
    }

    /**
     * Analyses the method with a RequestMapping and returns a list of supported representations.
     * <p/>
     * If the RequestMapping does not specify the produced or consumed representations,
     * "text/html" is returned in a singleton list.
     * <p/>
     * TODO: in case of a POST, text/html is not correct.
     *
     * @return list of allowed HTTP methods.
     * @throws NullPointerException if method is not annotated with @RequestMapping.
     */
    protected static List<String> supportedRepresentationsOf(final Method method) {
        final RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
        final LinkedHashSet<String> representations = new LinkedHashSet<String>();
        final String[] produces = methodRequestMapping.produces();
        if (produces != null) {
            representations.addAll(Arrays.asList(produces));
        }
        final String[] consumes = methodRequestMapping.consumes();
        if (consumes != null && consumes.length > 0) {

            // preserve order from methodRequestMapping:
            for (final String consumesRepresentation : consumes) {
                if (!representations.contains(consumesRepresentation)) {
                    representations.add(consumesRepresentation);
                }
            }
        } else {
            if (allowedHttpMethodsOf(method).equals(singleton(POST))) {
                representations.add("application/x-www-form-urlencoded");
            }
        }
        // default is HTTP GET
        if (representations.isEmpty()) {
            representations.add("text/html");
        }
        return new ArrayList<String>(representations);
    }

    private static Set<Allow> listOfStringsFrom(Object[] array) {
        final Set<Allow> result = EnumSet.noneOf(Allow.class);
        for (Object o : array) {
            result.add(Allow.valueOf(o.toString()));
        }
        return result;
    }
}