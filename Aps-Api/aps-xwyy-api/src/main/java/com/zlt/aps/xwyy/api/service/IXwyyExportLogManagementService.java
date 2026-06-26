package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyExportLogManagementDto;
import org.springframework.cloud.openfeign.FeignClient;
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
@FeignClient(contextId = "IXwyyExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyExportLogManagementService {

    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/xwyy/exportLogManagement/list")
    public TableDataInfo list(@RequestBody XwyyExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/xwyy/exportLogManagement/{id}")
    public XwyyExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/xwyy/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody XwyyExportLogManagementDto dto);

    /**
     * 导出接口
     *
     * @param dto 查询条件
     */
    @PostMapping("/xwyy/exportLogManagement/exportData")
    List<XwyyExportLogManagementDto> exportData(@RequestBody XwyyExportLogManagementDto dto);
}
