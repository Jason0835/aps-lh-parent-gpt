package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.dto.GsqExportLogManagementDto;
import com.zlt.aps.gsq.entity.GsqExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface GsqExportLogManagementMapper extends BaseMapper<GsqExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<GsqExportLogManagementDto> listExportLogManagement(GsqExportLogManagementDto dto);

}
