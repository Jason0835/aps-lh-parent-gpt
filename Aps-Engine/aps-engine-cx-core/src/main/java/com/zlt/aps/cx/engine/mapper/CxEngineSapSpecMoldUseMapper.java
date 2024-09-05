package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineSapSpecMoldUse;

import java.util.List;

/**
 * 规格使用模数Mapper接口
 * 
 * @author zlt
 * @date 2022-01-18
 */
public interface CxEngineSapSpecMoldUseMapper
{
    /**
     * 查询规格使用模数列表
     * 
     * @param cxEngineSapSpecMoldUse 规格使用模数
     * @return 规格使用模数集合
     */
    public List<CxEngineSapSpecMoldUse> selectSapSpecMoldUseList(CxEngineSapSpecMoldUse cxEngineSapSpecMoldUse);

}
