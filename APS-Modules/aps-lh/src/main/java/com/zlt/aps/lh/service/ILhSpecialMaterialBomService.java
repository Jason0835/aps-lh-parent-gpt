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

    /**
     * 校验分类冲突。
     * 有物料情况：同一工厂+物料编码下，19.5寸宽基和22.5寸宽基互斥，芯片胎可与它们组合。
     * 有结构无物料情况：同一工厂+结构下，19.5寸宽基和22.5寸宽基互斥，芯片胎可与它们组合。
     *
     * @param entity 待校验实体
     * @return 冲突结果，null表示无冲突，非null表示冲突描述
     */
    String checkCategoryConflict(LhSpecialMaterialBom entity);
}
