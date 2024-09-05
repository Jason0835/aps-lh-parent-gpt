package com.zlt.aps.gdyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.dto.GdyyImportLogManagementDto;
import com.zlt.aps.gdyy.entity.GdyyImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface GdyyImportLogManagementMapper extends BaseMapper<GdyyImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<GdyyImportLogManagementDto> listImportLogManagement(GdyyImportLogManagementDto dto);

}
