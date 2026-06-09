package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmSpecifyMachine;
import com.zlt.aps.tm.mapper.TmSpecifyMachineMapper;
import com.zlt.aps.tm.service.ITmSpecifyMachineService;
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
public class TmSpecifyMachineServiceImpl extends AbstractDocService<TmSpecifyMachine> implements ITmSpecifyMachineService {

    @Resource
    private TmSpecifyMachineMapper tmSpecifyMachineMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0807";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0807");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmSpecifyMachine query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.SpecifyMachine.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "treadCode", "machineCode"));
    }
}
