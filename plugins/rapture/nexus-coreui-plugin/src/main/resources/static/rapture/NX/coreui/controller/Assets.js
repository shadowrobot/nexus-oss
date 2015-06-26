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
/*global Ext, NX*/

/**
 * Assets controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Assets', {
  extend: 'Ext.app.Controller',
  requires: [
    'NX.I18n',
    'Ext.util.Format'
  ],

  views: [
    'component.AssetContainer',
    'component.AssetInfo',
    'component.AssetAttributes',
    'component.AssetList',
    'component.ComponentDetails'
  ],

  refs: [
    {ref: 'assetContainer', selector: 'nx-coreui-component-assetcontainer'},
    {ref: 'assetList', selector: 'nx-coreui-component-asset-list'},
    {ref: 'assetInfo', selector: 'nx-coreui-component-assetinfo'}
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'asset-type-default': {file: 'file_extension_default.png', variants: ['x16', 'x32']},
      'asset-type-application-java-archive': {file: 'file_extension_jar.png', variants: ['x16', 'x32']},
      'asset-type-text-xml': {file: 'file_extension_xml.png', variants: ['x16', 'x32']},
      'asset-type-application-xml': {file: 'file_extension_xml.png', variants: ['x16', 'x32']}
    });

    me.listen({
      component: {
        'nx-coreui-component-assetcontainer': {
          updated: me.showAssetInfo
        },
        'nx-coreui-component-details': {
          updated: me.showComponentDetails
        },
        'nx-coreui-component-asset-list': {
          updated: me.loadAssets,
          cellclick: me.updateAssetContainer
        },
        'nx-coreui-component-assetinfo button[action=deleteAsset]': {
          afterrender: me.bindDeleteAssetButton,
          click: me.deleteAsset
        }
      }
    });
  },

  /**
   * @private
   * Shows information about selected component/asset.
   */
  showAssetInfo: function(container, componentModel, assetModel) {
    var info = container.down('nx-coreui-component-assetinfo'),
      attributes = container.down('nx-coreui-component-assetattributes'),
      panel;

    if (!info) {
      info = container.add({xtype: 'nx-coreui-component-assetinfo', weight: 10});
    }
    info.setAssetModel(assetModel, componentModel.get('format'));

    if (!attributes) {

      attributes = Ext.create('widget.nx-coreui-component-assetattributes');
      panel = Ext.create('Ext.Panel', {
        ui: 'nx-inset',
        weight: 10
      });
      panel.add(attributes);
      container.add(panel);
    }
    attributes.setAssetModel(assetModel, componentModel.get('format'));
  },

  showComponentDetails: function(container, componentModel) {
    var repositoryInfo = {},
        componentInfo = {};

    if (componentModel) {
      repositoryInfo[NX.I18n.get('Search_Assets_Repository')] = componentModel.get('repositoryName');
      repositoryInfo[NX.I18n.get('Search_Assets_Format')] = componentModel.get('format');
      componentInfo[NX.I18n.get('Search_Assets_Group')] = componentModel.get('group');
      componentInfo[NX.I18n.get('Search_Assets_Name')] = componentModel.get('name');
      componentInfo[NX.I18n.get('Search_Assets_Version')] = componentModel.get('version');

      container.down('#repositoryInfo').showInfo(repositoryInfo);
      container.down('#componentInfo').showInfo(componentInfo);
    }
  },

  /**
   * @private
   *
   * Filter asset store based on component model.
   *
   * @param {NX.coreui.view.component.AssetList} grid assets grid
   * @param {NX.coreui.model.Component} componentModel component owning the assets to be loaded
   */
  loadAssets: function(grid, componentModel) {
    var assetStore = grid.getStore(),
        filters;

    if (componentModel) {
      assetStore.clearFilter(true);
      filters = [
        {
          property: 'repositoryName',
          value: componentModel.get('repositoryName')
        },
        {
          property: 'componentId',
          value: componentModel.getId()
        },
        {
          property: 'componentName',
          value: componentModel.get('name')
        }
      ];
      if (componentModel.get('group')) {
        filters.push({
          property: 'componentGroup',
          value: componentModel.get('group')
        });
      }
      if (componentModel.get('version')) {
        filters.push({
          property: 'componentVersion',
          value: componentModel.get('version')
        });
      }
      assetStore.addFilter(filters);
    }
  },

  /**
   * @private
   *
   * Update asset shown in asset container.
   */
  updateAssetContainer: function(gridView, td, cellIndex, assetModel) {
    var me = this,
        assetContainer = me.getAssetContainer();

    assetContainer.refreshInfo(gridView.up('grid').componentModel, assetModel);
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission.
   */
  bindDeleteAssetButton: function(button) {
    var me = this,
        component = me.getAssetList().componentModel;
    button.mon(
        NX.Conditions.isPermitted('nexus:repository-view:' + component.get('format') + ':' +
            component.get('repositoryName') + ':delete')
    ),
    {
      satisfied: button.enable,
      unsatisfied: button.disable,
      scope: button
    }
  },

  /**                                
   * @private
   * Remove selected asset.
   */
  deleteAsset: function () {
    var me = this,
        info = me.getAssetInfo();

    if (info) {
      var asset = info.assetModel;
      NX.Dialogs.askConfirmation(NX.I18n.get('AssetInfo_Delete_Title'), asset.get('name'), function () {
        NX.direct.coreui_Component.deleteAsset(asset.getId(), asset.get('repositoryName'), function (response) {
          if (Ext.isObject(response) && response.success) {
            me.getAssetList().getStore().load();
            Ext.util.History.back();
            NX.Messages.add({ text: NX.I18n.format('AssetInfo_Delete_Success', asset.get('name')), type: 'success' });
          }
        });
      });
    }
  }

});
