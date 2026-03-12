package com.zlt.aps.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 质控状态枚举
 * @author Yelq
 */
@Getter
@AllArgsConstructor
public enum QualityStateEnum {
  IN_PRODUCTION("4","投产");
  /**
   * 操作业务编码
   */
  private final String code;

  /**
   * 操作业务名称
   */
  private final String name;
}
