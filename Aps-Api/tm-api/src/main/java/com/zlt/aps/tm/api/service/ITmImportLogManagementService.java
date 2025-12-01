package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmImportLogManagementDto;
import com.zlt.aps.tm.api.domain.dto.TmImportErrorLogManagementDto;
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
@FeignClient(contextId = "ITmImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmImportLogManagementService
{

    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/tm/importLogManagement/list")
    public TableDataInfo list(@RequestBody TmImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/tm/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody TmImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/tm/importLogManagement/{id}")
    public TmImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/tm/importLogManagement/edit")
    public AjaxResult edit(@RequestBody TmImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @PostMapping("/tm/importLogManagement/importData")
    List<TmImportLogManagementDto> importData(@RequestBody TmImportLogManagementDto dto);
}
