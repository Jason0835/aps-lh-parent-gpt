package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.enums.ClassNumThreePlanEnums;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmMachineMaintenance;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmMachineMaintenanceMapper;
import com.zlt.aps.tm.mapper.TmShiftConfigMapper;
import com.zlt.aps.tm.service.ITmMachineMaintenanceService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmMachineMaintenanceServiceImpl extends AbstractDocService<TmMachineMaintenance> implements ITmMachineMaintenanceService {

    @Resource
    private TmMachineMaintenanceMapper tmMachineMaintenanceMapper;

    @Autowired
    private TmMachineInfoMapper machineInfoMapper;

    @Autowired
    private TmShiftConfigMapper tmShiftConfigMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0804";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0804");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmMachineMaintenance query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.machineMaintenance.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("machineCode", "stopStartTime", "stopShift"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TmMachineMaintenance> list, List<TmMachineMaintenance> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseEntity::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        List<TmMachineInfo> machineInfoList = machineInfoMapper.selectList(wrapper);
        Map<String, TmMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMachineCode()), Function.identity(), (s1, s2) -> s1));
        serviceCheckParams.put("machineMap", machineInfoMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmMachineMaintenance importDocEntity, List<com.ruoyi.api.gateway.system.domain.ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 导入时自动计算停机班次
        importDocEntity.setStopShift(resolveStopShift(importDocEntity.getStopStartTime()));

        @SuppressWarnings("unchecked")
        Map<String, TmMachineInfo> machineInfoMap = (Map<String, TmMachineInfo>) serviceCheckParams.get("machineMap");
        String mapKey = GenerageMapKeyUtils.createMapKey(importDocEntity.getFactoryCode(), importDocEntity.getMachineCode());
        if (!machineInfoMap.containsKey(mapKey)) {
            String message = I18nUtil.getMessage("ui.data.alert.tm.machineCodeNotExist");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 根据停机开始时间解析班次
     * 从班次配置表 T_TM_SHIFT_CONFIG 中查询所有启用的班次，匹配停机时间所在的班次，
     * 将 shift_name（夜班/早班/中班）映射为 class_num_three_plan 字典值（01/02/03）
     *
     * @param stopStartTime 停机开始时间
     * @return 班次字典编码（01=夜班, 02=早班, 03=中班），未匹配返回 null
     */
    public String resolveStopShift(Date stopStartTime) {
        if (stopStartTime == null) {
            return null;
        }

        List<TmShiftConfig> shiftConfigs = tmShiftConfigMapper.selectList(
                new QueryWrapper<TmShiftConfig>()
                        .eq("OPEN_FLAG", "1")
        );

        if (shiftConfigs == null || shiftConfigs.isEmpty()) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(stopStartTime);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int timeMinutes = hour * 60 + minute;

        for (TmShiftConfig config : shiftConfigs) {
            int startMinutes = parseTimeToMinutes(config.getPlanStartTime());
            int endMinutes = parseTimeToMinutes(config.getPlanEndTime());

            boolean matched;
            if ("1".equals(config.getCrossDayFlag())) {
                if (endMinutes <= startMinutes) {
                    matched = timeMinutes >= startMinutes || timeMinutes < endMinutes;
                } else {
                    matched = timeMinutes >= startMinutes && timeMinutes < endMinutes;
                }
            } else {
                matched = timeMinutes >= startMinutes && timeMinutes < endMinutes;
            }

            if (matched) {
                return mapShiftNameToCode(config.getShiftName());
            }
        }

        return null;
    }

    /**
     * 将班次名称映射为 class_num_three_plan 字典编码
     * 夜班→01, 早班→02, 中班→03
     */
    private String mapShiftNameToCode(String shiftName) {
        if (shiftName == null) {
            return null;
        }
        if (shiftName.equals(I18nUtil.getMessage(ClassNumThreePlanEnums.CLASS_NIGHT.getClassName(), Locale.CHINA))) {
            return ClassNumThreePlanEnums.CLASS_NIGHT.getClassIndex();
        }
        if (shiftName.equals(I18nUtil.getMessage(ClassNumThreePlanEnums.CLASS_MORNING.getClassName(), Locale.CHINA))) {
            return ClassNumThreePlanEnums.CLASS_MORNING.getClassIndex();
        }
        if (shiftName.equals(I18nUtil.getMessage(ClassNumThreePlanEnums.CLASS_DAY.getClassName(), Locale.CHINA))) {
            return ClassNumThreePlanEnums.CLASS_DAY.getClassIndex();
        }
        return null;
    }

    /**
     * 将 HH:mm:ss 格式的时间字符串转换为分钟数
     */
    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return 0;
        }
        String[] parts = timeStr.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return h * 60 + m;
    }
}
