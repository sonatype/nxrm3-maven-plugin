/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3.impl.rest;

import org.sonatype.nexus.api.repository.v3.impl.rest.ComponentInfoResponseHandler;

/**
 * Factory class to construct NXRM3 REST response handlers
 */
public class StagingResponseHandlerFactory
{
  static final String ASSOCIATE_KEY = "components associated";

  static final String DISASSOCIATE_KEY = "components disassociated";

  static final String MOVE_KEY = "components moved";

  static final String DELETE_KEY = "components deleted";

  private StagingResponseHandlerFactory() {
  }

  public static ComponentInfoResponseHandler newAssociateHandler() {
    return new ComponentInfoResponseHandler(ASSOCIATE_KEY);
  }

  public static ComponentInfoResponseHandler newDisassociateHandler() {
    return new ComponentInfoResponseHandler(DISASSOCIATE_KEY);
  }

  public static ComponentInfoResponseHandler newMoveHandler() {
    return new ComponentInfoResponseHandler(MOVE_KEY);
  }

  public static ComponentInfoResponseHandler newDeleteHandler() {
    return new ComponentInfoResponseHandler(DELETE_KEY);
  }
}
