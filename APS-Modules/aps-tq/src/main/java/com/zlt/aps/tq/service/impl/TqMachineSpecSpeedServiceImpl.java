package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.aps.tq.mapper.TqMachineSpecSpeedMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.ITqMachineSpecSpeedService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎圈机台生产速度Service实现
 * <p>维护胎圈机台-规格维度的生产速度与定额配置，保存前校验机台+胎圈规格组合唯一性，
 * 数据表T_TQ_MACHINE_SPEC_SPEED</p>
 *
 * @author zlt
 */
@Slf4j
@Service
public class TqMachineSpecSpeedServiceImpl extends AbstractDocService<TqMachineSpecSpeed> implements ITqMachineSpecSpeedService {

    @Resource
    private TqMachineSpecSpeedMapper tqMachineSpecSpeedMapper;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Override
    protected String getDocTypeCode() {
        return "TQ_MACHINE_SPEC_SPEED";
    }

    /**
     * 校验机台编码+胎圈规格组合唯一性
     * <p>使用LambdaQueryWrapper查询，排除自身ID（编辑场景），统计未删除记录中是否存在相同组合</p>
     *
     * @param machineSpecSpeed 实体（机台编码+胎圈规格）
     * @return UserConstants.UNIQUE="0" 唯一，UserConstants.NOT_UNIQUE="1" 不唯一
     */
    @Override
    public String checkUnique(TqMachineSpecSpeed machineSpecSpeed) {
        LambdaQueryWrapper<TqMachineSpecSpeed> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(machineSpecSpeed.getId() != null, TqMachineSpecSpeed::getId, machineSpecSpeed.getId());
        wrapper.eq(TqMachineSpecSpeed::getMachineCode, machineSpecSpeed.getMachineCode());
        wrapper.eq(TqMachineSpecSpeed::getBeadCode, machineSpecSpeed.getBeadCode());
        if (tqMachineSpecSpeedMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("machineCode", "beadCode");
    }

    @Override
    public void deleteAll() {
        tqMachineSpecSpeedMapper.deleteAll();
    }

    /**
     * 导入胎圈机台生产速度数据
     * <p>导入流程：Excel通过机台名称(machineName)列识别机台，后台映射为machineCode后保存；
     * 支持覆盖更新(updateSupport=true)和增量新增两种模式</p>
     *
     * @param list          解析后的实体列表
     * @param updateSupport 是否覆盖更新已存在记录
     * @param importLogId   导入日志ID
     * @return 导入结果（成功数/失败数/错误明细）
     */
    @Override
    public AjaxResult importData(List<TqMachineSpecSpeed> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqMachineSpecSpeed> importList = new ArrayList<>();

        // 1. 加载机台信息，构建机台名称→机台编码映射
        List<TqMachineInfo> machineInfoList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, String> machineCodeMap = machineInfoList.stream()
                .collect(Collectors.toMap(TqMachineInfo::getMachineName, TqMachineInfo::getMachineCode, (v1, v2) -> v1));

        // 2. Excel内重复性校验（机台名称+胎圈编码重复）
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(a -> (a.getMachineName() + "_" + a.getBeadCode()), Collectors.counting()));

        // 3. 逐行校验与数据转换
        for (int i = 0; i < list.size(); i++) {
            TqMachineSpecSpeed machineSpecSpeed = list.get(i);

            // 3.1 Excel内重复行检测
            String groupKey = machineSpecSpeed.getMachineName() + "_" + machineSpecSpeed.getBeadCode();
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                machineSpecSpeed.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.specifyMachine.column.machineName");
                String columnName2 = I18nUtil.getMessage("ui.tq.machineSpecSpeed.column.beadCode");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            // 3.2 字段注解校验（@ImportValidated：machineName必填、beadCode必填、standardSpeed必填等）
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, machineSpecSpeed);

            // 3.3 通过机台名称映射机台编码，并校验机台存在性
            String machineName = machineSpecSpeed.getMachineName();
            if (!StringUtil.isEmpty(machineName)) {
                String machineCode = machineCodeMap.get(machineName);
                if (machineCode == null) {
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
                } else {
                    machineSpecSpeed.setMachineCode(machineCode);
                }
            }

            // 3.4 校验通过则加入待保存列表
            if (CollectionUtils.isEmpty(validated)) {
                importList.add(machineSpecSpeed);
            } else {
                failureNum++;
                machineSpecSpeed.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 4. 批量保存
        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                // 覆盖更新模式：使用MERGE SQL（ON DUPLICATE KEY UPDATE）
                successNum = importList.size();
                tqMachineSpecSpeedMapper.mergeSql(importList);
            } else {
                // 增量模式：逐条校验数据库唯一性后保存
                for (int i = 0; i < list.size(); i++) {
                    TqMachineSpecSpeed excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = tqMachineSpecSpeedMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.tq.machineSpecSpeed.column.conflict"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("导入胎圈机台生产速度异常", e);
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
