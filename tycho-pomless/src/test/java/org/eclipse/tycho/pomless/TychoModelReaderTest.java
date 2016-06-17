/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.PlexusTestCase;

public class TychoModelReaderTest extends PlexusTestCase {

    private TychoModelReader tychoModelReader;

    @Override
    protected void setUp() throws Exception {
        tychoModelReader = (TychoModelReader) lookup(ModelReader.class, "tycho");
    }

    public void testReadBundle() throws Exception {
        File buildProperties = new File(getPolyglotTestDir(), "bundle1/build.properties");
        Model model = tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("pomless.bundle", model.getArtifactId());
        assertEquals("0.1.0-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-plugin", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("bundle1/META-INF/MANIFEST.MF", model.getLocation(""));
    }

    public void testReadTestBundle() throws Exception {
        File buildProperties = new File(getPolyglotTestDir(), "bundle1.tests/build.properties");
        Model model = tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
        assertEquals("pomless.bundle.tests", model.getArtifactId());
        assertEquals("1.0.1", model.getVersion());
        assertEquals("eclipse-test-plugin", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("bundle1.tests/META-INF/MANIFEST.MF", model.getLocation(""));
    }

    public void testReadFeature() throws Exception {
        File buildProperties = new File(getPolyglotTestDir(), "feature/build.properties");
        Model model = tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
        assertEquals("pomless.feature", model.getArtifactId());
        assertEquals("1.0.0-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-feature", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("feature/feature.xml", model.getLocation(""));
    }

    public void testMissingManifestOrFeature() throws Exception {
        File buildProperties = new File(getTestResourcesDir(), "modelreader/missingManifestOrFeature/build.properties");
        try {
            tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
            fail();
        } catch (IOException e) {
            // expected
        }
    }

    public void testIllFormedFeature() throws Exception {
        File buildProperties = new File(getTestResourcesDir(), "modelreader/features/illFormed/build.properties");
        try {
            tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
            fail();
        } catch (ModelParseException e) {
            // expected
        }
    }

    public void testFeatureWithoutId() throws Exception {
        File buildProperties = new File(getTestResourcesDir(), "modelreader/features/missingId/build.properties");
        try {
            tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
            fail();
        } catch (ModelParseException e) {
            assertThat(e.getMessage(), containsString("missing feature id in"));
        }
    }

    public void testFeatureWithoutVersion() throws Exception {
        File buildProperties = new File(getTestResourcesDir(), "modelreader/features/missingVersion/build.properties");
        try {
            tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
            fail();
        } catch (ModelParseException e) {
            assertThat(e.getMessage(), containsString("missing feature version in"));
        }
    }

    public void testBundleWithoutSymbolicName() throws Exception {
        File buildProperties = new File(getTestResourcesDir(), "modelreader/plugins/missingBsn/build.properties");
        try {
            tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
            fail();
        } catch (ModelParseException e) {
            assertThat(e.getMessage(), containsString("Bundle-SymbolicName missing in"));
        }
    }

    public void testBundleWithoutVersion() throws Exception {
        File buildProperties = new File(getTestResourcesDir(), "modelreader/plugins/missingVersion/build.properties");
        try {
            tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
            fail();
        } catch (ModelParseException e) {
            assertThat(e.getMessage(), containsString("Bundle-Version missing in"));
        }
    }

    public void testNoParent() throws Exception {
        File buildProperties = new File(getTestResourcesDir(), "modelreader/noParent/bundle/build.properties");
        try {
            tychoModelReader.read((Reader) null, createReaderOptions(buildProperties));
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString("No parent pom file found in"));
        }
    }

    public void testFindParent() throws Exception {
        Parent parentReference = tychoModelReader.findParent(new File(getTestResourcesDir(),
                "modelreader/grandparentInheritance/bundle/"));
        assertEquals("bundle-parent", parentReference.getArtifactId());
        assertEquals("grandparent.groupid", parentReference.getGroupId());
        assertEquals("1.2.3", parentReference.getVersion());
    }

    private void assertParent(Parent parent) {
        assertNotNull(parent);
        assertEquals("testParent.groupId", parent.getGroupId());
        assertEquals("testparent", parent.getArtifactId());
        assertEquals("0.0.1-SNAPSHOT", parent.getVersion());
    }

    private void assertLocation(String expectedLocation, InputLocation location) {
        assertNotNull(location);
        assertEquals(0, location.getLineNumber());
        assertEquals(0, location.getColumnNumber());
        InputSource source = location.getSource();
        assertNotNull(source);
        assertEquals(new File(getPolyglotTestDir(), expectedLocation).toString(), source.getLocation());
        assertNotNull(source.getModelId());
        assertTrue(source.getModelId().matches("^testParent.groupId:.*:.*"));
    }

    private Map<String, String> createReaderOptions(File buildProperties) {
        Map<String, String> options = new HashMap<>();
        options.put(ModelProcessor.SOURCE, buildProperties.getAbsolutePath());
        return options;
    }

    private File getPolyglotTestDir() {
        return new File(getTestResourcesDir(), "testpomless/");
    }

    private File getTestResourcesDir() {
        return new File(getBasedir(), "src/test/resources/");
    }

}
