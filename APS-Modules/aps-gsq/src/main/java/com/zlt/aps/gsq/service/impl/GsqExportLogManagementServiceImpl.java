package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.gsq.api.domain.dto.GsqExportLogManagementDto;
import com.zlt.aps.gsq.entity.GsqExportLogManagement;
import com.zlt.aps.gsq.mapper.GsqExportLogManagementMapper;
import com.zlt.aps.gsq.service.GsqExportLogManagementService;
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
public class GsqExportLogManagementServiceImpl extends ServiceImpl<GsqExportLogManagementMapper, GsqExportLogManagement> implements GsqExportLogManagementService {

    @Resource
    private GsqExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<GsqExportLogManagementDto> selectExportLogManagementList(GsqExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
