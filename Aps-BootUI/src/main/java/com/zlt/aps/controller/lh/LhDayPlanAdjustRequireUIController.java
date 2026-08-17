package com.zlt.aps.controller.lh;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import com.zlt.aps.lh.api.service.ILhDayPlanAdjustRequireRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 硫化日计划调整需求 UI 控制器。
 */
@Api(tags = "硫化日计划调整需求")
@Controller
@RequestMapping("/lh/lhDayPlanAdjustRequire")
public class LhDayPlanAdjustRequireUIController extends BaseUIController<LhDayPlanAdjustRequire> {

    @Autowired
    private ILhDayPlanAdjustRequireRemoteService remoteService;

    /**
     * 查询列表。
     *
     * @param queryVO 查询条件
     * @return 分页列表
     */
    @ApiOperation("查询硫化日计划调整需求列表")
    @RequiresPermissions("lh:dayPlanAdjustRequire:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhDayPlanAdjustRequire queryVO) {
        return remoteService.list(queryVO);
    }

    /**
     * 保存当前行。
     *
     * @param entity 当前行数据
     * @return 保存结果
     */
    @ApiOperation("保存硫化日计划调整需求")
    @RequiresPermissions("lh:dayPlanAdjustRequire:save")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(@RequestBody LhDayPlanAdjustRequire entity) {
        return remoteService.save(entity);
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.lhDayPlanAdjustRequire.modelName");
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }
}
