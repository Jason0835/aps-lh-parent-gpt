package com.zlt.aps.gdyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.gdyy.api.domain.dto.GdyyImportLogManagementDto;
import com.zlt.aps.gdyy.entity.GdyyImportLogManagement;
import com.zlt.aps.gdyy.mapper.GdyyImportLogManagementMapper;
import com.zlt.aps.gdyy.service.GdyyImportLogManagementService;
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
public class GdyyImportLogManagementServiceImpl extends ServiceImpl<GdyyImportLogManagementMapper, GdyyImportLogManagement> implements GdyyImportLogManagementService {

    @Resource
    private GdyyImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<GdyyImportLogManagementDto> selectImportLogManagementList(GdyyImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
