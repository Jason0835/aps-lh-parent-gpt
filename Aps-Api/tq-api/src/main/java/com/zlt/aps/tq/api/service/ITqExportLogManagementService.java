package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.dto.TqExportLogManagementDto;
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
@FeignClient(contextId = "ITqExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tq:tq}")
public interface ITqExportLogManagementService
{
    
    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/tq/exportLogManagement/list")
    public TableDataInfo list(@RequestBody TqExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/tq/exportLogManagement/{id}")
    public TqExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/tq/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody TqExportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/tq/exportLogManagement/exportData")
    List<TqExportLogManagementDto> exportData(@SpringQueryMap TqExportLogManagementDto dto);
}
