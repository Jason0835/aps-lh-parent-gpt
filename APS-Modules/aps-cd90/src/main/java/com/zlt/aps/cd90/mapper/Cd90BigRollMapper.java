package com.zlt.aps.cd90.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.dto.Cd90BigRollDto;
import com.zlt.aps.cd90.entity.Cd90BigRoll;

import java.util.List;

/**
 * <p>
 * 90度裁断帘布大卷信息表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
public interface Cd90BigRollMapper extends BaseMapper<Cd90BigRoll> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<Cd90BigRollDto> listBigRoll(Cd90BigRollDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<Cd90BigRoll> list);
}
