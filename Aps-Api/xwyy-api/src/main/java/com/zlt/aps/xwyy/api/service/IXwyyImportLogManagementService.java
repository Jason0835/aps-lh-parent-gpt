package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyImportErrorLogManagementDto;
import com.zlt.aps.xwyy.api.domain.dto.XwyyImportLogManagementDto;
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
@FeignClient(contextId = "IXwyyImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.xwyy:xwyy}")
public interface IXwyyImportLogManagementService
{
    
    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/xwyy/importLogManagement/list")
    public TableDataInfo list(@RequestBody XwyyImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/xwyy/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody XwyyImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/xwyy/importLogManagement/{id}")
    public XwyyImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/xwyy/importLogManagement/edit")
    public AjaxResult edit(@RequestBody XwyyImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/xwyy/importLogManagement/importData")
    List<XwyyImportLogManagementDto> importData(@SpringQueryMap XwyyImportLogManagementDto dto);
}
