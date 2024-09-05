package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.tq.api.domain.dto.TqImportLogManagementDto;
import com.zlt.aps.tq.entity.TqImportLogManagement;
import com.zlt.aps.tq.mapper.TqImportLogManagementMapper;
import com.zlt.aps.tq.service.TqImportLogManagementService;
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
public class TqImportLogManagementServiceImpl extends ServiceImpl<TqImportLogManagementMapper, TqImportLogManagement> implements TqImportLogManagementService {

    @Resource
    private TqImportLogManagementMapper importLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<TqImportLogManagementDto> selectImportLogManagementList(TqImportLogManagementDto dto) {
        return importLogManagementMapper.listImportLogManagement(dto);
    }

}
