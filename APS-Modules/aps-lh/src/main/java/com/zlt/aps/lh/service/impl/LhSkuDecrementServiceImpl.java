package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.lh.api.domain.entity.LhSkuDecrement;
import com.zlt.aps.lh.mapper.LhSkuDecrementMapper;
import com.zlt.aps.lh.service.ILhSkuDecrementService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * SKU减量清单服务实现
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LhSkuDecrementServiceImpl extends AbstractDocService<LhSkuDecrement> implements ILhSkuDecrementService {

    @Resource
    private LhSkuDecrementMapper lhSkuDecrementMapper;

    @Override
    protected String getDocTypeCode() {
        return "0";
    }

    @Override
    public String[] getQueryFormulas() {
        return new String[0];
    }

    @Override
    public void normalizeConfirmData(LhSkuDecrement entity) {
        if (entity == null) {
            return;
        }
        if (StringUtil.isBlank(entity.getFactoryCode())) {
            entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        entity.setFactoryCode(StringUtils.trim(entity.getFactoryCode()));
        entity.setMaterialCode(StringUtils.trim(entity.getMaterialCode()));
        entity.setMaterialDesc(StringUtils.trim(entity.getMaterialDesc()));
        entity.setEmbryoDesc(StringUtils.trim(entity.getEmbryoDesc()));
        entity.setProductStatus(StringUtils.trim(entity.getProductStatus()));
        entity.setRemark(StringUtils.trim(entity.getRemark()));
        Date currentDate = new Date();
        if (entity.getYear() == null) {
            entity.setYear(DateUtil.year(currentDate));
        }
        if (entity.getMonth() == null) {
            entity.setMonth(DateUtil.month(currentDate) + 1);
        }
    }
    @Override public String checkUnique(LhSkuDecrement entity) {
        QueryWrapper<LhSkuDecrement> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID", entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        queryWrapper.eq("YEAR", entity.getYear());
        queryWrapper.eq("MONTH", entity.getMonth());
        queryWrapper.eq("MATERIAL_CODE", entity.getMaterialCode());
        return lhSkuDecrementMapper.selectCount(queryWrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "year", "month", "materialCode");
    }
}
