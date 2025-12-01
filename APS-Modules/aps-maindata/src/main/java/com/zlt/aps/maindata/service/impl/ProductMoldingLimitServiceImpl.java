package com.zlt.aps.maindata.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.ProductMoldingLimitMapper;
import com.zlt.aps.maindata.service.IProductMoldingLimitService;
import com.zlt.aps.monthplan.api.domain.entity.ProductMoldingLimit;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductMoldingLimitServiceImpl.java
 * 描    述：ProductMoldingLimitServiceImpl基础数据-品种限制成型机业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Service
public class ProductMoldingLimitServiceImpl extends AbstractDocService<ProductMoldingLimit> implements IProductMoldingLimitService {

    private final ProductMoldingLimitMapper productMoldingLimitMapper;

    public ProductMoldingLimitServiceImpl(ProductMoldingLimitMapper productMoldingLimitMapper) {
        this.productMoldingLimitMapper = productMoldingLimitMapper;
    }

    @Override
    protected String getDocTypeCode() {
        return "0124";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0124");
        return sysDocType;
    }

    @Override
    public String checkUnique(ProductMoldingLimit docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.productMoldingLimit.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "sapCode", "embryoCode", "machineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<ProductMoldingLimit> list, List<ProductMoldingLimit> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> String.join("|", item.getFactoryCode(), item.getSapCode(), item.getEmbryoCode(), item.getMachineCode()), Collectors.counting()));
        serviceCheckParams.put("groupMap", groupMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(ProductMoldingLimit importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Map<String, Long> groupMap = (Map<String, Long>) serviceCheckParams.getOrDefault("groupMap", new HashMap<>());
        String mapKey = String.join("|", importDocEntity.getFactoryCode(), importDocEntity.getSapCode(), importDocEntity.getEmbryoCode(), importDocEntity.getMachineCode());
        if (groupMap.containsKey(mapKey)) {
            if (groupMap.get(mapKey) > 1) {
                String message = I18nUtil.getMessage("ui.data.alert.mdmProductConstruction.excelRepeat");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, String.format(message, errorRowNum), importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
