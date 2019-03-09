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
package com.indoqa.nexus.downloader.integration.tests.utilities;

import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.CoreMatchers;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class PathMatchers {

    public static Matcher<Path> exists() {
        return new FeatureMatcher<Path, Boolean>(CoreMatchers.is(true), "exists", "exists") {

            @Override
            protected Boolean featureValueOf(Path actual) {
                return Files.exists(actual);
            }
        };
    }

    public static Matcher<Path> isFile() {
        return new FeatureMatcher<Path, Boolean>(CoreMatchers.is(true), "isFile", "isFile") {

            @Override
            protected Boolean featureValueOf(Path actual) {
                return Files.isRegularFile(actual);
            }
        };
    }

    public static Matcher<Path> isDirectory() {
        return new FeatureMatcher<Path, Boolean>(CoreMatchers.is(true), "isFile", "isFile") {

            @Override
            protected Boolean featureValueOf(Path actual) {
                return Files.isDirectory(actual);
            }
        };
    }
    public static Matcher<Path> isSymbolicLink() {
        return new FeatureMatcher<Path, Boolean>(CoreMatchers.is(true), "isFile", "isFile") {

            @Override
            protected Boolean featureValueOf(Path actual) {
                return Files.isSymbolicLink(actual);
            }
        };
    }
}
