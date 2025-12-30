package com.zlt.aps.factory.domain.dto;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Set;

/**
 * 分组计划 - TBR为结构，PCR为英寸(寸口、寸别)
 * 日排产信息对象
 * 用于判断日排产是否达到胎胚种类数，硫化配比数等
 *
 * @author ZLT
 * @date 20251229
 */
@Data
public class GroupPlanDayProductionInfoHelper implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 硫化组编号
     */
    private Integer lhGroupNo;
    /**
     * 分组计划类型
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
     * 生胎代码
     */
    private String embryoCode;
    /**
     * 计划ID
     */
    private Long monthPlanId;
    /**
     * 使用的模具信息集合-双模
     */
    private Set<String> mouldCodeInfoSet;
    /**
     * 日排产量
     */
    private Long productionQty;
    /**
     * 日损耗量：换模或是换活字块损耗
     * 不到具体的计划ID
     */
    private Long lossQty;
    /**
     * 成型机台信息
     */
    private Set<String> cxMachineInfoSet;
    /**
     * 是否虚单--搭配排产
     */
    private String isVirtual;

    /**
     * 构建日排产信息
     *
     * @param groupPlan            排产计划
     * @param cxLhProductionHelper 硫化组
     * @param productionQty        排产量
     * @param lossQty              sku日损耗量
     * @param isVirtual            是否虚单 1 是 0 否
     * @return
     */
    public static GroupPlanDayProductionInfoHelper buildDayProductionInfo(MonthPlanProductionRequirePlanVo groupPlan, CxLhProductionHelper cxLhProductionHelper, Long productionQty, Long lossQty, String isVirtual) {
        GroupPlanDayProductionInfoHelper productionInfo = new GroupPlanDayProductionInfoHelper(groupPlan.getMonthPlanId(), cxLhProductionHelper.getProductionDay(), cxLhProductionHelper.getLhGroupNo(), groupPlan.getStructureName());
        //排产量
        productionInfo.setProductionQty(productionQty);
        productionInfo.setLossQty(lossQty);
        //物料信息、生胎信息
        productionInfo.setEmbryoCode(groupPlan.getEmbryoCode());
        productionInfo.setMaterialCode(groupPlan.getMaterialCode());
        productionInfo.setMaterialDesc(groupPlan.getMaterialDesc());
        //模具、成型信息
        productionInfo.setCxMachineInfoSet(cxLhProductionHelper.getCxMachineInfo());
        productionInfo.setMouldCodeInfoSet(cxLhProductionHelper.getProductionMouldSet());
        if (YesOrNoEnum.YES.getCode().equals(isVirtual)) {
            productionInfo.setIsVirtual(YesOrNoEnum.YES.getCode());
        } else {
            productionInfo.setIsVirtual(YesOrNoEnum.NO.getCode());
        }
        return productionInfo;
    }

    /**
     * 是否有效
     * 排产日、硫化组、分组名、物料描述不可为空
     *
     * @return
     */
    public boolean isEffective() {
        if (null == productionDay || null == lhGroupNo) {
            return false;
        }
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(materialDesc)) {
            return false;
        }
        return true;
    }

    /**
     * 在同一天同一个结构同一个硫化组同一个Sku只能有一条
     *
     * @return
     */
    public String getDuplicateKey() {
        String keyFormat = "%s|*|%s|*|%s|*|%s";
        return String.format(keyFormat, productionDay, lhGroupNo, groupName, materialDesc);
    }

    /**
     * 构造函数
     *
     * @param monthPlanId   排产计划
     * @param productionDay 排产日
     * @param lhGroupNo     硫化组编号
     * @param groupName     分组计划 TBR 结构 PCR 寸口
     */
    public GroupPlanDayProductionInfoHelper(Long monthPlanId, Integer productionDay, Integer lhGroupNo, String groupName) {
        this.monthPlanId = monthPlanId;
        this.productionDay = productionDay;
        this.lhGroupNo = lhGroupNo;
        this.groupName = groupName;
    }
}
