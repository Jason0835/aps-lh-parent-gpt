package com.zlt.aps.cx.controller;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxMdmMonthProdPlan1;
import com.zlt.aps.cx.api.domain.entity.CxMdmMonthProdPlan2;
import com.zlt.aps.cx.api.domain.entity.Gante;
import com.zlt.aps.cx.api.domain.entity.MdmMonthProdPlan;
import com.zlt.aps.cx.service.MdmMonthProdPlanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 主计划月度生产计划Controller
 *
 * @author zlt
 * @date 2021-09-15
 */
@RestController
@RequestMapping("/mdmMonthProdPlan")
public class MdmMonthProdPlanController extends BaseController {
    @Autowired
    private MdmMonthProdPlanService mdmMonthProdPlanService;

    /**
     * 查询主计划月度生产计划列表
     */
    @ApiOperation("查询主计划月度生产计划列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmMonthProdPlan mdmMonthProdPlan) {
        startPage();
        mdmMonthProdPlan.setOrderStr(orderStr());
        List<MdmMonthProdPlan> list = mdmMonthProdPlanService.selectMdmMonthProdPlanList(mdmMonthProdPlan);
        return getDataTable(list);
    }

    /**
     * 获取主计划月度生产计划详细信息
     */
    @ApiOperation("获取主计划月度生产计划详细信息")
    @GetMapping(value = "/{id}")
    public MdmMonthProdPlan getInfo(@PathVariable("id") Long id) {
        return mdmMonthProdPlanService.selectMdmMonthProdPlanById(id);
    }

    /**
     * 新增主计划月度生产计划
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增主计划月度生产计划")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmMonthProdPlan mdmMonthProdPlan) {
        //唯一性校验
        List<MdmMonthProdPlan> list = mdmMonthProdPlanService.selectMdmMonthProdPlanList(mdmMonthProdPlan);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.quota.unique"));
        } else {
            return mdmMonthProdPlanService.insertMdmMonthProdPlan(mdmMonthProdPlan);
        }
    }

    /**
     * 修改主计划月度生产计划
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改主计划月度生产计划")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MdmMonthProdPlan mdmMonthProdPlan) {
        int a=mdmMonthProdPlanService.updateMdmMonthProdPlan(mdmMonthProdPlan);
        if(a==-999){
            return AjaxResult.error(I18nUtil.getMessage("ui.data.excut.error"));
        }
        return toAjax(a);
    }

    /**
     * 修改主计划月度生产计划
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改主计划月度生产计划")
    @PostMapping("/updateExpectedExcessArrears")
    public AjaxResult updateExpectedExcessArrears(@RequestBody MdmMonthProdPlan mdmMonthProdPlan) {
        return toAjax(mdmMonthProdPlanService.updateExpectedExcessArrears(mdmMonthProdPlan));
    }

    /**
     * 删除主计划月度生产计划
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除主计划月度生产计划")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(mdmMonthProdPlanService.deleteMdmMonthProdPlanByIds(ids));
    }

    /**
     * 导出主计划月度生产计划列表
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出主计划月度生产计划列表")
    @PostMapping("/getList")
    public List<MdmMonthProdPlan> getList(@RequestBody MdmMonthProdPlan mdmMonthProdPlan) {
        startPage();
        mdmMonthProdPlan.setOrderStr(orderStr());
        return mdmMonthProdPlanService.selectMdmMonthProdPlanList(mdmMonthProdPlan);
    }

    /**
     * 预计超欠产导出
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("预计超欠产导出")
    @PostMapping("/expectedExport")
    public List<CxMdmMonthProdPlan1> expectedExport(@RequestBody MdmMonthProdPlan mdmMonthProdPlan) {
        startPage("pp.material_code,pp.embryo_code");
        return mdmMonthProdPlanService.expectedExport(mdmMonthProdPlan);
    }

    /**
     * 超欠产导出
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("超欠产导出")
    @PostMapping("/overProdExport")
    public List<CxMdmMonthProdPlan2> overProdExport(@RequestBody MdmMonthProdPlan mdmMonthProdPlan) {
        startPage("pp.material_code,pp.embryo_code");
        return mdmMonthProdPlanService.overProdExport(mdmMonthProdPlan);
    }

    /**
     * 校验主计划月度生产计划唯一性
     */
    @ApiOperation("校验主计划月度生产计划唯一性")
    @PostMapping("/checkMdmMonthProdPlanUnique")
    public String checkMdmMonthProdPlanUnique(@RequestBody MdmMonthProdPlan mdmMonthProdPlan) {
        return mdmMonthProdPlanService.checkMdmMonthProdPlanUnique(mdmMonthProdPlan);
    }

    /**
     * 导入
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入主计划月度生产计划数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody byte[] data, @RequestParam("mainPlanMonth") String mainPlanMonth, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId, @RequestParam("isFinamized") boolean isFinamized,@RequestParam Map<String, String> dictMap) throws Exception  {
        return mdmMonthProdPlanService.importData(data, mainPlanMonth, updateSupport, importLogId, isFinamized,dictMap);
    }

    /**
     * 下发主计划
     */
    @Log(title = "ui.data.column.mdmMonthProdPlan.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("下发主计划")
    @PostMapping("/issuePlan")
    public AjaxResult issuePlan(@RequestBody MdmMonthProdPlan mdmMonthProdPlan, @RequestParam Map<String, String> map) {
        return mdmMonthProdPlanService.issuePlan(mdmMonthProdPlan, map);
    }


    /**
     * 查询月计划甘特图数据
     */
    @PostMapping("/getMonthPlanGanteData")
    public List<Gante> getMonthPlanGanteData(@RequestBody Gante gante){
        return mdmMonthProdPlanService.getMonthPlanGanteData(gante);
    }


    /**
     * 查询月计划柱状图数据
     */
    @GetMapping("/dailyChart/{scheduleDate}")
    public Map<String,List<Integer>> dailyChart(@PathVariable("scheduleDate") String scheduleDate){
        return mdmMonthProdPlanService.dailyChart(scheduleDate);
    }



}
