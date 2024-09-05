package com.zlt.aps.xwyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.xwyy.api.domain.dto.XwyyImportLogManagementDto;
import com.zlt.aps.xwyy.entity.XwyyImportLogManagement;
import com.zlt.aps.xwyy.mapper.XwyyImportLogManagementMapper;
import com.zlt.aps.xwyy.service.XwyyImportLogManagementService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工序导入日志管理Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class XwyyImportLogManagementServiceImpl extends ServiceImpl<XwyyImportLogManagementMapper, XwyyImportLogManagement> implements XwyyImportLogManagementService {

    @Resource
    private XwyyImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<XwyyImportLogManagementDto> selectImportLogManagementList(XwyyImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
