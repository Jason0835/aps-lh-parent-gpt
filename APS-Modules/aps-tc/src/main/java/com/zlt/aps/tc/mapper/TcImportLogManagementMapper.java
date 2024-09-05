package com.zlt.aps.tc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.dto.TcImportLogManagementDto;
import com.zlt.aps.tc.entity.TcImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface TcImportLogManagementMapper extends BaseMapper<TcImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<TcImportLogManagementDto> listImportLogManagement(TcImportLogManagementDto dto);

}
