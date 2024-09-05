package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.dto.TqSpecifyMachineDto;
import com.zlt.aps.tq.entity.TqSpecifyMachine;

import java.util.List;

/**
 * <p>
 * 胎圈定点机台表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface TqSpecifyMachineMapper extends BaseMapper<TqSpecifyMachine> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<TqSpecifyMachineDto> listSpecifyMachine(TqSpecifyMachineDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TqSpecifyMachineDto> list);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
