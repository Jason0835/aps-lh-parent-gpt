package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixTakePlan;
import com.zlt.aps.lh.service.MixTakePlanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支领计划Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/take")
public class MixTakePlanController extends BaseController
{
    @Autowired
    private MixTakePlanService mixTakePlanService;

    /**
     * 查询支领计划列表
     */
    @ApiOperation("查询支领计划列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixTakePlan mixTakePlan)
    {
        startPage();
        mixTakePlan.setOrderStr(orderStr());
        List<MixTakePlan> list = mixTakePlanService.selectMixTakePlanList(mixTakePlan);
        return getDataTable(list);
    }

    /**
     * 获取支领计划详细信息
     */
    @ApiOperation("获取支领计划详细信息")
    @GetMapping(value = "/{id}")
    public MixTakePlan getInfo(@PathVariable("id") Long id){
        return mixTakePlanService.selectMixTakePlanById(id);
    }

    /**
     * 新增支领计划
     */
    @Log(title = "ui.data.column.take.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增支领计划")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixTakePlan mixTakePlan){
        return toAjax(mixTakePlanService.insertMixTakePlan(mixTakePlan));
    }

    /**
     * 修改支领计划
     */
    @Log(title = "ui.data.column.take.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改支领计划")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixTakePlan mixTakePlan){
        return toAjax(mixTakePlanService.updateMixTakePlan(mixTakePlan));
    }

    /**
     * 删除支领计划
     */
    @Log(title = "ui.data.column.take.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除支领计划")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixTakePlanService.deleteMixTakePlanByIds(ids));
    }

    /**
     * 导出支领计划列表
     */
    @Log(title = "ui.data.column.take.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出支领计划列表")
    @PostMapping("/getList")
    public List<MixTakePlan> getList(@RequestBody MixTakePlan mixTakePlan){
        startPage();
        mixTakePlan.setOrderStr(orderStr());
        return  mixTakePlanService.selectMixTakePlanList(mixTakePlan);
    }

    /**
     * 校验支领计划唯一性
     */
    @ApiOperation("校验支领计划唯一性")
    @PostMapping("/checkMixTakePlanUnique")
    public String checkMixTakePlanUnique(@RequestBody MixTakePlan mixTakePlan){
        return mixTakePlanService.checkMixTakePlanUnique(mixTakePlan);
    }

    /**
     * 根据集合导入支领计划数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.take.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入支领计划数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixTakePlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixTakePlanService.importData(list, updateSupport, importLogId);
    }
}
