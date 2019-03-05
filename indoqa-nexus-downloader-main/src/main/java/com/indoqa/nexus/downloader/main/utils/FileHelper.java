/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
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
package com.indoqa.nexus.downloader.main.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.indoqa.nexus.downloader.main.Elements;
import com.indoqa.nexus.downloader.main.service.ServiceException;

public final class FileHelper {

    private FileHelper() {
        // hide utility class constructor
    }

    public static Elements getChildren(Path path, Predicate<Path> filter, int start, int count) {
        try {
            List<String> names = Files.list(path)
                .filter(filter)
                .map(Path::getFileName)
                .map(Path::toString)
                .map(FilenameUtils::getBaseName)
                .collect(Collectors.toList());

            return Elements.create(names, start, count);
        } catch (IOException e) {
            throw new ServiceException("Could not list children", e);
        }
    }
}
