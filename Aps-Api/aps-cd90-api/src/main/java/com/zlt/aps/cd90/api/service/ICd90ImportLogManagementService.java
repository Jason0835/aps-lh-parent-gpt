package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportErrorLogManagementDto;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportLogManagementDto;
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
@FeignClient(contextId = "ICd90ImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cd90:cd90}")
public interface ICd90ImportLogManagementService
{

    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/cd90/importLogManagement/list")
    public TableDataInfo list(@RequestBody Cd90ImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/cd90/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody Cd90ImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/cd90/importLogManagement/{id}")
    public Cd90ImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/cd90/importLogManagement/edit")
    public AjaxResult edit(@RequestBody Cd90ImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @PostMapping("/cd90/importLogManagement/importData")
    List<Cd90ImportLogManagementDto> importData(@RequestBody Cd90ImportLogManagementDto dto);
}
