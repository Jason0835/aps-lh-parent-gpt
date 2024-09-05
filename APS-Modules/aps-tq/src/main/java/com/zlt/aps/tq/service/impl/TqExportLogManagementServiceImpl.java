package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.tq.api.domain.dto.TqExportLogManagementDto;
import com.zlt.aps.tq.entity.TqExportLogManagement;
import com.zlt.aps.tq.mapper.TqExportLogManagementMapper;
import com.zlt.aps.tq.service.TqExportLogManagementService;
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
public class TqExportLogManagementServiceImpl extends ServiceImpl<TqExportLogManagementMapper, TqExportLogManagement> implements TqExportLogManagementService {

    @Resource
    private TqExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<TqExportLogManagementDto> selectExportLogManagementList(TqExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
