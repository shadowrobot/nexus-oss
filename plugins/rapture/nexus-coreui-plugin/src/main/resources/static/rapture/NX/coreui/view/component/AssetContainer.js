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
 * Asset tabs container.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.component.AssetContainer', {
  extend: 'Ext.Panel',
  alias: 'widget.nx-coreui-component-assetcontainer',
  requires: [
    'NX.Icons'
  ],

  autoScroll: true,

  layout: {
    type: 'vbox',
    align: 'stretch',
    pack: 'start'
  },

  /**
   * Currently shown component model.
   */
  componentModel: undefined,

  /**
   * Currently shown asset model.
   */
  assetModel: undefined,

  /**
   * @public
   * Shows an asset in container.
   * @param {NX.coreui.model.Component} componentModel parent component of asset fo be shown
   * @param {NX.coreui.model.Asset} assetModel asset to be shown
   */
  refreshInfo: function(componentModel, assetModel) {
    var me = this,
        iconName = 'asset-type-default',
        contentType;

    me.componentModel = componentModel;
    me.assetModel = assetModel;

    if (me.componentModel && me.assetModel) {
      if (me.hidden) {
        me.show();
      }
      contentType = me.assetModel.get('contentType').replace('/', '-');
      if (NX.getApplication().getIconController().findIcon('asset-type-' + contentType, 'x16')) {
        iconName = 'asset-type-' + contentType;
      }
      me.setIconCls(NX.Icons.cls(iconName, 'x16'));
      me.setTitle(me.assetModel.get('name'));

      me.fireEvent('updated', me, me.componentModel, me.assetModel);
    }
    else {
      me.hide();
    }
  }

});
