package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyImportLogManagementDto;
import com.zlt.aps.xwyy.entity.XwyyImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface XwyyImportLogManagementMapper extends BaseMapper<XwyyImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<XwyyImportLogManagementDto> listImportLogManagement(XwyyImportLogManagementDto dto);

}
