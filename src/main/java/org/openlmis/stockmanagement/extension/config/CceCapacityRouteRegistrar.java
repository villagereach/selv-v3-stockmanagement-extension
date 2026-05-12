/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.stockmanagement.extension.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CceCapacityRouteRegistrar {

  private static final Logger LOGGER = LoggerFactory.getLogger(CceCapacityRouteRegistrar.class);

  private static final String SERVICE_NAME = "stockmanagement";
  private static final String CONSUL_KV_KEY = "resources/api/stockCardSummaries/cce/capacity";

  @Value("${CONSUL_HOST:consul}")
  private String consulHost;

  @Value("${CONSUL_PORT:8500}")
  private String consulPort;

  // package-private so tests can replace it with a mock
  RestTemplate restTemplate = new RestTemplate();

  /**
   * Publishes the CCE capacity endpoint's consul routing entry so nginx forwards
   * {@code /api/stockCardSummaries/cce/capacity} to this service. Runs once after
   * application startup; failures are logged but do not block the service.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void registerRoute() {
    String url = "http://" + consulHost + ":" + consulPort + "/v1/kv/" + CONSUL_KV_KEY;
    try {
      restTemplate.exchange(url, HttpMethod.PUT,
          new HttpEntity<>(SERVICE_NAME), String.class);
      LOGGER.info("Registered consul route {} -> {}", CONSUL_KV_KEY, SERVICE_NAME);
    } catch (Exception ex) {
      LOGGER.warn("Could not register consul route {} -> {}: {}",
          CONSUL_KV_KEY, SERVICE_NAME, ex.getMessage());
    }
  }
}
