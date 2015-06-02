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
package org.sonatype.nexus.timeline.feeds.subscribers;

import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.Asynchronous;
import org.sonatype.nexus.common.event.EventSubscriber;
import org.sonatype.nexus.repository.storage.ComponentCreatedEvent;
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent;
import org.sonatype.nexus.repository.storage.ComponentEvent;
import org.sonatype.nexus.repository.storage.ComponentUpdatedEvent;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.timeline.feeds.FeedEvent;
import org.sonatype.nexus.timeline.feeds.FeedRecorder;

import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Subscriber listening for component events.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ComponentSubscriber
    extends AbstractFeedEventSubscriber
    implements EventSubscriber, Asynchronous
{
  @Inject
  public ComponentSubscriber(final FeedRecorder feedRecorder) {
    super(feedRecorder);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onEvent(ComponentEvent e) {
    String action;
    if (e instanceof ComponentCreatedEvent) {
      if (ProxyType.NAME.equals(e.getRepository().getType().getValue())) {
        action = FeedRecorder.COMPONENT_CACHED;
      }
      else {
        action = FeedRecorder.COMPONENT_DEPLOYED;
      }
    }
    else if (e instanceof ComponentUpdatedEvent) {
      if (ProxyType.NAME.equals(e.getRepository().getType().getValue())) {
        action = FeedRecorder.COMPONENT_CACHED_UPDATE;
      }
      else {
        action = FeedRecorder.COMPONENT_DEPLOYED_UPDATE;
      }
    }
    else if (e instanceof ComponentDeletedEvent) {
      action = FeedRecorder.COMPONENT_DELETED;
    }
    else {
      return;
    }

    final ClientInfo clientInfo = e.getClientInfo();

    final Map<String, String> data = Maps.newHashMap();
    // map is for display/templating purposes
    putIfNotNull(data, "repoName", e.getRepository().getName());
    putIfNotNull(data, "componentGroup", e.getComponent().group());
    putIfNotNull(data, "componentName", e.getComponent().name());
    putIfNotNull(data, "componentVersion", e.getComponent().version());
    putIfNotNull(data, "userId", getUserId(clientInfo, "n/a"));
    putIfNotNull(data, "userIp", clientInfo == null ? "n/a" : clientInfo.getRemoteIP());
    putIfNotNull(data, "userUa", clientInfo == null ? "n/a" : clientInfo.getUserAgent());
    // feed event is persisted, is searchable/filterable by these properties
    final FeedEvent fe = new FeedEvent(
        FeedRecorder.FAMILY_COMPONENT,
        action,
        new Date(),
        getUserId(clientInfo, null),
        "/repository/" + e.getRepository().getName() + e.getComponent().name(), // TODO: where to point?
        data
    );
    getFeedRecorder().addEvent(fe);
  }

  @Nullable
  private String getUserId(@Nullable final ClientInfo clientInfo, @Nullable final String defaultValue) {
    if (clientInfo == null) {
      return defaultValue;
    }
    if (clientInfo.getUserid() == null) {
      return defaultValue;
    }
    return clientInfo.getUserid();
  }
}