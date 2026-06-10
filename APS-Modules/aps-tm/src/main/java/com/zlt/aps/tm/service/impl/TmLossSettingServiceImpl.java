package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmLossSetting;
import com.zlt.aps.tm.mapper.TmLossSettingMapper;
import com.zlt.aps.tm.service.ITmLossSettingService;
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
public class TmLossSettingServiceImpl extends AbstractDocService<TmLossSetting> implements ITmLossSettingService {

    @Resource
    private TmLossSettingMapper tmLossSettingMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0810";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0810");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmLossSetting query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.LossSetting.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "treadCode", "machineCode", "settingLevel"));
    }
}
