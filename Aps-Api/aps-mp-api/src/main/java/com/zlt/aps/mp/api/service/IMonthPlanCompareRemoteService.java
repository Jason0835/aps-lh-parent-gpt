package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.dto.MonthPlanCompareDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 月计划与实际产量对比报表 Feign 远程服务接口
 *
 * @author APS
 * @date 2026-08-13
 */
@FeignClient(contextId = "IMonthPlanCompareRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMonthPlanCompareRemoteService {

    /**
     * 查询月计划与实际产量对比列表
     *
     * @param queryDto 查询条件
     * @return TableDataInfo 结果
     */
    @ApiOperation("查询月计划与实际产量对比列表")
    @PostMapping("/monthPlanCompare/list")
    TableDataInfo listMonthPlanCompare(@RequestBody MonthPlanCompareDto queryDto);

    /**
     * 导出月计划与实际产量对比数据
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return Excel 文件字节数组
     */
    @ApiOperation("导出月计划与实际产量对比数据")
    @PostMapping("/monthPlanCompare/export")
    byte[] exportMonthPlanCompare(@RequestBody MonthPlanCompareDto entity, @RequestParam("fileName") String fileName);
}
