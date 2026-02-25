package com.zlt.aps.mp.common.utils.poi;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpSimulatedResult;
import lombok.Data;

import java.util.List;

/**
 * 多sheet数据
 * @author Yelq
 */
@Data
public class WorksheetData {
  private String sheetName;
  private List<MpSimulatedResult> simulatedResults;
  private List<FactoryMonthPlanMouldDayResult> mouldDayResults;
}

