package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqImportErrorLogManagementDto;
import com.zlt.aps.gsq.api.domain.dto.GsqImportLogManagementDto;
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
@FeignClient(contextId = "IGsqImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gsq:gsq}")
public interface IGsqImportLogManagementService
{
    
    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/gsq/importLogManagement/list")
    public TableDataInfo list(@RequestBody GsqImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/gsq/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody GsqImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/gsq/importLogManagement/{id}")
    public GsqImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/gsq/importLogManagement/edit")
    public AjaxResult edit(@RequestBody GsqImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/gsq/importLogManagement/importData")
    List<GsqImportLogManagementDto> importData(@SpringQueryMap GsqImportLogManagementDto dto);
}
