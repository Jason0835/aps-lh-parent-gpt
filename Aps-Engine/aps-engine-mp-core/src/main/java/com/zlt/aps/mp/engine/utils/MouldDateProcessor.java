package com.zlt.aps.mp.engine.utils;

/**
 *
 * @author Yelq
 */
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Calendar;

/**
 * 模具排程日期处理器
 * 根据排程日期范围，过滤处理上机日期
 * @author Yelq
 */
public class MouldDateProcessor {

  /**
   * 添加指定天数到日期
   *
   * @param date
   *     原始日期
   * @return 添加天数后的日期
   */
  private static Date addDays(Date date) {
    if (date == null) {
      return null;
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    return calendar.getTime();
  }

  /**
   * 检查日期是否在指定范围内（包含边界）
   * @param dateToCheck 要检查的日期
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 是否在范围内
   */
  private static boolean isDateInRange(Date dateToCheck, Date startDate, Date endDate) {
    if (dateToCheck == null || startDate == null || endDate == null) {
      return false;
    }

    // 清除时间部分，只比较日期
    Date checkDate = clearTime(dateToCheck);
    Date rangeStart = clearTime(startDate);
    Date rangeEnd = clearTime(endDate);

    // 检查日期是否在范围内（包含边界）
    return (checkDate.equals(rangeStart) || checkDate.after(rangeStart))
        && (checkDate.equals(rangeEnd) || checkDate.before(rangeEnd));
  }

  /**
   * 清除日期的时间部分
   * @param date 原始日期
   * @return 只保留日期部分的日期
   */
  private static Date clearTime(Date date) {
    if (date == null) {
      return null;
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  /**
   * 批处理版本，提高大列表处理性能
   */
  public static List<MonthPlanProductMouldInfoVo> processBoardingDateBatch(
      List<MonthPlanProductMouldInfoVo> mouldDeliveryList,
      Date productionStartDate,
      Date productionEndDate) {
    if (CollectionUtils.isEmpty(mouldDeliveryList)) {
      return mouldDeliveryList;
    }

    // 并行处理以提高性能（对于大列表）
    if (mouldDeliveryList.size() > 1000) {
      return mouldDeliveryList.parallelStream()
          .map(vo -> processSingleVo(vo, productionStartDate, productionEndDate))
          .collect(java.util.stream.Collectors.toList());
    } else {
      return mouldDeliveryList.stream()
          .map(vo -> processSingleVo(vo, productionStartDate, productionEndDate))
          .collect(java.util.stream.Collectors.toList());
    }
  }

  /**
   * 处理单个对象
   */
  private static MonthPlanProductMouldInfoVo processSingleVo(
      MonthPlanProductMouldInfoVo vo,
      Date productionStartDate,
      Date productionEndDate) {

    if (productionStartDate == null || productionEndDate == null
        || productionStartDate.after(productionEndDate)) {
      vo.setBoardingDate(null);
      return vo;
    }

    Date boardingDate = vo.getBoardingDate();
    if (boardingDate == null) {
      return vo;
    }

    Date boardingDatePlusOne = addDays(boardingDate);
    boolean isInRange = isDateInRange(boardingDatePlusOne,
        productionStartDate, productionEndDate);

    if (!isInRange) {
      vo.setBoardingDate(null);
    }

    return vo;
  }

}
