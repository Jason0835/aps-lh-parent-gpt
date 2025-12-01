package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRemind;
import com.zlt.aps.xwyy.service.XwyyBigRollRemindService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帘布大卷原线提醒Controller
 *
 * @author chen
 * @date 2022-04-27
 */
@RestController
@RequestMapping("/xwyy/bigRollRemind")
public class XwyyBigRollRemindController extends BaseController {
    @Autowired
    private XwyyBigRollRemindService xwyyBigRollRemindService;

    /**
     * 查询帘布大卷原线提醒列表
     */
    @ApiOperation("查询帘布大卷原线提醒列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyBigRollRemind xwyyBigRollRemind) {
        startPage();
        xwyyBigRollRemind.setOrderStr(orderStr());
        List<XwyyBigRollRemind> list = xwyyBigRollRemindService.selectXwyyBigRollRemindList(xwyyBigRollRemind);
        return getDataTable(list);
    }

    /**
     * 获取帘布大卷原线提醒详细信息
     */
    @ApiOperation("获取帘布大卷原线提醒详细信息")
    @GetMapping(value = "/{id}")
    public XwyyBigRollRemind getInfo(@PathVariable("id") Long id) {
        return xwyyBigRollRemindService.selectXwyyBigRollRemindById(id);
    }

    /**
     * 新增帘布大卷原线提醒
     */
    @Log(title = "ui.data.column.bigRollRemind.modelName" , businessType = BusinessType.INSERT)
    @ApiOperation("新增帘布大卷原线提醒")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody XwyyBigRollRemind xwyyBigRollRemind) {
        return toAjax(xwyyBigRollRemindService.insertXwyyBigRollRemind(xwyyBigRollRemind));
    }

    /**
     * 修改帘布大卷原线提醒
     */
    @Log(title = "ui.data.column.bigRollRemind.modelName" , businessType = BusinessType.UPDATE)
    @ApiOperation("修改帘布大卷原线提醒")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyBigRollRemind xwyyBigRollRemind) {
        return toAjax(xwyyBigRollRemindService.updateXwyyBigRollRemind(xwyyBigRollRemind));
    }

    /**
     * 删除帘布大卷原线提醒
     */
    @Log(title = "ui.data.column.bigRollRemind.modelName" , businessType = BusinessType.DELETE)
    @ApiOperation("删除帘布大卷原线提醒")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(xwyyBigRollRemindService.deleteXwyyBigRollRemindByIds(ids));
    }

    /**
     * 导出帘布大卷原线提醒列表
     */
    @Log(title = "ui.data.column.bigRollRemind.modelName" , businessType = BusinessType.EXPORT)
    @ApiOperation("导出帘布大卷原线提醒列表")
    @PostMapping("/getList")
    public List<XwyyBigRollRemind> getList(@RequestBody XwyyBigRollRemind xwyyBigRollRemind) {
        startPage();
        xwyyBigRollRemind.setOrderStr(orderStr());
        return xwyyBigRollRemindService.selectXwyyBigRollRemindList(xwyyBigRollRemind);
    }

    /**
     * 校验帘布大卷原线提醒唯一性
     */
    @ApiOperation("校验帘布大卷原线提醒唯一性")
    @PostMapping("/checkXwyyBigRollRemindUnique")
    public String checkXwyyBigRollRemindUnique(@RequestBody XwyyBigRollRemind xwyyBigRollRemind) {
        return xwyyBigRollRemindService.checkXwyyBigRollRemindUnique(xwyyBigRollRemind);
    }

    /**
     * 根据集合导入帘布大卷原线提醒数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.bigRollRemind.modelName" , businessType = BusinessType.IMPORT)
    @ApiOperation("导入帘布大卷原线提醒数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyBigRollRemind> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyBigRollRemindService.importData(list, updateSupport, importLogId);
    }
}
