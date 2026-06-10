package com.zlt.aps.dj.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.dto.DjImportLogManagementDto;
import com.zlt.aps.dj.api.domain.entity.DjImportLogManagement;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface DjImportLogManagementMapper extends BaseMapper<DjImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<DjImportLogManagementDto> listImportLogManagement(DjImportLogManagementDto dto);

}
