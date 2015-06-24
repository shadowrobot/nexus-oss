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
 * Asset info panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.component.AssetAttributes', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-coreui-component-assetattributes',
  requires: [
    'NX.I18n'
  ],

  /**
   * model to display
   */
  assetModel: null,

  title: 'Attributes',
  ui: 'nx-subsection',
  frame: true,
  bodyBorder: false,
  itemId: 'attributeGrid',
  store: Ext.create('Ext.data.Store', {
    fields: ['facet', 'label', 'value'],
    groupField: 'facet'
  }),
  columns: {
    items: [
      {
        text: 'Facet',
        hidden: true,
        dataIndex: 'facet'
      },
      {
        text: 'label',
        flex: 1,
        dataIndex: 'label'
      },
      {
        text: 'Value',
        flex: 2,
        dataIndex: 'value',
        renderer: function (val) {
          return '<div style="word-wrap: break-word ;white-space:normal !important;">' + val + '</div>';
        }
      }
    ]
  },
  hideHeaders: true,
  overflowY: 'auto',
  scroll: false,
  features: [
    {
      ftype: 'grouping',
      groupHeaderTpl: '{name:capitalize}'
    }
  ],

  /**
   * @public
   * @param {object} assetModel the asset to display
   * @param {String} format the format for the asset
   */
  setAssetModel: function(assetModel, format) {
    var me = this,
      store = me.getStore();
    me.assetModel = assetModel;

    // update the grid attribute data
    store.data.clear();
    store.removeAll();
    Ext.iterate(me.assetModel.get('attributes'), function(facet, facetValues) {
      Ext.iterate(facetValues, function(key, value) {
        store.add({facet: facet, label: key, value: value});
      })
    });
  }
});