package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.dto.TqExportLogManagementDto;
import com.zlt.aps.tq.entity.TqExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface TqExportLogManagementMapper extends BaseMapper<TqExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<TqExportLogManagementDto> listExportLogManagement(TqExportLogManagementDto dto);

}
