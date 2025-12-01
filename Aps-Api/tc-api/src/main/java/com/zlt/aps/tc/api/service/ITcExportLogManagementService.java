package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcExportLogManagementDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 工序导出日志管理对外暴露接口
 * @author chenxueyuan
 */
@FeignClient(contextId = "ITcExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tc:tc}")
public interface ITcExportLogManagementService
{

    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/tc/exportLogManagement/list")
    public TableDataInfo list(@RequestBody TcExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/tc/exportLogManagement/{id}")
    public TcExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/tc/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody TcExportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @PostMapping("/tc/exportLogManagement/exportData")
    List<TcExportLogManagementDto> exportData(@RequestBody TcExportLogManagementDto dto);
}
