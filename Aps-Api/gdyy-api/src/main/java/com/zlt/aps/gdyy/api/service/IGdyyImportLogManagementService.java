package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyImportErrorLogManagementDto;
import com.zlt.aps.gdyy.api.domain.dto.GdyyImportLogManagementDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 工序导入日志管理对外暴露接口
 * @author duanjuntao
 */
@FeignClient(contextId = "IGdyyImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gdyy:gdyy}")
public interface IGdyyImportLogManagementService
{
    
    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping("/gdyy/importLogManagement/list")
    public TableDataInfo list(@RequestBody GdyyImportLogManagementDto dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping("/gdyy/importLogManagement/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody GdyyImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping("/gdyy/importLogManagement/{id}")
    public GdyyImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 修改工序导入日志
     */
    @PostMapping("/gdyy/importLogManagement/edit")
    public AjaxResult edit(@RequestBody GdyyImportLogManagementDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/gdyy/importLogManagement/importData")
    List<GdyyImportLogManagementDto> importData(@SpringQueryMap GdyyImportLogManagementDto dto);
}
