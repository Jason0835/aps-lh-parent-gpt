package com.zlt.aps.lh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.lh.api.domain.entity.LhSpecialMaterialBom;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 特殊物料清单配置服务接口
 *
 * @author zlt
 * @date 2026-05-06
 */
public interface ILhSpecialMaterialBomService extends IDocService<LhSpecialMaterialBom> {

    /**
     * 查询列表
     *
     * @param queryWrapper 查询条件
     * @return 结果列表
     */
    List<LhSpecialMaterialBom> selectList(QueryWrapper<LhSpecialMaterialBom> queryWrapper);

    /**
     * 校验唯一性
     *
     * @param query 校验对象
     * @return 唯一性结果
     */
    String checkUnique(LhSpecialMaterialBom query);
}
