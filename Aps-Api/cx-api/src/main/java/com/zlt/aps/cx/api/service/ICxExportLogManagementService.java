package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxExportLogManagementDto;
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
@FeignClient(contextId = "ICxExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxExportLogManagementService {

    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/cx/exportLogManagement/list")
    public TableDataInfo list(@RequestBody CxExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/cx/exportLogManagement/{id}")
    public CxExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/cx/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody CxExportLogManagementDto dto);

    /**
     * 导出接口
     *
     * @param dto 查询条件
     */
    @GetMapping("/cx/exportLogManagement/exportData")
    List<CxExportLogManagementDto> exportData(@SpringQueryMap CxExportLogManagementDto dto);
}
