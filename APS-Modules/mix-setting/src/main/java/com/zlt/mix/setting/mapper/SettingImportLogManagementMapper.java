package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.setting.api.domain.entity.SettingImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 */
public interface SettingImportLogManagementMapper extends BaseMapper<SettingImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<SettingImportLogManagement> listImportLogManagement(SettingImportLogManagement dto);

}
