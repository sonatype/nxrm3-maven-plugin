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
package org.sonatype.nexus.api.repository.v3.impl.rest;

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
