package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.dto.TmImportLogManagementDto;
import com.zlt.aps.tm.entity.TmImportLogManagement;

import java.util.List;

/**
 * 工序导出日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface TmImportLogManagementMapper extends BaseMapper<TmImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<TmImportLogManagementDto> listImportLogManagement(TmImportLogManagementDto dto);

}
