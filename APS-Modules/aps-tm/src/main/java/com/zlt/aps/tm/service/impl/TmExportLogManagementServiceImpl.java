package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.tm.api.domain.dto.TmExportLogManagementDto;
import com.zlt.aps.tm.entity.TmExportLogManagement;
import com.zlt.aps.tm.mapper.TmExportLogManagementMapper;
import com.zlt.aps.tm.service.TmExportLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导出日志管理Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class TmExportLogManagementServiceImpl extends ServiceImpl<TmExportLogManagementMapper, TmExportLogManagement> implements TmExportLogManagementService {

    @Resource
    private TmExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<TmExportLogManagementDto> selectExportLogManagementList(TmExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
