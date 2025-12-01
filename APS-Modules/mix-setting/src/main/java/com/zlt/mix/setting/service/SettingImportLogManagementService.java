package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.entity.SettingImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Service接口
 */
public interface SettingImportLogManagementService extends IService<SettingImportLogManagement>
{
    /**
     * 查询工序导入日志管理列表
     * 
     * @param dto 工序导入日志管理     * @return 工序导入日志管理集合
     */
     List<SettingImportLogManagement> selectImportLogManagementList(SettingImportLogManagement dto);
}
