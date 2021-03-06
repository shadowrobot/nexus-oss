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
package org.sonatype.nexus.repository.search

import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryImpl
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.sisu.goodies.eventbus.EventBus
import org.sonatype.sisu.litmus.testsupport.TestSupport

import org.elasticsearch.action.ListenableActionFuture
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.client.AdminClient
import org.elasticsearch.client.Client
import org.elasticsearch.client.IndicesAdminClient
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.powermock.api.mockito.PowerMockito.when

class SearchServiceImplTest
    extends TestSupport
{

  @Mock
  Provider<Client> clientProvider

  @Mock
  Client client

  @Mock
  AdminClient adminClient

  @Mock
  IndicesAdminClient indicesAdminClient

  @Mock
  IndicesExistsRequestBuilder indicesExistsRequestBuilder

  @Mock
  ListenableActionFuture<IndicesExistsResponse> actionFuture

  @Mock
  IndicesExistsResponse indicesExistsResponse

  @Mock
  RepositoryManager repositoryManager

  @Mock
  SecurityHelper securityHelper

  @Mock
  List<IndexSettingsContributor> indexSettingsContributors

  @Mock
  EventBus eventBus

  @InjectMocks
  SearchServiceImpl searchService

  @Before
  public void setup() {
    when(clientProvider.get()).thenReturn(client);
    when(client.admin()).thenReturn(adminClient)
    when(adminClient.indices()).thenReturn(indicesAdminClient)
  }

  @Test
  public void testCreateIndexAlreadyExists() throws Exception {
    ArgumentCaptor<String> varArgs = captureRepoNameArg()

    Repository repository = new RepositoryImpl(eventBus, new HostedType(), new TestFormat('test'))
    repository.name = 'test'
    searchService.createIndex(repository)

    assertThat(varArgs.getAllValues(), contains('test'))
  }

  /**
   * Search indices are stored in all lowercase so ensure that we normalize our repo names to lowercase when checking
   * to see if the index already exists.
   */
  @Test
  public void testCreateIndexForCaseInsensitivity() throws Exception {
    ArgumentCaptor<String> varArgs = captureRepoNameArg()

    Repository repository = new RepositoryImpl(eventBus, new HostedType(), new TestFormat('test'))
    repository.name = 'UPPERCASE'
    searchService.createIndex(repository)

    assertThat(varArgs.getAllValues(), contains('uppercase'))
  }

  private ArgumentCaptor<String> captureRepoNameArg() {
    ArgumentCaptor<String> varArgs = ArgumentCaptor.forClass(String.class);
    when(indicesAdminClient.prepareExists(varArgs.capture())).thenReturn(indicesExistsRequestBuilder)
    when(indicesExistsRequestBuilder.execute()).thenReturn(actionFuture)
    when(actionFuture.actionGet()).thenReturn(indicesExistsResponse)
    when(indicesExistsResponse.isExists()).thenReturn(true)
    varArgs
  }

  class TestFormat
      extends Format
  {

    TestFormat(final String value) {
      super(value)
    }
  }
}
