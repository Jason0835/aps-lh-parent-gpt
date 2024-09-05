package com.zlt.aps.nc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.dto.NcSpecifyMachineDto;
import com.zlt.aps.nc.entity.NcSpecifyMachine;

import java.util.List;

/**
 * <p>
 * 内衬定点机台表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface NcSpecifyMachineMapper extends BaseMapper<NcSpecifyMachine> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<NcSpecifyMachineDto> listSpecifyMachine(NcSpecifyMachineDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<NcSpecifyMachineDto> list);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();
}
