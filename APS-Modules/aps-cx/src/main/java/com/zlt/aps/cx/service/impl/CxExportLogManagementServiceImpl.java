package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.api.domain.dto.CxExportLogManagementDto;
import com.zlt.aps.cx.entity.CxExportLogManagement;
import com.zlt.aps.cx.mapper.CxExportLogManagementMapper;
import com.zlt.aps.cx.service.CxExportLogManagementService;
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
public class CxExportLogManagementServiceImpl extends ServiceImpl<CxExportLogManagementMapper, CxExportLogManagement> implements CxExportLogManagementService {

    @Resource
    private CxExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<CxExportLogManagementDto> selectExportLogManagementList(CxExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
