package com.zlt.aps.cd90.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.dto.Cd90MachineRollMappingDto;
import com.zlt.aps.cd90.entity.Cd90MachineRollMapping;

import java.util.List;

/**
 * <p>
 * 90度裁断帘布大卷与机台的映射表 Mapper 接口
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
public interface Cd90MachineRollMappingMapper extends BaseMapper<Cd90MachineRollMapping> {
    /**
     * 根据条件查询90度裁断帘布大卷与机台的映射表
     *
     * @param dto
     * @return
     */
    List<Cd90MachineRollMappingDto> listMachineRollMapping(Cd90MachineRollMappingDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<Cd90MachineRollMapping> list);

    List<Cd90MachineRollMappingDto> checkUnique(Cd90MachineRollMappingDto dto);

    void deleteAll();
}
