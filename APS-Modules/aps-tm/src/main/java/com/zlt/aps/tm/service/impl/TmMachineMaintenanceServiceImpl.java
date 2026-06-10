package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmMachineMaintenance;
import com.zlt.aps.tm.mapper.TmMachineMaintenanceMapper;
import com.zlt.aps.tm.service.ITmMachineMaintenanceService;
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
public class TmMachineMaintenanceServiceImpl extends AbstractDocService<TmMachineMaintenance> implements ITmMachineMaintenanceService {

    @Resource
    private TmMachineMaintenanceMapper tmMachineMaintenanceMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0804";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0804");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmMachineMaintenance query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            String msg = I18nUtil.getMessage("ui.data.alert.tm.machineMaintenance.notUnique");
            throw new ServiceException(com.ruoyi.common.utils.StringUtils.format(msg,
                    query.getFactoryCode()));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode"));
    }
}
