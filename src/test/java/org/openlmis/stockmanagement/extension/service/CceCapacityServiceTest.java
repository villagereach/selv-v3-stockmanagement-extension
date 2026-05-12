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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.referencedata.ProgramDto;
import org.openlmis.stockmanagement.dto.referencedata.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.extension.dto.CceCapacityResult;
import org.openlmis.stockmanagement.extension.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.extension.dto.referencedata.TemperatureMeasurementDto;
import org.openlmis.stockmanagement.extension.dto.referencedata.VolumeMeasurementDto;
import org.openlmis.stockmanagement.extension.service.cce.CceService;
import org.openlmis.stockmanagement.extension.service.referencedata.CceOrderableReferenceDataService;
import org.openlmis.stockmanagement.extension.service.referencedata.CceProgramReferenceDataService;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummariesV2DtoBuilder;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;

@RunWith(MockitoJUnitRunner.class)
public class CceCapacityServiceTest {

  private static final UUID FACILITY_ID = UUID.randomUUID();

  @Mock
  private CceOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private CceProgramReferenceDataService programReferenceDataService;

  @Mock
  private StockCardSummariesService stockCardSummariesService;

  @Mock
  private StockCardSummariesV2DtoBuilder summariesV2DtoBuilder;

  @Mock
  private CceService cceService;

  @InjectMocks
  private CceCapacityService service;

  private OrderableDto bcg;
  private OrderableDto rota;
  private ProgramDto pav;

  @Before
  public void setUp() {
    bcg = refrigeratedOrderable(20.0, 8.0);
    rota = refrigeratedOrderable(10.0, 4.0);
    pav = newProgram();

    when(cceService.getVolumeByFacilityId(FACILITY_ID)).thenReturn(20);
    when(orderableReferenceDataService.findAll()).thenReturn(asList(bcg, rota));
    when(programReferenceDataService.findAll()).thenReturn(singletonList(pav));
    when(stockCardSummariesService.findStockCards(any(StockCardSummariesV2SearchParams.class)))
        .thenReturn(mock(StockCardSummaries.class));
  }

  @Test
  public void shouldReturnTotalUsedAndAvailableForFacility() {
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(asList(summary(bcg, 500), summary(rota, 100)));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    // BCG: 20 mL * 500 = 10,000 mL; Rota: 10 mL * 100 = 1,000 mL; total 11,000 mL = 11 L
    assertEquals(20, result.getTotalVolume());
    assertEquals(11, result.getVolumeInUse());
    assertEquals(9, result.getAvailableVolume());
  }

  @Test
  public void shouldReturnZeroVolumeInUseWhenNoRefrigeratedOrderablesExist() {
    when(orderableReferenceDataService.findAll())
        .thenReturn(singletonList(nonRefrigeratedOrderable()));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    assertEquals(0, result.getVolumeInUse());
    verify(stockCardSummariesService, never())
        .findStockCards(any(StockCardSummariesV2SearchParams.class));
  }

  @Test
  public void shouldExcludeOrderablesWithMaxTemperatureAboveLimit() {
    OrderableDto warmOrderable = refrigeratedOrderable(20.0, 10.0); // > 8°C
    when(orderableReferenceDataService.findAll())
        .thenReturn(asList(bcg, warmOrderable));
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(singletonList(summary(bcg, 500)));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    // only BCG counts: 20 * 500 / 1000 = 10
    assertEquals(10, result.getVolumeInUse());
  }

  @Test
  public void shouldIncludeOrderableAtTemperatureBoundary() {
    OrderableDto boundary = refrigeratedOrderable(20.0, 8.0);
    when(orderableReferenceDataService.findAll()).thenReturn(singletonList(boundary));
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(singletonList(summary(boundary, 1000)));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    assertEquals(20, result.getVolumeInUse());
  }

  @Test
  public void shouldExcludeOrderablesMissingInBoxCubeDimension() {
    OrderableDto noDim = OrderableDto.builder()
        .id(UUID.randomUUID())
        .minimumTemperature(new TemperatureMeasurementDto(2.0, "CEL"))
        .maximumTemperature(new TemperatureMeasurementDto(8.0, "CEL"))
        .build();
    when(orderableReferenceDataService.findAll()).thenReturn(singletonList(noDim));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    assertEquals(0, result.getVolumeInUse());
    verify(stockCardSummariesService, never())
        .findStockCards(any(StockCardSummariesV2SearchParams.class));
  }

  @Test
  public void shouldAccumulateVolumeAcrossPrograms() {
    ProgramDto covax = newProgram();
    when(programReferenceDataService.findAll()).thenReturn(asList(pav, covax));
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(singletonList(summary(bcg, 500)))
        .thenReturn(singletonList(summary(rota, 200)));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    // pav: 20 * 500 = 10,000; covax: 10 * 200 = 2,000; total 12,000 mL = 12 L
    assertEquals(12, result.getVolumeInUse());
    verify(stockCardSummariesService, times(2))
        .findStockCards(any(StockCardSummariesV2SearchParams.class));
  }

  @Test
  public void shouldSwallowErrorFromOneProgramAndContinue() {
    ProgramDto covax = newProgram();
    when(programReferenceDataService.findAll()).thenReturn(asList(pav, covax));

    when(stockCardSummariesService.findStockCards(any(StockCardSummariesV2SearchParams.class)))
        .thenThrow(new RuntimeException("program forbidden"))
        .thenReturn(mock(StockCardSummaries.class));
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(singletonList(summary(bcg, 500)));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    assertEquals(10, result.getVolumeInUse());
  }

  @Test
  public void shouldSkipNullProgramAndNullProgramId() {
    ProgramDto programWithoutId = new ProgramDto();
    when(programReferenceDataService.findAll())
        .thenReturn(asList(null, programWithoutId, pav));
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(singletonList(summary(bcg, 500)));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    assertEquals(10, result.getVolumeInUse());
    verify(stockCardSummariesService, times(1))
        .findStockCards(any(StockCardSummariesV2SearchParams.class));
  }

  @Test
  public void shouldRestrictToProvidedOrderableIds() {
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(singletonList(summary(bcg, 500)));

    service.calculateCceCapacity(FACILITY_ID, true, singletonList(bcg.getId()));

    ArgumentCaptor<StockCardSummariesV2SearchParams> captor =
        ArgumentCaptor.forClass(StockCardSummariesV2SearchParams.class);
    verify(stockCardSummariesService).findStockCards(captor.capture());
    assertEquals(singletonList(bcg.getId()), captor.getValue().getOrderableIds());
  }

  @Test
  public void shouldIgnoreSummariesWithNullStockOnHand() {
    // canFulfillForMe entry with null SOH causes summary.getStockOnHand() to return null
    CanFulfillForMeEntryDto entryWithNullSoh = new CanFulfillForMeEntryDto();
    StockCardSummaryV2Dto nullSummary = new StockCardSummaryV2Dto(
        versionRef(bcg.getId()), singleton(entryWithNullSoh));
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(singletonList(nullSummary));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    assertEquals(0, result.getVolumeInUse());
  }

  @Test
  public void shouldAccumulateInMillilitresWithoutPerProductTruncation() {
    // Two summaries each contributing 500 mL — round to 0 individually but to 1 L together.
    OrderableDto half = refrigeratedOrderable(1.0, 8.0);
    when(orderableReferenceDataService.findAll()).thenReturn(singletonList(half));
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(asList(summary(half, 500), summary(half, 500)));

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    // 1 * 500 + 1 * 500 = 1000 mL = 1 L (not 0 + 0)
    assertEquals(1, result.getVolumeInUse());
  }

  @Test
  public void shouldPassNonEmptyOnlyFlagThroughToParamsAndBuilder() {
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(emptyList());

    service.calculateCceCapacity(FACILITY_ID, false, emptyList());

    ArgumentCaptor<StockCardSummariesV2SearchParams> paramsCaptor =
        ArgumentCaptor.forClass(StockCardSummariesV2SearchParams.class);
    verify(stockCardSummariesService).findStockCards(paramsCaptor.capture());
    assertEquals(false, paramsCaptor.getValue().isNonEmptyOnly());

    ArgumentCaptor<Boolean> flagCaptor = ArgumentCaptor.forClass(Boolean.class);
    verify(summariesV2DtoBuilder).build(any(), any(), any(), flagCaptor.capture());
    assertEquals(Boolean.FALSE, flagCaptor.getValue());
  }

  @Test
  public void shouldReturnZeroAvailableWhenTotalIsZero() {
    when(cceService.getVolumeByFacilityId(FACILITY_ID)).thenReturn(0);
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(emptyList());

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    assertEquals(0, result.getTotalVolume());
    assertEquals(0, result.getVolumeInUse());
    assertEquals(0, result.getAvailableVolume());
  }

  @Test
  public void shouldReportNegativeAvailableWhenInUseExceedsTotal() {
    when(cceService.getVolumeByFacilityId(FACILITY_ID)).thenReturn(20);
    when(summariesV2DtoBuilder.build(any(), any(), any(), anyBoolean()))
        .thenReturn(singletonList(summary(bcg, 2500))); // 20 * 2500 = 50,000 mL = 50 L

    CceCapacityResult result = service.calculateCceCapacity(FACILITY_ID, true, emptyList());

    assertEquals(20, result.getTotalVolume());
    assertEquals(50, result.getVolumeInUse());
    assertEquals(-30, result.getAvailableVolume());
  }

  // ---- helpers ----

  private static OrderableDto refrigeratedOrderable(double packVolumeMl, double maxTempCelsius) {
    return OrderableDto.builder()
        .id(UUID.randomUUID())
        .inBoxCubeDimension(new VolumeMeasurementDto(packVolumeMl, "MLT"))
        .minimumTemperature(new TemperatureMeasurementDto(2.0, "CEL"))
        .maximumTemperature(new TemperatureMeasurementDto(maxTempCelsius, "CEL"))
        .build();
  }

  private static OrderableDto nonRefrigeratedOrderable() {
    return OrderableDto.builder()
        .id(UUID.randomUUID())
        .inBoxCubeDimension(new VolumeMeasurementDto(10.0, "MLT"))
        .build();
  }

  private static ProgramDto newProgram() {
    ProgramDto program = new ProgramDto();
    program.setId(UUID.randomUUID());
    return program;
  }

  private static StockCardSummaryV2Dto summary(OrderableDto orderable, int stockOnHand) {
    CanFulfillForMeEntryDto entry = new CanFulfillForMeEntryDto();
    entry.setStockOnHand(stockOnHand);
    return new StockCardSummaryV2Dto(versionRef(orderable.getId()), singleton(entry));
  }

  private static VersionObjectReferenceDto versionRef(UUID id) {
    return new VersionObjectReferenceDto(id, "http://test", "orderables", 1L);
  }
}
