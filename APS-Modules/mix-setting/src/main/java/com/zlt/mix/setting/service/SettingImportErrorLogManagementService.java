package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.dto.SettingImportErrorLogManagementDto;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 */
public interface SettingImportErrorLogManagementService extends IService<SettingImportErrorLogManagementDto>
{
    /**
     * 查询工序导入日志管理错误日志列表
     *
     */
    List<SettingImportErrorLogManagementDto> selectImportErrorLogManagementList(SettingImportErrorLogManagementDto dto);
}
