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
package org.apache.sling.feature.extension.apiregions.analyser;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.Analyser;
import org.apache.sling.feature.analyser.AnalyserResult;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.extension.apiregions.scanner.ApiRegionsExtensionScanner;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.scanner.Scanner;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckApiRegionsCrossFeatureDupsTest {
    @Test
    public void testOverlapError() throws Exception {
        Path fp = new File(getClass().getResource("/crossfeatdups/fm1.json").getFile()).toPath();
        String fm = new String(Files.readAllBytes(fp));

        Feature f = FeatureJSONReader.read(new StringReader(fm), null);

        Scanner scanner = getScanner();
        AnalyserTask at = new CheckApiRegionsCrossFeatureDups();
        Analyser a = new Analyser(scanner, at);
        AnalyserResult res = a.analyse(f);
        assertEquals(1, res.getErrors().size());
        assertEquals(0, res.getWarnings().size());

        String err = res.getErrors().get(0);
        assertTrue(err.contains("a.b.c"));
        assertTrue(err.contains("g:f3:1"));
        assertTrue(err.contains("feature-export2"));
    }

    private Scanner getScanner() throws IOException {
        ArtifactProvider ap = new ArtifactProvider() {
            @Override
            public URL provide(ArtifactId id) {
                switch (id.toMvnId()) {
                case "g:exp1:1":
                    return getClass().getResource("/crossfeatdups/test-bundles/feature-export.jar");
                case "g:exp2:1":
                    return getClass().getResource("/crossfeatdups/test-bundles/feature-export2.jar");
                case "g:noexp:1":
                    return getClass().getResource("/crossfeatdups/test-bundles/no-exports.jar");
                }
                return null;
            }
        };
        Scanner scanner = new Scanner(ap, Collections.singletonList(new ApiRegionsExtensionScanner()), Collections.emptyList());
        return scanner;
    }
}
