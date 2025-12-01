package com.zlt.aps.gdyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.dto.GdyySpecifyMachineDto;
import com.zlt.aps.gdyy.entity.GdyySpecifyMachine;

import java.util.List;

/**
 * <p>
 * 纤维压延定点机台表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface GdyySpecifyMachineMapper extends BaseMapper<GdyySpecifyMachine> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<GdyySpecifyMachineDto> listSpecifyMachine(GdyySpecifyMachineDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<GdyySpecifyMachine> list);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();

}
