package com.zlt.aps.nc.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.nc.api.domain.entity.NcGlueOrder;
import com.zlt.aps.nc.mapper.NcGlueOrderMapper;
import com.zlt.aps.nc.service.NcGlueOrderService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.StringUtil;

/**
 * <p>
 * 内衬胶料顺序维护 服务实现类
 * </p>
 *
 * @author zlt
 * @since 2026-06-28
 */
@Service
public class NcGlueOrderServiceImpl extends AbstractDocService<NcGlueOrder> implements NcGlueOrderService {

    @Resource
    private NcGlueOrderMapper ncGlueOrderMapper;

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    @Override
    public List<NcGlueOrder> listGlueOrder(NcGlueOrder dto) {
        LambdaQueryWrapper<NcGlueOrder> wrapper = new LambdaQueryWrapper<>();
        if (dto.getGlueGroupId() != null) {
            wrapper.eq(NcGlueOrder::getGlueGroupId, dto.getGlueGroupId());
        }
        if (!StringUtil.isEmpty(dto.getGlueCode())) {
            wrapper.like(NcGlueOrder::getGlueCode, dto.getGlueCode());
        }
        wrapper.orderByAsc(NcGlueOrder::getOrderNum);
        List<NcGlueOrder> entityList = ncGlueOrderMapper.selectList(wrapper);
        return entityList.stream().map(entity -> {
            NcGlueOrder resultDto = new NcGlueOrder();
            BeanUtils.copyProperties(entity, resultDto);
            return resultDto;
        }).collect(Collectors.toList());
    }

    /**
     * 根据胶料code判断胶料组号是否已经存在
     */
    @Override
    public String checkGlueCodeUnique(NcGlueOrder dto) {
        if (dto == null || StringUtils.isBlank(dto.getGlueCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<NcGlueOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NcGlueOrder::getGlueCode, dto.getGlueCode());
        if (dto.getId() != null) {
            wrapper.ne(NcGlueOrder::getId, dto.getId()); // 编辑的时候校验，要过滤掉自身的id
        }
        List<NcGlueOrder> list = ncGlueOrderMapper.selectList(wrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @Override
    public void deleteGlueOrder(Long[] ids) {
        LambdaUpdateWrapper<NcGlueOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(NcGlueOrder::getId, Arrays.asList(ids));
        wrapper.set(NcGlueOrder::getIsDelete, DeleteFlagEnum.DELETED.getCode());
        ncGlueOrderMapper.update(null, wrapper);
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<NcGlueOrder> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<NcGlueOrder> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.cxStock.embryoCodeNotUnique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcGlueOrder docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcGlueOrder docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    LambdaQueryWrapper<NcGlueOrder> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(NcGlueOrder::getFactoryCode, docEntity.getFactoryCode());
                    queryWrapper.eq(NcGlueOrder::getGlueGroupId, docEntity.getGlueGroupId());
                    logger.info("updateSupport:{}", docEntity);
                    List<NcGlueOrder> existList = ncGlueOrderMapper.selectList(queryWrapper);
                    if (existList.size() > 1) {
                        failureNum++;
                        String multipleMsg = I18nUtil.getMessage("ui.data.alert.cxStock.multipleRecords");
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, String.format(multipleMsg, errorNum), importErrorLogs);
                        continue;
                    } else if (existList.size() == 1) {
                        docEntity.setId(existList.get(0).getId());
                        importList.add(docEntity);
                        successNum++;
                    }
                } else {
                    failureNum++;
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                            String.format(uniqueMsg, errorNum), importErrorLogs);
                }
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        }
        baseDao.saveBatch(importList);
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
