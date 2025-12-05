package com.zlt.aps.monthplan.factory.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanVersionVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanSaleRequirePlanVo;
import com.zlt.aps.monthplan.factory.dto.FactoryProductionPlanVersionDto;

import java.util.Date;
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
     * 按分厂 + 年月的方式生成销售需求月度计划
     * 1、获取分厂、年、月提报的销售需求订单
     * 2、根据重要客户、是否必保计划，更新其重要客户、必保计划标记
     * 3、获取库存信息记录，并根据库存对冲顺序进行库存分配
     * 4、记录库存分配结果
     * 5、获取备货计划、最小批量的上调控制水位及最小批量值得到最终计划需求量
     *
     * @param createCondition
     * @return
     */
    AjaxResult createSaleRequirePlan(MonthPlanSaleRequirePlanVo createCondition);

    /**
     * 一键排产，生产模具排产计划
     * 1.第一步初始化
     * 2.第二步模具排产
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult factoryWholeCourseProduction(FactoryProductionParamVo factoryProductionParam);

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
