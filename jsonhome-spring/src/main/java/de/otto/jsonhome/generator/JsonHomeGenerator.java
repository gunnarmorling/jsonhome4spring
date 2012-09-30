package de.otto.jsonhome.generator;

import de.otto.jsonhome.annotation.LinkRelationType;
import de.otto.jsonhome.model.JsonHome;
import de.otto.jsonhome.model.ResourceLink;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import static de.otto.jsonhome.model.DirectLink.directLink;
import static de.otto.jsonhome.model.JsonHome.emptyJsonHome;
import static de.otto.jsonhome.model.JsonHome.jsonHome;
import static de.otto.jsonhome.model.JsonHomeBuilder.jsonHomeBuilder;
import static de.otto.jsonhome.model.ResourceLinkHelper.mergeResources;
import static de.otto.jsonhome.model.TemplatedLink.templatedLink;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A generator used to create JsonHome documents from Spring controllers.
 *
 * @author Guido Steinacker
 * @since 15.09.12
 */

public class JsonHomeGenerator {

    private final URI rootUri;
    private final HrefVarsGenerator hrefVarsGenerator = new HrefVarsGenerator();

    public static JsonHomeGenerator jsonHomeFor(final URI rootUri) {
        return new JsonHomeGenerator(rootUri);
    }

    protected JsonHomeGenerator(final URI rootUri) {
        if (rootUri == null) {
            throw new NullPointerException("Parameter rootUri must not be null.");
        }
        this.rootUri = rootUri;
    }

    public JsonHome with(final Class<?> controller) {
        if (isSpringController(controller)) {
            return jsonHomeBuilder()
                    .addResources(resourceLinksFor(controller))
                    .build();
        } else {
            return emptyJsonHome();
        }
    }

    public JsonHome with(final Collection<Class<?>> controller) {
        List<ResourceLink> resources = new ArrayList<ResourceLink>();
        for (final Class<?> controllerClass : controller) {
            if (isSpringController(controllerClass)) {
                resources = mergeResources(resources, resourceLinksFor(controllerClass));
            }
        }
        return jsonHome(resources);
    }

    protected List<ResourceLink> resourceLinksFor(final Class<?> controller) {
        List<ResourceLink> resourceLinks = emptyList();
        for (final Method method : controller.getMethods()) {
            resourceLinks = mergeResources(resourceLinks, resourceLinksForMethod(controller, method));
        }
        return resourceLinks;
    }

    /**
     * Analyses the a method of a controller (having a RequestMapping) and returns the list of ResourceLinks of this method.
     *
     * @param controller the controller of the method.
     * @param method the method
     * @return list of resource links.
     */
    protected List<ResourceLink> resourceLinksForMethod(final Class<?> controller,
                                                        final Method method) {
        final List<String> parentResourcePaths = parentResourcePathsFrom(controller);
        final List<ResourceLink> resourceLinks = new ArrayList<ResourceLink>();
        final RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
        if (methodRequestMapping != null) {
            final List<String> representations = supportedRepresentationsOf(method);
            final List<String> allows = allowedHttpMethodsOf(method);
            for (String resourcePathPrefix : parentResourcePaths) {
                final String[] resourcePathSuffixes = methodRequestMapping.value().length > 0
                        ? methodRequestMapping.value()
                        : new String[] {""};
                for (String resourcePathSuffix : resourcePathSuffixes) {
                    final String resourcePath = rootUri + resourcePathPrefix + resourcePathSuffix;
                    final URI relationType = relationTypeFrom(controller, method);
                    if (relationType != null) {
                        if (resourcePath.matches(".*\\{.*\\}")) {
                            resourceLinks.add(templatedLink(
                                    relationType,
                                    resourcePath,
                                    hrefVarsGenerator.hrefVarsFor(method),
                                    allows,
                                    representations
                            ));
                        } else {
                            resourceLinks.add(directLink(
                                    relationType,
                                    URI.create(resourcePath),
                                    allows,
                                    representations
                            ));
                        }
                    }
                }
            }
        }
        return resourceLinks;
    }

    /**
     * Analyses the controller (possibly annotated with RequestMapping) and returns the list of resource paths defined by the mapping.
     *
     * @param controller the controller.
     * @return list of resource paths.
     */
    protected List<String> parentResourcePathsFrom(final Class<?> controller) {
        final RequestMapping controllerRequestMapping = controller.getAnnotation(RequestMapping.class);
        final List<String> resourcePathPrefixes;
        if (controllerRequestMapping != null) {
            resourcePathPrefixes = asList(controllerRequestMapping.value());
        } else {
            resourcePathPrefixes = asList("");
        }
        return resourcePathPrefixes;
    }

    /**
     * Analyses the method with a RequestMapping and returns a list of allowed http methods (GET, PUT, etc.).
     *
     * If the RequestMapping does not specify the allowed HTTP methods, "GET" is returned in a singleton list.
     *
     * @return list of allowed HTTP methods.
     * @throws NullPointerException if method is not annotated with @RequestMapping.
     */
    protected List<String> allowedHttpMethodsOf(final Method method) {
        final RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
        final List<String> allows = listOfStringsFrom(methodRequestMapping.method());
        if (allows.isEmpty()) {
            return singletonList("GET");
        } else {
            return allows;
        }
    }

    /**
     * Analyses the method with a RequestMapping and returns a list of supported representations.
     *
     * If the RequestMapping does not specify the produced or consumed representations,
     * "text/html" is returned in a singleton list.
     *
     * TODO: in case of a POST, text/html is not correct.
     *
     * @return list of allowed HTTP methods.
     * @throws NullPointerException if method is not annotated with @RequestMapping.
     */
    protected List<String> supportedRepresentationsOf(final Method method) {
        final RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
        final LinkedHashSet<String> representations = new LinkedHashSet<String>();
        final String[] produces = methodRequestMapping.produces();
        if (produces != null) {
            representations.addAll(asList(produces));
        }
        final String[] consumes = methodRequestMapping.consumes();
        if (consumes != null) {
            // preserve order from methodRequestMapping:
            for (final String consumesRepresentation : consumes) {
                if (!representations.contains(consumesRepresentation)) {
                    representations.add(consumesRepresentation);
                }
            }
        }
        // default is HTTP GET
        if (representations.isEmpty()) {
            representations.add("text/html");
        }
        return new ArrayList<String>(representations);
    }

    /**
     * Analyses a method of a controller and returns the fully qualified URI of the link-relation type.
     *
     * If the neither the method, nor the controller is annotated with LinkRelationType, null is returned.
     *
     * The LinkRelationType of the method is overriding the LinkRelationType of the Controller.
     *
     * @param controller the controller
     * @param method the method
     * @return URI of the link-relation type, or null
     */
    protected URI relationTypeFrom(final Class<?> controller, final Method method) {
        final LinkRelationType controllerLinkRelationType = controller.getAnnotation(LinkRelationType.class);
        final LinkRelationType methodLinkRelationType = method.getAnnotation(LinkRelationType.class);
        if (controllerLinkRelationType == null && methodLinkRelationType == null) {
            return null;
        } else {
            final String linkRelationType = methodLinkRelationType != null
                    ? methodLinkRelationType.value()
                    : controllerLinkRelationType.value();
            return URI.create(linkRelationType.startsWith("http://")
                    ? linkRelationType
                    : rootUri + linkRelationType);
        }
    }

    private List<String> listOfStringsFrom(Object[] array) {
        final List<String> result = new ArrayList<String>(array.length);
        for (Object o : array) {
            result.add(o.toString());
        }
        return result;
    }

    private boolean isSpringController(final Class<?> controller) {
        return controller.getAnnotation(Controller.class) != null;
    }

}
