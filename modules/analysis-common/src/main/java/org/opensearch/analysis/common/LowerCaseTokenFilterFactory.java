/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.analysis.common;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.el.GreekLowerCaseFilter;
import org.apache.lucene.analysis.ga.IrishLowerCaseFilter;
import org.apache.lucene.analysis.tr.TurkishLowerCaseFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;
import org.opensearch.index.analysis.NormalizingTokenFilterFactory;

/**
 * Factory for {@link LowerCaseFilter} and some language-specific variants
 * supported by the {@code language} parameter:
 * <ul>
 *   <li>greek: {@link GreekLowerCaseFilter}
 *   <li>irish: {@link IrishLowerCaseFilter}
 *   <li>turkish: {@link TurkishLowerCaseFilter}
 * </ul>
 */
public class LowerCaseTokenFilterFactory extends AbstractTokenFilterFactory implements NormalizingTokenFilterFactory {

    private final String lang;

    LowerCaseTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        this.lang = settings.get("language", null);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        if (lang == null) {
            return new LowerCaseFilter(tokenStream);
        } else if (lang.equalsIgnoreCase("greek")) {
            return new GreekLowerCaseFilter(tokenStream);
        } else if (lang.equalsIgnoreCase("irish")) {
            return new IrishLowerCaseFilter(tokenStream);
        } else if (lang.equalsIgnoreCase("turkish")) {
            return new TurkishLowerCaseFilter(tokenStream);
        } else {
            throw new IllegalArgumentException("language [" + lang + "] not support for lower case");
        }
    }

}


