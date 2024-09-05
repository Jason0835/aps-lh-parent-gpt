package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 导出记录Service接口
 *
 * @author zlt
 * @date 2021-07-24
 */
@FeignClient(contextId = "iExportLogService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.system:system}")
public interface IExportLogService {


    /**
     * 查询导出记录列表
     */
    @PostMapping("/exportLog/list")
    TableDataInfo list(@RequestBody ExportLog exportLog);


    /**
     * 新增导出记录
     */
    @PostMapping("/exportLog/add")
    AjaxResult add(@RequestBody ExportLog exportLog);


    /**
     * 修改导出记录
     */
    @PostMapping("/exportLog/edit")
    AjaxResult edit(@RequestBody ExportLog exportLog);


    /**
     * 删除导出记录
     */
    @DeleteMapping("/exportLog/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/exportLog/{id}")
    ExportLog getInfo(@PathVariable("id") Long id);


    /**
     * 校验导出记录唯一性
     */
    @PostMapping("/exportLog/checkExportLogUnique")
    String checkExportLogUnique(@RequestBody ExportLog exportLog);


    /**
     * 导出导出记录列表
     */
    @PostMapping("/exportLog/getList")
    List<ExportLog> getList(@RequestBody ExportLog exportLog);


}
