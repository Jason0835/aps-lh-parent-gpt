package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.setting.api.domain.dto.SettingImportErrorLogManagementDto;

import java.util.List;

/**
 * 工序导入日志管理错误信息Mapper接口
 */
public interface SettingImportErrorLogManagementMapper extends BaseMapper<SettingImportErrorLogManagementDto>
{
    /**
     * 根据条件工序导出日志管理
     * @return
     */
    List<SettingImportErrorLogManagementDto> listImportErrorLogManagement(SettingImportErrorLogManagementDto dto);

}
