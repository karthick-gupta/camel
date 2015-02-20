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
package org.apache.camel.component.log;

import org.apache.camel.Component;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.ProcessorEndpoint;
import org.apache.camel.processor.CamelLogProcessor;
import org.apache.camel.processor.ThroughputLogger;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.util.CamelLogger;
import org.apache.camel.util.ServiceHelper;
import org.slf4j.Logger;

/**
 * Logger endpoint.
 */
@UriEndpoint(scheme = "log", producerOnly = true, label = "core,monitoring")
public class LogEndpoint extends ProcessorEndpoint {

    private volatile Processor logger;
    private Logger providedLogger;
    private ExchangeFormatter localFormatter;
    @UriPath(description = "Name of the logging category to use") @Metadata(required = "true")
    private String loggerName;
    @UriParam(defaultValue = "INFO", enums = "ERROR,WARN,INFO,DEBUG,TRACE,OFF")
    private String level;
    @UriParam
    private String marker;
    @UriParam
    private Integer groupSize;
    @UriParam
    private Long groupInterval;
    @UriParam(defaultValue = "true")
    private Boolean groupActiveOnly;
    @UriParam
    private Long groupDelay;

    public LogEndpoint() {
    }

    public LogEndpoint(String endpointUri, Component component) {
        super(endpointUri, component);
    }

    public LogEndpoint(String endpointUri, Component component, Processor logger) {
        super(endpointUri, component);
        setLogger(logger);
    }

    @Override
    protected void doStart() throws Exception {
        if (logger == null) {
            // setup a new logger here
            CamelLogger camelLogger;
            LoggingLevel loggingLevel = LoggingLevel.INFO;
            if (level != null) {
                loggingLevel = LoggingLevel.valueOf(level);
            }
            if (providedLogger == null) {
                camelLogger = new CamelLogger(loggerName, loggingLevel, getMarker());
            } else {
                camelLogger = new CamelLogger(providedLogger, loggingLevel, getMarker());
            }
            if (getGroupSize() != null) {
                logger = new ThroughputLogger(camelLogger, getGroupSize());
            } else if (getGroupInterval() != null) {
                Boolean groupActiveOnly = getGroupActiveOnly() != null ? getGroupActiveOnly() : Boolean.TRUE;
                Long groupDelay = getGroupDelay();
                logger = new ThroughputLogger(camelLogger, this.getCamelContext(), getGroupInterval(), groupDelay, groupActiveOnly);
            } else {
                logger = new CamelLogProcessor(camelLogger, localFormatter);
            }
            // the logger is the processor
            setProcessor(this.logger);
            
        }
        ServiceHelper.startService(logger);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(logger);
    }

    public void setLogger(Processor logger) {
        this.logger = logger;
        // the logger is the processor
        setProcessor(this.logger);
    }

    public Processor getLogger() {
        return logger;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new LogProducer(this, this.logger);
    }

    @Override
    protected String createEndpointUri() {
        return "log:" + logger.toString();
    }

    /**
     * Logging level to use.
     * <p/>
     * The default value is INFO.
     */
    public String getLevel() {
        return level;
    }

    /**
     * Logging level to use.
     * <p/>
     * The default value is INFO.
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * An optional Marker name to use.
     */
    public String getMarker() {
        return marker;
    }

    /**
     * An optional Marker name to use.
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }

    /**
     * An integer that specifies a group size for throughput logging.
     */
    public Integer getGroupSize() {
        return groupSize;
    }

    /**
     * An integer that specifies a group size for throughput logging.
     */
    public void setGroupSize(Integer groupSize) {
        this.groupSize = groupSize;
    }

    /**
     * If specified will group message stats by this time interval (in millis)
     */
    public Long getGroupInterval() {
        return groupInterval;
    }

    /**
     * If specified will group message stats by this time interval (in millis)
     */
    public void setGroupInterval(Long groupInterval) {
        this.groupInterval = groupInterval;
    }

    /**
     * If true, will hide stats when no new messages have been received for a time interval, if false, show stats regardless of message traffic.
     */
    public Boolean getGroupActiveOnly() {
        return groupActiveOnly;
    }

    /**
     * If true, will hide stats when no new messages have been received for a time interval, if false, show stats regardless of message traffic.
     */
    public void setGroupActiveOnly(Boolean groupActiveOnly) {
        this.groupActiveOnly = groupActiveOnly;
    }

    /**
     * Set the initial delay for stats (in millis)
     */
    public Long getGroupDelay() {
        return groupDelay;
    }

    /**
     * Set the initial delay for stats (in millis)
     */
    public void setGroupDelay(Long groupDelay) {
        this.groupDelay = groupDelay;
    }

    public ExchangeFormatter getLocalFormatter() {
        return localFormatter;
    }

    public void setLocalFormatter(ExchangeFormatter localFormatter) {
        this.localFormatter = localFormatter;
    }

    public Logger getProvidedLogger() {
        return providedLogger;
    }

    public void setProvidedLogger(Logger providedLogger) {
        this.providedLogger = providedLogger;
    }

    /**
     * The logger name to use
     */
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * The logger name to use
     */
    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }
}
