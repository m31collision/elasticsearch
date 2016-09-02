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

package org.elasticsearch.index.rankeval;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ParseFieldRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.SearchRequestParsers;
import org.elasticsearch.search.aggregations.AggregatorParsers;
import org.elasticsearch.search.suggest.Suggesters;
import org.elasticsearch.test.ESTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;

public class QuerySpecTests extends ESTestCase {

    private static SearchModule searchModule;
    private static SearchRequestParsers searchRequestParsers;

    /**
    * setup for the whole base test class
    */
    @BeforeClass
    public static void init() throws IOException {
        AggregatorParsers aggsParsers = new AggregatorParsers(new ParseFieldRegistry<>("aggregation"),
                new ParseFieldRegistry<>("aggregation_pipes"));
        searchModule = new SearchModule(Settings.EMPTY, false, emptyList());
        IndicesQueriesRegistry queriesRegistry = searchModule.getQueryParserRegistry();
        Suggesters suggesters = searchModule.getSuggesters();
        searchRequestParsers = new SearchRequestParsers(queriesRegistry, aggsParsers, suggesters);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        searchModule = null;
        searchRequestParsers = null;
    }

    // TODO add some sort of roundtrip testing like we have now for queries?
    public void testParseFromXContent() throws IOException {
        String querySpecString = " {\n"
         + "   \"id\": \"my_qa_query\",\n"
         + "   \"request\": {\n"
         + "           \"query\": {\n"
         + "               \"bool\": {\n"
         + "                   \"must\": [\n"
         + "                       {\"match\": {\"beverage\": \"coffee\"}},\n"
         + "                       {\"term\": {\"browser\": {\"value\": \"safari\"}}},\n"
         + "                       {\"term\": {\"time_of_day\": {\"value\": \"morning\",\"boost\": 2}}},\n"
         + "                       {\"term\": {\"ip_location\": {\"value\": \"ams\",\"boost\": 10}}}]}\n"
         + "           },\n"
         + "           \"size\": 10\n"
         + "   },\n"
         + "   \"ratings\": [ "
         + "        {\"key\": {\"index\": \"test\", \"type\": \"testtype\", \"doc_id\": \"1\"}, \"rating\" : 1 }, "
         + "        {\"key\": {\"index\": \"test\", \"type\": \"testtype\", \"doc_id\": \"2\"}, \"rating\" : 0 }, "
         + "        {\"key\": {\"index\": \"test\", \"type\": \"testtype\", \"doc_id\": \"3\"}, \"rating\" : 1 }]\n"
         + "}";
        XContentParser parser = XContentFactory.xContent(querySpecString).createParser(querySpecString);
        QueryParseContext queryContext = new QueryParseContext(searchRequestParsers.queryParsers, parser, ParseFieldMatcher.STRICT);
        RankEvalContext rankContext = new RankEvalContext(ParseFieldMatcher.STRICT, queryContext,
                searchRequestParsers);
        QuerySpec specification = QuerySpec.fromXContent(parser, rankContext);
        assertEquals("my_qa_query", specification.getSpecId());
        assertNotNull(specification.getTestRequest());
        List<RatedDocument> ratedDocs = specification.getRatedDocs();
        assertEquals(3, ratedDocs.size());
        assertEquals("1", ratedDocs.get(0).getKey().getDocID());
        assertEquals(1, ratedDocs.get(0).getRating());
        assertEquals("2", ratedDocs.get(1).getKey().getDocID());
        assertEquals(0, ratedDocs.get(1).getRating());
        assertEquals("3", ratedDocs.get(2).getKey().getDocID());
        assertEquals(1, ratedDocs.get(2).getRating());
    }
}