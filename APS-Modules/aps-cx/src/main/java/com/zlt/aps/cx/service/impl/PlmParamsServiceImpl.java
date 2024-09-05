package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.api.domain.entity.PlmConstructionInfo;
import com.zlt.aps.cx.mapper.PlmParamsMapper;
import com.zlt.aps.cx.service.PlmParamsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * PLM工序参数维护功能逻辑层
 */
@Service
public class PlmParamsServiceImpl extends ServiceImpl<PlmParamsMapper, PlmConstructionInfo> implements PlmParamsService {

    @Resource
    private PlmParamsMapper plmParamsMapper;

    /**
     * 查询PLM参数信息列表
     *
     * @param params PLM参数信息
     * @return PLM参数信息
     */
    @Override
    public List<PlmConstructionInfo> selectParamsList(PlmConstructionInfo params) {
        return plmParamsMapper.listParams(params);
    }
}
