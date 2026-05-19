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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class CceCapacityRouteRegistrarTest {

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private CceCapacityRouteRegistrar registrar;

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(registrar, "consulHost", "consul");
    ReflectionTestUtils.setField(registrar, "consulPort", "8500");
    // Speed up retries so tests don't sleep for real wall-clock time.
    ReflectionTestUtils.setField(registrar, "maxAttempts", 3);
    ReflectionTestUtils.setField(registrar, "retryBackoffMs", 0L);
  }

  @Test
  public void shouldPutCceCapacityRouteToConsulOnStartup() {
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.PUT),
        any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("true"));

    registrar.registerRoute();

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HttpEntity> bodyCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate, times(1)).exchange(urlCaptor.capture(), eq(HttpMethod.PUT),
        bodyCaptor.capture(), eq(String.class));

    assertEquals(
        "http://consul:8500/v1/kv/resources/api/stockCardSummaries/cce/capacity",
        urlCaptor.getValue());
    assertEquals("stockmanagement", bodyCaptor.getValue().getBody());
  }

  @Test
  public void shouldUseCustomConsulHostAndPort() {
    ReflectionTestUtils.setField(registrar, "consulHost", "consul.example.com");
    ReflectionTestUtils.setField(registrar, "consulPort", "9500");
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.PUT),
        any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("true"));

    registrar.registerRoute();

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.PUT),
        any(HttpEntity.class), eq(String.class));

    assertEquals(
        "http://consul.example.com:9500/v1/kv/resources/api/stockCardSummaries/cce/capacity",
        urlCaptor.getValue());
  }

  @Test
  public void shouldRetryAndSucceedOnLaterAttempt() {
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.PUT),
        any(HttpEntity.class), eq(String.class)))
        .thenThrow(new RestClientException("connection refused"))
        .thenThrow(new RestClientException("connection refused"))
        .thenReturn(ResponseEntity.ok("true"));

    registrar.registerRoute();

    verify(restTemplate, times(3)).exchange(any(String.class), eq(HttpMethod.PUT),
        any(HttpEntity.class), eq(String.class));
  }

  @Test
  public void shouldRetryMaxAttemptsAndSwallowFinalFailure() {
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.PUT),
        any(HttpEntity.class), eq(String.class)))
        .thenThrow(new RestClientException("connection refused"));

    // must not throw — startup should never be blocked
    registrar.registerRoute();

    verify(restTemplate, times(3)).exchange(any(String.class), eq(HttpMethod.PUT),
        any(HttpEntity.class), eq(String.class));
  }
}
