package com.zlt.aps.dj.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;

/**
 * <p>
 * 垫胶卷曲信息表 Mapper 接口
 * </p>
 *
 * @author zlt
 * @since 2023-09-07
 */
public interface DjCurlRollMapper extends BaseMapper<DjCurlRoll> {

    /**
     * 根据条件查询垫胶卷曲长度列表
     *
     * @param dto
     * @return
     */
    List<DjCurlRoll> listCurlRoll(DjCurlRoll dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<DjCurlRoll> list);
}
