package com.zlt.aps.nc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.dto.NcExportLogManagementDto;
import com.zlt.aps.nc.entity.NcExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface NcExportLogManagementMapper extends BaseMapper<NcExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<NcExportLogManagementDto> listExportLogManagement(NcExportLogManagementDto dto);

}
