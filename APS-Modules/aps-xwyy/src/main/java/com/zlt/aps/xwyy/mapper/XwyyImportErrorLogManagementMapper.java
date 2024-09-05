package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyImportErrorLogManagementDto;

import java.util.List;
import java.util.Map;

/**
 * 工序导入日志管理错误信息Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface XwyyImportErrorLogManagementMapper extends BaseMapper<XwyyImportErrorLogManagementDto>
{
    /**
     * 根据条件工序导出日志管理
     * @param map
     * @return
     */
    List<XwyyImportErrorLogManagementDto> listImportErrorLogManagement(XwyyImportErrorLogManagementDto dto);

}
