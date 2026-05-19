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

package org.openlmis.stockmanagement.extension.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.stockmanagement.dto.referencedata.ProgramDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.extension.dto.CceCapacityResult;
import org.openlmis.stockmanagement.extension.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.extension.dto.referencedata.TemperatureMeasurementDto;
import org.openlmis.stockmanagement.extension.dto.referencedata.VolumeMeasurementDto;
import org.openlmis.stockmanagement.extension.service.cce.CceService;
import org.openlmis.stockmanagement.extension.service.referencedata.CceOrderableReferenceDataService;
import org.openlmis.stockmanagement.extension.service.referencedata.CceProgramReferenceDataService;
import org.openlmis.stockmanagement.extension.util.SecurityContexts;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummariesV2DtoBuilder;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class CceCapacityService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CceCapacityService.class);

  private static final double MAX_REFRIGERATION_TEMP = 8.0;
  private static final int MILLILITERS_PER_LITER = 1000;
  private static final String SECURITY_CONTEXT_CLIENT_ID = "cce-capacity";

  @Autowired
  private CceOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private CceProgramReferenceDataService programReferenceDataService;

  @Autowired
  private StockCardSummariesService stockCardSummariesService;

  @Autowired
  private StockCardSummariesV2DtoBuilder summariesV2DtoBuilder;

  @Autowired
  private CceService cceService;

  /**
   * Calculates total, used, and available CCE capacity (in liters) for a facility.
   *
   * <p>Total volume is fetched from the CCE service. Volume in use is computed by iterating over
   * all programs at the facility and summing, for each refrigerated approved product (maximum
   * temperature not greater than {@value #MAX_REFRIGERATION_TEMP}°C), the stock on hand reported
   * by the v2 stock card summaries logic. Per-product contribution is
   * {@code inBoxCubeDimension × stockOnHand / 1000}, in millilitres.
   *
   * @param facilityId    facility to compute capacity for.
   * @param nonEmptyOnly  if true, only summaries with at least one stock card are counted.
   * @param orderableIds  restrict the calculation to these orderables; if empty,
   *                      all refrigerated orderables are considered.
   * @return a result holding total volume, used volume, and available volume.
   */
  public CceCapacityResult calculateCceCapacity(UUID facilityId, boolean nonEmptyOnly,
      List<UUID> orderableIds) {
    int totalVolume = cceService.getVolumeByFacilityId(facilityId);
    int volumeInUse = (int) Math.round(
        calculateVolumeInUseMl(facilityId, nonEmptyOnly, orderableIds) / MILLILITERS_PER_LITER);
    return new CceCapacityResult(totalVolume, volumeInUse, totalVolume - volumeInUse);
  }

  private double calculateVolumeInUseMl(UUID facilityId, boolean nonEmptyOnly,
      List<UUID> orderableIds) {
    Map<UUID, OrderableDto> refrigeratedOrderables = orderableReferenceDataService.findAll()
        .stream()
        .filter(this::isRefrigerated)
        .collect(Collectors.toMap(OrderableDto::getId, o -> o, (a, b) -> a));

    if (refrigeratedOrderables.isEmpty()) {
      return 0;
    }

    List<UUID> targetOrderableIds = (orderableIds != null && !orderableIds.isEmpty())
        ? orderableIds.stream().filter(refrigeratedOrderables::containsKey).collect(
            Collectors.toList())
        : refrigeratedOrderables.keySet().stream().collect(Collectors.toList());

    if (targetOrderableIds.isEmpty()) {
      return 0;
    }

    List<ProgramDto> programs = programReferenceDataService.findAll();
    LOGGER.debug("Iterating CCE capacity across {} program(s) for facility {}",
        programs.size(), facilityId);

    double totalMl = 0;
    for (ProgramDto program : programs) {
      if (program == null || program.getId() == null) {
        continue;
      }
      totalMl += volumeInUseMlForProgram(facilityId, program.getId(), nonEmptyOnly,
          targetOrderableIds, refrigeratedOrderables);
    }
    return totalMl;
  }

  private double volumeInUseMlForProgram(UUID facilityId, UUID programId, boolean nonEmptyOnly,
      List<UUID> orderableIds, Map<UUID, OrderableDto> refrigeratedOrderables) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("facilityId", facilityId.toString());
    queryParams.add("programId", programId.toString());
    queryParams.add("nonEmptyOnly", Boolean.toString(nonEmptyOnly));
    for (UUID orderableId : orderableIds) {
      queryParams.add("orderableId", orderableId.toString());
    }
    StockCardSummariesV2SearchParams params = new StockCardSummariesV2SearchParams(queryParams);

    StockCardSummaries summaries;
    SecurityContext originalContext = SecurityContextHolder.getContext();
    try {
      SecurityContextHolder.setContext(SecurityContexts.clientOnly(SECURITY_CONTEXT_CLIENT_ID));
      summaries = stockCardSummariesService.findStockCards(params);
    } catch (PermissionMessageException ex) {
      LOGGER.debug("Skipping program {} for facility {}: {}", programId, facilityId,
          ex.getMessage());
      return 0;
    } finally {
      SecurityContextHolder.setContext(originalContext);
    }

    List<StockCardSummaryV2Dto> dtos = summariesV2DtoBuilder.build(
        summaries.getPageOfApprovedProducts(),
        summaries.getStockCardsForFulfillOrderables(),
        summaries.getOrderableFulfillMap(),
        nonEmptyOnly);

    double volumeMl = 0;
    for (StockCardSummaryV2Dto summary : dtos) {
      if (summary.getOrderable() == null) {
        continue;
      }
      OrderableDto orderable = refrigeratedOrderables.get(summary.getOrderable().getId());
      Integer stockOnHand = summary.getStockOnHand();
      Double packVolumeMl = getPackVolumeMl(orderable);
      if (stockOnHand != null && packVolumeMl != null) {
        volumeMl += packVolumeMl * stockOnHand;
      }
    }
    return volumeMl;
  }

  private boolean isRefrigerated(OrderableDto orderable) {
    VolumeMeasurementDto cubeDimension = orderable.getInBoxCubeDimension();
    TemperatureMeasurementDto maxTemp = orderable.getMaximumTemperature();
    if (cubeDimension == null || maxTemp == null) {
      return false;
    }
    Double maxValue = maxTemp.getValue();
    return maxValue != null && maxValue <= MAX_REFRIGERATION_TEMP;
  }

  private Double getPackVolumeMl(OrderableDto orderable) {
    if (orderable == null || orderable.getInBoxCubeDimension() == null) {
      return null;
    }
    return orderable.getInBoxCubeDimension().getValue();
  }
}
