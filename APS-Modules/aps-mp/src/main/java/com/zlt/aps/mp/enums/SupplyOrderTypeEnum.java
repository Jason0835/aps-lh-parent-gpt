package com.zlt.aps.mp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Yelq
 */

@Getter
@AllArgsConstructor
public enum SupplyOrderTypeEnum {

  CYCLE_PRODUCTION_STOCK("2","周期排产储备"),
  PRECEDENT_STOCK("4","常规储备");
  final String code;
  final String desc;
}
