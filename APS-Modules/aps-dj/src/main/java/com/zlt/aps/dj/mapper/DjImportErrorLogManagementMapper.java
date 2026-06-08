package com.zlt.aps.dj.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.dto.DjImportErrorLogManagementDto;

/**
 * 工序导入日志管理错误信息Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface DjImportErrorLogManagementMapper extends BaseMapper<DjImportErrorLogManagementDto>
{
    /**
     * 根据条件工序导出日志管理
     * @param map
     * @return
     */
    List<DjImportErrorLogManagementDto> listImportErrorLogManagement(DjImportErrorLogManagementDto dto);

}
