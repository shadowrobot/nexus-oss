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
package org.sonatype.nexus.testsuite.maven.perf;

import javax.inject.Inject;

import org.sonatype.nexus.log.LogManager;
import org.sonatype.nexus.log.LoggerLevel;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testsuite.maven.Maven2Client;
import org.sonatype.nexus.testsuite.maven.MavenITSupport;
import org.sonatype.tests.http.server.fluent.Server;

import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * Metadata concurrent requests IT.
 */
@ExamReactorStrategy(PerClass.class)
public class MavenConcurrentRequestIT
    extends MavenITSupport
{
  @org.ops4j.pax.exam.Configuration
  public static Option[] configureNexus() {
    return options(nexusDistribution("org.sonatype.nexus.assemblies", "nexus-base-template"),
        // set start-level to just after the nexus edition has been installed so we can re-use its dependencies
        mavenBundle("org.sonatype.http-testing-harness", "server-provider").versionAsInProject().startLevel(101)
    );
  }

  private static final String RELEASE_XML_ARTIFACT_PATH = "group/artifact/1.0/artifact-1.0.xml";

  private static final String RELEASE_ZIP_ARTIFACT_PATH = "group/artifact/1.0/artifact-1.0.zip";

  private static final String RELEASE_TXT_ARTIFACT_PATH = "group/artifact/1.0/artifact-1.0.txt";

  private GeneratorBehaviour xmlArtifactGenerator;

  private GeneratorBehaviour zipArtifactGenerator;

  private GeneratorBehaviour txtArtifactGenerator;

  @Inject
  private RepositoryManager repositoryManager;

  @Inject
  private LogManager logManager;

  private Repository mavenCentral;

  private Server upstream;

  private Maven2Client centralClient;

  @Before
  public void setupMavenDebugStorage() {
    logManager.setLoggerLevel("org.sonatype.nexus.repository.storage", LoggerLevel.DEBUG);
  }

  @Before
  public void prepare() throws Exception {
    xmlArtifactGenerator = new GeneratorBehaviour(new XmlGenerator());
    zipArtifactGenerator = new GeneratorBehaviour(new ZipGenerator());
    txtArtifactGenerator = new GeneratorBehaviour(new TextGenerator());

    upstream = Server.withPort(0)
        .serve("/" + RELEASE_XML_ARTIFACT_PATH).withBehaviours(
            xmlArtifactGenerator
        )
        .serve("/" + RELEASE_ZIP_ARTIFACT_PATH).withBehaviours(
            zipArtifactGenerator
        )
        .serve("/" + RELEASE_TXT_ARTIFACT_PATH).withBehaviours(
            txtArtifactGenerator
        )
        .start();

    Repository repo = repositoryManager.get("maven-central");
    assertThat(repo, notNullValue());
    Configuration mavenCentralConfiguration = repo.getConfiguration();
    mavenCentralConfiguration.attributes("proxy").set("remoteUrl", "http://localhost:" + upstream.getPort() + "/");
    mavenCentral = repositoryManager.update(mavenCentralConfiguration);
    centralClient = new Maven2Client(HttpClients.custom().build(), HttpClientContext.create(),
        resolveUrl(nexusUrl, "/repository/" + mavenCentral.getName() + "/").toURI());
  }

  @After
  public void cleanup()
      throws Exception
  {
    if (upstream != null) {
      upstream.stop();
    }
  }

  @Test
  public void sanity() throws Exception {
    HttpResponse response;

    response = centralClient.get(RELEASE_XML_ARTIFACT_PATH);
    EntityUtils.consume(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

    response = centralClient.get(RELEASE_ZIP_ARTIFACT_PATH);
    EntityUtils.consume(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

    response = centralClient.get(RELEASE_TXT_ARTIFACT_PATH);
    EntityUtils.consume(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
  }
}
