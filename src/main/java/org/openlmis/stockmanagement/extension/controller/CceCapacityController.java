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

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.openlmis.stockmanagement.extension.dto.CceCapacityResult;
import org.openlmis.stockmanagement.extension.service.CceCapacityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CceCapacityController {

  @Autowired
  private CceCapacityService cceCapacityService;

  /**
   * Get total, used, and available CCE capacity (in liters) for a facility.
   *
   * <p>Volume in use is summed across all programs at the facility.
   *
   * @param facilityId    facility to compute capacity for.
   * @param nonEmptyOnly  if true, only orderables with positive stock on hand are counted.
   * @param orderableIds  optional list of orderable ids to restrict the calculation to.
   * @return the CCE capacity result for the facility.
   */
  @GetMapping(value = "/stockCardSummaries/cce/capacity")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public CceCapacityResult getCceCapacity(
      @RequestParam("facilityId") UUID facilityId,
      @RequestParam(value = "nonEmptyOnly", defaultValue = "true") boolean nonEmptyOnly,
      @RequestParam(value = "orderableId", required = false) List<UUID> orderableIds) {

    return cceCapacityService.calculateCceCapacity(facilityId, nonEmptyOnly,
        orderableIds == null ? Collections.emptyList() : orderableIds);
  }
}
