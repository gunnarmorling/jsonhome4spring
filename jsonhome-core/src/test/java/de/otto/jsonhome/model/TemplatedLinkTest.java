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
package de.otto.jsonhome.model;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static de.otto.jsonhome.fixtures.LinkFixtures.*;
import static de.otto.jsonhome.model.Allow.GET;
import static de.otto.jsonhome.model.Allow.POST;
import static de.otto.jsonhome.model.Allow.PUT;
import static de.otto.jsonhome.model.Documentation.emptyDocumentation;
import static de.otto.jsonhome.model.TemplatedLink.templatedLink;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.EnumSet.of;
import static org.testng.Assert.*;

/**
 * @author Guido Steinacker
 * @since 16.09.12
 */
public class TemplatedLinkTest {

    @Test
    public void shouldReturnValidJson() throws Exception {
        // given
        final TemplatedLink aboutPageLink = ABOUTPAGE_LINK;
        // when
        final Map<String, ?> json = aboutPageLink.toJson();
        // then
        assertNotNull(json);
        assertNull(json.get("href"));
        assertEquals(json.get("href-template"), REL_PAGE_HREF);
        assertEquals(json.get("href-vars"), singletonMap("pageId", "http://example.org/json-home/vartype/shop/page/pageId"));
        final Map hints = (Map) json.get("hints");
        assertNotNull(hints);
        assertEquals(hints.get("allow"), of(GET));
        assertEquals(hints.get("representations"), Arrays.asList("text/html", "application/json"));

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void mergeWithShouldFailWithExceptionIfOtherDoesNotHaveSameRelationType() {
        // given
        final TemplatedLink aboutPageLink = ABOUTPAGE_LINK;
        final TemplatedLink otherTemplatedLink = TemplatedLink.templatedLink(
                RESOURCELINK_SHOP_STOREFRONT,
                REL_PAGE_HREF,
                asList(new HrefVar("pageId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET),
                        asList("text/html")
                ));
        // when
        aboutPageLink.mergeWith(otherTemplatedLink);
        // then an exception is thrown
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void mergeWithShouldFailWithExceptionIfOtherLinksToDifferentResource() {
        // given
        final TemplatedLink aboutPageLink = ABOUTPAGE_LINK;
        final TemplatedLink otherTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                "/foo/{fooId}",
                asList(new HrefVar("fooId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET),
                        asList("text/html")
                ));
        // when
        aboutPageLink.mergeWith(otherTemplatedLink);
        // then an exception is thrown
    }

    @Test
    public void mergeWithShouldMergeAllowsSpec() {
        // given
        final TemplatedLink aboutPageLink = ABOUTPAGE_LINK;
        final TemplatedLink otherTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                REL_PAGE_HREF,
                asList(new HrefVar("pageId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(PUT),
                        asList("text/html", "application/json")
                )
        );
        // when
        final ResourceLink resourceLink = aboutPageLink.mergeWith(otherTemplatedLink);
        // then
        assertEquals(resourceLink.getHints().getAllows(), of(GET, PUT));
    }

    @Test
    public void mergeWithShouldMergeRepresentations() {
        // given
        final TemplatedLink aboutPageLink = ABOUTPAGE_LINK;
        final TemplatedLink otherTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                REL_PAGE_HREF,
                asList(new HrefVar("pageId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET),
                        asList("text/html", "application/foo")
                )
        );
        // when
        final ResourceLink resourceLink = aboutPageLink.mergeWith(otherTemplatedLink);
        // then
        assertEquals(resourceLink.getHints().getRepresentations(), asList("text/html", "application/json", "application/foo"));
    }

    /**
     * The json-home Draft is relying on uri templates as defined in
     * http://tools.ietf.org/html/rfc6570. Spring is using a different approach, where it is possible to
     * have different methods supporting multiple uri templates per resource. This is currently not
     * supported by the json-home generator.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void mergeWithShouldFailForDifferentHrefTemplates() {
        // given
        final TemplatedLink aboutPageLink = ABOUTPAGE_LINK;
        final TemplatedLink otherTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                "pages/foo/{pageId}",
                asList(new HrefVar("pageId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET),
                        asList("text/html", "application/json")
                )
        );
        // when
        aboutPageLink.mergeWith(otherTemplatedLink);
        // then an exception is thrown
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void mergeWithShouldFailForDifferentHrefVars() {
        // given
        final TemplatedLink aboutPageLink = ABOUTPAGE_LINK;
        final TemplatedLink otherTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                "pages/{pageId}",
                asList(new HrefVar("foo", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET),
                        asList("text/html", "application/json")
                )
        );
        // when
        aboutPageLink.mergeWith(otherTemplatedLink);
        // then an exception is thrown
    }

    @Test(enabled = false)
    public void mergeWithShouldMergeAcceptPut() {
        // given
        final TemplatedLink thisTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                REL_PAGE_HREF,
                asList(new HrefVar("pageId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET, PUT),
                        asList("application/foo"),
                        asList("application/json"),
                        Collections.<String>emptyList()
                )
        );
        final TemplatedLink thatTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                REL_PAGE_HREF,
                asList(new HrefVar("pageId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET, PUT),
                        asList("application/foo"),
                        asList("application/foo"),
                        Collections.<String>emptyList()
                )
        );
        // when
        final ResourceLink resourceLink = thisTemplatedLink.mergeWith(thatTemplatedLink);
        // then
        final Hints hints = resourceLink.getHints();
        assertEquals(hints.getRepresentations(), asList("application/foo"));
        assertEquals(hints.getAcceptPut(), asList("application/json", "application/foo"));
    }

    @Test(enabled = false)
    public void mergeWithShouldMergeAcceptPost() {
        // given
        final TemplatedLink thisTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                REL_PAGE_HREF,
                asList(new HrefVar("pageId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET, POST),
                        asList("application/foo"),
                        Collections.<String>emptyList(),
                        asList("application/json")
                )
        );
        final TemplatedLink thatTemplatedLink = templatedLink(
                RESOURCELINK_SHOP_PAGE,
                REL_PAGE_HREF,
                asList(new HrefVar("pageId", VAR_TYPE_PAGEID, emptyDocumentation())),
                new Hints(
                        of(GET, POST),
                        asList("application/foo"),
                        Collections.<String>emptyList(),
                        asList("application/foo")
                )
        );
        // when
        final ResourceLink resourceLink = thisTemplatedLink.mergeWith(thatTemplatedLink);
        // then
        final Hints hints = resourceLink.getHints();
        assertEquals(hints.getRepresentations(), asList("application/foo"));
        assertEquals(hints.getAcceptPost(), asList("application/json", "application/foo"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void mergeWithDirectLinkShouldFailWithIllegalStateException() {
        // given
        final TemplatedLink templatedLink = ABOUTPAGE_LINK;
        final DirectLink directLink = STOREFRONT_LINK;
        // when
        templatedLink.mergeWith(directLink);
        // then an exception is thrown.
    }

    @Test
    public void mergeWithSelfShouldReturnEqualTemplatedLink() {
        // given
        final TemplatedLink aboutLink = ABOUTPAGE_LINK;
        // when
        final ResourceLink mergedLink = aboutLink.mergeWith(aboutLink);
        // then
        assertEquals(aboutLink, mergedLink);
    }
}
