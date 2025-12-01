package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成型产能分配信息辅助对象
 *
 * @author ZLT
 * @date 20250813
 */
@Data
public class MoldingMachineAllocationInfoVo implements Serializable {

    /**
     * 寸口（保留2位小数）
     */
    private BigDecimal proSize;
    /**
     * 工装类别 0 通用 1 大鼓
     */
    private String workWearType;
    /**
     * 成型法 取数据字典molding_method的编码
     */
    private String mouldMethod;
    /**
     * 胎体布层级
     */
    private Integer tireFabricNumber;
    /**
     * 分配的需求量
     */
    private Long allocationQty;
    /**
     * 分配的天数
     */
    private Integer allocationDays;
    /**
     * 对应的寸口天产能
     */
    private Long proSizeQuotaQty;
    /**
     * 对应的天最大模具数，配比 * 2
     */
    private Integer dayMaxMouldQty;

    /**
     * 获取寸口产能分组key
     * 寸口|*|工装类别|*|成型法|*|胎体布层级
     *
     * @return
     */
    public String getSizeCapacityGroupKey() {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, proSize, workWearType, mouldMethod, tireFabricNumber);
    }

    /**
     * 因一次法成型产能都是多层，故而需要将单层和多层合并
     * 获取产能分布组Key
     * 寸口|*|工装类别|*|成型法|*|胎体布层级
     * 胎体布层级动态 单层或是多层
     *
     * @param tireFabricNumber
     * @return
     */
    public String getTireFabricNumberCapacityGroupKey(Integer tireFabricNumber) {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, proSize, workWearType, mouldMethod, tireFabricNumber);
    }

}
