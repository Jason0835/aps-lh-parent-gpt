package com.zlt.aps.nc.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcMachineMaintenance;
import com.zlt.aps.nc.mapper.NcMachineInfoMapper;
import com.zlt.aps.nc.mapper.NcMachineMaintenanceMapper;
import com.zlt.aps.nc.service.NcMachineMaintenanceService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 内衬机台维修计划ServiceImpl业务层处理
 *
 * @author zlt
 * @date 2026-08-06
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class NcMachineMaintenanceServiceImpl extends AbstractDocService<NcMachineMaintenance>
        implements NcMachineMaintenanceService {

    @Autowired
    private NcMachineMaintenanceMapper maintenanceMapper;

    @Autowired
    private NcMachineInfoMapper machineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "";
    }

    /**
     * 唯一性校验：工厂 + 机台 + 停机开始时间，重复时抛出异常阻止保存
     *
     * @param docEntityVO 维修计划实体
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(NcMachineMaintenance docEntityVO) {
        if (this.countDuplicate(docEntityVO) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.machineMaintenance.notUnique"));
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 统计相同工厂、机台、停机开始时间的记录数（编辑时排除自身）
     *
     * @param docEntityVO 维修计划实体
     * @return 重复记录数
     */
    private Long countDuplicate(NcMachineMaintenance docEntityVO) {
        QueryWrapper<NcMachineMaintenance> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getId()), "ID", docEntityVO.getId());
        queryWrapper.eq("FACTORY_CODE", docEntityVO.getFactoryCode());
        queryWrapper.eq("MACHINE_CODE", docEntityVO.getMachineCode());
        queryWrapper.eq("STOP_START_TIME", docEntityVO.getStopStartTime());
        return maintenanceMapper.selectCount(queryWrapper);
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "machineCode", "stopStartTime"));
    }

    /**
     * 导入数据（内衬模块采用 List 直传模式），逐行校验：字段必填、行内重复、机台存在性、库内重复
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入结果提示
     */
    @Override
    public AjaxResult importData(List<NcMachineMaintenance> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<NcMachineMaintenance> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.machineMaintenance.notUnique");
        String machineNotExistMsg = I18nUtil.getMessage("ui.data.alert.nc.machineMaintenance.machineNotExist");

        // 加载内衬机台编码集合，用于校验导入记录对应的机台是否存在
        Map<String, NcMachineInfo> machineInfoMap = machineInfoMapper.selectList(null).stream()
                .collect(Collectors.toMap(NcMachineInfo::getMachineCode, Function.identity(), (s1, s2) -> s1));

        // 第一轮：字段校验、行内重复校验、机台存在性校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcMachineMaintenance docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));

            // 业务校验：机台必须存在于内衬机台档案中
            if (!machineInfoMap.containsKey(docEntity.getMachineCode())) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, machineNotExistMsg, importErrorLogs);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 第二轮：库内重复校验，updateSupport 时更新已存在记录
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcMachineMaintenance docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (this.countDuplicate(docEntity) <= 0) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    LambdaQueryWrapper<NcMachineMaintenance> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(NcMachineMaintenance::getFactoryCode, docEntity.getFactoryCode());
                    queryWrapper.eq(NcMachineMaintenance::getMachineCode, docEntity.getMachineCode());
                    queryWrapper.eq(NcMachineMaintenance::getStopStartTime, docEntity.getStopStartTime());
                    List<NcMachineMaintenance> existList = maintenanceMapper.selectList(queryWrapper);
                    if (existList.size() > 1) {
                        failureNum++;
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, String.format(uniqueMsg, errorNum), importErrorLogs);
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
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }
}
