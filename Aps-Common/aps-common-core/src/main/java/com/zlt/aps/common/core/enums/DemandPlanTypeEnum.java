package com.zlt.aps.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 需求计划类型
 * @author Yelq
 */
@Getter
@AllArgsConstructor
public enum DemandPlanTypeEnum {
  MONTH_DEMAND("01","月计划需求"),
  PREDICTION_DEMAND("02","预测需求");
  private final String code;
  private final String desc;
}
