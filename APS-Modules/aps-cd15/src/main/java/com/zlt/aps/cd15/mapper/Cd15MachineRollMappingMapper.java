package com.zlt.aps.cd15.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.dto.Cd15MachineRollMappingDto;
import com.zlt.aps.cd15.entity.Cd15MachineRollMapping;

import java.util.List;

/**
 * <p>
 * 钢带大卷与机台的映射表 Mapper 接口
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
public interface Cd15MachineRollMappingMapper extends BaseMapper<Cd15MachineRollMapping> {
    /**
     * 根据条件查询钢带大卷与机台的映射表
     *
     * @param dto
     * @return
     */
    List<Cd15MachineRollMappingDto> listMachineRollMapping(Cd15MachineRollMappingDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<Cd15MachineRollMappingDto> list);

    void deleteAll();
}
