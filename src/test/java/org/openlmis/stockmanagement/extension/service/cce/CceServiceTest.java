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

package org.openlmis.stockmanagement.extension.service.cce;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.stockmanagement.extension.dto.cce.VolumeDto;
import org.openlmis.stockmanagement.util.RequestParameters;

public class CceServiceTest {

  private CceService service;

  @Before
  public void setUp() {
    // spy so we can stub the inherited findOne(...) without standing up the
    // BaseCommunicationService auth/restTemplate scaffolding
    service = spy(new CceService());
  }

  @Test
  public void shouldReturnVolumeFromResponse() {
    doReturn(new VolumeDto(42)).when(service).findOne(any(RequestParameters.class));

    int volume = service.getVolumeByFacilityId(UUID.randomUUID());

    assertEquals(42, volume);
  }

  @Test
  public void shouldReturnZeroWhenResponseIsNull() {
    doReturn(null).when(service).findOne(any(RequestParameters.class));

    int volume = service.getVolumeByFacilityId(UUID.randomUUID());

    assertEquals(0, volume);
  }

  @Test
  public void shouldReturnZeroWhenResponseVolumeIsNull() {
    doReturn(new VolumeDto(null)).when(service).findOne(any(RequestParameters.class));

    int volume = service.getVolumeByFacilityId(UUID.randomUUID());

    assertEquals(0, volume);
  }
}
