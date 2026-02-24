package com.zlt.aps.mp.engine.utils;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.scheduling.ProductionContext;
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

    private ProductionProcessUtils() {

    }
}
