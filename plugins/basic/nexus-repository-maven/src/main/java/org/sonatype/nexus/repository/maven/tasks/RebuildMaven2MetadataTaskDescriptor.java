/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.maven.tasks;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2Format;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * Task descriptor for {@link RebuildMaven2MetadataTask}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RebuildMaven2MetadataTaskDescriptor
    extends TaskDescriptorSupport<RebuildMaven2MetadataTask>
{
  public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";

  public static final String GROUPID_FIELD_ID = "groupId";

  public static final String ARTIFACTID_FIELD_ID = "artifactId";

  public static final String BASEVERSION_FIELD_ID = "baseVersion";

  public RebuildMaven2MetadataTaskDescriptor() {
    super(RebuildMaven2MetadataTask.class,
        "Rebuild Maven Repository Metadata",
        new RepositoryCombobox(
            REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the hosted Maven repository to rebuild metadata",
            true
        ).includingAnyOfFormats(Maven2Format.NAME).includingAnyOfTypes(HostedType.NAME),
        new StringTextFormField(
            GROUPID_FIELD_ID,
            "GroupId",
            "Maven groupId to narrow operation (limit to given groupId only)",
            false
        ),
        new StringTextFormField(
            ARTIFACTID_FIELD_ID,
            "ArtifactId (only if GroupId given)",
            "Maven artifactId to narrow operation (limit to given groupId:artifactId, only used if groupId set)",
            false
        ),
        new StringTextFormField(
            BASEVERSION_FIELD_ID,
            "Base Version (only if ArtifactId given)",
            "Maven base version to narrow operation (limit to given groupId:artifactId:baseVersion, used if groupId and artifactId set!)",
            false
        )
    );
  }
}
