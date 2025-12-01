package com.zlt.mix.schedule.engine.util;

import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.vo.GlueFactoryRequireVo;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 排程需求量量计算工具
 */
public class ScheduleRequireUtils {

    /**
     * 判断夜班是否有需求量
     *
     * @param factoryRequireMap 分厂需求量
     * @param glue              胶料
     * @return 是否有分厂需求量
     */
    public static boolean checkNightRequire(Map<String, GlueFactoryRequireVo> factoryRequireMap, String glue) {
        if (factoryRequireMap == null || factoryRequireMap.isEmpty() || StringUtils.isBlank(glue)) {
            return false;
        }

        GlueFactoryRequireVo factoryRequireVo = factoryRequireMap.get(glue);
        return factoryRequireVo != null
                && factoryRequireVo.getRequireClass() != null
                && GlueEngineConstants.SHIFT_CLASS_MID == factoryRequireVo.getRequireClass()
                && factoryRequireVo.getRequireDifference() != null
                && factoryRequireVo.getRequireDifference().compareTo(BigDecimal.ZERO) > 0;
    }
}
