package com.zlt.aps.cd90.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportLogManagementDto;
import com.zlt.aps.cd90.entity.Cd90ImportLogManagement;

import java.util.List;

/**
 * 工序导入日志管理Mapper接口
 * 
 * @author zlt
 * @date 2021-06-07
 */
public interface Cd90ImportLogManagementMapper extends BaseMapper<Cd90ImportLogManagement>
{
    /**
     * 根据条件工序导出日志管理
     * @param dto
     * @return
     */
    List<Cd90ImportLogManagementDto> listImportLogManagement(Cd90ImportLogManagementDto dto);

}
