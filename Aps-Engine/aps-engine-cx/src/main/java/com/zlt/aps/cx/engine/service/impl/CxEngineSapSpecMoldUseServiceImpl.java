package com.zlt.aps.cx.engine.service.impl;

import com.zlt.aps.cx.engine.domain.CxEngineSapSpecMoldUse;
import com.zlt.aps.cx.engine.mapper.CxEngineSapSpecMoldUseMapper;
import com.zlt.aps.cx.engine.service.CxEngineSapSpecMoldUseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  规格配置使用模数逻辑处理
 */
@Slf4j
@Service("cxEngineSapSpecMoldUseService")
public class CxEngineSapSpecMoldUseServiceImpl implements CxEngineSapSpecMoldUseService {
    @Autowired
    private CxEngineSapSpecMoldUseMapper cxEngineSapSpecMoldUseMapper;

    /**
     * 获取全部配置信息
     * @param cxEngineSapSpecMoldUse 规格使用模数
     * @return
     */
    @Override
    public List<CxEngineSapSpecMoldUse> selectSapSpecMoldUseList(CxEngineSapSpecMoldUse cxEngineSapSpecMoldUse) {
        return cxEngineSapSpecMoldUseMapper.selectSapSpecMoldUseList(cxEngineSapSpecMoldUse);
    }
}
