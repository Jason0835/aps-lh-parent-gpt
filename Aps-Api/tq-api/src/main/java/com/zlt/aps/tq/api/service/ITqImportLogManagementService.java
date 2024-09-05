package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.dto.TqImportErrorLogManagementDto;
import com.zlt.aps.tq.api.domain.dto.TqImportLogManagementDto;
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
@FeignClient(contextId = "ITqImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tq:tq}")
public interface ITqImportLogManagementService
{
    
    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/tq/importLogManagement/list")
    public TableDataInfo list(@RequestBody TqImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/tq/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody TqImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/tq/importLogManagement/{id}")
    public TqImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/tq/importLogManagement/edit")
    public AjaxResult edit(@RequestBody TqImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/tq/importLogManagement/importData")
    List<TqImportLogManagementDto> importData(@SpringQueryMap TqImportLogManagementDto dto);
}
