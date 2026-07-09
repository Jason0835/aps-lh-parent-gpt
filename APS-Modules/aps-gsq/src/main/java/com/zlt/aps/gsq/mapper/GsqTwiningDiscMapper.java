package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;

import java.util.List;

/**
 * 钢丝圈缠绕盘 Mapper 接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface GsqTwiningDiscMapper extends BaseMapper<GsqTwiningDisc> {

    /**
     * 查询钢丝圈缠绕盘列表
     *
     * @param entity 查询条件
     * @return 列表
     */
    List<GsqTwiningDisc> listTwiningDisc(GsqTwiningDisc entity);

    /**
     * 校验缠绕盘编码是否已存在
     *
     * @param entity 实体
     * @return 已存在数量（0表示唯一，>0表示不唯一）
     */
    int checkUnique(GsqTwiningDisc entity);

    /**
     * 批量合并保存（存在则更新，否则新增），用于导入场景
     *
     * @param list 待保存数据集合
     */
    void mergeSql(List<GsqTwiningDisc> list);
}
