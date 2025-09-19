/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.sonatype.nexus.api.common.ServerConfig;
import org.sonatype.nexus.api.exception.RepositoryManagerException;
import org.sonatype.nexus.api.repository.v3.Asset;
import org.sonatype.nexus.api.repository.v3.Component;
import org.sonatype.nexus.api.repository.v3.ComponentInfo;
import org.sonatype.nexus.api.repository.v3.NxrmVersion;
import org.sonatype.nexus.api.repository.v3.Repository;
import org.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import org.sonatype.nexus.api.repository.v3.SearchBuilder;
import org.sonatype.nexus.api.repository.v3.Tag;
import org.sonatype.nexus.api.repository.v3.impl.rest.GetRepositoriesResponseHandler;
import org.sonatype.nexus.api.repository.v3.impl.rest.GetTagResponseHandler;
import org.sonatype.nexus.api.repository.v3.impl.rest.GetVersionResponseHandler;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import static org.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static org.sonatype.nexus.api.common.NexusStringUtils.isBlank;
import static org.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;

import static org.sonatype.nexus.api.repository.v3.impl.rest.StagingResponseHandlerFactory.newAssociateHandler;
import static org.sonatype.nexus.api.repository.v3.impl.rest.StagingResponseHandlerFactory.newDeleteHandler;
import static org.sonatype.nexus.api.repository.v3.impl.rest.StagingResponseHandlerFactory.newDisassociateHandler;
import static org.sonatype.nexus.api.repository.v3.impl.rest.StagingResponseHandlerFactory.newMoveHandler;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

/**
 * Nexus Repository Manager 3.x implementation of {@link RepositoryManagerV3Client}
 *
 * @since 3.0
 */
public class DefaultNexusRepositoryV3Client
    implements RepositoryManagerV3Client
{
  static final String BASE_API_PATH = "service/rest";

  static final String VERSION_API = BASE_API_PATH + "/wonderland/status";

  static final String REPOSITORIES_API = BASE_API_PATH + "/v1/repositories";

  static final String UPLOAD_API = BASE_API_PATH + "/v1/components";

  static final String TAGS_API = BASE_API_PATH + "/v1/tags";

  static final String TAGS_ASSOCIATE_API = TAGS_API + "/associate";

  static final String STAGING_API = BASE_API_PATH + "/v1/staging";

  static final String MOVE_API = STAGING_API + "/move";

  static final String DELETE_API = STAGING_API + "/delete";

  private final ServerConfig serverConfig;

  private final NexusRepositoryHttpClient nxrmClient;

  private final Optional<HttpContext> httpClientContext;

  /**
   * Constructs a client using the specified configuration options
   *
   * @param serverConfig {@link ServerConfig} for the target NXRM3 server
   * @param httpClient {@link HttpClient} to use for executing the REST API calls
   */
  public DefaultNexusRepositoryV3Client(final ServerConfig serverConfig, final HttpClient httpClient) {
    this.serverConfig = requireNonNull(serverConfig, "Nexus server configuration is required");
    this.nxrmClient = new NexusRepositoryHttpClient(requireNonNull(httpClient, "HttpClient is required"));

    if (serverConfig.getAuthentication() != null) { // setup preemptive auth when auth is provided
      URI nexusUri = serverConfig.getAddress();
      AuthCache authCache = new BasicAuthCache();
      BasicScheme basicAuth = new BasicScheme();
      authCache.put(new HttpHost(nexusUri.getHost(), nexusUri.getPort(), nexusUri.getScheme()), basicAuth);

      HttpClientContext context = HttpClientContext.create();
      context.setAuthCache(authCache);
      httpClientContext = of(context);
    }
    else {
      httpClientContext = empty();
    }
  }

  @Override
  public NxrmVersion getVersion() throws RepositoryManagerException {
    return nxrmClient.execute(new HttpGet(serverConfig.getAddress().resolve(VERSION_API)),
        new GetVersionResponseHandler(), httpClientContext, of("Get server version"));
  }

  @Override
  public List<Repository> getRepositories() throws RepositoryManagerException {
    return nxrmClient.execute(new HttpGet(serverConfig.getAddress().resolve(REPOSITORIES_API)),
        new GetRepositoriesResponseHandler(), httpClientContext, of("Get repositories"));
  }

  @Override
  public void upload(final String repositoryName, final Component component) throws RepositoryManagerException {
    upload(repositoryName, component, null);
  }

  @Override
  public void upload(
      final String repositoryName,
      final Component component,
      final String tagName) throws RepositoryManagerException
  {
    checkArgument(isNotBlank(repositoryName), "Repository name is required");
    requireNonNull(component, "Component is required");
    checkArgument(component.getAssets() != null && component.getAssets().size() > 0,
        "Upload requires at least one asset in the component");

    URI uploadUri =
        buildUri(serverConfig.getAddress().resolve(UPLOAD_API), new BasicNameValuePair("repository", repositoryName));
    HttpPost post = new HttpPost(uploadUri);
    post.setEntity(buildUploadEntity(component, tagName));

    nxrmClient.execute(post, httpClientContext, of("Upload component"));
  }

  @Override
  public Optional<Tag> getTag(final String name) throws RepositoryManagerException {
    checkArgument(isNotBlank(name), "Tag name is required");
    URI getTagUri = buildUri(serverConfig.getAddress().resolve(TAGS_API + "/" + name));
    HttpGet get = new HttpGet(getTagUri);
    return nxrmClient.execute(get, new GetTagResponseHandler(), httpClientContext, of("Get tag"));
  }

  @Override
  public Tag createTag(final String name) throws RepositoryManagerException {
    return createTag(name, null);
  }

  @Override
  public Tag createTag(
      final String name,
      final Map<String, Object> attributes) throws RepositoryManagerException
  {
    Tag tag = attributes == null ? new Tag(name) : new Tag(name, attributes);
    URI createUri = buildUri(serverConfig.getAddress().resolve(TAGS_API));
    HttpPost createPost = new HttpPost(createUri);
    createPost.setEntity(new StringEntity(tag.toJson(), ContentType.APPLICATION_JSON));
    nxrmClient.execute(createPost, httpClientContext, of("Create tag"));

    return tag;
  }

  @Override
  public List<ComponentInfo> associate(
      final String tagName,
      final Map<String, String> searchParameters) throws RepositoryManagerException
  {
    checkArgument(isNotBlank(tagName), "Tag name is required");
    checkArgument(searchParameters != null && !searchParameters.isEmpty(), "Search parameters are required");

    URI associateUri = buildUri(serverConfig.getAddress().resolve(TAGS_ASSOCIATE_API + "/" + tagName),
        getRequestParameters(searchParameters));
    HttpPost post = new HttpPost(associateUri);

    return nxrmClient.execute(post, newAssociateHandler(), httpClientContext, of("Associate tag"));
  }

  @Override
  public List<ComponentInfo> disassociate(
      final String tagName,
      final Map<String, String> searchParameters) throws RepositoryManagerException
  {
    checkArgument(isNotBlank(tagName), "Tag name is required");
    checkArgument(searchParameters != null && !searchParameters.isEmpty(), "Search parameters are required");

    URI disassociateUrl = buildUri(serverConfig.getAddress().resolve(TAGS_ASSOCIATE_API + "/" + tagName),
        getRequestParameters(searchParameters));
    HttpDelete delete = new HttpDelete(disassociateUrl);

    return nxrmClient.execute(delete, newDisassociateHandler(), httpClientContext, of("Disassociate tag"));
  }

  @Override
  public List<ComponentInfo> move(final String destination, final String tagName) throws RepositoryManagerException {
    checkArgument(isNotBlank(tagName), "Tag name is required");
    return move(destination, SearchBuilder.create().withTag(tagName).build());
  }

  @Override
  public List<ComponentInfo> move(
      final String destination,
      final Map<String, String> searchParameters) throws RepositoryManagerException
  {
    checkArgument(isNotBlank(destination), "Destination repository is required");
    checkArgument(searchParameters != null && !searchParameters.isEmpty(), "Search parameters are required");

    URI moveUri = buildUri(serverConfig.getAddress().resolve(MOVE_API + "/" + destination),
        getRequestParameters(searchParameters));
    HttpPost post = new HttpPost(moveUri);

    return nxrmClient.execute(post, newMoveHandler(), httpClientContext, of("Move components"));
  }

  @Override
  public List<ComponentInfo> delete(final String tagName) throws RepositoryManagerException {
    checkArgument(isNotBlank(tagName), "Tag name is required");
    return delete(SearchBuilder.create().withTag(tagName).build());
  }

  @Override
  public List<ComponentInfo> delete(final Map<String, String> searchParameters) throws RepositoryManagerException {
    checkArgument(searchParameters != null && !searchParameters.isEmpty(), "Search parameters are required");

    URI deleteUri = buildUri(serverConfig.getAddress().resolve(DELETE_API), getRequestParameters(searchParameters));
    HttpPost post = new HttpPost(deleteUri);

    return nxrmClient.execute(post, newDeleteHandler(), httpClientContext, of("Delete components"));
  }

  private URI buildUri(
      final URI baseUri,
      final BasicNameValuePair... requestParameters) throws RepositoryManagerException
  {
    URIBuilder builder = new URIBuilder(baseUri).addParameters(asList(requestParameters));

    try {
      return builder.build();
    }
    catch (URISyntaxException e) {
      throw new RepositoryManagerException("Invalid server URL " + builder.toString(), e);
    }
  }

  private HttpEntity buildUploadEntity(Component upload, String tagName) {
    String format = upload.getFormat();
    int assetNum = 0;
    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
    addAllAttributes(entityBuilder, upload.getAttributes(), format);

    for (Asset asset : upload.getAssets()) {
      String assetName = "asset" + assetNum++;
      String attrPrefix = isBlank(format) ? assetName : format + "." + assetName;

      entityBuilder.addBinaryBody(assetName, asset.getData(), APPLICATION_OCTET_STREAM, asset.getFilename());

      addAllAttributes(entityBuilder, asset.getAttributes(), attrPrefix);
    }

    if (isNotBlank(tagName)) {
      String tagKey = isBlank(format) ? "tag" : upload.getFormat() + ".tag";
      entityBuilder.addPart(tagKey, new StringBody(tagName, TEXT_PLAIN));
    }

    return entityBuilder.build();
  }

  private static void addAllAttributes(
      final MultipartEntityBuilder builder,
      final Map<String, String> attributes,
      final String attributePrefix)
  {
    Map<String, String> prefixedAttributes;

    if (isNotBlank(attributePrefix)) {
      String prefix = attributePrefix + ".";
      prefixedAttributes = new HashMap<>(attributes.entrySet().stream().collect(toMap(e -> {
        String key = e.getKey();
        return key.startsWith(prefix) ? key : prefix + key;
      }, Entry::getValue)));
    }
    else {
      prefixedAttributes = attributes;
    }

    for (Entry<String, String> attr : prefixedAttributes.entrySet()) {
      builder.addPart(attr.getKey(), new StringBody(attr.getValue(), TEXT_PLAIN));
    }
  }

  private static BasicNameValuePair[] getRequestParameters(final Map<String, String> searchParameters) {
    return searchParameters.entrySet().stream().map(e -> new BasicNameValuePair(e.getKey(), e.getValue()))
        .toArray(BasicNameValuePair[]::new);
  }

  // Visible for testing
  public HttpClient getHttpClient() {
    return nxrmClient.httpClient();
  }

  // Visible for testing
  public URI getBaseUri() {
    return serverConfig.getAddress();
  }

  // Visible for testing
  public HttpContext getHttpClientContext() {
    return httpClientContext.orElse(null);
  }
}
