package com.zlt.aps.cx.controller;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.ConstructionExportLog;
import com.zlt.aps.cx.service.ConstructionExportLogService;
import com.zlt.aps.cx.service.CxProductConstructionInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 施工信息导出日志Controller
 *
 * @author zlt
 * @date 2021-12-28
 */
@RestController
@RequestMapping("/constructionExportLog")
public class ConstructionExportLogController extends BaseController
{
    @Autowired
    private ConstructionExportLogService constructionExportLogService;

    @Autowired
    private CxProductConstructionInfoService cxProductConstructionInfoService;

    /**
     * 查询施工信息导出日志列表
     */
    @ApiOperation("查询施工信息导出日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ConstructionExportLog constructionExportLog)
    {
        startPage();
        constructionExportLog.setOrderStr(orderStr());
        List<ConstructionExportLog> list = constructionExportLogService.selectConstructionExportLogList(constructionExportLog);
        return getDataTable(list);
    }

    @PostMapping("/getExcelData")
    public byte[] list(@RequestBody List<String> list,@RequestParam("fileType")String fileType){
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return cxProductConstructionInfoService.createProcedureConstructionExcel(fileType, list);
    }

    /**
     * 获取施工信息导出日志详细信息
     */
    @ApiOperation("获取施工信息导出日志详细信息")
    @GetMapping(value = "/{id}")
    public ConstructionExportLog getInfo(@PathVariable("id") Long id){
        return constructionExportLogService.selectConstructionExportLogById(id);
    }

    /**
     * 新增施工信息导出日志
     */
    @Log(title = "ui.data.column.constructionExportLog.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增施工信息导出日志")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody ConstructionExportLog constructionExportLog){
        return toAjax(constructionExportLogService.insertConstructionExportLog(constructionExportLog));
    }

    /**
     * 修改施工信息导出日志
     */
    @Log(title = "ui.data.column.constructionExportLog.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改施工信息导出日志")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody ConstructionExportLog constructionExportLog){
        return toAjax(constructionExportLogService.updateConstructionExportLog(constructionExportLog));
    }

    /**
     * 删除施工信息导出日志
     */
    @Log(title = "ui.data.column.constructionExportLog.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除施工信息导出日志")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(constructionExportLogService.deleteConstructionExportLogByIds(ids));
    }

    /**
     * 导出施工信息导出日志列表
     */
    @Log(title = "ui.data.column.constructionExportLog.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出施工信息导出日志列表")
    @PostMapping("/getList")
    public List<ConstructionExportLog> getList(@RequestBody ConstructionExportLog constructionExportLog){
        startPage();
        constructionExportLog.setOrderStr(orderStr());
        return  constructionExportLogService.selectConstructionExportLogList(constructionExportLog);
    }

    /**
     * 校验施工信息导出日志唯一性
     */
    @ApiOperation("校验施工信息导出日志唯一性")
    @PostMapping("/checkConstructionExportLogUnique")
    public String checkConstructionExportLogUnique(@RequestBody ConstructionExportLog constructionExportLog){
        return constructionExportLogService.checkConstructionExportLogUnique(constructionExportLog);
    }

    /**
     * 根据集合导入施工信息导出日志数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.constructionExportLog.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入施工信息导出日志数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<ConstructionExportLog> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return constructionExportLogService.importData(list, updateSupport, importLogId);
    }
}
