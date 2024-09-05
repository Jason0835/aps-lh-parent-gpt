package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.tc.api.domain.dto.TcExportLogManagementDto;
import com.zlt.aps.tc.entity.TcExportLogManagement;
import com.zlt.aps.tc.mapper.TcExportLogManagementMapper;
import com.zlt.aps.tc.service.TcExportLogManagementService;
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
public class TcExportLogManagementServiceImpl extends ServiceImpl<TcExportLogManagementMapper, TcExportLogManagement> implements TcExportLogManagementService {

    @Resource
    private TcExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<TcExportLogManagementDto> selectExportLogManagementList(TcExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
