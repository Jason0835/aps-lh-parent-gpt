package com.zlt.aps.cd15.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.dto.Cd15SpecifyMachineDto;
import com.zlt.aps.cd15.entity.Cd15SpecifyMachine;

import java.util.List;

/**
 * <p>
 * 15度裁断定点机台表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface Cd15SpecifyMachineMapper extends BaseMapper<Cd15SpecifyMachine> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<Cd15SpecifyMachineDto> listSpecifyMachine(Cd15SpecifyMachineDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<Cd15SpecifyMachineDto> list);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
