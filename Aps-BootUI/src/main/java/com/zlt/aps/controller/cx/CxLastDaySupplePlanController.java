package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.service.ICxLastDaySupplePlanService;
import com.zlt.aps.cx.api.service.ICxParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 成型前日计划增补Controller
 * @author chen
 * @date 2022-02-09
 */
@Api(tags = "成型前日计划增补")
@Controller
@RequestMapping("/cx/lastDaySupplyPlan")
public class CxLastDaySupplePlanController extends BaseController {

    @Autowired
    private ICxLastDaySupplePlanService iCxLastDaySupplyPlanService;

    @Autowired
    private ICxParamsService iCxParamsService;

    /**
     * 跳转至前日增补计划页面
     */
    @GetMapping()
    public String lastDaySupplyPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));
        CxParamsDto dto = new CxParamsDto();
        dto.setParamCode("MINIMUM_LH_MACHINE_COM_RATIO");
        List<CxParamsDto> list = iCxParamsService.exportData(dto);
        String minimumLhMachine = "";
        if (CollectionUtils.isNotEmpty(list)) {
            minimumLhMachine = list.get(0).getParamValue();
        }
        mmap.put("minimumLhMachine", minimumLhMachine);

        dto.setParamCode("MONTH_PLAN_OS");
        List<CxParamsDto> list2 = iCxParamsService.exportData(dto);
        String monthPlanSurplusTip = "";
        if (CollectionUtils.isNotEmpty(list2)) {
            monthPlanSurplusTip = list2.get(0).getParamValue();
        }
        mmap.put("monthPlanSurplusTip", monthPlanSurplusTip);
        return "cx/cxScheduleResult/lastDaySupplyPlan";
    }

    /**
     * 转机台页面
     */
    @GetMapping("/changeMachine/{id}")
    public String changeMachine(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("editType", "3");
        mmap.put("cxScheduleResult", iCxLastDaySupplyPlanService.getInfo(id));
        return "cx/cxScheduleResult/changePlanOrMachine";
    }

    /**
     * 新增页面
     */
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("lastDaySupplyPlan", new CxLastDaySupplePlanDto());
        return "cx/cxScheduleResult/insertLastDaySupplyPlan";
    }

    /**
     * 修改硫化机台数页面
     */
    @GetMapping("/modifyLhMachineQty/{id}")
    public String modifyLhMachineQty(@PathVariable("id") Long id, ModelMap mmap) {
        CxLastDaySupplePlanDto lastDaySupplePlanDto = iCxLastDaySupplyPlanService.getInfo(id);
        mmap.put("cxScheduleResult", lastDaySupplePlanDto);
        return "cx/cxScheduleResult/supplyPlanModifyLhMachineQty";
    }

    /**
     * 前日增补计划列表
     */
    @ApiOperation("前日增补计划列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        return iCxLastDaySupplyPlanService.list(cxLastDaySupplePlanDto);
    }

    /**
     * 生成前日增补计划
     */
    @ApiOperation("生成前日增补计划")
    @PostMapping("/generateSupplyPlan")
    @ResponseBody
    public AjaxResult generateSupplyPlan(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        return iCxLastDaySupplyPlanService.generateSupplyPlan(cxLastDaySupplePlanDto);
    }

    /**
     * 重新生成前日增补计划
     */
    @ApiOperation("重新生成前日增补计划")
    @PostMapping("/regenerateSupplyPlan")
    @ResponseBody
    public AjaxResult regenerateSupplyPlan(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        return iCxLastDaySupplyPlanService.regenerateSupplyPlan(cxLastDaySupplePlanDto);
    }

    /**
     * 确认前日增补计划
     */
    @ApiOperation("确认前日增补计划")
    @PostMapping("/confirmSupplyPlan")
    @ResponseBody
    public AjaxResult confirmSupplyPlan(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        return iCxLastDaySupplyPlanService.confirmSupplyPlan(cxLastDaySupplePlanDto);
    }

    /**
     * 修改前日增补计划量
     */
    @ApiOperation("修改前日增补计划量")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult modifySupplyPlanQty(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        return iCxLastDaySupplyPlanService.edit(cxLastDaySupplePlanDto);
    }

    /**
     * 修改前日增补计划机台
     */
    @ApiOperation("修改前日增补计划机台")
    @RequiresPermissions("cx:lastDaySupplyPlan:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        return iCxLastDaySupplyPlanService.changeMachine(cxLastDaySupplePlanDto);
    }

    /**
     * 删除成型前日计划增补
     */
    @ApiOperation("删除成型前日计划增补（id不为空）")
    @RequiresPermissions("cx:lastDaySupplyPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeLastDaySupplyPlan(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxLastDaySupplyPlanService.remove(arr);
    }

    /**
     * 新增前日增补计划量
     */
    @ApiOperation("新增成型前日计划增补")
    @RequiresPermissions("cx:lastDaySupplyPlan:add")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxLastDaySupplePlanDto cxLastDaySupplePlanDto) {
        return iCxLastDaySupplyPlanService.insertCxLastDaySupplePlan(cxLastDaySupplePlanDto);
    }

    /**
     * 校验-使用模数
     */
    @PostMapping("/modifyMoldsValidate")
    @ResponseBody
    public AjaxResult modifyMoldsValidate(CxLastDaySupplePlanDto entity) {
        return iCxLastDaySupplyPlanService.modifyMoldsValidate(entity);
    }

    /**
     * 修改-使用模数
     */
    @ApiOperation("修改使用模数")
    @RequiresPermissions("cx:lastDaySupplyPlan:modifyLhMachineQty")
    @PostMapping("/modifyMolds")
    @ResponseBody
    public AjaxResult modifyMolds(CxLastDaySupplePlanDto entity) {
        return iCxLastDaySupplyPlanService.modifyMolds(entity);
    }

}
