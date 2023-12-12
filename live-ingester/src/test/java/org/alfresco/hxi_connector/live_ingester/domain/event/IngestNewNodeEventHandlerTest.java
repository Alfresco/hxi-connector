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

package org.alfresco.hxi_connector.live_ingester.domain.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.alfresco.hxi_connector.live_ingester.domain.model.in.IngestNewNodeEvent;
import org.alfresco.hxi_connector.live_ingester.domain.model.in.Node;
import org.alfresco.hxi_connector.live_ingester.domain.model.out.event.EventPublisher;
import org.alfresco.hxi_connector.live_ingester.domain.model.out.event.UpdateNodeMetadataEvent;
import org.alfresco.hxi_connector.live_ingester.domain.model.transform.request.TransformRequest;
import org.alfresco.hxi_connector.live_ingester.domain.model.transform.request.TransformRequester;

@ExtendWith(MockitoExtension.class)
class IngestNewNodeEventHandlerTest
{
    @Mock
    IngestNewNodeEvent ingestNewNodeEvent;
    @Mock
    Node node;
    @Mock
    UpdateNodeMetadataEvent updateNodeMetadataEvent;
    @Mock
    TransformRequest transformRequest;
    @Mock
    UpdateNodeEventMapper updateNodeEventMapper;
    @Mock
    EventPublisher eventPublisher;
    @Mock
    TransformRequestMapper transformRequestMapper;
    @Mock
    TransformRequester transformRequester;
    @InjectMocks
    IngestNewNodeEventHandler ingestNewNodeEventHandler;

    @BeforeEach
    void setUp()
    {
        given(ingestNewNodeEvent.node()).willReturn(node);
    }

    @Test
    void nodeWithoutContent_PublishMetadataButNoTransform()
    {
        // given
        given(node.contentMimeType()).willReturn(Optional.empty());
        given(updateNodeEventMapper.map(ingestNewNodeEvent)).willReturn(updateNodeMetadataEvent);

        // when
        ingestNewNodeEventHandler.handle(ingestNewNodeEvent);

        // then
        verify(eventPublisher).publishMessage(updateNodeMetadataEvent);
        verifyNoInteractions(transformRequestMapper, transformRequester);
    }

    @Test
    void nodeWithContent_PublishMetadataAndRequestTransform()
    {
        // given
        given(node.contentMimeType()).willReturn(Optional.of("application/msword"));
        given(updateNodeEventMapper.map(ingestNewNodeEvent)).willReturn(updateNodeMetadataEvent);
        given(transformRequestMapper.map(ingestNewNodeEvent)).willReturn(transformRequest);

        // when
        ingestNewNodeEventHandler.handle(ingestNewNodeEvent);

        // then
        verify(eventPublisher).publishMessage(updateNodeMetadataEvent);
        verify(transformRequester).requestTransform(transformRequest);
    }
}
