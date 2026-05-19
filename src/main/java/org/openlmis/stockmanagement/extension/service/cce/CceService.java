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

import java.util.UUID;
import org.openlmis.stockmanagement.extension.dto.cce.VolumeDto;
import org.openlmis.stockmanagement.service.BaseCommunicationService;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CceService extends BaseCommunicationService<VolumeDto> {

  @Value("${cce.url:${BASE_URL}}")
  private String cceUrl;

  @Override
  protected String getServiceUrl() {
    return cceUrl;
  }

  @Override
  protected String getUrl() {
    return "/api/inventoryItems/volume";
  }

  @Override
  protected Class<VolumeDto> getResultClass() {
    return VolumeDto.class;
  }

  @Override
  protected Class<VolumeDto[]> getArrayResultClass() {
    return VolumeDto[].class;
  }

  /**
   * Returns total volume (in liters) of functioning CCE inventory items
   * at the given facility.
   *
   * @param facilityId facility id to query CCE volume for.
   * @return volume in liters, or zero if the facility has no functioning CCE.
   */
  public int getVolumeByFacilityId(UUID facilityId) {
    VolumeDto volumeDto = findOne(RequestParameters.init().set("facilityId", facilityId));
    return volumeDto != null && volumeDto.getVolume() != null ? volumeDto.getVolume() : 0;
  }
}
