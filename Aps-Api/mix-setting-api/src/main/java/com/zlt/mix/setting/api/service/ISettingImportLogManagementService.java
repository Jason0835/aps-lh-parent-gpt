package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.dto.SettingImportErrorLogManagementDto;
import com.zlt.mix.setting.api.domain.entity.SettingImportLogManagement;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 工序导入日志管理对外暴露接口
 */
@FeignClient(contextId = "ISettingImportLogManagementService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.schedule:mixSchedule}")
public interface ISettingImportLogManagementService
{

    String prefix = "/setting/importLogManagement";
    
    /**
     * 查询工序导入日志信息列表
     */
    @PostMapping(value = prefix + "/list")
    public TableDataInfo list(@RequestBody SettingImportLogManagement dto);

    /**
     * 获取工序导入日志错误日志详细信息
     */
    @PostMapping(value = prefix + "/errorView")
    public TableDataInfo getImportErrorLogManagement(@RequestBody SettingImportErrorLogManagementDto dto);

    /**
     * 获取工序导入日志详细信息
     */
    @GetMapping(value = prefix + "/{id}")
    public SettingImportLogManagement getImportLogManagement(@PathVariable("id") Long id);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping(value = prefix + "/importData")
    List<SettingImportLogManagement> importData(@SpringQueryMap SettingImportLogManagement dto);
}
