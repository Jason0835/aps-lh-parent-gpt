package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 月底补量排产参数对象信息
 *
 * @author ZLT
 * @date 20260128
 */
@Getter
public class BoostProductionInfoHelper implements Serializable {
    /**
     * 补量计划-随意一条
     */
    private MonthPlanProductionRequirePlanVo productionSkuInfo;
    /**
     * 硫化组信息
     */
    private CxLhProductionHelper cxLhGroup;
    /**
     * 使用的成型机信息
     */
    private Set<String> cxMachineInfoSet;
    /**
     * 机台-单机台使用
     */
    private CxMachineBaseInfoVo cxMachineInfo;
    /**
     * 标记单台
     */
    private boolean isSingleCxMachine;
    /**
     * 分组计划
     */
    private ProductionPlanGroupInfo productionPlanInfo;
    /**
     * 理论开始补量日
     */
    private Integer startBoostDay;
    /**
     * 补量已排产量
     */
    private Integer startPlannedQty;
    /**
     * 结束天数-结构结尾天数
     */
    private Integer endBoostDay;
    /**
     * 起始天是否已经排产完毕
     */
    private boolean startFinish;
    /**
     * 补量使用的模具
     */
    private List<ProductionMouldInfoVo> doubleMouldList;

    /**
     * 构建对象
     *
     * @param productionSkuInfo 补量的计划
     * @param doubleMouldList   使用的模具
     * @param cxLhGroup         硫化组
     * @param cxMachineInfoSet  使用的成型机
     * @param startBoostDay     开始补量日
     * @param startPlannedQty   开始补量已排产量
     * @param startFinish       开始补量日是否已排产完毕
     * @param endBoostDay       补量结束日
     * @return
     */
    public static BoostProductionInfoHelper builder(MonthPlanProductionRequirePlanVo productionSkuInfo,
                                                    List<ProductionMouldInfoVo> doubleMouldList,
                                                    ProductionPlanGroupInfo productionPlanInfo,
                                                    CxMachineBaseInfoVo cxMachineInfo,
                                                    CxLhProductionHelper cxLhGroup,
                                                    Set<String> cxMachineInfoSet,
                                                    Integer startBoostDay, Integer startPlannedQty, boolean startFinish,
                                                    Integer endBoostDay) {
        BoostProductionInfoHelper boostInfo = new BoostProductionInfoHelper();
        boostInfo.productionSkuInfo = productionSkuInfo;
        boostInfo.doubleMouldList = doubleMouldList;
        boostInfo.productionPlanInfo = productionPlanInfo;
        boostInfo.cxMachineInfo = cxMachineInfo;
        if (null != cxMachineInfo) {
            boostInfo.isSingleCxMachine = true;
        } else {
            boostInfo.isSingleCxMachine = false;
        }
        boostInfo.cxLhGroup = cxLhGroup;
        boostInfo.cxMachineInfoSet = cxMachineInfoSet;
        boostInfo.startBoostDay = startBoostDay;
        boostInfo.startPlannedQty = startPlannedQty;
        boostInfo.startFinish = startFinish;
        boostInfo.endBoostDay = endBoostDay;
        return boostInfo;
    }
}
