package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import com.zlt.aps.tm.mapper.TmGlueMachineRealMapper;
import com.zlt.aps.tm.service.ITmGlueMachineRealService;
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
public class TmGlueMachineRealServiceImpl extends AbstractDocService<TmGlueMachineReal> implements ITmGlueMachineRealService {

    @Resource
    private TmGlueMachineRealMapper tmGlueMachineRealMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0802";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0802");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmGlueMachineReal query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            String msg = I18nUtil.getMessage("ui.data.alert.tm.GlueMachineReal.notUnique");
            throw new ServiceException(com.ruoyi.common.utils.StringUtils.format(msg,
                    query.getFactoryCode(), query.getGlueCode(), query.getMachineCode()));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "glueCode", "machineCode"));
    }
}
