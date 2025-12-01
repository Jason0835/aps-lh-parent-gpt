package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistRequirement;
import com.zlt.aps.xwyy.service.XwyyAssistRequirementService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延外厂需求Controller
 *
 * @author chen
 * @date 2022-03-14
 */
@RestController
@RequestMapping("/xwyy/assistRequirement")
public class XwyyAssistRequirementController extends BaseController {
    @Autowired
    private XwyyAssistRequirementService xwyyAssistRequirementService;

    /**
     * 查询纤维压延外厂需求列表
     */
    @ApiOperation("查询纤维压延外厂需求列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyAssistRequirement xwyyAssistRequirement) {
        startPage();
        xwyyAssistRequirement.setOrderStr(orderStr());
        List<XwyyAssistRequirement> list = xwyyAssistRequirementService.selectXwyyAssistRequirementList(xwyyAssistRequirement);
        return getDataTable(list);
    }

    /**
     * 获取纤维压延外厂需求详细信息
     */
    @ApiOperation("获取纤维压延外厂需求详细信息")
    @GetMapping(value = "/{id}")
    public XwyyAssistRequirement getInfo(@PathVariable("id") Long id) {
        return xwyyAssistRequirementService.selectXwyyAssistRequirementById(id);
    }

    /**
     * 新增纤维压延外厂需求
     */
    @Log(title = "ui.data.column.assistRequirement.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增纤维压延外厂需求")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody XwyyAssistRequirement xwyyAssistRequirement) {
        if (UserConstants.NOT_UNIQUE.equals(xwyyAssistRequirementService.checkXwyyAssistRequirementUnique(xwyyAssistRequirement))) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.xwyyAssistRequirement.exist"));
        }
        return toAjax(xwyyAssistRequirementService.insertXwyyAssistRequirement(xwyyAssistRequirement));
    }

    /**
     * 修改纤维压延外厂需求
     */
    @Log(title = "ui.data.column.assistRequirement.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改纤维压延外厂需求")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyAssistRequirement xwyyAssistRequirement) {
        if (UserConstants.NOT_UNIQUE.equals(xwyyAssistRequirementService.checkXwyyAssistRequirementUnique(xwyyAssistRequirement))) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.xwyyAssistRequirement.exist"));
        }
        return toAjax(xwyyAssistRequirementService.updateXwyyAssistRequirement(xwyyAssistRequirement));
    }

    /**
     * 删除纤维压延外厂需求
     */
    @Log(title = "ui.data.column.assistRequirement.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除纤维压延外厂需求")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(xwyyAssistRequirementService.deleteXwyyAssistRequirementByIds(ids));
    }

    /**
     * 导出纤维压延外厂需求列表
     */
    @Log(title = "ui.data.column.assistRequirement.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出纤维压延外厂需求列表")
    @PostMapping("/getList")
    public List<XwyyAssistRequirement> getList(@RequestBody XwyyAssistRequirement xwyyAssistRequirement) {
        xwyyAssistRequirement.setOrderStr(orderStr());
        return xwyyAssistRequirementService.selectXwyyAssistRequirementList(xwyyAssistRequirement);
    }

    /**
     * 校验纤维压延外厂需求唯一性
     */
    @ApiOperation("校验纤维压延外厂需求唯一性")
    @PostMapping("/checkXwyyAssistRequirementUnique")
    public String checkXwyyAssistRequirementUnique(@RequestBody XwyyAssistRequirement xwyyAssistRequirement) {
        return xwyyAssistRequirementService.checkXwyyAssistRequirementUnique(xwyyAssistRequirement);
    }

    /**
     * 根据集合导入纤维压延外厂需求数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.assistRequirement.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入纤维压延外厂需求数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyAssistRequirement> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate")String scheduleDate) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyAssistRequirementService.importData(list, importLogId, DateUtils.parseDate(scheduleDate));
    }
}
