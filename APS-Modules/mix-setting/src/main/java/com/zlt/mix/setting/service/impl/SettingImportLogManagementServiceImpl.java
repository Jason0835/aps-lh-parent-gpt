package com.zlt.mix.setting.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.mix.setting.api.domain.entity.SettingImportLogManagement;
import com.zlt.mix.setting.mapper.SettingImportLogManagementMapper;
import com.zlt.mix.setting.service.SettingImportLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导入日志管理Service业务层处理
 */
@Service
public class SettingImportLogManagementServiceImpl extends ServiceImpl<SettingImportLogManagementMapper, SettingImportLogManagement> implements SettingImportLogManagementService {

    @Resource
    private SettingImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<SettingImportLogManagement> selectImportLogManagementList(SettingImportLogManagement dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
