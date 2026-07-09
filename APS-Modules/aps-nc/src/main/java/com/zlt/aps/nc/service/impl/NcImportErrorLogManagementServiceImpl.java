package com.zlt.aps.nc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.dto.NcImportErrorLogManagementDto;
import com.zlt.aps.nc.mapper.NcImportErrorLogManagementMapper;
import com.zlt.aps.nc.service.NcImportErrorLogManagementService;
import com.zlt.bill.common.service.AbstractDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工序导入日志管理错误日志Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class NcImportErrorLogManagementServiceImpl extends AbstractDocService<NcImportErrorLogManagementDto> implements NcImportErrorLogManagementService {

    @Autowired
    private NcImportErrorLogManagementMapper ncImportErrorLogManagementMapper;

    /**
     * 查询工序导出日志管理错误日志列表
     *
     * @param id 工序导出日志管理id errorRow错误行数
     * @return 工序导出日志错误日志管理
     */
    @Override
    public List<NcImportErrorLogManagementDto> selectImportErrorLogManagementList(NcImportErrorLogManagementDto dto) {
        LambdaQueryWrapper<NcImportErrorLogManagementDto> wrapper = new LambdaQueryWrapper<>();
        if (dto != null) {
            if (StringUtils.isNotEmpty(dto.getImportLogId())) {
                wrapper.eq(NcImportErrorLogManagementDto::getImportLogId, dto.getImportLogId());
            }
            if (StringUtils.isNotEmpty(dto.getErrorRow())) {
                wrapper.eq(NcImportErrorLogManagementDto::getErrorRow, dto.getErrorRow());
            }
        }
        wrapper.orderByAsc(NcImportErrorLogManagementDto::getId);
        return ncImportErrorLogManagementMapper.selectList(wrapper);
    }

    @Override
    protected String getDocTypeCode() {
        return null;
    }

}
