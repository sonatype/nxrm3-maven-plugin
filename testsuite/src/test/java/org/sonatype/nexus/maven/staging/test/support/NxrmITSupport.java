/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2019-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.maven.staging.test.support;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.sonatype.nexus.api.repository.v3.SearchBuilder;

import org.sonatype.sisu.litmus.testsupport.inject.InjectedTestSupport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertNotNull;

/*
 * Abstract base class for NXRM3 integration tests. Please note that the test container is started outside of the test
 * classes, which means tests are not likely to be run against a fresh instance of nexus. If a fresh container is
 * necessary consider starting a second image for specific tests, but doing so will increase the test time for each
 * image start
 */
public abstract class NxrmITSupport
    extends InjectedTestSupport
{
  static final String NX3_PORT_SYS_PROP = "nexus3.it.port";

  protected static URI nexusItUri;

  @BeforeClass
  public static void setupNexusUri() throws Exception {
    String port = getPort();
    assertNotNull(
        "Unable to load Nexus 3 integration test server port from system property '" + NX3_PORT_SYS_PROP + "'", port);

    nexusItUri = new URI("http://localhost:" + port);
  }

  protected static String getPort() {
    return System.getProperty(NX3_PORT_SYS_PROP);
  }

  protected ComponentItem waitForComponentWithTags(final Map<String, String> search, String... tags) {
    waitForComponent(search);

    Matcher<? extends Iterable<?>> tagMatcher = (tags == null || tags.length == 0) ? hasSize(0) : contains(tags);
    return await().atMost(10, SECONDS)
        .until(() -> componentSearch(search).items.get(0), hasProperty("tags", tagMatcher));
  }

  ComponentItem waitForComponent(final Map<String, String> search) {
    return await().atMost(10, SECONDS).until(() -> componentSearch(search).items, hasSize(1)).get(0);
  }

  protected void verifyComponent(final String repository,
                                 final String group,
                                 final String name,
                                 final String version,
                                 final String... tags)
  {
    // verify server can retrieve the component
    Map<String, String> search = SearchBuilder.create().withRepository(repository).withGroup(group).withName(name)
        .withVersion(version).build();

    ComponentItem component;

    try {
      component = waitForComponentWithTags(search, tags);
    }
    catch (Exception e) {
      throw new AssertionError("Component (group: " + group + "; name: " + name + "; version: " + version +
          ") was not found in Nexus Repository Manager repository: " + repository, e);
    }

    stream(tags).forEach(tag -> assertThat(component.tags, hasItem(tag)));
  }

  ComponentsResponse componentSearch(final Map<String, String> search)
      throws Exception
  {
    StringBuilder query = new StringBuilder("/service/rest/v1/search?");

    for (String key : search.keySet()) {
      query.append(key).append("=").append(search.get(key)).append("&");
    }

    return componentQuery(query.substring(0, query.length() - 1)); // trims trailing &
  }

  ComponentsResponse componentQuery(final String query)
      throws Exception
  {
    HttpGet get = new HttpGet(nexusItUri.resolve(query));
    get.setHeader("Accept", "application/json");
    CloseableHttpClient http = HttpClientBuilder.create().build();
    String responseBody = http.execute(get, new BasicResponseHandler());
    return new ObjectMapper().readerFor(ComponentsResponse.class).readValue(responseBody);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ComponentsResponse
  {
    public List<ComponentItem> items;

    public ComponentsResponse() {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ComponentItem
  {
    public String group;

    public String name;

    public String version;

    public List<AssetItem> assets;

    public List<String> tags;

    public ComponentItem() {}

    public List<String> getTags() {
      return tags;
    }

    public void setTags(final List<String> tags) {
      this.tags = tags;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AssetItem
  {
    public String path;

    public Checksum checksum;

    public AssetItem() {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Checksum
  {
    public String md5;

    public Checksum() {}
  }
}
