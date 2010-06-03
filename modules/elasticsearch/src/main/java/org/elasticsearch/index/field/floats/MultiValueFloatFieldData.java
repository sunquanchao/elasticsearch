/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.field.floats;

import org.elasticsearch.index.field.FieldDataOptions;
import org.elasticsearch.util.ThreadLocals;

/**
 * @author kimchy (Shay Banon)
 */
public class MultiValueFloatFieldData extends FloatFieldData {

    private static final int VALUE_CACHE_SIZE = 100;

    private static ThreadLocal<ThreadLocals.CleanableValue<float[][]>> valuesCache = new ThreadLocal<ThreadLocals.CleanableValue<float[][]>>() {
        @Override protected ThreadLocals.CleanableValue<float[][]> initialValue() {
            float[][] value = new float[VALUE_CACHE_SIZE][];
            for (int i = 0; i < value.length; i++) {
                value[i] = new float[i];
            }
            return new ThreadLocals.CleanableValue<float[][]>(value);
        }
    };

    // order with value 0 indicates no value
    private final int[][] order;

    public MultiValueFloatFieldData(String fieldName, FieldDataOptions options, int[][] order, float[] values, int[] freqs) {
        super(fieldName, options, values, freqs);
        this.order = order;
    }

    @Override public boolean multiValued() {
        return true;
    }

    @Override public boolean hasValue(int docId) {
        return order[docId] != null;
    }

    @Override public float value(int docId) {
        int[] docOrders = order[docId];
        if (docOrders == null) {
            return 0;
        }
        return values[docOrders[0]];
    }

    @Override public float[] values(int docId) {
        int[] docOrders = order[docId];
        if (docOrders == null) {
            return EMPTY_FLOAT_ARRAY;
        }
        float[] floats;
        if (docOrders.length < VALUE_CACHE_SIZE) {
            floats = valuesCache.get().get()[docOrders.length];
        } else {
            floats = new float[docOrders.length];
        }
        for (int i = 0; i < docOrders.length; i++) {
            floats[i] = values[docOrders[i]];
        }
        return floats;
    }
}