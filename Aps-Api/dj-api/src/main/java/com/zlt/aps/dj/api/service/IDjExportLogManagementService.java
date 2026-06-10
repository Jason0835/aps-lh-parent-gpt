package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjExportLogManagementDto;

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
@FeignClient(contextId = "INcExportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:nc}")
public interface IDjExportLogManagementService
{

    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/dj/exportLogManagement/list")
    public TableDataInfo list(@RequestBody DjExportLogManagementDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/dj/exportLogManagement/{id}")
    public DjExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/dj/exportLogManagement/edit")
    public AjaxResult edit(@RequestBody DjExportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @PostMapping("/dj/exportLogManagement/exportData")
    List<DjExportLogManagementDto> exportData(@RequestBody DjExportLogManagementDto dto);
}
