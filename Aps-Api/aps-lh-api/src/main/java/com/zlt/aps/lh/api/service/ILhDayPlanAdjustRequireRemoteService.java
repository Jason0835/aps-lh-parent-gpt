package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 硫化日计划调整需求远程服务。
 */
@FeignClient(contextId = "ILhDayPlanAdjustRequireRemoteService",
        value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhDayPlanAdjustRequireRemoteService {

    /**
     * 查询硫化日计划调整需求列表。
     *
     * @param queryVO 查询条件
     * @return 分页列表
     */
    @ApiOperation("查询硫化日计划调整需求列表")
    @PostMapping("/lhDayPlanAdjustRequire/list")
    TableDataInfo list(@RequestBody LhDayPlanAdjustRequire queryVO);

    /**
     * 保存当前行三次调整。
     *
     * @param entity 当前行数据
     * @return 保存结果
     */
    @ApiOperation("保存硫化日计划调整需求")
    @PostMapping("/lhDayPlanAdjustRequire/save")
    AjaxResult save(@RequestBody LhDayPlanAdjustRequire entity);
}
