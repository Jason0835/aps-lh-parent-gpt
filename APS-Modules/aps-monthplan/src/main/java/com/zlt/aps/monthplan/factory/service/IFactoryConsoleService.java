package com.zlt.aps.monthplan.factory.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanVersionVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.monthplan.factory.dto.FactoryProductionPlanVersionDto;

import java.util.List;

/**
 * 分厂控制台业务接口定义类
 *
 * @author ZLT
 * @date 20250211
 */
public interface IFactoryConsoleService {
    /**
     * 根据条件，获取对应分厂的销售需求计划版本及排产版本信息列表
     *
     * @param queryCondition
     * @return
     */
    List<FactoryProductionPlanVersionDto> getProductionVersionList(FactoryProductionPlanVo queryCondition);

    /**
     * 根据条件，获取对应分厂的还未选择的销售需求计划版本信息
     *
     * @param queryCondition
     * @return
     */
    List<FactoryMonthPlanVersionVo> getNoSelectedVersionList(FactoryProductionPlanVo queryCondition);

    /**
     * 一键排产，生产模具排产计划
     * 1.第一步初始化
     * 2.第二步排分组产能(结构)
     * 3.第三步模具排产
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult oneClickProductionProcess(FactoryProductionParamVo factoryProductionParam);

    /**
     * 重新初始化模具排产的初始化数据
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult reinitializeMouldingProduction(FactoryProductionParamVo factoryProductionParam);

    /**
     * 重新进行模具排产
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult reMouldingProduction(FactoryProductionParamVo factoryProductionParam);

    /**
     * 按分厂+年月+需求版本的方式，删除对应的需求制造计划及排产版本
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult deleteMonthPlanRequire(FactoryProductionParamVo factoryProductionParam);

    /**
     * 按分厂 + 年月 + 排产版本的方式删除排产计划版本
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult deleteMonthPlanProductionVersion(FactoryProductionParamVo factoryProductionParam);
}
