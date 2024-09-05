package com.zlt.aps.lh.controller;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldAdjustPlan;
import com.zlt.aps.lh.service.LhMachineInfoService;
import com.zlt.aps.lh.service.LhMoldAdjustPlanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 硫化模具调整计划Controller
 *
 * @author chen
 * @date 2022-03-23
 */
@RestController
@RequestMapping("/moldAdjustPlan")
public class LhMoldAdjustPlanController extends BaseController {
    @Autowired
    private LhMoldAdjustPlanService lhMoldAdjustPlanService;
    @Autowired
    private LhMachineInfoService lhMachineInfoService;

    /**
     * 查询硫化模具调整计划列表
     */
    @ApiOperation("查询硫化模具调整计划列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan) {
        startPage();
        lhMoldAdjustPlan.setOrderStr(orderStr());
        List<LhMoldAdjustPlan> list = lhMoldAdjustPlanService.selectLhMoldAdjustPlanList(lhMoldAdjustPlan);
        return getDataTable(list);
    }

    /**
     * 获取硫化模具调整计划详细信息
     */
    @ApiOperation("获取硫化模具调整计划详细信息")
    @GetMapping(value = "/{id}")
    public LhMoldAdjustPlan getInfo(@PathVariable("id") Long id) {
        return lhMoldAdjustPlanService.selectLhMoldAdjustPlanById(id);
    }

    /**
     * 新增硫化模具调整计划
     */
    @Log(title = "ui.data.column.moldAdjustPlan.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增硫化模具调整计划")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan) {
        return toAjax(lhMoldAdjustPlanService.insertLhMoldAdjustPlan(lhMoldAdjustPlan));
    }

    /**
     * 修改硫化模具调整计划
     */
    @Log(title = "ui.data.column.moldAdjustPlan.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化模具调整计划")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan) {
        return toAjax(lhMoldAdjustPlanService.updateLhMoldAdjustPlan(lhMoldAdjustPlan));
    }

    /**
     * 删除硫化模具调整计划
     */
    @Log(title = "ui.data.column.moldAdjustPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除硫化模具调整计划")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(lhMoldAdjustPlanService.deleteLhMoldAdjustPlanByIds(ids));
    }

    /**
     * 导出硫化模具调整计划列表
     */
    @Log(title = "ui.data.column.moldAdjustPlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化模具调整计划列表")
    @PostMapping("/getList")
    public List<LhMoldAdjustPlan> getList(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan) {
        Map<String, String> machineInfoMap = lhMachineInfoService.selectMachineInfoList(new LhMachineInfo()).stream().collect(Collectors.toMap(LhMachineInfo::getMachineCode, LhMachineInfo::getMachineName));
        startPage();
        lhMoldAdjustPlan.setOrderStr(orderStr());
        List<LhMoldAdjustPlan> list = lhMoldAdjustPlanService.selectLhMoldAdjustPlanList(lhMoldAdjustPlan);
        // 导出将机台code转为机台名称
        for (LhMoldAdjustPlan moldAdjustPlan : list) {
            String machineName = machineInfoMap.get(moldAdjustPlan.getLhMachineCode());
            if (StringUtil.isBlank(machineName)) {
                machineName = "";
            }
            moldAdjustPlan.setLhMachineCode(machineName);
        }
        return list;
    }

    /**
     * 校验硫化模具调整计划唯一性
     */
    @ApiOperation("校验硫化模具调整计划唯一性")
    @PostMapping("/checkLhMoldAdjustPlanUnique")
    public String checkLhMoldAdjustPlanUnique(@RequestBody LhMoldAdjustPlan lhMoldAdjustPlan) {
        return lhMoldAdjustPlanService.checkLhMoldAdjustPlanUnique(lhMoldAdjustPlan);
    }

    /**
     * 根据集合导入硫化模具调整计划数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.moldAdjustPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入硫化模具调整计划数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhMoldAdjustPlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return lhMoldAdjustPlanService.importData(list, updateSupport, importLogId);
    }
}
