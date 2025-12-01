package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.GlueDemandPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlan;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分厂胶料需求计划Service接口
 * @author chen
 * @date 2022-04-18
 */
@FeignClient(contextId = "IGlueDemandPlanService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueDemandPlanService {

    /**
     * 查询分厂胶料需求计划列表
     */
    @PostMapping("/glueDemandPlan/list")
    TableDataInfo listGlueDemandPlan(@RequestBody GlueDemandPlan glueDemandPlan);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/glueDemandPlan/{id}")
    GlueDemandPlan getGlueDemandPlanInfo(@PathVariable("id") Long id);

    /**
    * 保存分厂胶料需求计划信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/glueDemandPlan/save")
    AjaxResult saveGlueDemandPlan(@RequestBody GlueDemandPlan glueDemandPlan);

    /**
     * 批量删除分厂胶料需求计划
     */
    @PostMapping("/glueDemandPlan/delete/{ids}")
    AjaxResult deleteGlueDemandPlan(@PathVariable("ids") Long[] ids);

    /**
     * 校验分厂胶料需求计划唯一性
     */
    @ApiOperation("校验分厂胶料需求计划唯一性")
    @PostMapping("/glueDemandPlan/checkGlueDemandPlanUnique")
    String checkGlueDemandPlanUnique(@RequestBody GlueDemandPlan glueDemandPlan);

    /**
     * 导出分厂胶料需求计划列表
     */
    @PostMapping("/glueDemandPlan/exportData")
    byte[] exportData(@RequestBody GlueDemandPlanExportDictDto dictDto);

    /**
     * 导入分厂胶料需求计划数据
     */
    @ApiOperation("导入分厂胶料需求计划")
    @PostMapping("/glueDemandPlan/importData")
    public AjaxResult importData(@RequestBody List<GlueDemandPlanInit> list, @RequestParam("importLogId") Long importLogId, @RequestParam("isSkip") Boolean isSkip);

    /**
     * 拆分需求计划
     * @param list 拆分后的数据
     * @param id 要拆分的数据id
     * @return 结果
     */
    @ApiOperation("拆分需求计划")
    @PostMapping("/glueDemandPlan/splitPlan")
    public AjaxResult splitPlan(@RequestBody List<GlueDemandPlan> list, @RequestParam("id") Long id);

    /**
     * 重新匹配密炼区
     *
     * @param glueDemandPlan 需要重新匹配的计划日期
     * @return 结果
     */
    @ApiOperation("重新匹配密炼区")
    @PostMapping("/glueDemandPlan/rematch")
    public AjaxResult rematch(@RequestBody GlueDemandPlan glueDemandPlan);

    /**
     * 检测对应日期和分厂的数据是否存在
     *
     * @param glueDemandPlan 日期和分厂
     * @return 是否唯一的常量值
     */
    @ApiOperation("检测对应日期和分厂的数据是否存在")
    @PostMapping("/glueDemandPlan/checkPlanDateAndFactoryExist")
    String checkPlanDateAndFactoryExist(@RequestBody GlueDemandPlan glueDemandPlan);
}
