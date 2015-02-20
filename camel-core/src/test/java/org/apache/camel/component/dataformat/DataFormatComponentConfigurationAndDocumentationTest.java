/**
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
package org.apache.camel.component.dataformat;

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ComponentConfiguration;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.EndpointConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.JsonSchemaHelper;
import org.junit.Test;

public class DataFormatComponentConfigurationAndDocumentationTest extends ContextTestSupport {

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Test
    public void testComponentConfiguration() throws Exception {
        DataFormatComponent comp = context.getComponent("dataformat", DataFormatComponent.class);
        EndpointConfiguration conf = comp.createConfiguration("dataformaat:marshal:string?charset=iso-8859-1");

        assertEquals("iso-8859-1", conf.getParameter("charset"));

        ComponentConfiguration compConf = comp.createComponentConfiguration();
        String json = compConf.createParameterJsonSchema();
        assertNotNull(json);

        assertTrue(json.contains("\"name\": { \"kind\": \"path\", \"required\": \"true\", \"type\": \"string\", \"javaType\": \"java.lang.String\","
                        + " \"deprecated\": \"false\", \"description\": \"Name of data format\" }"));
        assertTrue(json.contains("\"operation\": { \"kind\": \"path\", \"required\": \"true\", \"type\": \"string\""));
        assertTrue(json.contains("\"synchronous\": { \"kind\": \"parameter\", \"type\": \"boolean\""));
    }

    @Test
    public void testComponentDocumentation() throws Exception {
        CamelContext context = new DefaultCamelContext();
        String html = context.getComponentDocumentation("dataformat");
        assertNotNull("Should have found some auto-generated HTML", html);
    }

    @Test
    public void testFlatpackDefaultValue() throws Exception {
        CamelContext context = new DefaultCamelContext();
        String json = context.getEipParameterJsonSchema("flatpack");
        assertNotNull(json);

        assertTrue(json.contains("\"name\": \"flatpack"));

        // the default value is a bit tricky as its ", which is written escaped as \"
        assertTrue(json.contains("\"textQualifier\": { \"kind\": \"attribute\", \"required\": \"false\", \"type\": \"string\""
                + ", \"javaType\": \"java.lang.String\", \"deprecated\": \"false\", \"defaultValue\": \"\\\"\""));

        List<Map<String, String>> rows = JsonSchemaHelper.parseJsonSchema("properties", json, true);
        assertEquals(9, rows.size());

        Map<String, String> found = null;
        for (Map<String, String> row : rows) {
            if ("textQualifier".equals(row.get("name"))) {
                found = row;
                break;
            }
        }
        assertNotNull(found);
        assertEquals("textQualifier", found.get("name"));
        assertEquals("attribute", found.get("kind"));
        assertEquals("false", found.get("required"));
        assertEquals("string", found.get("type"));
        assertEquals("java.lang.String", found.get("javaType"));
        assertEquals("false", found.get("deprecated"));
        assertEquals("\"", found.get("defaultValue"));
        assertEquals("If the text is qualified with a char such as \"", found.get("description"));
    }

    @Test
    public void testUniVocityTsvEscapeChar() throws Exception {
        CamelContext context = new DefaultCamelContext();
        String json = context.getEipParameterJsonSchema("univocity-tsv");
        assertNotNull(json);

        assertTrue(json.contains("\"name\": \"univocity-tsv"));

        // the default value is a bit tricky as its \, which is written escaped as \\
        assertTrue(json.contains("\"escapeChar\": { \"kind\": \"attribute\", \"required\": \"false\", \"type\": \"string\", \"javaType\": \"java.lang.String\","
                + " \"deprecated\": \"false\", \"defaultValue\": \"\\\\\", \"description\": \"The escape character.\""));

        List<Map<String, String>> rows = JsonSchemaHelper.parseJsonSchema("properties", json, true);
        assertEquals(15, rows.size());

        Map<String, String> found = null;
        for (Map<String, String> row : rows) {
            if ("escapeChar".equals(row.get("name"))) {
                found = row;
                break;
            }
        }
        assertNotNull(found);
        assertEquals("escapeChar", found.get("name"));
        assertEquals("attribute", found.get("kind"));
        assertEquals("false", found.get("required"));
        assertEquals("string", found.get("type"));
        assertEquals("java.lang.String", found.get("javaType"));
        assertEquals("false", found.get("deprecated"));
        assertEquals("\\", found.get("defaultValue"));
        assertEquals("The escape character.", found.get("description"));
    }

}
