package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixTakePlan;


/**
 * 支领计划Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixTakePlanService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixTakePlanService {

    /**
     * 查询支领计划列表
     */
    @ApiOperation("查询支领计划列表")
    @PostMapping("/take/list")
    TableDataInfo list(@RequestBody MixTakePlan mixTakePlan);

    /**
    * 新增支领计划
    */
    @ApiOperation("新增支领计划")
    @PostMapping("/take/add")
    AjaxResult add(@RequestBody MixTakePlan mixTakePlan);

    /**
     * 修改支领计划
     */
    @ApiOperation("修改支领计划")
    @PostMapping("/take/edit")
    AjaxResult edit(@RequestBody MixTakePlan mixTakePlan);

    /**
     * 删除支领计划
     */
    @ApiOperation("删除支领计划")
    @DeleteMapping("/take/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/take/{id}")
    MixTakePlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验支领计划唯一性
     */
    @ApiOperation("校验支领计划唯一性")
    @PostMapping("/take/checkMixTakePlanUnique")
    String checkMixTakePlanUnique(@RequestBody MixTakePlan mixTakePlan);

    /**
     * 导出支领计划列表
     */
    @ApiOperation("导出支领计划列表")
    @PostMapping("/take/getList")
    List<MixTakePlan> getList(@RequestBody MixTakePlan mixTakePlan);

    /**
     * 导入支领计划数据
     */
    @ApiOperation("导入支领计划")
    @PostMapping("/take/importData")
    public AjaxResult importData(@RequestBody List<MixTakePlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
