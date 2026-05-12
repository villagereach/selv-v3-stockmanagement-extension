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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openlmis.stockmanagement.dto.referencedata.ProgramDto;
import org.openlmis.stockmanagement.service.referencedata.BaseReferenceDataService;
import org.springframework.stereotype.Service;

@Service
public class CceProgramReferenceDataService extends BaseReferenceDataService<ProgramDto> {

  @Override
  protected String getUrl() {
    return "/api/programs/";
  }

  @Override
  protected Class<ProgramDto> getResultClass() {
    return ProgramDto.class;
  }

  @Override
  protected Class<ProgramDto[]> getArrayResultClass() {
    return ProgramDto[].class;
  }

  /**
   * Returns all programs known to the reference data service.
   *
   * <p>Used by the CCE capacity calculation to iterate over all programs when summing
   * volume in use across the facility.
   *
   * @return list of programs, empty list if none were found.
   */
  public List<ProgramDto> findAll() {
    return new ArrayList<>(findAll("", Collections.emptyMap()));
  }
}
