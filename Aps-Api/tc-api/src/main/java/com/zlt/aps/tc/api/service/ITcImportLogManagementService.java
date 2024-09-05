package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcImportErrorLogManagementDto;
import com.zlt.aps.tc.api.domain.dto.TcImportLogManagementDto;
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
@FeignClient(contextId = "ITcImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tc:tc}")
public interface ITcImportLogManagementService
{
    
    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/tc/importLogManagement/list")
    public TableDataInfo list(@RequestBody TcImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/tc/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody TcImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/tc/importLogManagement/{id}")
    public TcImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/tc/importLogManagement/edit")
    public AjaxResult edit(@RequestBody TcImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/tc/importLogManagement/importData")
    List<TcImportLogManagementDto> importData(@SpringQueryMap TcImportLogManagementDto dto);
}
