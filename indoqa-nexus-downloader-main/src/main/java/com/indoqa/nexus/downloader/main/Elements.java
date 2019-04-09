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
package com.indoqa.nexus.downloader.main;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Elements implements Iterable<String> {

    private int start;
    private int totalCount;
    private List<String> values;

    public static Elements create(List<String> values, int start, int count) {
        Elements result = new Elements();
        result.setStart(start);
        result.setTotalCount(values.size());

        if (start < values.size()) {
            result.setValues(values.subList(start, Math.min(values.size(), start + count)));
        } else {
            result.setValues(Collections.emptyList());
        }

        return result;
    }

    public int getStart() {
        return this.start;
    }

    public int getTotalCount() {
        return this.totalCount;
    }

    public List<String> getValues() {
        return this.values;
    }

    @Override
    public Iterator<String> iterator() {
        return this.values.iterator();
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
