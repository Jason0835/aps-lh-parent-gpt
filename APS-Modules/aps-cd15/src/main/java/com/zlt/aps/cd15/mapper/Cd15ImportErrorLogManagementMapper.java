package com.zlt.aps.cd15.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportErrorLogManagementDto;

import java.util.List;
import java.util.Map;

/**
 * 工序导入日志管理错误信息Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface Cd15ImportErrorLogManagementMapper extends BaseMapper<Cd15ImportErrorLogManagementDto>
{
    /**
     * 根据条件工序导出日志管理
     * @param map
     * @return
     */
    List<Cd15ImportErrorLogManagementDto> listImportErrorLogManagement(Cd15ImportErrorLogManagementDto dto);

}
