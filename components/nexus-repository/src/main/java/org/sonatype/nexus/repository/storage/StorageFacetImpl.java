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

package org.sonatype.nexus.repository.storage;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.node.LocalNodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardAspect;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.hibernate.validator.constraints.NotEmpty;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.INITIALISED;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.FacetSupport.State.STOPPED;

/**
 * Default {@link StorageFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class StorageFacetImpl
    extends FacetSupport
    implements StorageFacet
{
  private final LocalNodeAccess localNodeAccess;

  private final BlobStoreManager blobStoreManager;

  private final Provider<DatabaseInstance> databaseInstanceProvider;

  private final BucketEntityAdapter bucketEntityAdapter;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final AssetEntityAdapter assetEntityAdapter;

  private final ClientInfoProvider clientInfoProvider;

  private final ContentValidatorSelector contentValidatorSelector;

  private final MimeRulesSourceSelector mimeRulesSourceSelector;

  private final List<Supplier<StorageTxHook>> hookSuppliers;

  @VisibleForTesting
  static final String CONFIG_KEY = "storage";

  @VisibleForTesting
  static class Config
  {
    @NotEmpty
    public String blobStoreName;

    @NotNull(groups = HostedType.ValidationGroup.class)
    public WritePolicy writePolicy;

    public boolean strictContentTypeValidation = true;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "blobStoreName='" + blobStoreName + '\'' +
          ", writePolicy=" + writePolicy +
          ", strictContentTypeValidation=" + strictContentTypeValidation +
          '}';
    }
  }

  private Config config;

  private Bucket bucket;

  private WritePolicySelector writePolicySelector;

  @Inject
  public StorageFacetImpl(final LocalNodeAccess localNodeAccess,
                          final BlobStoreManager blobStoreManager,
                          final @Named(ComponentDatabase.NAME) Provider<DatabaseInstance> databaseInstanceProvider,
                          final BucketEntityAdapter bucketEntityAdapter,
                          final ComponentEntityAdapter componentEntityAdapter,
                          final AssetEntityAdapter assetEntityAdapter,
                          final ClientInfoProvider clientInfoProvider,
                          final ContentValidatorSelector contentValidatorSelector,
                          final MimeRulesSourceSelector mimeRulesSourceSelector)
  {
    this.localNodeAccess = checkNotNull(localNodeAccess);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.databaseInstanceProvider = checkNotNull(databaseInstanceProvider);

    this.bucketEntityAdapter = checkNotNull(bucketEntityAdapter);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.clientInfoProvider = checkNotNull(clientInfoProvider);
    this.contentValidatorSelector = checkNotNull(contentValidatorSelector);
    this.mimeRulesSourceSelector = checkNotNull(mimeRulesSourceSelector);

    this.hookSuppliers = new ArrayList<>();
    this.hookSuppliers.add(new Supplier<StorageTxHook>()
    {
      @Override
      public StorageTxHook get() {
        return new EventsHook(getEventBus(), getRepository());
      }
    });
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class,
        Default.class, getRepository().getType().getValidationGroup()
    );
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);
    log.debug("Config: {}", config);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    initSchema();
    initBucket();
    writePolicySelector = WritePolicySelector.DEFAULT;
    super.doInit(configuration);
  }

  private void initSchema() {
    try (ODatabaseDocumentTx db = databaseInstanceProvider.get().connect()) {
      bucketEntityAdapter.register(db);
      componentEntityAdapter.register(db);
      assetEntityAdapter.register(db);
    }
  }

  private void initBucket() {
    // get or create the bucket for the repository and set bucketId for fast lookup later
    try (ODatabaseDocumentTx db = databaseInstanceProvider.get().acquire()) {
      String repositoryName = getRepository().getName();
      bucket = bucketEntityAdapter.getByRepositoryName(db, repositoryName);
      if (bucket == null) {
        bucketEntityAdapter.add(db, bucket = new Bucket().repositoryName(repositoryName));
        db.commit();
      }
    }
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  @Override
  protected void doDelete() throws Exception {
    // TODO: Make this a soft delete and cleanup later so it doesn't block for large repos.
    try (StorageTx tx = openStorageTx(databaseInstanceProvider.get().acquire())) {
      tx.begin();
      tx.deleteBucket(tx.getBucket());
      tx.commit();
    }
  }

  @Override
  @Guarded(by = INITIALISED)
  public void registerHookSupplier(final Supplier<StorageTxHook> hookSupplier) {
    checkNotNull(hookSupplier);
    hookSuppliers.add(hookSupplier);
  }

  @Override
  @Guarded(by = INITIALISED)
  public void registerWritePolicySelector(final WritePolicySelector writePolicySelector) {
    checkNotNull(writePolicySelector);
    this.writePolicySelector = writePolicySelector;
  }

  @Override
  @Guarded(by = STARTED)
  public Supplier<StorageTx> txSupplier() {
    return new Supplier<StorageTx>()
    {
      public StorageTx get() {
        return openStorageTx(databaseInstanceProvider.get().acquire());
      }
    };
  }

  @Override
  @Guarded(by = STOPPED)
  public void unregisterHookSupplier(final Supplier<StorageTxHook> hookSupplier) {
    checkNotNull(hookSupplier);
    hookSuppliers.remove(hookSupplier);
  }

  /**
   * Returns the "principal name" to be used with current instance of {@link StorageTx}.
   */
  @Nonnull
  private String createdBy() {
    ClientInfo clientInfo = clientInfoProvider.getCurrentThreadClientInfo();
    if (clientInfo == null || clientInfo.getUserid() == null) {
      return "system";
    }
    return clientInfo.getUserid();
  }

  @Nonnull
  private StorageTx openStorageTx(final ODatabaseDocumentTx db) {
    final List<StorageTxHook> hooks = new ArrayList<>(hookSuppliers.size());
    for (Supplier<StorageTxHook> hookSupplier : hookSuppliers) {
      hooks.add(hookSupplier.get());
    }
    BlobStore blobStore = blobStoreManager.get(config.blobStoreName);
    return StateGuardAspect.around(
        new StorageTxImpl(
            createdBy(),
            new BlobTx(localNodeAccess, blobStore),
            db,
            bucket,
            config.writePolicy == null ? WritePolicy.ALLOW : config.writePolicy,
            writePolicySelector,
            bucketEntityAdapter,
            componentEntityAdapter,
            assetEntityAdapter,
            config.strictContentTypeValidation,
            contentValidatorSelector.validator(getRepository()),
            mimeRulesSourceSelector.ruleSource(getRepository()),
            new StorageTxHooks(hooks)
        )
    );
  }

}
