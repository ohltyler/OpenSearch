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

package org.opensearch.indices.exists.types;

import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.plugins.Plugin;
import org.opensearch.test.OpenSearchIntegTestCase;
import org.opensearch.test.InternalSettingsPlugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.opensearch.cluster.metadata.IndexMetadata.SETTING_BLOCKS_READ;
import static org.opensearch.cluster.metadata.IndexMetadata.SETTING_BLOCKS_WRITE;
import static org.opensearch.cluster.metadata.IndexMetadata.SETTING_READ_ONLY;
import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertBlocked;
import static org.hamcrest.Matchers.equalTo;

public class TypesExistsIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(InternalSettingsPlugin.class);
    }

    public void testSimple() throws Exception {
        Client client = client();
        CreateIndexResponse response1 = client.admin().indices().prepareCreate("test1")
                .addMapping("type1", jsonBuilder().startObject().startObject("type1").endObject().endObject())
                .execute().actionGet();
        CreateIndexResponse response2 = client.admin().indices().prepareCreate("test2")
                .addMapping("type2", jsonBuilder().startObject().startObject("type2").endObject().endObject())
                .execute().actionGet();
        client.admin().indices().prepareAliases().addAlias("test1", "alias1").execute().actionGet();
        assertAcked(response1);
        assertAcked(response2);

        TypesExistsResponse response = client.admin().indices().prepareTypesExists("test1").setTypes("type1").execute().actionGet();
        assertThat(response.isExists(), equalTo(true));
        response = client.admin().indices().prepareTypesExists("test1").setTypes("type2").execute().actionGet();
        assertThat(response.isExists(), equalTo(false));
        try {
            client.admin().indices().prepareTypesExists("notExist").setTypes("type1").execute().actionGet();
            fail("Exception should have been thrown");
        } catch (IndexNotFoundException e) {}
        try {
            client.admin().indices().prepareTypesExists("notExist").setTypes("type0").execute().actionGet();
            fail("Exception should have been thrown");
        } catch (IndexNotFoundException e) {}
        response = client.admin().indices().prepareTypesExists("alias1").setTypes("type1").execute().actionGet();
        assertThat(response.isExists(), equalTo(true));
        response = client.admin().indices().prepareTypesExists("*").setTypes("type1").execute().actionGet();
        assertThat(response.isExists(), equalTo(false));
        response = client.admin().indices().prepareTypesExists("test1", "test2").setTypes("type1").execute().actionGet();
        assertThat(response.isExists(), equalTo(false));
        response = client.admin().indices().prepareTypesExists("test1", "test2").setTypes("type2").execute().actionGet();
        assertThat(response.isExists(), equalTo(false));
    }

    public void testTypesExistsWithBlocks() throws IOException {
        assertAcked(prepareCreate("ro").addMapping("type1", jsonBuilder().startObject().startObject("type1").endObject().endObject()));
        ensureGreen("ro");

        // Request is not blocked
        for (String block : Arrays.asList(SETTING_BLOCKS_READ, SETTING_BLOCKS_WRITE, SETTING_READ_ONLY)) {
            try {
                enableIndexBlock("ro", block);
                assertThat(client().admin().indices().prepareTypesExists("ro").setTypes("type1").execute().actionGet().isExists(),
                    equalTo(true));
            } finally {
                disableIndexBlock("ro", block);
            }
        }

        // Request is blocked
        try {
            enableIndexBlock("ro", IndexMetadata.SETTING_BLOCKS_METADATA);
            assertBlocked(client().admin().indices().prepareTypesExists("ro").setTypes("type1"));
        } finally {
            disableIndexBlock("ro", IndexMetadata.SETTING_BLOCKS_METADATA);
        }
    }
}
