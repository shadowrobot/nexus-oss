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
package org.sonatype.nexus.coreui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.MissingFacetException
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.security.RepositoryViewPermission
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.security.SecurityHelper

import com.google.common.collect.ImmutableList
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod

import static org.sonatype.nexus.repository.security.BreadActions.READ

/**
 * Component {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Component')
class ComponentComponent
    extends DirectComponentSupport
{
  private static String UNATTACHED = "unattached";

  private static final Closure ASSET_CONVERTER = { Asset asset, String componentName, String repositoryName ->
    new AssetXO(
        id: asset.entityMetadata.id,
        name: asset.name() ?: componentName,
        contentType: asset.contentType() ?: 'unknown',
        size: asset.size() ?: 0,
        repositoryName: repositoryName,
        lastUpdated: asset.lastUpdated().millis,
        blobRef: asset.blobRef() ? asset.blobRef.toString() : '',
        attributes: asset.attributes().backing()
    )
  }

  @Inject
  SecurityHelper securityHelper

  @Inject
  RepositoryManager repositoryManager

  @DirectMethod
  PagedResponse<ComponentXO> read(final StoreLoadParameters parameters) {
    Repository repository = repositoryManager.get(parameters.getFilter('repositoryName'))
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, READ))

    if (!repository.configuration.online) {
      return null
    }

    def sort = parameters.sort?.get(0)
    def querySuffix = ''
    if (sort) {
      querySuffix += " ORDER BY ${sort.property} ${sort.direction}"
      if (sort.property == StorageFacet.P_GROUP) {
        querySuffix += ", ${StorageFacet.P_NAME} ASC,${StorageFacet.P_VERSION} ASC"
      }
      else if (sort.property == StorageFacet.P_NAME) {
        querySuffix += ", ${StorageFacet.P_VERSION} ASC,${StorageFacet.P_GROUP} ASC"
      }
    }
    if (parameters.start) {
      querySuffix += " SKIP ${parameters.start}"
    }
    if (parameters.limit) {
      querySuffix += " LIMIT ${parameters.limit}"
    }

    StorageTx storageTx = repository.facet(StorageFacet).openTx()
    try {
      def repositories
      try {
        repositories = repository.facet(GroupFacet).leafMembers()
        querySuffix = " GROUP BY ${StorageFacet.P_GROUP},${StorageFacet.P_NAME},${StorageFacet.P_VERSION}" + querySuffix
      }
      catch (MissingFacetException e) {
        repositories = ImmutableList.of(repository)
      }

      def whereClause = null
      def queryParams = null
      def filter = parameters.getFilter('filter')
      if (filter) {
        whereClause = "${StorageFacet.P_NAME} LIKE :nameFilter OR ${StorageFacet.P_GROUP} LIKE :groupFilter OR ${StorageFacet.P_VERSION} LIKE :versionFilter"
        queryParams = [
            'nameFilter': "%${filter}%",
            'groupFilter': "%${filter}%",
            'versionFilter': "%${filter}%"
        ]
      }

      List<ComponentXO> results = storageTx.findComponents(whereClause, queryParams, repositories, querySuffix)
          .collect { component ->
        new ComponentXO(
            id: component.entityMetadata.id,
            repositoryName: repository.name,
            group: component.group(),
            name: component.name(),
            version: component.version(),
            format: component.format()
        )
      }
      if (parameters.start == 0 && !filter && readUnattachedAssets(storageTx, repositories, repository.name)) {
        results = new ArrayList<>(results)
        results.add(0, new ComponentXO(
            id: UNATTACHED,
            repositoryName: repository.name
        ))
      }
      return new PagedResponse<ComponentXO>(
          (results.size() < parameters.limit ? 0 : parameters.limit) + results.size() + parameters.start,
          results
      )
    }
    finally {
      storageTx.close()
    }
  }

  @DirectMethod
  List<AssetXO> readAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    Repository repository = repositoryManager.get(repositoryName)
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, READ))

    if (!repository.configuration.online) {
      return null
    }

    def componentId = parameters.getFilter('componentId')

    StorageTx storageTx = repository.facet(StorageFacet).openTx()
    try {
      def repositories
      try {
        repositories = repository.facet(GroupFacet).leafMembers()
      }
      catch (MissingFacetException e) {
        repositories = ImmutableList.of(repository)
      }

      if (componentId == UNATTACHED) {
        return readUnattachedAssets(storageTx, repositories, repositoryName)
      }
      else if (repositories.size() == 1) {
        Component component = storageTx.findComponent(new EntityId(componentId), storageTx.getBucket())
        if (component == null) {
          log.warn 'Component {} not found', componentId
          return null
        }

        return storageTx.browseAssets(component).collect(ASSET_CONVERTER.rcurry(component.name(), repositoryName))
      }
      else {
        def componentGroup = parameters.getFilter('componentGroup')
        def componentName = parameters.getFilter('componentName')
        def componentVersion = parameters.getFilter('componentVersion')

        if (componentName) {
          def whereClause = "${StorageFacet.P_COMPONENT}.${StorageFacet.P_NAME} = :name"
          def params = ['name': componentName]
          if (componentGroup) {
            whereClause += " AND ${StorageFacet.P_COMPONENT}.${StorageFacet.P_GROUP} = :group"
            params << ['group': componentGroup]
          }
          if (componentVersion) {
            whereClause += " AND ${StorageFacet.P_COMPONENT}.${StorageFacet.P_VERSION} = :version"
            params << ['version': componentVersion]
          }
          def groupBy = " GROUP BY ${StorageFacet.P_NAME}"
          return storageTx.findAssets(whereClause, params, repositories, groupBy).
              collect(ASSET_CONVERTER.rcurry(componentName, repositoryName))
        }
      }
    }
    finally {
      storageTx.close()
    }
  }

  private List<AssetXO> readUnattachedAssets(final StorageTx storageTx, final Iterable<Repository> repositories,
                                             String requestedRepositoryName)
  {
    return storageTx.findAssets("${StorageFacet.P_COMPONENT} IS NULL", null, repositories, null).
        collect(ASSET_CONVERTER.rcurry(null, requestedRepositoryName))
  }
}
