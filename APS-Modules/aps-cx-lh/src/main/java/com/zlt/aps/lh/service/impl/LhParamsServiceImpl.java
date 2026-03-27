package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.lh.mapper.LhParamsEntityMapper;
import com.zlt.aps.lh.service.ILhParamsService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhParamsServiceImpl.java
 * 描    述：LhParamsServiceImpl硫化参数信息业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhParamsServiceImpl extends AbstractDocService<LhParams> implements ILhParamsService {


    @Resource
    private LhParamsEntityMapper lhParamsEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0101";
    }


    /**
     * 查询List
     * @param queryWrapper
     * @return
     */
    @Override
    public List<LhParams> selectList(QueryWrapper<LhParams> queryWrapper){
        return lhParamsEntityMapper.selectList(queryWrapper);
    }

    /**
     * 校验唯一性
     */
    @Override
    public String checkUnique(LhParams query){
        if (query == null) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<LhParams> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(query.getId() != null, LhParams::getId, query.getId());
        wrapper.eq(query.getFactoryCode() != null, LhParams::getFactoryCode, query.getFactoryCode());
        wrapper.eq(query.getParamCode() != null, LhParams::getParamCode, query.getParamCode());
        Long count = lhParamsEntityMapper.selectCount(wrapper);
        if (count > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }


    /**
     * 根据参数编码查询参数信息
     * @param paramCode
     * @return
     */
    @Override
    public LhParams selectOneByParamCode(String paramCode,String factoryCode){
        if(StringUtils.isBlank(paramCode) || StringUtils.isBlank(factoryCode)){
            return null;
        }
        //后续可以通过缓存来获取
        LambdaQueryWrapper<LhParams> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(paramCode != null, LhParams::getParamCode, paramCode);
        wrapper.eq(factoryCode != null, LhParams::getFactoryCode, factoryCode);
        return lhParamsEntityMapper.selectOne(wrapper);
    }

    @Override
    public Map<String,String> listLhParams(String factoryCode) {
        Map<String,String> params=new HashMap<>();
        QueryWrapper<LhParams> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        List<LhParams> paramsList = lhParamsEntityMapper.selectList(queryWrapper);
        if(PubUtil.isNotEmpty(paramsList)){
            for (LhParams lhParam:paramsList)
            {
                params.put(lhParam.getParamCode(),lhParam.getParamValue());
            }
        }
        return params;
    }
}
