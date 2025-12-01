package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyExportLogManagementDto;
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
@FeignClient(contextId = "IGdyyExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gdyy:gdyy}")
public interface IGdyyExportLogManagementService
{

    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/gdyy/exportLogManagement/list")
    public TableDataInfo list(@RequestBody GdyyExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/gdyy/exportLogManagement/{id}")
    public GdyyExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/gdyy/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody GdyyExportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @PostMapping("/gdyy/exportLogManagement/exportData")
    List<GdyyExportLogManagementDto> exportData(@RequestBody GdyyExportLogManagementDto dto);
}
