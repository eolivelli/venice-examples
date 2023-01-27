/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.example.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.io.common.IOConfigUtils;
import org.apache.pulsar.io.core.SinkContext;
import org.apache.pulsar.io.core.annotations.FieldDoc;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Configuration class for the ElasticSearch Sink Connector.
 */
@Data
@Accessors(chain = true)
@Slf4j
public class VeniceSinkConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @FieldDoc(
        defaultValue = "",
        help = "The url of the Venice router"
    )
    private String veniceRouterUrl = "http://venice-router:7777";

    @FieldDoc(
            defaultValue = "",
            help = "The url of the ZK service used by Venice"
    )
    private String veniceZookeeper = "zookeeper:2181";

    @FieldDoc(
        defaultValue = "",
        help = "The name of the Venice store"
    )
    private String storeName = "test-store";

    public static VeniceSinkConfig load(Map<String, Object> map, SinkContext sinkContext) throws IOException {
        log.info("Loading config {}", map);
        return IOConfigUtils.loadWithSecrets(map, VeniceSinkConfig.class, sinkContext);
    }

}
