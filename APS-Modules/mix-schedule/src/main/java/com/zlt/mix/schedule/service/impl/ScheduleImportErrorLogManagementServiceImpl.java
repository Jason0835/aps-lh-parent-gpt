package com.zlt.mix.schedule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.mix.schedule.api.domain.dto.ScheduleImportErrorLogManagementDto;
import com.zlt.mix.schedule.mapper.ScheduleImportErrorLogManagementMapper;
import com.zlt.mix.schedule.service.ScheduleImportErrorLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导入日志管理错误日志Service业务层处理
 */
@Service
public class ScheduleImportErrorLogManagementServiceImpl extends ServiceImpl<ScheduleImportErrorLogManagementMapper, ScheduleImportErrorLogManagementDto> implements ScheduleImportErrorLogManagementService {

    @Resource
    private ScheduleImportErrorLogManagementMapper importErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<ScheduleImportErrorLogManagementDto> selectImportErrorLogManagementList(ScheduleImportErrorLogManagementDto dto) {
        return importErrorLogManagementMapper.listImportErrorLogManagement(dto);
    }

}
