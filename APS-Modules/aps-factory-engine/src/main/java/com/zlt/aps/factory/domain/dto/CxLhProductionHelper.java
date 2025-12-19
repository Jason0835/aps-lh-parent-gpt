package com.zlt.aps.factory.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 成型配比下的硫化机台排产信息
 * 用以记录成型硫化配比的收尾排产信息，只保留最后一个信息
 *
 * @author ZLT
 * @date 20251219
 */
@Data
public class CxLhProductionHelper implements Serializable {
    /**
     * 硫化配比分组编号 1~最大组
     */
    private Integer lhGroupNo;
    /**
     * 分组信息--TBR = 结构名
     */
    private String groupName;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料描述
     */
    private String materialDesc;

    /**
     * 当天排产量
     */
    private Long productionQty;

    /**
     * 排产模具
     */
    private Set<String> productionMouldSet;

    /**
     * 排产天 周期第几天
     */
    private Integer productionDay;

    /**
     * 天日硫化量--满产
     */
    private Long dayMaxProductionQty;

    /**
     * 构建空的成型下硫化分组信息
     *
     * @param groupName 分组计划名-TBR为结构名
     * @param lhGroupNo 虚拟的硫化分组
     * @return
     */
    public static CxLhProductionHelper createEmptyLhGroup(String groupName, Integer lhGroupNo) {
        CxLhProductionHelper cxLh = new CxLhProductionHelper();
        cxLh.setGroupName(groupName);
        cxLh.setLhGroupNo(lhGroupNo);
        return cxLh;
    }
}
