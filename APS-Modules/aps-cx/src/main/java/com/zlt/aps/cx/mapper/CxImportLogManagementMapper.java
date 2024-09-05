package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.CxImportLogManagementDto;
import com.zlt.aps.cx.entity.CxImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface CxImportLogManagementMapper extends BaseMapper<CxImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<CxImportLogManagementDto> listImportLogManagement(CxImportLogManagementDto dto);

}
