package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixRubberPlan;
import com.zlt.aps.lh.service.MixRubberPlanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胶料日计划计划Controller
 *
 * @author zlt
 * @date 2021-11-10
 */
@RestController
@RequestMapping("/rubberPlan")
public class MixRubberPlanController extends BaseController
{
    @Autowired
    private MixRubberPlanService mixRubberPlanService;

    /**
     * 查询胶料日计划计划列表
     */
    @ApiOperation("查询胶料日计划计划列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixRubberPlan mixRubberPlan)
    {
        startPage();
        mixRubberPlan.setOrderStr(orderStr());
        List<MixRubberPlan> list = mixRubberPlanService.selectMixRubberPlanList(mixRubberPlan);
        return getDataTable(list);
    }

    /**
     * 获取胶料日计划计划详细信息
     */
    @ApiOperation("获取胶料日计划计划详细信息")
    @GetMapping(value = "/{id}")
    public MixRubberPlan getInfo(@PathVariable("id") Long id){
        return mixRubberPlanService.selectMixRubberPlanById(id);
    }

    /**
     * 新增胶料日计划计划
     */
    @Log(title = "ui.data.column.rubberPlan.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增胶料日计划计划")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixRubberPlan mixRubberPlan){
        return toAjax(mixRubberPlanService.insertMixRubberPlan(mixRubberPlan));
    }

    /**
     * 修改胶料日计划计划
     */
    @Log(title = "ui.data.column.rubberPlan.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胶料日计划计划")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixRubberPlan mixRubberPlan){
        return toAjax(mixRubberPlanService.updateMixRubberPlan(mixRubberPlan));
    }

    /**
     * 删除胶料日计划计划
     */
    @Log(title = "ui.data.column.rubberPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除胶料日计划计划")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixRubberPlanService.deleteMixRubberPlanByIds(ids));
    }

    /**
     * 导出胶料日计划计划列表
     */
    @Log(title = "ui.data.column.rubberPlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胶料日计划计划列表")
    @PostMapping("/getList")
    public List<MixRubberPlan> getList(@RequestBody MixRubberPlan mixRubberPlan){
        startPage();
        mixRubberPlan.setOrderStr(orderStr());
        return  mixRubberPlanService.selectMixRubberPlanList(mixRubberPlan);
    }

    /**
     * 校验胶料日计划计划唯一性
     */
    @ApiOperation("校验胶料日计划计划唯一性")
    @PostMapping("/checkMixRubberPlanUnique")
    public String checkMixRubberPlanUnique(@RequestBody MixRubberPlan mixRubberPlan){
        return mixRubberPlanService.checkMixRubberPlanUnique(mixRubberPlan);
    }

    /**
     * 根据集合导入胶料日计划计划数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.rubberPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入胶料日计划计划数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixRubberPlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixRubberPlanService.importData(list, updateSupport, importLogId);
    }
}
