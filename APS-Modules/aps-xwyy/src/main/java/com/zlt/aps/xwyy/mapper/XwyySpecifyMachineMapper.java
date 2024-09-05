package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyySpecifyMachineDto;
import com.zlt.aps.xwyy.entity.XwyySpecifyMachine;

import java.util.List;

/**
 * <p>
 * 纤维压延定点机台表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface XwyySpecifyMachineMapper extends BaseMapper<XwyySpecifyMachine> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<XwyySpecifyMachineDto> listSpecifyMachine(XwyySpecifyMachineDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyySpecifyMachine> list);

    /**
     * 删除全部定点机台数据
     */
    void deleteAllSpecifyMachine();

}
