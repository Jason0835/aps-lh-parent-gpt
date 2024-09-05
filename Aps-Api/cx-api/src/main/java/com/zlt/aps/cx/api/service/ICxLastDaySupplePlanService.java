package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * 成型前日计划增补Service接口
 * @author chen
 * @date 2022-02-09
 */
@FeignClient(contextId = "ICxLastDaySupplePlanService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxLastDaySupplePlanService {

    /**
     * 查询成型前日计划增补列表
     */
    @ApiOperation("查询成型前日计划增补列表")
    @PostMapping("/lastDaySupplyPlan/list")
    public TableDataInfo list(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 根据id查询成型前日计划增补
     */
    @ApiOperation("根据id查询成型前日计划增补")
    @PostMapping("/lastDaySupplyPlan/changeMachine/{id}")
    public CxLastDaySupplePlanDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改成型前日计划增补
     */
    @ApiOperation("修改成型前日计划增补")
    @PostMapping("/lastDaySupplyPlan/edit")
    public AjaxResult edit(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 修改成型前日计划增补机台
     */
    @ApiOperation("修改成型前日计划增补机台")
    @PostMapping("/lastDaySupplyPlan/changeMachine")
    public AjaxResult changeMachine(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 删除成型前日计划增补
     */
    @ApiOperation("删除成型前日计划增补")
    @DeleteMapping("/lastDaySupplyPlan/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 生成成型前日计划增补
     */
    @ApiOperation("生成成型前日计划增补")
    @PostMapping("/lastDaySupplyPlan/generateSupplyPlan")
    public AjaxResult generateSupplyPlan(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlanDto);

    /**
     * 生成成型前日计划增补
     */
    @ApiOperation("重新生成成型前日计划增补")
    @PostMapping("/lastDaySupplyPlan/regenerateSupplyPlan")
    public AjaxResult regenerateSupplyPlan(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlanDto);

    /**
     * 确认成型前日计划增补
     */
    @ApiOperation("确认成型前日计划增补")
    @PostMapping("/lastDaySupplyPlan/confirmSupplyPlan")
    public AjaxResult confirmSupplyPlan(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlanDto);

    /**
     * 新增成型前日增补计划
     *
     * @param cxScheduleResult 前日增补计划
     * @return 结果
     */
    @PostMapping("/lastDaySupplyPlan/insertCxLastDaySupplePlan")
    public AjaxResult insertCxLastDaySupplePlan(@RequestBody CxLastDaySupplePlanDto cxLastDaySupplePlan);

    /**
     * 校验-使用模数
     */
    @PostMapping("/lastDaySupplyPlan/modifyMoldsValidate")
    public AjaxResult modifyMoldsValidate(@RequestBody CxLastDaySupplePlanDto cxScheduleResult);

    /**
     * 修改-使用模数
     */
    @PostMapping("/lastDaySupplyPlan/modifyMolds")
    public AjaxResult modifyMolds(@RequestBody CxLastDaySupplePlanDto cxScheduleResult);
}
