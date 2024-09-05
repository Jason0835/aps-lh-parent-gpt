package com.zlt.aps.xwyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.xwyy.api.domain.dto.XwyyExportLogManagementDto;
import com.zlt.aps.xwyy.entity.XwyyExportLogManagement;
import com.zlt.aps.xwyy.mapper.XwyyExportLogManagementMapper;
import com.zlt.aps.xwyy.service.XwyyExportLogManagementService;
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
public class XwyyExportLogManagementServiceImpl extends ServiceImpl<XwyyExportLogManagementMapper, XwyyExportLogManagement> implements XwyyExportLogManagementService {

    @Resource
    private XwyyExportLogManagementMapper exportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<XwyyExportLogManagementDto> selectExportLogManagementList(XwyyExportLogManagementDto dto) {
        return exportLogManagementMapper.listExportLogManagement(dto);
    }

}
