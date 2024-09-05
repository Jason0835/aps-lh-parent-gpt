package com.zlt.aps.gdyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.gdyy.api.domain.dto.GdyyExportLogManagementDto;
import com.zlt.aps.gdyy.entity.GdyyExportLogManagement;
import com.zlt.aps.gdyy.mapper.GdyyExportLogManagementMapper;
import com.zlt.aps.gdyy.service.GdyyExportLogManagementService;
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
public class GdyyExportLogManagementServiceImpl extends ServiceImpl<GdyyExportLogManagementMapper, GdyyExportLogManagement> implements GdyyExportLogManagementService {

    @Resource
    private GdyyExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<GdyyExportLogManagementDto> selectExportLogManagementList(GdyyExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
