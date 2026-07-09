package com.zlt.aps.nc.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.dto.NcExportLogManagementDto;
import com.zlt.aps.nc.entity.NcExportLogManagement;
import com.zlt.aps.nc.mapper.NcExportLogManagementMapper;
import com.zlt.aps.nc.service.NcExportLogManagementService;

/**
 * 工序导出日志管理Service业务层处理
 *
 * @author zlt
 * @date 2026-07-07
 */
@Service
public class NcExportLogManagementServiceImpl extends ServiceImpl<NcExportLogManagementMapper, NcExportLogManagement>
        implements NcExportLogManagementService {

    @Autowired
    private NcExportLogManagementMapper ncExportLogManagementMapper;

    /**
     * 查询工序导出日志管理列表
     *
     * @param dto 工序导出日志管理
     * @return 工序导出日志管理
     */
    @Override
    public List<NcExportLogManagementDto> selectExportLogManagementList(NcExportLogManagementDto dto) {
        LambdaQueryWrapper<NcExportLogManagement> wrapper = new LambdaQueryWrapper<>();
        if (dto != null) {
            if (StringUtils.isNotEmpty(dto.getProcedureCode())) {
                wrapper.like(NcExportLogManagement::getProcedureCode, dto.getProcedureCode());
            }
            if (StringUtils.isNotEmpty(dto.getFunctionCode())) {
                wrapper.like(NcExportLogManagement::getFunctionCode, dto.getFunctionCode());
            }
            if (StringUtils.isNotEmpty(dto.getFunctionName())) {
                wrapper.like(NcExportLogManagement::getFunctionName, dto.getFunctionName());
            }
            if (dto.getId() != null) {
                wrapper.eq(NcExportLogManagement::getId, dto.getId());
            }
        }
        wrapper.orderByDesc(NcExportLogManagement::getId);
        List<NcExportLogManagement> list = ncExportLogManagementMapper.selectList(wrapper);
        return list.stream().map(entity -> {
            NcExportLogManagementDto resultDto = new NcExportLogManagementDto();
            BeanUtils.copyProperties(entity, resultDto);
            return resultDto;
        }).collect(Collectors.toList());
    }
}
