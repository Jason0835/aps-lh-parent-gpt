package com.zlt.mix.setting.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.mix.setting.api.domain.dto.SettingImportErrorLogManagementDto;
import com.zlt.mix.setting.mapper.SettingImportErrorLogManagementMapper;
import com.zlt.mix.setting.service.SettingImportErrorLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导入日志管理错误日志Service业务层处理
 */
@Service
public class SettingImportErrorLogManagementServiceImpl extends ServiceImpl<SettingImportErrorLogManagementMapper, SettingImportErrorLogManagementDto> implements SettingImportErrorLogManagementService {

    @Resource
    private SettingImportErrorLogManagementMapper importErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<SettingImportErrorLogManagementDto> selectImportErrorLogManagementList(SettingImportErrorLogManagementDto dto) {
        return importErrorLogManagementMapper.listImportErrorLogManagement(dto);
    }

}
