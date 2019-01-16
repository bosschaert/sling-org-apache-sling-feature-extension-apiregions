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
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.builder.PostProcessHandler;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class BundleArtifactFeatureHandler extends AbstractHandler implements PostProcessHandler {
    @Override
    public void postProcess(HandlerContext context, Feature feature, Extension extension) {
        if (!API_REGIONS_NAME.equals(extension.getName()))
            return;

        try {
            writeBundleToFeatureMap(context, feature);
            writeFeatureToRegionAndPackageMap(context, feature, extension);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBundleToFeatureMap(HandlerContext context, Feature feature) throws IOException {
        File bundlesFile = getDataFile(context, "bundles.properties");
        Properties map = loadProperties(bundlesFile);

        for (Artifact b : feature.getBundles()) {
            String id = b.getId().toMvnId().trim();
            String fid = feature.getId().toMvnId().trim();

            String m = map.getProperty(id);
            if (m != null) {
                List<String> l = Arrays.asList(m.split(","));
                if (!l.contains(fid))
                    m = m.trim() + "," + fid;
            } else {
                m = fid;
            }
            map.put(id, m);
        }

        storeProperties(map, bundlesFile);
    }

    private void writeFeatureToRegionAndPackageMap(HandlerContext context, Feature feature, Extension extension) throws IOException {
        JsonReader jr = Json.createReader(new StringReader(extension.getJSON()));
        JsonArray ja = jr.readArray();

        File featuresFile = getDataFile(context, "features.properties");
        File regionsFile = getDataFile(context, "regions.properties");
        Properties frMap = loadProperties(featuresFile);
        Properties rpMap = loadProperties(regionsFile);

        for (JsonValue jv : ja) {
            if (jv instanceof JsonObject) {
                JsonObject jo = (JsonObject) jv;
                String fid = feature.getId().toMvnId();

                Set<String> regionSet = new HashSet<>();
                String regions = frMap.getProperty(fid);
                if (regions != null) {
                    regionSet.addAll(Arrays.asList(regions.split(",")));
                }
                String region = jo.getString(NAME_KEY);
                regionSet.add(region);

                frMap.put(fid, regionSet.stream().collect(Collectors.joining(",")));

                Set<String> packageSet = new HashSet<>();
                String packages = rpMap.getProperty(region);
                if (packages != null) {
                    packageSet.addAll(Arrays.asList(packages.split(",")));
                }
                if (jo.containsKey(EXPORTS_KEY)) {
                    JsonArray eja = jo.getJsonArray(EXPORTS_KEY);
                    for (int i=0; i < eja.size(); i++) {
                        packageSet.add(eja.getString(i));
                    }
                }
                rpMap.put(region, packageSet.stream().collect(Collectors.joining(",")));
            }
        }

        storeProperties(frMap, featuresFile);
        storeProperties(rpMap, regionsFile);
    }
}
