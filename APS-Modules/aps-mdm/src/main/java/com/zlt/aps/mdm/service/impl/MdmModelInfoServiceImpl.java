package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.enums.OperationBusinessEnums;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.service.IMdmModelInfoService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuMouldRel;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmModelInfoServiceImpl.java
 * 描    述：MdmModelInfoServiceImpl模具信息业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-24
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmModelInfoServiceImpl extends AbstractDocService<MdmModelInfo> implements IMdmModelInfoService {

    @Autowired
    private MdmProductModelRelationEntityMapper productModelRelationMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private IMesItfService iMesItfService;

    private static final String MOULD_CODE_SPLIT = "-";

    @Override
    protected String getDocTypeCode() {
        return "0112-1";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0112-1");
        return sysDocType;
    }

    @Override
    public int save(MdmModelInfo docEntityVO) {
        docEntityVO.setBaseVale(null);
        String mouldCode = docEntityVO.getMouldCode();
        /*List<String> mouldCodeList = Collections.singletonList(mouldCode);
        LambdaQueryWrapper<MdmSkuMouldRel> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MdmSkuMouldRel::getMouldCode, mouldCodeList);
        List<MdmSkuMouldRel> productModelRelations = productModelRelationMapper.selectList(wrapper);
        Map<String, MdmSkuMouldRel> modelRelationMap = productModelRelations.stream().collect(Collectors.toMap(MdmSkuMouldRel::getMouldCode, Function.identity(), (s1, s2) -> s1));
        if (!modelRelationMap.containsKey(mouldCode)) {
            throw new RuntimeException(I18nUtil.getMessage("biz.mouldUseStatus.mouldCodeNotExist"));
        }*/
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.modelinfo.notUnique"));
        }
        // 赋值模具
        if (mouldCode.contains(MOULD_CODE_SPLIT)) {
            String[] splitArr = mouldCode.split(MOULD_CODE_SPLIT);
            docEntityVO.setMouldNo(splitArr[1]);
        }
        return baseDao.save(docEntityVO);
    }

    @Override
    public String checkUnique(MdmModelInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.modelinfo.notUnique"));
        }
        // 赋值模具
        String mouldCode = docEntityVO.getMouldCode();
        if (mouldCode.contains(MOULD_CODE_SPLIT)) {
            String[] splitArr = mouldCode.split(MOULD_CODE_SPLIT);
            docEntityVO.setMouldNo(splitArr[1]);
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "mouldCode"));
    }

    /**
     * 赋值寸口
     *
     * @param list 列表
     */
    @Override
    public void setProSize(List<MdmModelInfo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<String> mouldCodeList = list.stream().map(MdmModelInfo::getMouldCode).collect(Collectors.toList());

        List<MdmSkuMouldRel> modelRelationList = new ArrayList<>();
        List<List<String>> splitList = ScmListUtils.getSplitList(mouldCodeList, 500);
        for (List<String> mouldCodes : splitList) {
            modelRelationList.addAll(productModelRelationMapper.selectByMouldCode(mouldCodes));
        }

        Map<String, String> mouldCodeProSizeMap = modelRelationList.stream().collect(Collectors.toMap(MdmSkuMouldRel::getMouldCode, MdmSkuMouldRel::getProSize, (s1, s2) -> {
            if (StringUtils.isBlank(s1) || s2.contains(s1)) {
                return s2;
            }
            if (StringUtils.isBlank(s2) || s1.contains(s2)) {
                return s1;
            }
            return s1 + "," + s2;
        }));

        for (MdmModelInfo mdmModelInfo : list) {
            String mouldCode = mdmModelInfo.getMouldCode();
            if (mouldCodeProSizeMap.containsKey(mouldCode)) {
                mdmModelInfo.setProSize(mouldCodeProSizeMap.get(mouldCode).replaceAll(".00", ""));
            }
        }
    }

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @Override
    public AjaxResult mesCapture() {
        String redisValue = redisService.getCacheObject(OperationBusinessEnums.GRAB_MOLD.getCode());
        if (StringUtils.isNotBlank(redisValue)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.modelInfo.generating"));
        }
        return iMesItfService.syncModelInfo(new MdmModelInfo());
    }
}
