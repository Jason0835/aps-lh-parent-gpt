package com.zlt.aps.monthplan.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.dto.FactoryFinalVersionQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanSaleRequirePlanVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 工厂月度计划控制台业务
 *
 * @author ZLT
 * @date 20250213
 */
@FeignClient(contextId = "IFactoryConsoleRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryConsoleRemoteService {

    /**
     * 查询分厂的月份排产计划
     *
     * @param queryCondition 查询条件
     * @return 结果集合
     */
    @ApiOperation("查询工厂的月份排产计划----分厂同一计划可写入多个版本")
    @PostMapping("/factoryConsole/productionVersionList")
    TableDataInfo getProductionVersionList(@RequestBody FactoryProductionPlanVo queryCondition);

    /**
     * 查询分厂月份对应还没选择的需求计划版本列表
     *
     * @param queryCondition 查询条件
     * @return 结果集合
     */
    @ApiOperation("查询分厂月份对应还没选择的需求计划版本列表")
    @PostMapping("/factoryConsole/getNoSelectedVersionList")
    TableDataInfo getNoSelectedVersionList(@RequestBody FactoryProductionPlanVo queryCondition);

    /**
     * 确认对工厂 + 年月 + 需求计划版本进行工厂排产
     *
     * @param confirmParam 需求信息
     * @return 结果信息
     */
    @ApiOperation("查询分厂月份对应还没选择的需求计划版本列表")
    @PostMapping("/factoryConsole/confirmProductionRequireVersion")
    AjaxResult confirmProductionRequireVersion(@RequestBody FactoryProductionPlanVo confirmParam);

    /**
     * 创建导入模板的版本信息，主要获取版本周期
     *
     * @param param 分厂编码、年份、月份
     * @return
     */
    @ApiOperation("根据分厂、年、月获取其周期信息")
    @PostMapping("/factoryConsole/createImportVersion")
    MpFactoryProductionVersion createImportVersion(@RequestBody FactoryProductionParamVo param);

    /**
     * 按分厂+年月的方式生成销售需求月度计划
     *
     * @param createCondition 条件
     * @return 结果
     */
    @ApiOperation("按分厂+年月的方式生成销售需求月度计划----可多次生成多个版本")
    @PostMapping("/factoryConsole/createSaleRequirePlan")
    AjaxResult createSaleRequirePlan(@RequestBody MonthPlanSaleRequirePlanVo createCondition);

    /**
     * 按分厂+日期，获取对应的定稿版本信息
     *
     * @param queryCondition 条件
     * @return 结果
     */
    @ApiOperation("按分厂+ 日期获取分厂的定稿排产版本信息")
    @PostMapping("/factoryConsole/getFinalVersionInfo")
    AjaxResult getFinalVersion(@RequestBody FactoryFinalVersionQueryDto queryCondition);

    /**
     * 按分厂 + 年月 + 需求计划版本的方式初始化分厂排产信息
     *
     * @param factoryProductionParam 分厂排产参数
     * @return
     */
    @ApiOperation("按分厂 + 年月 + 需求计划版本的方式初始化分厂排产")
    @PostMapping("/factoryConsole/initFactoryProduction")
    AjaxResult initFactoryProduction(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 按分厂 + 年月 + 排产版本的方式进行分厂排产
     *
     * @param factoryProductionParam 分厂排产参数
     * @return
     */
    @ApiOperation("按分厂 + 年月 + 排产版本的方式分厂排产模具")
    @PostMapping("/factoryConsole/factoryMouldingProduction")
    AjaxResult factoryMouldingProduction(@RequestBody FactoryProductionParamVo factoryProductionParam);

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
     * 按分厂 + 年月 + 需求版本的方式删除需求计划版本及对应的排产版本
     *
     * @param factoryProductionParam
     * @return
     */
    @ApiOperation("按分厂 + 年月 + 需求版本的方式删除需求计划版本及对应的排产版本")
    @PostMapping("/factoryConsole/deleteMonthPlanRequire")
    AjaxResult deleteMonthPlanRequire(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 按分厂 + 年月 + 排产版本的方式删除排产计划版本
     *
     * @param factoryProductionParam
     * @return
     */
    @ApiOperation("按分厂 + 年月 + 排产版本的方式删除排产计划版本")
    @PostMapping("/factoryConsole/deleteMonthPlanProductionVersion")
    AjaxResult deleteMonthPlanProductionVersion(@RequestBody FactoryProductionParamVo factoryProductionParam);
}
