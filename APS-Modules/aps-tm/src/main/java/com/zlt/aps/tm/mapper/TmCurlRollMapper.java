package com.zlt.aps.tm.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;

/**
 * <p>
 * 胎面卷曲信息表 Mapper 接口
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
public interface TmCurlRollMapper extends BaseMapper<TmCurlRoll> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<TmCurlRoll> listCurlRoll(TmCurlRoll dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<TmCurlRoll> list);
}
