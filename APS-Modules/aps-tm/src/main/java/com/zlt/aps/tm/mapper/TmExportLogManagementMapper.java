package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.dto.TmExportLogManagementDto;
import com.zlt.aps.tm.entity.TmExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface TmExportLogManagementMapper extends BaseMapper<TmExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<TmExportLogManagementDto> listExportLogManagement(TmExportLogManagementDto dto);

}
