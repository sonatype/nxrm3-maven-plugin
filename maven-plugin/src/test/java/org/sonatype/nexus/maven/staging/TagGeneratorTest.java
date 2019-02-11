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
package org.sonatype.nexus.maven.staging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TagGeneratorTest
{
  private static final String HYPHEN_DELIMITER = "-";

  private static final String ARTIFACT_ID = "theArtifactId";

  private static final String PROJECT_VERSION = "theProjectVersion";

  private static final long CURRENT_TIME = 8239183288L;

  @Mock
  private CurrentTimeSource currentTimeSource;

  private TagGenerator tagGenerator;

  @Before
  public void setup() {
    tagGenerator = new TagGenerator(currentTimeSource);
  }

  @Test
  public void testGenerate() {
    when(currentTimeSource.get()).thenReturn(CURRENT_TIME);

    String expected = String.join(HYPHEN_DELIMITER, ARTIFACT_ID, PROJECT_VERSION, Long.toString(CURRENT_TIME));

    assertThat(tagGenerator.generate(ARTIFACT_ID, PROJECT_VERSION), equalTo(expected));
  }
}
