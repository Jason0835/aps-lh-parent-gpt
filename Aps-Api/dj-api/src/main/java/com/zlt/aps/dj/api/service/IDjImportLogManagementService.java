package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjImportErrorLogManagementDto;
import com.zlt.aps.dj.api.domain.dto.DjImportLogManagementDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 工序导入日志管理对外暴露接口
 * @author duanjuntao
 */
@FeignClient(contextId = "INcImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:nc}")
public interface IDjImportLogManagementService
{

    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/dj/importLogManagement/list")
    public TableDataInfo list(@RequestBody DjImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/dj/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody DjImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/dj/importLogManagement/{id}")
    public DjImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/dj/importLogManagement/edit")
    public AjaxResult edit(@RequestBody DjImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @PostMapping("/dj/importLogManagement/importData")
    List<DjImportLogManagementDto> importData(@RequestBody DjImportLogManagementDto dto);
}
