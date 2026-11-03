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
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.stockmanagement.extension.service.referencedata;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openlmis.stockmanagement.extension.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class CceOrderableReferenceDataServiceTest {

  private static final String PAGE_SIZE = "10000";

  private CceOrderableReferenceDataService service;

  @Before
  public void setUp() {
    // spy so we can stub the inherited getPage(...) without standing up the
    // BaseCommunicationService auth/restTemplate scaffolding
    service = spy(new CceOrderableReferenceDataService());
  }

  @Test
  public void shouldReturnTheOnlyPage() {
    OrderableDto only = orderable();
    doReturn(page(singletonList(only), 0, 1)).when(service).getPage(any(RequestParameters.class));

    List<OrderableDto> all = service.findAll();

    assertEquals(singletonList(only), all);
    assertEquals(singletonList("0"), requested("page", 1));
    assertEquals(singletonList(PAGE_SIZE), requested("size", 1));
  }

  @Test
  public void shouldConcatenatePagesUntilTheLastOne() {
    OrderableDto first = orderable();
    OrderableDto second = orderable();
    OrderableDto third = orderable();
    doReturn(page(asList(first, second), 0, 3), page(singletonList(third), 1, 3))
        .when(service).getPage(any(RequestParameters.class));

    List<OrderableDto> all = service.findAll();

    assertEquals(asList(first, second, third), all);
    assertEquals(asList("0", "1"), requested("page", 2));
    assertEquals(asList(PAGE_SIZE, PAGE_SIZE), requested("size", 2));
  }

  private List<String> requested(String key, int calls) {
    ArgumentCaptor<RequestParameters> captor = ArgumentCaptor.forClass(RequestParameters.class);
    verify(service, times(calls)).getPage(captor.capture());
    List<String> values = new ArrayList<>();
    for (RequestParameters parameters : captor.getAllValues()) {
      parameters.forEach(entry -> {
        if (key.equals(entry.getKey())) {
          values.addAll(entry.getValue());
        }
      });
    }
    return values;
  }

  private static Page<OrderableDto> page(List<OrderableDto> content, int index, long total) {
    // two items per page, so a total above two spans several pages
    return new PageImpl<>(content, PageRequest.of(index, 2), total);
  }

  private static OrderableDto orderable() {
    return OrderableDto.builder().id(UUID.randomUUID()).build();
  }
}
