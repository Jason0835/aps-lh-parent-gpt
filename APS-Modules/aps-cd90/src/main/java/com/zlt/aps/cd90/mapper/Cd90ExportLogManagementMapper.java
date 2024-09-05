package com.zlt.aps.cd90.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.dto.Cd90ExportLogManagementDto;
import com.zlt.aps.cd90.entity.Cd90ExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface Cd90ExportLogManagementMapper extends BaseMapper<Cd90ExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<Cd90ExportLogManagementDto> listExportLogManagement(Cd90ExportLogManagementDto dto);

}
