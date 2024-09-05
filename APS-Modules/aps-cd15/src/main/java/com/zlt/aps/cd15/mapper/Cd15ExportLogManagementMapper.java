package com.zlt.aps.cd15.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.dto.Cd15ExportLogManagementDto;
import com.zlt.aps.cd15.entity.Cd15ExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface Cd15ExportLogManagementMapper extends BaseMapper<Cd15ExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<Cd15ExportLogManagementDto> listExportLogManagement(Cd15ExportLogManagementDto dto);

}
