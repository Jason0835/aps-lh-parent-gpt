package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.vo.FactoryProductionParamVo;
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
@FeignClient(contextId = "monthPlanProductionService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryMonthProductionRemoteService {

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
     * 按工厂 + 年月 + 排产版本的方式进行分厂排产
     *
     * @param factoryProductionParam 分厂排产参数
     * @return
     */
    @ApiOperation("按工厂 + 年月 + 排产版本的方式分厂排产模具")
    @PostMapping("/factoryConsole/rescheduleMouldingProduction")
    AjaxResult rescheduleMouldingProduction(@RequestBody FactoryProductionParamVo factoryProductionParam);

}
