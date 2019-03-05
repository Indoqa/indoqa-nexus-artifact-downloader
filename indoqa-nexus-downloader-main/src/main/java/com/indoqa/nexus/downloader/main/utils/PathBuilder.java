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

public class PathBuilder {

    private final StringBuilder stringBuilder = new StringBuilder("/");

    public PathBuilder(String... element) {
        super();

        this.appendElement(element);
    }

    public PathBuilder appendElement(String... element) {
        for (String eachElement : element) {
            this.appendSlash();

            this.stringBuilder.append(eachElement);
        }

        return this;
    }

    public PathBuilder appendParam(String name) {
        this.appendSlash();

        this.stringBuilder.append(':');
        this.stringBuilder.append(name);

        return this;
    }

    public void appendSlash() {
        if (this.stringBuilder.charAt(this.stringBuilder.length() - 1) != '/') {
            this.stringBuilder.append('/');
        }
    }

    public String getPath() {
        return this.stringBuilder.toString();
    }
}
