package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.dto.GsqSpecifyMachineDto;
import com.zlt.aps.gsq.entity.GsqSpecifyMachine;

import java.util.List;

/**
 * <p>
 * 钢丝圈定点机台表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface GsqSpecifyMachineMapper extends BaseMapper<GsqSpecifyMachine> {

    /**
     * 根据条件查询定点机台顺序列表
     *
     * @param dto
     * @return
     */
    List<GsqSpecifyMachineDto> listSpecifyMachine(GsqSpecifyMachineDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GsqSpecifyMachineDto> list);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
