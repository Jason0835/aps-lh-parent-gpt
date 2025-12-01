package com.zlt.mix.schedule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.mix.schedule.api.domain.entity.ScheduleExportLogManagement;
import com.zlt.mix.schedule.mapper.ScheduleExportLogManagementMapper;
import com.zlt.mix.schedule.service.ScheduleExportLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导出日志管理Service业务层处理
 */
@Service
public class ScheduleExportLogManagementServiceImpl extends ServiceImpl<ScheduleExportLogManagementMapper, ScheduleExportLogManagement> implements ScheduleExportLogManagementService {

    @Resource
    private ScheduleExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<ScheduleExportLogManagement> selectExportLogManagementList(ScheduleExportLogManagement dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
