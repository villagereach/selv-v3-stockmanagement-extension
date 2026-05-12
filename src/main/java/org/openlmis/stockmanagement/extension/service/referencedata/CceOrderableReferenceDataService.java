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

package org.openlmis.stockmanagement.extension.service.referencedata;

import java.util.List;
import org.openlmis.stockmanagement.extension.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.service.referencedata.BaseReferenceDataService;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class CceOrderableReferenceDataService extends BaseReferenceDataService<OrderableDto> {

  private static final int MAX_PAGE_SIZE = 10000;

  @Override
  protected String getUrl() {
    return "/api/orderables/";
  }

  @Override
  protected Class<OrderableDto> getResultClass() {
    return OrderableDto.class;
  }

  @Override
  protected Class<OrderableDto[]> getArrayResultClass() {
    return OrderableDto[].class;
  }

  /**
   * Returns all orderables with the fields needed for CCE capacity calculation
   * (temperature limits and net volume per pack).
   *
   * @return list of orderables, empty list if none were found.
   */
  public List<OrderableDto> findAll() {
    return getPage(RequestParameters.init().set("size", MAX_PAGE_SIZE)).getContent();
  }
}
