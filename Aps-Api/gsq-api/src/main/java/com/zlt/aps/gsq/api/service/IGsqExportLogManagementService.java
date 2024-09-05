package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqExportLogManagementDto;
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
@FeignClient(contextId = "IGsqExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gsq:gsq}")
public interface IGsqExportLogManagementService
{
    
    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/gsq/exportLogManagement/list")
    public TableDataInfo list(@RequestBody GsqExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/gsq/exportLogManagement/{id}")
    public GsqExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/gsq/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody GsqExportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/gsq/exportLogManagement/exportData")
    List<GsqExportLogManagementDto> exportData(@SpringQueryMap GsqExportLogManagementDto dto);
}
