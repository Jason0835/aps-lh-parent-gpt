package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyExportLogManagementDto;
import com.zlt.aps.xwyy.entity.XwyyExportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface XwyyExportLogManagementMapper extends BaseMapper<XwyyExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<XwyyExportLogManagementDto> listExportLogManagement(XwyyExportLogManagementDto dto);

}
