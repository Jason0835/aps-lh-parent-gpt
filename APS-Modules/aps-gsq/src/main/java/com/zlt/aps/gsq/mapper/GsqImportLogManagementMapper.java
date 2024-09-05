package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.dto.GsqImportLogManagementDto;
import com.zlt.aps.gsq.entity.GsqImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface GsqImportLogManagementMapper extends BaseMapper<GsqImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<GsqImportLogManagementDto> listImportLogManagement(GsqImportLogManagementDto dto);

}
