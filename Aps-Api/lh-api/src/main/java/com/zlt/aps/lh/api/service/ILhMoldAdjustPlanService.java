package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldAdjustPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫化模具调整计划Service接口
 *
 * @author chen
 * @date 2022-03-23
 */
@FeignClient(contextId = "ILhMoldAdjustPlanService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhMoldAdjustPlanService {

    /**
     * 查询硫化模具调整计划列表
     */
    @ApiOperation("查询硫化模具调整计划列表")
    @PostMapping("/moldAdjustPlan/list")
    TableDataInfo list(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 新增硫化模具调整计划
     */
    @ApiOperation("新增硫化模具调整计划")
    @PostMapping("/moldAdjustPlan/add")
    AjaxResult add(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 修改硫化模具调整计划
     */
    @ApiOperation("修改硫化模具调整计划")
    @PostMapping("/moldAdjustPlan/edit")
    AjaxResult edit(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 删除硫化模具调整计划
     */
    @ApiOperation("删除硫化模具调整计划")
    @DeleteMapping("/moldAdjustPlan/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/moldAdjustPlan/{id}")
    LhMoldAdjustPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验硫化模具调整计划唯一性
     */
    @ApiOperation("校验硫化模具调整计划唯一性")
    @PostMapping("/moldAdjustPlan/checkLhMoldAdjustPlanUnique")
    String checkLhMoldAdjustPlanUnique(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 导出硫化模具调整计划列表
     */
    @ApiOperation("导出硫化模具调整计划列表")
    @PostMapping("/moldAdjustPlan/getList")
    List<LhMoldAdjustPlan> getList(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan);

    /**
     * 导入硫化模具调整计划数据
     */
    @ApiOperation("导入硫化模具调整计划")
    @PostMapping("/moldAdjustPlan/importData")
    public AjaxResult importData(@RequestBody List<LhMoldAdjustPlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
