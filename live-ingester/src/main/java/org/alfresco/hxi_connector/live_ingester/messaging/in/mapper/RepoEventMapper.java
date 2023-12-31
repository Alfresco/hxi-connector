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

package org.alfresco.hxi_connector.live_ingester.messaging.in.mapper;

import static java.util.Optional.ofNullable;

import static org.alfresco.hxi_connector.live_ingester.domain.utils.EnsureUtils.ensureThat;
import static org.alfresco.hxi_connector.live_ingester.messaging.in.utils.EventUtils.isEventTypeCreated;
import static org.alfresco.hxi_connector.live_ingester.messaging.in.utils.EventUtils.isEventTypeUpdated;

import java.time.ZonedDateTime;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.alfresco.hxi_connector.live_ingester.domain.usecase.content.IngestContentCommand;
import org.alfresco.hxi_connector.live_ingester.domain.usecase.metadata.IngestMetadataCommand;
import org.alfresco.hxi_connector.live_ingester.messaging.in.mapper.property.PropertiesMapper;
import org.alfresco.repo.event.v1.model.DataAttributes;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.repo.event.v1.model.UserInfo;

@Component
@RequiredArgsConstructor
public class RepoEventMapper
{
    private final PropertiesMapper propertiesMapper;

    public IngestContentCommand mapToIngestContentCommand(RepoEvent<DataAttributes<NodeResource>> event)
    {
        ensureThat(isEventTypeCreated(event), "Unsupported event type");

        return new IngestContentCommand(
                event.getTime().toInstant().toEpochMilli(),
                event.getData().getResource().getId());
    }

    public IngestMetadataCommand mapToIngestMetadataCommand(RepoEvent<DataAttributes<NodeResource>> event)
    {
        ensureThat(isEventTypeCreated(event) || isEventTypeUpdated(event), "Unsupported event type");

        return new IngestMetadataCommand(
                toMilliseconds(event.getTime()),
                event.getData().getResource().getId(),
                propertiesMapper.calculatePropertyDelta(event, NodeResource::getName),
                propertiesMapper.calculatePropertyDelta(event, NodeResource::getPrimaryAssocQName),
                propertiesMapper.calculatePropertyDelta(event, NodeResource::getNodeType),
                propertiesMapper.calculatePropertyDelta(event, node -> getUserId(node, NodeResource::getCreatedByUser)),
                propertiesMapper.calculatePropertyDelta(event, node -> getUserId(node, NodeResource::getModifiedByUser)),
                propertiesMapper.calculatePropertyDelta(event, NodeResource::getAspectNames),
                propertiesMapper.calculatePropertyDelta(event, NodeResource::isFile),
                propertiesMapper.calculatePropertyDelta(event, NodeResource::isFolder),
                propertiesMapper.calculatePropertyDelta(event, node -> toMilliseconds(node.getCreatedAt())),
                propertiesMapper.calculateCustomPropertiesDelta(event));
    }

    private Long toMilliseconds(ZonedDateTime time)
    {
        return time == null ? null : time.toInstant().toEpochMilli();
    }

    private String getUserId(NodeResource node, Function<NodeResource, UserInfo> userInfoGetter)
    {
        return ofNullable(node)
                .map(userInfoGetter)
                .map(UserInfo::getId)
                .orElse(null);
    }
}
