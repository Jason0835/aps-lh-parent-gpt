package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.dto.TmImportErrorLogManagementDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 工序导出日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface TmImportErrorLogManagementMapper extends BaseMapper<TmImportErrorLogManagementDto>
{
    /**
     * 根据条件工序导出日志管理
     * @param map
     * @return
     */
    List<TmImportErrorLogManagementDto> listImportErrorLogManagement(TmImportErrorLogManagementDto dto);

}
