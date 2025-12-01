package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 汇总胶料需求计划Service接口
 * @author chen
 * @date 2022-04-25
 */
@FeignClient(contextId = "IGlueCollectPlanService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueCollectPlanService {

    /**
     * 查询汇总胶料需求计划列表
     */
    @PostMapping("/glueCollectPlan/list")
    TableDataInfo listGlueCollectPlan(@RequestBody GlueCollectPlan glueCollectPlan);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/glueCollectPlan/{id}")
    GlueCollectPlan getGlueCollectPlanInfo(@PathVariable("id") Long id);

    /**
    * 保存汇总胶料需求计划信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/glueCollectPlan/save")
    AjaxResult saveGlueCollectPlan(@RequestBody GlueCollectPlan glueCollectPlan);

    /**
     * 批量删除汇总胶料需求计划
     */
    @PostMapping("/glueCollectPlan/delete/{ids}")
    AjaxResult deleteGlueCollectPlan(@PathVariable("ids") Long[] ids);

    /**
     * 导出汇总胶料需求计划列表
     */
    @PostMapping("/glueCollectPlan/exportData")
    byte[] exportData(@RequestBody GlueCollectPlan glueCollectPlan);

    /**
     * 汇总胶料需求计划列表
     */
    @PostMapping("/glueCollectPlan/summaryPlan")
    AjaxResult summaryPlan(@RequestBody GlueCollectPlan glueCollectPlan);

    /**
     * 汇总胶料需求计划选机台
     */
    @PostMapping("/glueCollectPlan/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody GlueCollectPlan glueCollectPlan);

    /**
     * 检测对应日期和分厂的数据是否存在
     */
    @PostMapping("/glueCollectPlan/checkPlanDateExist")
    String checkPlanDateExist(@RequestBody GlueCollectPlan glueCollectPlan);
}
