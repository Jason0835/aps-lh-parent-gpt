package com.zlt.aps.factory.utils;

import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.scheduling.ProductionContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 排产流程共用业务类
 *
 * @author ZLT
 * @date 20250715
 */
@Slf4j
public class ProductionProcessUtils {

    /**
     * 获取每日工作时长
     * 并转换成秒
     *
     * @param context
     * @return
     */
    public static BigDecimal getDayWorkHours(ProductionContext context) {
        BigDecimal dayWorkHours = (BigDecimal) context.getFactoryParams().get(FactoryConstant.SYS_PARAM_DAY_WORK_HOURS);
        if (null == dayWorkHours) {
            dayWorkHours = BigDecimal.valueOf(ProductionConstant.MAX_DAY_HOURS);
        }
        return dayWorkHours.multiply(BigDecimal.valueOf(ProductionConstant.HOUR_SECOND));
    }
    /**
     * 判断是否排产结束
     * 如果是双数模，则needProductionQty为1则视为结束-即双模排排双不排单
     * 如果是单数模，则needProductionQty为零则视为结束
     *
     * @param remainder         0 表示双数模 1 表示单数模
     * @param needProductionQty 剩余还需排产量
     * @return true 表示结束 false表示没有
     */
    public static boolean isProductionEnd(int remainder, Long needProductionQty) {
        //双数模，且剩余排产量小于等于1，则视为结束
        if (remainder == BigDecimal.ZERO.intValue() && needProductionQty <= BigDecimal.ONE.longValue()) {
            return true;
        }
        //单数模，且剩余排产量小于等于0，则视为结束
        if (remainder != BigDecimal.ZERO.intValue() && needProductionQty <= BigDecimal.ZERO.longValue()) {
            return true;
        }
        return false;
    }

    /**
     * 是否双模排产不排单数
     *
     * @param remainder             模具数/2的余数
     * @param leftOverProductionQty 还需排产量
     * @return
     */
    public static boolean isDoubleMouldNoProductionSingle(int remainder, Long leftOverProductionQty) {
        return remainder == BigDecimal.ZERO.intValue() && leftOverProductionQty == BigDecimal.ONE.longValue();
    }

    private ProductionProcessUtils() {

    }
}
