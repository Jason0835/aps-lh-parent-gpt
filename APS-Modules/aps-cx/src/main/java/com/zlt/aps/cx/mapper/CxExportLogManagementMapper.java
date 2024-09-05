package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.CxExportLogManagementDto;
import com.zlt.aps.cx.entity.CxExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface CxExportLogManagementMapper extends BaseMapper<CxExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<CxExportLogManagementDto> listExportLogManagement(CxExportLogManagementDto dto);

}
