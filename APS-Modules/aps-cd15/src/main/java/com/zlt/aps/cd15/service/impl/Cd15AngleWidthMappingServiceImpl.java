package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.mapper.Cd15AngleWidthMappingMapper;
import com.zlt.aps.cd15.service.ICd15AngleWidthMappingService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CD15角度宽度对应关系服务实现类
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15AngleWidthMappingServiceImpl extends AbstractDocService<Cd15AngleWidthMapping> implements ICd15AngleWidthMappingService {

    @Resource
    private Cd15AngleWidthMappingMapper cd15AngleWidthMappingMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_ANGLE_WIDTH_MAPPING";
    }

    @Override
    public String checkUnique(Cd15AngleWidthMapping entity) {
        LambdaQueryWrapper<Cd15AngleWidthMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15AngleWidthMapping::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15AngleWidthMapping::getCutAngle, entity.getCutAngle());
        wrapper.ne(entity.getId() != null, Cd15AngleWidthMapping::getId, entity.getId());
        return cd15AngleWidthMappingMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd15AngleWidthMapping> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15AngleWidthMapping> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int index = 0; index < list.size(); index++) {
            int errorNum = index + 2;
            Cd15AngleWidthMapping docEntity = list.get(index);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, index, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int index = 0; index < list.size(); index++) {
            int errorNum = index + 2;
            Cd15AngleWidthMapping docEntity = list.get(index);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            Cd15AngleWidthMapping exist = this.getExistMapping(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setClothWidthMax(docEntity.getClothWidthMax());
                exist.setRemark(docEntity.getRemark());
                cd15AngleWidthMappingMapper.updateById(exist);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum, MessageFormat.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (PubUtil.isEmpty(importList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        if (CollectionUtils.isNotEmpty(importList)) {
            successNum += baseDao.saveBatch(importList);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 查询同一工厂下相同角度的配置
     */
    private Cd15AngleWidthMapping getExistMapping(Cd15AngleWidthMapping entity) {
        LambdaQueryWrapper<Cd15AngleWidthMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15AngleWidthMapping::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15AngleWidthMapping::getCutAngle, entity.getCutAngle());
        return cd15AngleWidthMappingMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_ANGLE_WIDTH_MAPPING");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "cutAngle");
    }
}
