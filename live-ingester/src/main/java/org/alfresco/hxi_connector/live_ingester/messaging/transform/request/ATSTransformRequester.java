/*
 * #%L
 * Alfresco HX Insight Connector
 * %%
 * Copyright (C) 2023 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.hxi_connector.live_ingester.messaging.transform.request;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import org.alfresco.hxi_connector.live_ingester.domain.exception.LiveIngesterRuntimeException;
import org.alfresco.hxi_connector.live_ingester.domain.ports.transform_engine.TransformRequest;
import org.alfresco.hxi_connector.live_ingester.domain.ports.transform_engine.TransformRequester;
import org.alfresco.hxi_connector.live_ingester.messaging.transform.TransformConfig;
import org.alfresco.hxi_connector.live_ingester.messaging.transform.request.model.ATSTransformRequest;
import org.alfresco.hxi_connector.live_ingester.messaging.transform.request.model.ClientData;

@Slf4j
@Component
@RequiredArgsConstructor
public class ATSTransformRequester extends RouteBuilder implements TransformRequester
{
    private static final String LOCAL_ENDPOINT = "direct:" + ATSTransformRequester.class.getSimpleName();
    private static final String WORKSPACE_SPACES_STORE = "workspace://SpacesStore/";
    private static final String TIMEOUT_KEY = "timeout";

    private final CamelContext context;
    private final ObjectMapper objectMapper;
    private final TransformConfig transformConfig;

    @Override
    public void configure()
    {
        from(LOCAL_ENDPOINT)
                .marshal()
                .json()
                .to(transformConfig.getRequest().getEndpoint());
    }

    @Override
    public void requestTransform(TransformRequest transformRequest)
    {
        ATSTransformRequest atsTransformRequest = atsTransformRequestFrom(transformRequest);
        log.info("Sending request to ATS: {}", atsTransformRequest);
        context.createProducerTemplate()
                .sendBody(LOCAL_ENDPOINT, atsTransformRequest);
    }

    private ATSTransformRequest atsTransformRequestFrom(TransformRequest transformRequest)
    {
        String clientDataString = makeClientDataString(transformRequest);
        return ATSTransformRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .nodeRef(WORKSPACE_SPACES_STORE + transformRequest.nodeRef())
                .targetMediaType(transformRequest.targetMimeType())
                .replyQueue(transformConfig.getResponse().getQueueName())
                .transformOptions(getTransformRequestOptions(transformConfig))
                .clientData(clientDataString)
                .build();
    }

    private Map<String, String> getTransformRequestOptions(TransformConfig transformConfig)
    {
        return Map.of(TIMEOUT_KEY, String.valueOf(transformConfig.getRequest().getTimeout()));
    }

    private String makeClientDataString(TransformRequest transformRequest)
    {
        ClientData clientData = new ClientData(transformRequest.eventTimestamp(), transformRequest.nodeRef());
        try
        {
            return objectMapper.writeValueAsString(clientData);
        }
        catch (JsonProcessingException e)
        {
            throw new LiveIngesterRuntimeException("Failed to construct client data string for Transform Service request.", e);
        }
    }
}
