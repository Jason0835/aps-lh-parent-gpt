package com.zlt.aps.factory.deduct;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.Data;

import java.util.Set;

/**
 * 降模排产Vo
 *
 * @author Sandy
 * @date 2025/12/24
 */
@Data
public class DeductMouldVo {

    /**
     * SKU编码
     */
    private String materialCode;

    /**
     * 总需求量
     */
    private Integer totalQty;

    /**
     * 剩余未排产量
     */
    private Integer remainingQty;

    /**
     * 分配的机台数量
     */
    private Integer machinesAssigned;

    /**
     * 开始排产日
     */
    private Integer startDate;

    /**
     * 结构收尾日
     */
    private Integer deadline;

    /**
     * 单机台日产量
     */
    private Integer dailyOutputPerMachine;

    /**
     * 参数：分配的机台数，默认3
     */
    private Integer paramAssignedMachines = 3;

    /**
     * 参数：临近收尾天数7天，默认7
     */
    private Integer paramNearDeadline7 = 7;

    /**
     * 参数：临近收尾天数7天，降低的台数，默认3台
     */
    private Integer paramReduceMachines3 = 3;

    /**
     * 参数：临近收尾天数5天，默认5
     */
    private Integer paramNearDeadline5 = 5;

    /**
     * 参数：临近收尾天数5天，降低的台数，默认2台
     */
    private Integer paramReduceMachines2 = 2;

    /**
     * 参数：临近收尾天数2天，默认2
     */
    private Integer paramNearDeadline2 = 2;

    /**
     * 参数：临近收尾天数2天，降低的台数，默认1台
     */
    private Integer paramReduceMachines1 = 1;

    /**
     * 参数：开产日比例
     */
    private double paramStartDayRatio = 0.5;

    /**
     * 停工日集合
     */
    private Set<Integer> shutDownDaySet;

    /**
     * 开产日集合
     */
    private Set<Integer> productionStartDaySet;

    /**
     * 构建降膜排产参数对象
     *
     * @param deadLineDay        收尾日
     * @param stopDays           停工日集合
     * @param openDays           开产日集合
     * @param paramConfiguration 排产参数对象
     * @param continueSkuInfo    续作Sku信息
     * @return
     */
    public static DeductMouldVo createDeductMouldBySku(Integer deadLineDay, Set<Integer> stopDays, Set<Integer> openDays, ProductionCapacityParamConfiguration paramConfiguration, CxContinueSkuInfoHelper continueSkuInfo) {
        DeductMouldVo deductMould = new DeductMouldVo();
        //参数设置
        deductMould.setShutDownDaySet(stopDays);
        deductMould.setProductionStartDaySet(openDays);
        //降膜排产-相关的参数 当前的硫化机台数超过该值 默认为3
        deductMould.setParamAssignedMachines(paramConfiguration.getDeductMouldMinLhMachineCount());
        //降膜排产-相关的参数 7天时降到3台
        deductMould.setParamNearDeadline7(paramConfiguration.getFirstNearDeadLineDay());
        deductMould.setParamReduceMachines3(paramConfiguration.getFirstNearDeadLineMaxLhMachineCount());
        //降膜排产-相关的参数 5天时降到2台
        deductMould.setParamNearDeadline5(paramConfiguration.getSecondNearDeadLineDay());
        deductMould.setParamReduceMachines2(paramConfiguration.getSecondNearDeadLineMaxLhMachineCount());
        //降膜排产-相关的参数 2天时降到1台
        deductMould.setParamNearDeadline2(paramConfiguration.getLastNearDeadLineDay());
        deductMould.setParamReduceMachines1(paramConfiguration.getLastNearDeadLineMaxLhMachineCount());

        //续作Sku信息 -
        deductMould.setMaterialCode(continueSkuInfo.getMaterialDesc());
        deductMould.setStartDate(ProductionConstant.MONTH_START_DAY);
        deductMould.setDeadline(deadLineDay);
        deductMould.setTotalQty(continueSkuInfo.getPlanDemandQty().intValue());
        Integer startLhMachineCount = continueSkuInfo.getMouldNumber() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        deductMould.setMachinesAssigned(startLhMachineCount);
        Integer dayLhCapacityQty = continueSkuInfo.getDayVulcanizationQty().intValue() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        deductMould.setDailyOutputPerMachine(dayLhCapacityQty);
        return deductMould;
    }
}
