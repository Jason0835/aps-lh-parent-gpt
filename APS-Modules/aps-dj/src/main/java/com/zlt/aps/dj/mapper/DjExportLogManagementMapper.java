package com.zlt.aps.dj.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.dto.DjExportLogManagementDto;
import com.zlt.aps.dj.api.domain.entity.DjExportLogManagement;

/**
 * 工序导出日志管理Mapper接口
 *
 * @author zlt
 * @date 2021-06-07
 */
public interface DjExportLogManagementMapper extends BaseMapper<DjExportLogManagement> {
    /**
     * 根据条件工序导出日志管理
     *
     * @param dto
     * @return
     */
    List<DjExportLogManagementDto> listExportLogManagement(DjExportLogManagementDto dto);

}
