package com.zlt.aps.nc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.dto.NcImportLogManagementDto;
import com.zlt.aps.nc.entity.NcImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface NcImportLogManagementMapper extends BaseMapper<NcImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<NcImportLogManagementDto> listImportLogManagement(NcImportLogManagementDto dto);

}
