package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixRubberPlan;


/**
 * 胶料日计划计划Service接口
 * @author zlt
 * @date 2021-11-10
 */
@FeignClient(contextId = "IMixRubberPlanService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixRubberPlanService {

    /**
     * 查询胶料日计划计划列表
     */
    @ApiOperation("查询胶料日计划计划列表")
    @PostMapping("/rubberPlan/list")
    TableDataInfo list(@RequestBody MixRubberPlan mixRubberPlan);

    /**
    * 新增胶料日计划计划
    */
    @ApiOperation("新增胶料日计划计划")
    @PostMapping("/rubberPlan/add")
    AjaxResult add(@RequestBody MixRubberPlan mixRubberPlan);

    /**
     * 修改胶料日计划计划
     */
    @ApiOperation("修改胶料日计划计划")
    @PostMapping("/rubberPlan/edit")
    AjaxResult edit(@RequestBody MixRubberPlan mixRubberPlan);

    /**
     * 删除胶料日计划计划
     */
    @ApiOperation("删除胶料日计划计划")
    @DeleteMapping("/rubberPlan/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rubberPlan/{id}")
    MixRubberPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验胶料日计划计划唯一性
     */
    @ApiOperation("校验胶料日计划计划唯一性")
    @PostMapping("/rubberPlan/checkMixRubberPlanUnique")
    String checkMixRubberPlanUnique(@RequestBody MixRubberPlan mixRubberPlan);

    /**
     * 导出胶料日计划计划列表
     */
    @ApiOperation("导出胶料日计划计划列表")
    @PostMapping("/rubberPlan/getList")
    List<MixRubberPlan> getList(@RequestBody MixRubberPlan mixRubberPlan);

    /**
     * 导入胶料日计划计划数据
     */
    @ApiOperation("导入胶料日计划计划")
    @PostMapping("/rubberPlan/importData")
    public AjaxResult importData(@RequestBody List<MixRubberPlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
