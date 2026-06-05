package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90ExportLogManagementDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 工序导出日志管理对外暴露接口
 *
 * @author chenxueyuan
 */
@FeignClient(contextId = "ICd90ExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90ExportLogManagementService {

    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/cd90/exportLogManagement/list")
    public TableDataInfo list(@RequestBody Cd90ExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/cd90/exportLogManagement/{id}")
    public Cd90ExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/cd90/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody Cd90ExportLogManagementDto dto);

    /**
     * 导出接口
     *
     * @param dto 查询条件
     */
    @PostMapping("/cd90/exportLogManagement/exportData")
    List<Cd90ExportLogManagementDto> exportData(@RequestBody Cd90ExportLogManagementDto dto);
}
