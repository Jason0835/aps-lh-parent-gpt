package com.zlt.aps.nc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.nc.api.domain.dto.NcExportLogManagementDto;
import com.zlt.aps.nc.entity.NcExportLogManagement;
import com.zlt.aps.nc.mapper.NcExportLogManagementMapper;
import com.zlt.aps.nc.service.NcExportLogManagementService;
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
public class NcExportLogManagementServiceImpl extends ServiceImpl<NcExportLogManagementMapper, NcExportLogManagement> implements NcExportLogManagementService {

    @Resource
    private NcExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<NcExportLogManagementDto> selectExportLogManagementList(NcExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
