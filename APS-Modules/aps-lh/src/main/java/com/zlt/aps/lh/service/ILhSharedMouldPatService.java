package com.zlt.aps.lh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.lh.api.domain.entity.LhSharedMouldPat;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 共用模具花纹配置服务接口
 *
 * @author zlt
 * @date 2026-05-14
 */
public interface ILhSharedMouldPatService extends IDocService<LhSharedMouldPat> {

    /**
     * 查询列表
     *
     * @param queryWrapper 查询条件
     * @return 结果列表
     */
    List<LhSharedMouldPat> selectList(QueryWrapper<LhSharedMouldPat> queryWrapper);

    /**
     * 校验唯一性
     *
     * @param query 校验对象
     * @return 唯一性结果
     */
    String checkUnique(LhSharedMouldPat query);
}
