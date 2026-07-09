package com.zlt.aps.nc.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.nc.api.domain.entity.NcGlueGroupOrder;
import com.zlt.aps.nc.mapper.NcGlueGroupOrderMapper;
import com.zlt.aps.nc.service.NcGlueGroupOrderService;
import com.zlt.aps.utils.ApsBeanUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;

/**
 * <p>
 * 内衬胶料组别顺序维护 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
@Service
public class NcGlueGroupOrderServiceImpl extends AbstractDocService<NcGlueGroupOrder>
        implements NcGlueGroupOrderService {

    @Resource
    private NcGlueGroupOrderMapper ncGlueGroupOrderMapper;

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    @Override
    public List<NcGlueGroupOrder> listGlueGroupOrder(NcGlueGroupOrder dto) {
        return ncGlueGroupOrderMapper.selectList(ApsBeanUtils.builderCondition(dto));
    }

    /**
     * 根据code判断胶料组号是否已经存在
     */
    @Override
    public String checkGlueGroupCodeUnique(NcGlueGroupOrder dto) {
        if (dto == null || StringUtils.isBlank(dto.getGlueGroupCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<NcGlueGroupOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("GLUE_GROUP_CODE", dto.getGlueGroupCode());
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());
        }
        List<NcGlueGroupOrder> list = ncGlueGroupOrderMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 查询出被使用了的胶料组别
     *
     * @param glueGroupIds
     * @return
     */
    @Override
    public List<String> listUserdGlueGroup(List<Long> glueGroupIds) {
        LambdaQueryWrapper<NcGlueGroupOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(NcGlueGroupOrder::getId, glueGroupIds);
        return ncGlueGroupOrderMapper.selectList(queryWrapper).stream().map(NcGlueGroupOrder::getGlueGroupCode)
                .distinct().collect(Collectors.toList());
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("glueGroupCode"));
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
    public AjaxResult importData(List<NcGlueGroupOrder> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<NcGlueGroupOrder> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.cxStock.embryoCodeNotUnique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcGlueGroupOrder docEntity = list.get(i);
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
            NcGlueGroupOrder docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    LambdaQueryWrapper<NcGlueGroupOrder> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(NcGlueGroupOrder::getFactoryCode, docEntity.getFactoryCode());
                    queryWrapper.eq(NcGlueGroupOrder::getGlueGroupCode, docEntity.getGlueGroupCode());
                    logger.info("updateSupport:{}", docEntity);
                    List<NcGlueGroupOrder> existList = ncGlueGroupOrderMapper.selectList(queryWrapper);
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
