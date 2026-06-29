package com.zlt.aps.common.engine.utils;

import cn.hutool.core.collection.CollectionUtil;
import com.zlt.aps.common.engine.enums.MachineRangeEnum;

import java.math.BigDecimal;
import java.util.List;

/**
 * 备库班数配置解析工具类
 * <p>
 * 根据机台数量匹配深度配置区间，返回对应的保证班数（depthClassQty）。
 * 通过内部 VO 传递配置数据，不依赖具体业务实体，各模块均可使用。
 * </p>
 */
public class DepthConfigResolver {

    /**
     * 深度配置内部 VO
     */
    public static class DepthConfigVo {
        private Integer machineQty;
        private String machineRange;
        private BigDecimal depthClassQty;

        public DepthConfigVo() {}

        public DepthConfigVo(Integer machineQty, String machineRange, BigDecimal depthClassQty) {
            this.machineQty = machineQty;
            this.machineRange = machineRange;
            this.depthClassQty = depthClassQty;
        }

        public Integer getMachineQty() {
            return machineQty;
        }

        public void setMachineQty(Integer machineQty) {
            this.machineQty = machineQty;
        }

        public String getMachineRange() {
            return machineRange;
        }

        public void setMachineRange(String machineRange) {
            this.machineRange = machineRange;
        }

        public BigDecimal getDepthClassQty() {
            return depthClassQty;
        }

        public void setDepthClassQty(BigDecimal depthClassQty) {
            this.depthClassQty = depthClassQty;
        }
    }

    /**
     * 根据机台数量和配置列表解析对应的保证班数
     *
     * @param machineQty  机台数量
     * @param configList  深度配置列表（建议按 machineQty 降序排列）
     * @return 匹配的保证班数，无匹配返回 null
     */
    public static BigDecimal resolveDepthClassQty(Integer machineQty, List<DepthConfigVo> configList) {
        if (machineQty == null || CollectionUtil.isEmpty(configList)) {
            return null;
        }
        for (DepthConfigVo config : configList) {
            Integer configQty = config.getMachineQty();
            if (configQty == null) {
                continue;
            }
            MachineRangeEnum rangeEnum = MachineRangeEnum.getByCode(config.getMachineRange());
            if (rangeEnum != null && rangeEnum.matches(machineQty, configQty)) {
                return config.getDepthClassQty();
            }
        }
        return null;
    }
}
