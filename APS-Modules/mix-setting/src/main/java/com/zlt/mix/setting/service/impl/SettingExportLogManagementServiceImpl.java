package com.zlt.mix.setting.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.mix.setting.api.domain.entity.SettingExportLogManagement;
import com.zlt.mix.setting.mapper.SettingExportLogManagementMapper;
import com.zlt.mix.setting.service.SettingExportLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导出日志管理Service业务层处理
 */
@Service
public class SettingExportLogManagementServiceImpl extends ServiceImpl<SettingExportLogManagementMapper, SettingExportLogManagement> implements SettingExportLogManagementService {

    @Resource
    private SettingExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<SettingExportLogManagement> selectExportLogManagementList(SettingExportLogManagement dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
