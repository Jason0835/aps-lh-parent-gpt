package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.api.domain.dto.CxImportLogManagementDto;
import com.zlt.aps.cx.entity.CxImportLogManagement;
import com.zlt.aps.cx.mapper.CxImportLogManagementMapper;
import com.zlt.aps.cx.service.CxImportLogManagementService;
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
public class CxImportLogManagementServiceImpl extends ServiceImpl<CxImportLogManagementMapper, CxImportLogManagement> implements CxImportLogManagementService {

    @Resource
    private CxImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<CxImportLogManagementDto> selectImportLogManagementList(CxImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
