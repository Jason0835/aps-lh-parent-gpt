package com.zlt.aps.monthplan.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.dto.FactoryFinalVersionQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionPlanVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 工厂月度计划
 * 排产控制台业务
 *
 * @author ZLT
 * @date 20251201
 */
@FeignClient(contextId = "IFactoryConsoleRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryConsoleRemoteService {

    /**
     * 查询工厂的月份排产计划
     *
     * @param queryCondition 查询条件
     * @return 结果集合
     */
    @ApiOperation("查询工厂的月份排产计划----工厂同一计划可写入多个版本")
    @PostMapping("/factoryConsole/productionVersionList")
    TableDataInfo getProductionVersionList(@RequestBody FactoryProductionPlanVo queryCondition);

    /**
     * 查询工厂月份对应还没选择的需求计划版本列表
     *
     * @param queryCondition 查询条件
     * @return 结果集合
     */
    @ApiOperation("查询工厂月份对应还没选择的需求计划版本列表")
    @PostMapping("/factoryConsole/getNoSelectedVersionList")
    TableDataInfo getNoSelectedVersionList(@RequestBody FactoryProductionPlanVo queryCondition);

    /**
     * 确认对工厂 + 年月 + 需求计划版本进行工厂排产
     *
     * @param confirmParam 需求信息
     * @return 结果信息
     */
    @ApiOperation("查询工厂月份对应还没选择的需求计划版本列表")
    @PostMapping("/factoryConsole/confirmProductionRequireVersion")
    AjaxResult confirmProductionRequireVersion(@RequestBody FactoryProductionPlanVo confirmParam);

    /**
     * 按工厂 + 年月 + 需求版本的方式进行工厂一键排产
     * 初始化->排结构->排模具
     *
     * @param factoryProductionParam 工厂排产参数
     * @return
     */
    @ApiOperation("按工厂 + 年月 + 需求版本的方式进行工厂一键排产 初始化->排结构->排模具")
    @PostMapping("/factoryConsole/oneClickProductionProcess")
    AjaxResult oneClickProductionProcess(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 按工厂 + 年月 + 需求版本 + 排产版本的方式进行排产数据的重新初始化
     *
     * @param factoryProductionParam 分厂排产参数
     * @return
     */
    @ApiOperation("按工厂 + 年月 + 需求版本 + 排产版本的方式进行排产数据的重新初始化")
    @PostMapping("/factoryConsole/resetConfigurationInitProduction")
    AjaxResult resetConfigurationInitProduction(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 按工厂 + 年月 + 需求版本 + 排产版本的方式进行分组计划产能分配重新排产
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/factoryConsole/resetGroupAllocationCapacityProduction")
    @ApiOperation("按工厂 + 年月 + 需求版本 + 排产版本的方式进行分组计划产能分配重新排产")
    AjaxResult resetGroupAllocationCapacityProduction(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 创建导入模板的版本信息，主要获取版本周期
     *
     * @param param 分厂编码、年份、月份
     * @return
     */
    @ApiOperation("根据工厂、年、月获取其周期信息")
    @PostMapping("/factoryConsole/createImportVersion")
    MpFactoryProductionVersion createImportVersion(@RequestBody FactoryProductionParamVo param);

    /**
     * 按工厂+日期，获取对应的定稿版本信息
     *
     * @param queryCondition 条件
     * @return 结果
     */
    @ApiOperation("按工厂+ 日期获取分厂的定稿排产版本信息")
    @PostMapping("/factoryConsole/getFinalVersionInfo")
    AjaxResult getFinalVersion(@RequestBody FactoryFinalVersionQueryDto queryCondition);

    /**
     * 按工厂 + 年月 + 排产版本的方式进行分厂排产
     *
     * @param factoryProductionParam 分厂排产参数
     * @return
     */
    @ApiOperation("按工厂 + 年月 + 排产版本的方式分厂排产模具")
    @PostMapping("/factoryConsole/factoryMouldingProduction")
    AjaxResult factoryMouldingProduction(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 按工厂 + 年月 + 需求版本的方式删除需求计划版本及对应的排产版本
     *
     * @param factoryProductionParam
     * @return
     */
    @ApiOperation("按工厂 + 年月 + 需求版本的方式删除需求计划版本及对应的排产版本")
    @PostMapping("/factoryConsole/deleteMonthPlanRequire")
    AjaxResult deleteMonthPlanRequire(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 按工厂 + 年月 + 排产版本的方式删除排产计划版本
     *
     * @param factoryProductionParam
     * @return
     */
    @ApiOperation("按工厂 + 年月 + 排产版本的方式删除排产计划版本")
    @PostMapping("/factoryConsole/deleteMonthPlanProductionVersion")
    AjaxResult deleteMonthPlanProductionVersion(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 定稿
     *
     * @param factoryMonthPlanProdFinal
     * @return
     */
    @ApiOperation("定稿 - 年月+分厂+需求计划版本+分厂月计划版本")
    @PostMapping("/factoryConsole/finalized")
    AjaxResult finalized(@RequestBody FactoryMonthPlanProductionFinalResult factoryMonthPlanProdFinal);

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    @PostMapping("/factoryConsole/versionList")
    @ApiOperation("查询对应年月+分厂的需求计划版本")
    public AjaxResult versionList(@RequestBody MpFactoryProductionVersion query);

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     */
    @PostMapping("/factoryConsole/getProductionVersionList")
    @ApiOperation("查询对应年月+分厂+需求计划版本的分厂月计划版本")
    public AjaxResult getProductionVersionList(@RequestBody MpFactoryProductionVersion query);

    /**
     * 获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产
     *
     * @param condition 查询条件
     * @return 结果
     */
    @ApiOperation("获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产")
    @PostMapping("/factoryConsole/getProductionMonthType")
    AjaxResult getProductionMonthType(@RequestBody FactoryMonthPlanProdFinal condition);

    @ApiOperation("检测需求月计划排产")
    @PostMapping("/factoryConsole/checkProductionDemandPlan")
    public AjaxResult checkProductionDemandPlan(@RequestBody FactoryProductionParamVo factoryProductionParam);
}
