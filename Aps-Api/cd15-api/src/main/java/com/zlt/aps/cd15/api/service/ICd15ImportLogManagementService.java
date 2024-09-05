package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportErrorLogManagementDto;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportLogManagementDto;
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
@FeignClient(contextId = "ICd15ImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cd15:cd15}")
public interface ICd15ImportLogManagementService
{
    
    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/cd15/importLogManagement/list")
    public TableDataInfo list(@RequestBody Cd15ImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/cd15/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody Cd15ImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/cd15/importLogManagement/{id}")
    public Cd15ImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/cd15/importLogManagement/edit")
    public AjaxResult edit(@RequestBody Cd15ImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/cd15/importLogManagement/importData")
    List<Cd15ImportLogManagementDto> importData(@SpringQueryMap Cd15ImportLogManagementDto dto);
}
