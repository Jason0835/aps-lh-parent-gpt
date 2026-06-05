package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.dto.Cd15ExportLogManagementDto;
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
@FeignClient(contextId = "ICd15ExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cd15:cd15}")
public interface ICd15ExportLogManagementService
{

    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/cd15/exportLogManagement/list")
    public TableDataInfo list(@RequestBody Cd15ExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/cd15/exportLogManagement/{id}")
    public Cd15ExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/cd15/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody Cd15ExportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @PostMapping("/cd15/exportLogManagement/exportData")
    List<Cd15ExportLogManagementDto> exportData(@RequestBody Cd15ExportLogManagementDto dto);
}
