package com.zlt.aps.mp.engine.handler;

import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.engine.constant.ProductionConstant;

/**
 * 硫化机计算工具类
 * @author zlt
 *
 */
public class LhMachineInfoCalculateHelper {
    /**
     * 获取硫化机总台数（处理单控机台）
     * @param lhMachineInfoList
     * @return
     */
    public static int getLhMachineCount(List<LhMachineInfo> lhMachineInfoList) {
        long singleControlMachineCount = lhMachineInfoList.stream()
                .filter(m -> !Objects.equals(m.getMaxMoldNum(), ProductionConstant.DOUBLE_MOULD_PRODUCTION)).count();
        Integer lhmachineCount = lhMachineInfoList.size();
        if (singleControlMachineCount > 0) {
            int reduceMachineCount = BigDecimalUtils
                    .div(singleControlMachineCount, ProductionConstant.DOUBLE_MOULD_PRODUCTION, 4)
                    .setScale(0, RoundingMode.DOWN).intValue(); // 单控机台数的一半需要扣除掉（向下取整）
            lhmachineCount -= reduceMachineCount;
        }
        return lhmachineCount;
    }
}
