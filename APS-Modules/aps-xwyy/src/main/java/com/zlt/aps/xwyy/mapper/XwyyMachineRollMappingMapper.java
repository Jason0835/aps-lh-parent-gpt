package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyMachineRollMappingDto;
import com.zlt.aps.xwyy.entity.XwyyMachineRollMapping;

import java.util.List;

/**
 * <p>
 * 纤维压延帘布大卷与机台的映射表 Mapper 接口
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
public interface XwyyMachineRollMappingMapper extends BaseMapper<XwyyMachineRollMapping> {
    /**
     * 根据条件查纤维压延帘布大卷与机台映射表
     *
     * @param dto
     * @return
     */
    List<XwyyMachineRollMappingDto> listXwyyMachineRollMapping(XwyyMachineRollMappingDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyMachineRollMapping> list);

}
