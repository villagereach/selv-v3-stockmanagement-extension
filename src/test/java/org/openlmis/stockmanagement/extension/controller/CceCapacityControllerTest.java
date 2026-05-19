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

package org.openlmis.stockmanagement.extension.controller;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openlmis.stockmanagement.extension.dto.CceCapacityResult;
import org.openlmis.stockmanagement.extension.service.CceCapacityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class CceCapacityControllerTest {

  private static final UUID FACILITY_ID = UUID.randomUUID();

  @Mock
  private CceCapacityService cceCapacityService;

  @InjectMocks
  private CceCapacityController controller;

  private CceCapacityResult expectedResult;

  @Before
  public void setUp() {
    expectedResult = new CceCapacityResult(20, 5, 15);
  }

  @Test
  public void shouldDelegateToServiceWithDefaultNonEmptyOnlyAndEmptyOrderables() {
    when(cceCapacityService.calculateCceCapacity(FACILITY_ID, true, emptyList()))
        .thenReturn(expectedResult);

    CceCapacityResult body = invokeWithoutOrderableIds(true);

    assertSame(expectedResult, body);
    verify(cceCapacityService).calculateCceCapacity(FACILITY_ID, true, emptyList());
  }

  @Test
  public void shouldForwardOrderableIdsToService() {
    List<UUID> orderableIds = asList(UUID.randomUUID(), UUID.randomUUID());
    when(cceCapacityService.calculateCceCapacity(eq(FACILITY_ID), eq(true), eq(orderableIds)))
        .thenReturn(expectedResult);

    CceCapacityResult body = controller.getCceCapacity(FACILITY_ID, true, orderableIds);

    assertSame(expectedResult, body);
    verify(cceCapacityService).calculateCceCapacity(FACILITY_ID, true, orderableIds);
  }

  @Test
  public void shouldPassNonEmptyOnlyFalseThroughToService() {
    when(cceCapacityService.calculateCceCapacity(FACILITY_ID, false, emptyList()))
        .thenReturn(expectedResult);

    invokeWithoutOrderableIds(false);

    verify(cceCapacityService).calculateCceCapacity(FACILITY_ID, false, emptyList());
  }

  @Test
  public void shouldReturnTheBodyValuesFromTheService() {
    when(cceCapacityService.calculateCceCapacity(FACILITY_ID, true, emptyList()))
        .thenReturn(expectedResult);

    CceCapacityResult body = invokeWithoutOrderableIds(true);

    assertEquals(20, body.getTotalVolume());
    assertEquals(5, body.getVolumeInUse());
    assertEquals(15, body.getAvailableVolume());
  }

  private CceCapacityResult invokeWithoutOrderableIds(boolean nonEmptyOnly) {
    return controller.getCceCapacity(FACILITY_ID, nonEmptyOnly, null);
  }

  // sanity guard: the controller should still compile to a public, non-streaming response shape
  @SuppressWarnings("unused")
  private ResponseEntity<CceCapacityResult> shapeReference() {
    return ResponseEntity.status(HttpStatus.OK).body(expectedResult);
  }
}
