package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.mapper.Cd90CurlLengthMapper;
import com.zlt.aps.cd90.service.ICd90CurlLengthService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 直裁卷曲长度业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90CurlLengthServiceImpl extends AbstractDocService<Cd90CurlLength> implements ICd90CurlLengthService {

    @Resource
    private Cd90CurlLengthMapper cd90CurlLengthMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD90_CURL_LENGTH";
    }

    /**
     * 校验同一工厂下帘布代号是否唯一。
     *
     * @param entity 卷曲长度信息
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd90CurlLength entity) {
        LambdaQueryWrapper<Cd90CurlLength> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90CurlLength::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90CurlLength::getClothCode, entity.getClothCode());
        wrapper.ne(entity.getId() != null, Cd90CurlLength::getId, entity.getId());
        return cd90CurlLengthMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 导入直裁卷曲长度。
     *
     * @param list 导入列表
     * @param updateSupport 是否更新已有数据
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    @Override
    public AjaxResult importData(List<Cd90CurlLength> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd90CurlLength> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        // 第一轮：基本校验（必填、重复行）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90CurlLength docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 第二轮：业务校验与写入
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90CurlLength docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            // 卷曲长度必须大于0
            if (docEntity.getCurlLength() == null || docEntity.getCurlLength() <= 0) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.alert.cd90CurlLength.curlLengthPositive"), importErrorLogs);
                continue;
            }

            Cd90CurlLength exist = getExistCurlLength(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setCurlLength(docEntity.getCurlLength());
                exist.setRemark(docEntity.getRemark());
                cd90CurlLengthMapper.updateById(exist);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
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

    private Cd90CurlLength getExistCurlLength(Cd90CurlLength entity) {
        LambdaQueryWrapper<Cd90CurlLength> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90CurlLength::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90CurlLength::getClothCode, entity.getClothCode());
        return cd90CurlLengthMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_CURL_LENGTH");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "clothCode");
    }
}