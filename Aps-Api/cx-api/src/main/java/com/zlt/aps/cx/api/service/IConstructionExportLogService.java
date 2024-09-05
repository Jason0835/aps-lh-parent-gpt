package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.cx.api.domain.entity.ConstructionExportLog;


/**
 * 施工信息导出日志Service接口
 * @author zlt
 * @date 2021-12-28
 */
@FeignClient(contextId = "IConstructionExportLogService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface IConstructionExportLogService {

    /**
     * 查询施工信息导出日志列表
     */
    @ApiOperation("查询施工信息导出日志列表")
    @PostMapping("/constructionExportLog/list")
    TableDataInfo list(@RequestBody ConstructionExportLog constructionExportLog);

    @PostMapping("/constructionExportLog/getExcelData")
    byte[] getExcelData(@RequestBody List<String> list,@RequestParam("fileType")String fileType);

    /**
    * 新增施工信息导出日志
    */
    @ApiOperation("新增施工信息导出日志")
    @PostMapping("/constructionExportLog/add")
    AjaxResult add(@RequestBody ConstructionExportLog constructionExportLog);

    /**
     * 修改施工信息导出日志
     */
    @ApiOperation("修改施工信息导出日志")
    @PostMapping("/constructionExportLog/edit")
    AjaxResult edit(@RequestBody ConstructionExportLog constructionExportLog);

    /**
     * 删除施工信息导出日志
     */
    @ApiOperation("删除施工信息导出日志")
    @DeleteMapping("/constructionExportLog/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/constructionExportLog/{id}")
    ConstructionExportLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验施工信息导出日志唯一性
     */
    @ApiOperation("校验施工信息导出日志唯一性")
    @PostMapping("/constructionExportLog/checkConstructionExportLogUnique")
    String checkConstructionExportLogUnique(@RequestBody ConstructionExportLog constructionExportLog);

    /**
     * 导出施工信息导出日志列表
     */
    @ApiOperation("导出施工信息导出日志列表")
    @PostMapping("/constructionExportLog/getList")
    List<ConstructionExportLog> getList(@RequestBody ConstructionExportLog constructionExportLog);

    /**
     * 导入施工信息导出日志数据
     */
    @ApiOperation("导入施工信息导出日志")
    @PostMapping("/constructionExportLog/importData")
    public AjaxResult importData(@RequestBody List<ConstructionExportLog> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
