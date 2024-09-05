package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineSapSpecMoldUse;

import java.util.List;

/**
 * 规格使用模数Service接口
 * 
 * @author zlt
 * @date 2022-01-18
 */
public interface CxEngineSapSpecMoldUseService
{

    /**
     * 查询规格使用模数列表
     * 
     * @param cxEngineSapSpecMoldUse 规格使用模数
     * @return 规格使用模数集合
     */
     List<CxEngineSapSpecMoldUse> selectSapSpecMoldUseList(CxEngineSapSpecMoldUse cxEngineSapSpecMoldUse);


}
