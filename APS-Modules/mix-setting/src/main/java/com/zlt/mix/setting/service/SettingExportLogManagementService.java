package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.entity.SettingExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Service接口
 */
public interface SettingExportLogManagementService extends IService<SettingExportLogManagement> {
    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理     * @return 工序导出日志管理集合
     */
    List<SettingExportLogManagement> selectExportLogManagementList(SettingExportLogManagement dto);

}
