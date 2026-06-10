package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmMouthPlate;
import com.zlt.aps.tm.mapper.TmMouthPlateMapper;
import com.zlt.aps.tm.service.ITmMouthPlateService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmMouthPlateServiceImpl extends AbstractDocService<TmMouthPlate> implements ITmMouthPlateService {

    @Resource
    private TmMouthPlateMapper tmMouthPlateMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0806";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0806");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmMouthPlate query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.mouthPlate.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "mouthPlateCode"));
    }
}
