package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.mapper.Cd90StorageLaneLimitMapper;
import com.zlt.aps.cd90.service.ICd90StorageLaneLimitService;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90StorageLaneLimitServiceImpl extends AbstractDocService<Cd90StorageLaneLimit> implements ICd90StorageLaneLimitService {

    @Resource
    private Cd90StorageLaneLimitMapper mapper;
    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Override
    protected String getDocTypeCode() {
        return "CD90_STORAGE_LANE_LIMIT";
    }

    @Override
    public String checkUnique(Cd90StorageLaneLimit entity) {
        validateBusiness(entity);
        LambdaQueryWrapper<Cd90StorageLaneLimit> w = new LambdaQueryWrapper<>();
        w.eq(Cd90StorageLaneLimit::getFactoryCode, entity.getFactoryCode());
        w.eq(Cd90StorageLaneLimit::getLaneDate, entity.getLaneDate());
        w.eq(Cd90StorageLaneLimit::getShiftCode, entity.getShiftCode());
        w.eq(Cd90StorageLaneLimit::getStorageLaneCode, entity.getStorageLaneCode());
        // 唯一键去掉 MATERIAL_CODE:同库排同班次唯一(空库排或有帘布库排均唯一),null 用 isNull 匹配
        if (StringUtils.isBlank(entity.getMaterialCode())) {
            w.isNull(Cd90StorageLaneLimit::getMaterialCode);
        } else {
            w.eq(Cd90StorageLaneLimit::getMaterialCode, entity.getMaterialCode());
        }
        w.ne(entity.getId() != null, Cd90StorageLaneLimit::getId, entity.getId());
        return mapper.selectCount(w) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd90StorageLaneLimit> list, boolean updateSupport, Long importLogId) {
        int sn = 0, fn = 0;
        List<Cd90StorageLaneLimit> il = new ArrayList<>();
        List<ImportErrorLog> el = new ArrayList<>();
        String um = I18nUtil.getMessage("import.validated.unique");
        List<String> tireFabricCodeList = mdmConstructionInfoService.listTireFabricCodes();
        Set<String> tireFabricCodes = CollectionUtils.isEmpty(tireFabricCodeList)
                ? new HashSet<>()
                : new HashSet<>(tireFabricCodeList);
        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            Cd90StorageLaneLimit de = list.get(i);
            List<ImportErrorLog> v = ImportExcelValidatedUtils.validated(importLogId, en, de);
            ImportExcelValidatedUtils.validatedRepeat(list, de, i, 2, importLogId, v);
            if (!isTireFabricCodeExists(de, tireFabricCodes)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), en, I18nUtil.getMessage("ui.data.column.cd90SpecifyMachine.clothInvalid"), v);
            }
            try {
                validateBusiness(de);
            } catch (IllegalArgumentException ex) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), en, ex.getMessage(), v);
            }
            if (CollectionUtils.isNotEmpty(v)) {
                fn++;
                de.setId(-999L);
                el.addAll(v);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            int en = i + 2;
            Cd90StorageLaneLimit de = list.get(i);
            if (de.getId() != null && de.getId() == -999L) continue;
            Cd90StorageLaneLimit ex = getExist(de);
            if (ex == null) {
                de.setRowState(RowStateEnum.ADDED);
                il.add(de);
            } else if (updateSupport) {
                ex.setMaterialCode(de.getMaterialCode());
                ex.setCarNum(de.getCarNum());
                ex.setMaxCarNum(de.getMaxCarNum());
                ex.setAvailableCarNum(de.getAvailableCarNum());
                ex.setRemark(de.getRemark());
                mapper.updateById(ex);
                sn++;
            } else {
                fn++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, en, String.format(um, en), el);
            }
        }
        if (PubUtil.isEmpty(il) && sn == 0)
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + sn + "," + fn, el);
        if (CollectionUtils.isNotEmpty(il)) sn += baseDao.saveBatch(il);
        if (fn > 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + sn + "," + fn, el);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + sn);
    }

    private Cd90StorageLaneLimit getExist(Cd90StorageLaneLimit entity) {
        LambdaQueryWrapper<Cd90StorageLaneLimit> w = new LambdaQueryWrapper<>();
        w.eq(Cd90StorageLaneLimit::getFactoryCode, entity.getFactoryCode());
        w.eq(Cd90StorageLaneLimit::getLaneDate, entity.getLaneDate());
        w.eq(Cd90StorageLaneLimit::getShiftCode, entity.getShiftCode());
        w.eq(Cd90StorageLaneLimit::getStorageLaneCode, entity.getStorageLaneCode());
        // 唯一键去掉 MATERIAL_CODE,同库排同班次唯一;null 用 isNull 匹配
        if (StringUtils.isBlank(entity.getMaterialCode())) {
            w.isNull(Cd90StorageLaneLimit::getMaterialCode);
        } else {
            w.eq(Cd90StorageLaneLimit::getMaterialCode, entity.getMaterialCode());
        }
        return mapper.selectOne(w);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("CD90_STORAGE_LANE_LIMIT");
        return t;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一键去掉 MATERIAL_CODE:同库排同班次唯一(空库排或有帘布库排均唯一)
        return Arrays.asList("factoryCode", "laneDate", "shiftCode", "storageLaneCode");
    }

    private boolean isTireFabricCodeExists(Cd90StorageLaneLimit entity, Set<String> tireFabricCodes) {
        if (StringUtils.isBlank(entity.getMaterialCode())) {
            return true;
        }
        return tireFabricCodes.contains(entity.getMaterialCode());
    }

    /**
     * 业务校验:MAX_CAR_NUM 必填且 >0;CAR_NUM <= MAX_CAR_NUM;空库排(MATERIAL_CODE 为空)时 CAR_NUM 必须 = 0。
     * 在 checkUnique 开头调用,新增/编辑保存前拦截;导入时由实体注解+本方法共同保障。
     */
    private void validateBusiness(Cd90StorageLaneLimit entity) {
        if (entity == null) {
            return;
        }
        Integer maxCarNum = entity.getMaxCarNum();
        if (maxCarNum == null || maxCarNum <= 0) {
            throw new IllegalArgumentException("库排最大车数必须维护且大于0");
        }
        Integer carNum = entity.getCarNum();
        if (carNum == null) {
            throw new IllegalArgumentException("当前车数不能为空(空库排请填0)");
        }
        if (carNum < 0) {
            throw new IllegalArgumentException("当前车数不能为负数");
        }
        if (carNum > maxCarNum) {
            throw new IllegalArgumentException("当前车数不能大于最大车数");
        }
        if (StringUtils.isBlank(entity.getMaterialCode()) && carNum != 0) {
            throw new IllegalArgumentException("空库排(胎体代码为空)时当前车数必须为0");
        }
    }
}