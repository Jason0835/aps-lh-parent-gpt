package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.api.domain.dto.BomInfoDto;
import com.zlt.aps.cx.entity.BomInfo;
import com.zlt.aps.cx.mapper.BomInfoMapper;
import com.zlt.aps.cx.service.BomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * BOM信息Service业务层处理
 *
 * @author Chen
 * @date 2021-06-11
 */
@Service
public class BomInfoServiceImpl extends ServiceImpl<BomInfoMapper, BomInfo> implements BomInfoService {

    @Autowired
    private BomInfoMapper bomInfoMapper;


    /**
     * 查询BOM信息列表
     *
     * @param bomInfo BOM信息
     * @return BOM信息
     */
    @Override
    public List<BomInfoDto> selectBomInfoList(BomInfo bomInfo) {
        return bomInfoMapper.selectBomInfoList(bomInfo);
    }

}
