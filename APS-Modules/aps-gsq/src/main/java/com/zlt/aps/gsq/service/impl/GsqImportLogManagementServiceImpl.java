package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.gsq.api.domain.dto.GsqImportLogManagementDto;
import com.zlt.aps.gsq.entity.GsqImportLogManagement;
import com.zlt.aps.gsq.mapper.GsqImportLogManagementMapper;
import com.zlt.aps.gsq.service.GsqImportLogManagementService;
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
public class GsqImportLogManagementServiceImpl extends ServiceImpl<GsqImportLogManagementMapper, GsqImportLogManagement> implements GsqImportLogManagementService {

    @Resource
    private GsqImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<GsqImportLogManagementDto> selectImportLogManagementList(GsqImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
