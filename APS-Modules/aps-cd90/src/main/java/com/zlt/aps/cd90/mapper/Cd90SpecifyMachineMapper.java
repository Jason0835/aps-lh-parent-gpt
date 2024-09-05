package com.zlt.aps.cd90.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.dto.Cd90SpecifyMachineDto;
import com.zlt.aps.cd90.entity.Cd90SpecifyMachine;

import java.util.List;

/**
 * <p>
 * 90度裁断定点机台表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface Cd90SpecifyMachineMapper extends BaseMapper<Cd90SpecifyMachine> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<Cd90SpecifyMachineDto> listSpecifyMachine(Cd90SpecifyMachineDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<Cd90SpecifyMachine> list);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
