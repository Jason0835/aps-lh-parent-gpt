package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcExportLogManagementDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 工序导出日志管理对外暴露接口
 * @author chenxueyuan
 */
@FeignClient(contextId = "INcExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.nc:nc}")
public interface INcExportLogManagementService
{
    
    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/nc/exportLogManagement/list")
    public TableDataInfo list(@RequestBody NcExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/nc/exportLogManagement/{id}")
    public NcExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/nc/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody NcExportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/nc/exportLogManagement/exportData")
    List<NcExportLogManagementDto> exportData(@SpringQueryMap NcExportLogManagementDto dto);
}
