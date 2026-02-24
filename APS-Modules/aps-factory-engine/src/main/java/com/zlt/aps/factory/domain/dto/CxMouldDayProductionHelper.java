package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Set;

/**
 * 成型模具日排产信息对象
 *
 * @author ZLT
 * @date 20251219
 */
@Data
public class CxMouldDayProductionHelper implements Serializable {
    /**
     * 成型机台
     */
    private String cxMachineCode;
    /**
     * 硫化组编号
     */
    private String lhGroupNo;
    /**
     * 型腔模号
     */
    private String mouldCode;
    /**
     * 需求计划ID 可是需求计划或是试制量试计划
     */
    private Long monthPlanId;
    /**
     * MES物料编码
     */
    private String mesMaterialCode;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 产品结构
     */
    private String structureName;
    /**
     * 成型法
     */
    private String mouldMethod;
    /**
     * 规格代号
     */
    private String specCode;
    /**
     * 生胎代码
     */
    private String embryoCode;
    /**
     * 英寸
     */
    private String proSize;
    /**
     * 规格
     */
    private String specifications;
    /**
     * 主花纹
     */
    private String mainPattern;
    /**
     * 花纹
     */
    private String pattern;
    /**
     * 排产数量
     */
    private Integer productionQty;
    /**
     * 排产日 1~31
     */
    private Integer productionDate;

    /**
     * 构建成型硫化组模具日排产信息
     *
     * @param groupPlan            排产计划
     * @param cxMachineCode        成型机台
     * @param day                  排产日
     * @param productionQty        双模排产量
     * @param cxLhProductionHelper 硫化组信息
     * @return
     */
    @Deprecated
    public static CxMouldDayProductionHelper createCxMouldDayProductionInfo(MonthPlanProductionRequirePlanVo groupPlan, String cxMachineCode, Integer day, Integer productionQty, CxLhProductionHelper cxLhProductionHelper) {
        CxMouldDayProductionHelper mouldProductionHelper = new CxMouldDayProductionHelper();
        BeanUtils.copyProperties(groupPlan, mouldProductionHelper);
        mouldProductionHelper.setMonthPlanId(groupPlan.getMonthPlanId());
        mouldProductionHelper.setCxMachineCode(cxMachineCode);
        mouldProductionHelper.setProductionDate(day);
        mouldProductionHelper.setProductionQty(productionQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION);
        mouldProductionHelper.setLhGroupNo(String.valueOf(cxLhProductionHelper.getLhGroupNo()));
        return mouldProductionHelper;
    }

    /**
     * 构建成型硫化组模具日排产信息
     *
     * @param groupPlan         排产计划
     * @param cxMachineCodeInfo 成型机台
     * @param day               排产日
     * @param productionQty     双模排产量
     * @return
     */
    public static CxMouldDayProductionHelper createCxMouldDayProductionInfo(MonthPlanProductionRequirePlanVo groupPlan, Set<String> cxMachineCodeInfo, Integer day, Integer productionQty) {
        CxMouldDayProductionHelper mouldProductionHelper = new CxMouldDayProductionHelper();
        BeanUtils.copyProperties(groupPlan, mouldProductionHelper);
        mouldProductionHelper.setMonthPlanId(groupPlan.getMonthPlanId());
        mouldProductionHelper.setCxMachineCode(String.join(StringConstant.COMMA, cxMachineCodeInfo));
        mouldProductionHelper.setProductionDate(day);
        mouldProductionHelper.setProductionQty(productionQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION);
        return mouldProductionHelper;
    }
}
