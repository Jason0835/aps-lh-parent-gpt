package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxImportErrorLogManagementDto;
import com.zlt.aps.cx.api.domain.dto.CxImportLogManagementDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 工序导入日志管理对外暴露接口
 * @author duanjuntao
 */
@FeignClient(contextId = "ICxImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cx:cx}")
public interface ICxImportLogManagementService
{
    
    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/cx/importLogManagement/list")
    public TableDataInfo list(@RequestBody CxImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/cx/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody CxImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/cx/importLogManagement/{id}")
    public CxImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/cx/importLogManagement/edit")
    public AjaxResult edit(@RequestBody CxImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/cx/importLogManagement/importData")
    List<CxImportLogManagementDto> importData(@SpringQueryMap CxImportLogManagementDto dto);
}
