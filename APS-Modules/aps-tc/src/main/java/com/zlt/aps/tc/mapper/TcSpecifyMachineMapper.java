package com.zlt.aps.tc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.dto.TcSpecifyMachineDto;
import com.zlt.aps.tc.entity.TcSpecifyMachine;

import java.util.List;

/**
 * <p>
 * 胎面定点机台表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface TcSpecifyMachineMapper extends BaseMapper<TcSpecifyMachine> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<TcSpecifyMachineDto> listSpecifyMachine(TcSpecifyMachineDto dto);

    /**
     * 合并操作，存在则更新，否则新增
     */
    public void mergeSql(List<TcSpecifyMachineDto> list);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
