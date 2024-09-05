package com.zlt.aps.nc.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;

/**
 * <p>
 * 内衬卷曲信息表 Mapper 接口
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
public interface NcCurlRollMapper extends BaseMapper<NcCurlRoll> {

    /**
     * 根据条件查询内衬卷曲长度列表
     *
     * @param dto
     * @return
     */
    List<NcCurlRoll> listCurlRoll(NcCurlRoll dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<NcCurlRoll> list);
}
