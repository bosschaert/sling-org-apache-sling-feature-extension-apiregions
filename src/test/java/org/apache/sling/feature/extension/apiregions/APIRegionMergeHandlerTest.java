/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.extension.apiregions;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.HandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class APIRegionMergeHandlerTest {
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
    }

    @After
    public void tearDown() throws IOException {
        // Delete the temp dir again
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void testCanMerge() {
        APIRegionMergeHandler armh = new APIRegionMergeHandler();

        Extension ex = new Extension(ExtensionType.JSON, "api-regions", false);
        assertTrue(armh.canMerge(ex));
        assertFalse(armh.canMerge(new Extension(ExtensionType.JSON, "foo", false)));
    }

    @Test
    public void testAPIRegionMerging() {
        APIRegionMergeHandler armh = new APIRegionMergeHandler();

        Feature tf = new Feature(ArtifactId.fromMvnId("x:t:1"));
        Feature sf = new Feature(ArtifactId.fromMvnId("y:s:2"));

        Extension tgEx = new Extension(ExtensionType.JSON, "api-regions", false);
        tgEx.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"xyz\"],"
                + "\"some-key\":\"some-val\"}]");

        Extension srEx = new Extension(ExtensionType.JSON, "api-regions", false);
        srEx.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"test\"]},"
                + "{\"name\":\"something\","
                + "\"exports\": [\"a.ha\"],"
                + "\"my-key\": \"my-val\"}]");

        HandlerContext hc = Mockito.mock(HandlerContext.class);
        armh.merge(hc, tf, sf, tgEx, srEx);

        String expectedJSON = "[{\"name\":\"global\","
                + "\"exports\": [\"a.b.c\",\"d.e.f\", \"test\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"xyz\"],"
                + "\"some-key\":\"some-val\"},"
                + "{\"name\":\"something\","
                + "\"exports\": [\"a.ha\"],"
                + "\"my-key\": \"my-val\"}]";
        JsonReader er = Json.createReader(new StringReader(expectedJSON));
        JsonReader ar = Json.createReader(new StringReader(tgEx.getJSON()));
        JsonArray ea = er.readArray();
        JsonArray aa = ar.readArray();

        assertEquals(ea, aa);
    }


    @Test
    public void testRegionExportsNoInheritance() throws Exception {
        APIRegionMergeHandler armh = new APIRegionMergeHandler();

        Feature tf = new Feature(ArtifactId.fromMvnId("x:t:1"));
        Feature sf = new Feature(ArtifactId.fromMvnId("y:s:2"));

        Extension srEx = new Extension(ExtensionType.JSON, "api-regions", false);
        srEx.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"deprecated\","
                + "\"exports\":[\"klm\",\"#ignored\",\"qrs\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"xyz\"]},"
                + "{\"name\":\"forbidden\","
                + "\"exports\":[\"abc\",\"klm\"]}]");

        HandlerContext hc = Mockito.mock(HandlerContext.class);
        armh.merge(hc, tf, sf, null, srEx);

        Extension tgEx = tf.getExtensions().iterator().next();

        String expectedJSON = "[{\"name\":\"global\",\"exports\":[\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"deprecated\",\"exports\":[\"klm\",\"qrs\"]},"
                + "{\"name\":\"internal\",\"exports\":[\"xyz\"]},"
                + "{\"name\":\"forbidden\",\"exports\":[\"abc\",\"klm\"]}]";
        JsonReader er = Json.createReader(new StringReader(expectedJSON));
        JsonReader ar = Json.createReader(new StringReader(tgEx.getJSON()));
        JsonArray ea = er.readArray();
        JsonArray aa = ar.readArray();

        assertEquals(ea, aa);
    }

    @Test
    public void testStoreBundleOrigins() throws Exception {
        HandlerContext hc = Mockito.mock(HandlerContext.class);
        Mockito.when(hc.getConfiguration()).thenReturn(
                Collections.singletonMap(AbstractHandler.FILE_STORAGE_DIR_KEY,
                        tempDir.toString()));

        APIRegionMergeHandler armh = new APIRegionMergeHandler();

        Feature tf = new Feature(ArtifactId.fromMvnId("g:t:1"));
        Feature sf1 = new Feature(ArtifactId.fromMvnId("g:s1:1"));
        Extension sf1Ex = new Extension(ExtensionType.JSON, "api-regions", false);
        sf1Ex.setJSON("[]");

        sf1.getBundles().add(new Artifact(ArtifactId.fromMvnId("a:b1:1")));
        sf1.getBundles().add(new Artifact(ArtifactId.fromMvnId("a:b2:1")));

        armh.merge(hc, tf, sf1, null, sf1Ex);

        Feature sf2 = new Feature(ArtifactId.fromMvnId("g:s2:1"));
        Extension sf2Ex = new Extension(ExtensionType.JSON, "api-regions", false);
        sf2Ex.setJSON("[]");

        sf2.getBundles().add(new Artifact(ArtifactId.fromMvnId("a:b2:1")));
        sf2.getBundles().add(new Artifact(ArtifactId.fromMvnId("a:b3:1")));
        sf2.getBundles().add(new Artifact(ArtifactId.fromMvnId("a:b2:1")));

        armh.merge(hc, tf, sf2, tf.getExtensions().getByName("api-regions"), sf2Ex);

        Feature sf3 = new Feature(ArtifactId.fromMvnId("g:s3:1"));
        Extension sf3Ex = new Extension(ExtensionType.JSON, "api-regions", false);
        sf3Ex.setJSON("[]");

        sf3.getBundles().add(new Artifact(ArtifactId.fromMvnId("a:b2:1")));

        armh.merge(hc, tf, sf3, tf.getExtensions().getByName("api-regions"), sf3Ex);

        Properties bo = new Properties();
        bo.load(new FileInputStream(new File(tempDir.toFile(), "bundleOrigins.properties")));
        assertEquals(3, bo.size());

        assertEquals("g:s1:1", bo.get("a:b1:1"));
        assertEquals("g:s1:1,g:s2:1,g:s3:1", bo.get("a:b2:1"));
        assertEquals("g:s2:1", bo.get("a:b3:1"));
    }

    @Test
    public void testStoreRegionOrigins() throws Exception {
        HandlerContext hc = Mockito.mock(HandlerContext.class);
        Mockito.when(hc.getConfiguration()).thenReturn(
                Collections.singletonMap(AbstractHandler.FILE_STORAGE_DIR_KEY,
                        tempDir.toString()));

        APIRegionMergeHandler armh = new APIRegionMergeHandler();

        Feature tf = new Feature(ArtifactId.fromMvnId("x:t:1"));
        Feature sf1 = new Feature(ArtifactId.fromMvnId("y:s:2"));

        Extension sr1Ex = new Extension(ExtensionType.JSON, "api-regions", false);
        sr1Ex.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"deprecated\","
                + "\"exports\":[\"klm\",\"#ignored\",\"qrs\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"xyz\"]},"
                + "{\"name\":\"forbidden\","
                + "\"exports\":[\"abc\",\"klm\"]}]");

        armh.merge(hc, tf, sf1, null, sr1Ex);

        Feature sf2 = new Feature(ArtifactId.fromMvnId("z:s:1"));

        Extension sr2Ex = new Extension(ExtensionType.JSON, "api-regions", false);
        sr2Ex.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"g.h.i\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[]},"
                + "{\"name\":\"somethingelse\","
                + "\"exports\":[\"qqq\"]}]");
        armh.merge(hc, tf, sf2, tf.getExtensions().getByName("api-regions"), sr2Ex);

        Properties ro = new Properties();
        ro.load(new FileInputStream(new File(tempDir.toFile(), "regionOrigins.properties")));
        assertEquals(2, ro.size());
        assertEquals("global,deprecated,internal,forbidden", ro.get("y:s:2"));
        assertEquals("global,internal,somethingelse", ro.get("z:s:1"));
    }
}
