package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmOutbountOrdersNotScanEntityMapper;
import com.zlt.aps.maindata.service.IMdmOutbountOrdersNotScanService;
import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmOutbountOrdersNotScanServiceImpl extends AbstractDocService<MdmOutbountOrdersNotScan> implements IMdmOutbountOrdersNotScanService {

    @Autowired
    private MdmOutbountOrdersNotScanEntityMapper mdmOutbountOrdersNotScanEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0217";
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Collections.emptyList();
    }

    @Override
    public String checkUnique(MdmOutbountOrdersNotScan docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmOutbountOrdersNotScan.notUnique"));
        }
        return unique;
    }
}
