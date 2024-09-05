package com.zlt.aps.tc.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;

/**
 * <p>
 * 胎侧卷曲信息表 Mapper 接口
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
public interface TcCurlRollMapper extends BaseMapper<TcCurlRoll> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<TcCurlRoll> listCurlRoll(TcCurlRoll dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<TcCurlRoll> list);
}
