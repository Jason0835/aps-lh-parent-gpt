package com.zlt.aps.factory.service;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.CycleStructureMinLhMachineQtyVo;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;

import java.util.List;

/**
 * 排产调用数据获取服务类
 * 月份排产计划 - 需求数据获取接口定义类
 *
 * @author ZLT
 * @date 20260204
 */
public interface DpRequireDataService {

    /**
     * 根据查询条件，获取分厂的排产制造需求计划数据
     *
     * @param context 排产上下文
     * @return
     */
    List<DpDemandPlan> getFactoryMonthPlan(Context context);

    /**
     * 获取周期结构的最低硫化配比信息
     *
     * @param context 排产上下文
     * @return
     */
    List<CycleStructureMinLhMachineQtyVo> getCycleLhRatioInfo(Context context);

}
