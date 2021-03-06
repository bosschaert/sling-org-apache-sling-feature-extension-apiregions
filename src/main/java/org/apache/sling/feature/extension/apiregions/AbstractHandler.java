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

import org.apache.sling.feature.builder.HandlerContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Properties;

class AbstractHandler {
    static final String API_REGIONS_NAME = "api-regions";
    static final String GLOBAL_NAME = "global";

    static final String NAME_KEY = "name";
    static final String EXPORTS_KEY = "exports";

    private static final String FILE_PREFIX = "apiregions.";
    private static final String FILE_STORAGE_DIR_KEY = "fileStorage";

    protected File getDataFile(HandlerContext context, String name) throws IOException {
        String stg = context.getConfiguration().get(FILE_STORAGE_DIR_KEY);
        Path p;
        if (stg != null) {
            p = new File(stg, name).toPath();
        } else {
            p = Files.createTempFile(FILE_PREFIX, name);
        }
        File f = p.toFile();
        f.deleteOnExit();

        System.setProperty(FILE_PREFIX + name, f.getCanonicalPath());
        return f;
    }

    protected Properties loadProperties(File file) throws IOException, FileNotFoundException {
        Properties map = new Properties();
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                map.load(is);
            }
        }
        return map;
    }

    protected void storeProperties(Properties properties, File file) throws IOException, FileNotFoundException {
        try (OutputStream os = new FileOutputStream(file)) {
            properties.store(os, "Generated at " + new Date());
        }
    }
}
