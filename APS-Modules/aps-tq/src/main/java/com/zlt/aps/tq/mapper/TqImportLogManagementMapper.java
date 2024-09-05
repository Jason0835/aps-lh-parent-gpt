package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.dto.TqImportLogManagementDto;
import com.zlt.aps.tq.entity.TqImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface TqImportLogManagementMapper extends BaseMapper<TqImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<TqImportLogManagementDto> listImportLogManagement(TqImportLogManagementDto dto);

}
