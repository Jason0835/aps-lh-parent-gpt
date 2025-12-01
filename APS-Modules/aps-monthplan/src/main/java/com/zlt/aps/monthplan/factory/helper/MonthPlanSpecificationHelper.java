package com.zlt.aps.monthplan.factory.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.IncrementConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.IncrementService;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;

import java.util.Arrays;

/**
 * @author Yelq
 */
public class MonthPlanSpecificationHelper {
  private static final Integer[] PRODUCTION_DAY_FIELDS = FactoryConstant.PRODUCTION_CYCLE;
  private static final String NOTICE_NO_PREFIX_PATTERN = IncrementConstant.MONTH_PLAN_ADJUST_NOTICE + "%s";
  public static final String SERIAL_NUMBER_FORMAT = "%06d";
  /**
   * 构建查询包装器 - 避免重复代码
   */
  public static LambdaQueryWrapper<MonthPlanProductionFinalResult> buildFinalResultQuery(
      String factoryCode, Integer year, Integer month,
      String monthPlanVersion, String productionVersion) {
    return new LambdaQueryWrapper<MonthPlanProductionFinalResult>()
        .eq(MonthPlanProductionFinalResult::getFactoryCode, factoryCode)
        .eq(MonthPlanProductionFinalResult::getYear, year)
        .eq(MonthPlanProductionFinalResult::getMonth, month)
        .eq(MonthPlanProductionFinalResult::getMonthPlanVersion, monthPlanVersion)
        .eq(MonthPlanProductionFinalResult::getProductionVersion, productionVersion)
        .eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getValue());
  }

  /**
   * 计算生产数量总和
   */
  public static long calculateProductionQty(MonthPlanProductionFinalResult param) {
    return Arrays.stream(PRODUCTION_DAY_FIELDS)
        .mapToLong(day -> {
          String fieldName = (day > 0 ? "day" : "preDay") + Math.abs(day);
          Object value = param.getFieldValueByFieldName(fieldName);
          return value != null ? (Long) value : 0L;
        })
        .sum();
  }

  /**
   * 生成通知单号
   */
  public static String generateNoticeNo(IncrementService incrementService) {
    String dateStr = DateUtils.dateTimeNow("yyyyMMdd");
    String prefix = String.format(NOTICE_NO_PREFIX_PATTERN, dateStr);
    return incrementService.getBillNoSequenceByExpire(prefix, 3, 60 * 24 * 7);
  }
}
