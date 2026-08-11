package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.enums.ClassNumThreePlanEnums;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcMachineMaintenance;
import com.zlt.aps.tc.api.domain.entity.TcShiftConfig;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.mapper.TcMachineMaintenanceMapper;
import com.zlt.aps.tc.mapper.TcShiftConfigMapper;
import com.zlt.aps.tc.service.ITcMachineMaintenanceService;
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
public class TcMachineMaintenanceServiceImpl extends AbstractDocService<TcMachineMaintenance> implements ITcMachineMaintenanceService {

    @Resource
    private TcMachineMaintenanceMapper tcMachineMaintenanceMapper;

    @Autowired
    private TcMachineInfoMapper tcMachineInfoMapper;

    @Autowired
    private TcShiftConfigMapper tcShiftConfigMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0904";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0904");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcMachineMaintenance query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.machineMaintenance.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("machineCode", "stopStartTime", "stopShift"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TcMachineMaintenance> list, List<TcMachineMaintenance> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        LambdaQueryWrapper<TcMachineInfo> wrapper = new LambdaQueryWrapper<>();
        List<TcMachineInfo> machineInfoList = tcMachineInfoMapper.selectList(wrapper);
        Map<String, TcMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMachineCode()), Function.identity(), (s1, s2) -> s1));
        serviceCheckParams.put("machineMap", machineInfoMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TcMachineMaintenance importDocEntity, List<com.ruoyi.api.gateway.system.domain.ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 导入时自动计算停机班次
        importDocEntity.setStopShift(this.resolveStopShift(importDocEntity.getFactoryCode(), importDocEntity.getStopStartTime()));

        @SuppressWarnings("unchecked")
        Map<String, TcMachineInfo> machineInfoMap = (Map<String, TcMachineInfo>) serviceCheckParams.get("machineMap");
        String mapKey = GenerageMapKeyUtils.createMapKey(importDocEntity.getFactoryCode(), importDocEntity.getMachineCode());
        if (!machineInfoMap.containsKey(mapKey)) {
            String message = I18nUtil.getMessage("ui.data.alert.tc.machineCodeNotExist");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 根据停机开始时间解析班次
     * 从班次配置表 T_TC_SHIFT_CONFIG 中查询所有启用的班次，匹配停机时间所在的班次，
     * 将 shift_name（夜班/早班/中班）映射为 class_num_three_plan 字典值（01/02/03）
     *
     * @param stopStartTime 停机开始时间
     * @return 班次字典编码（01=夜班, 02=早班, 03=中班），未匹配返回 null
     */
    public String resolveStopShift(Date stopStartTime) {
        return this.resolveStopShift(null, stopStartTime);
    }

    /**
     * 根据指定工厂的启用班制解析停机开始时间所属班次。
     *
     * @param factoryCode 工厂编号，为空时兼容历史调用并查询全部工厂
     * @param stopStartTime 停机开始时间
     * @return 班次字典编码（01=夜班、02=早班、03=中班）；未匹配返回空值
     */
    @Override
    public String resolveStopShift(String factoryCode, Date stopStartTime) {
        if (stopStartTime == null) {
            return null;
        }

        LambdaQueryWrapper<TcShiftConfig> shiftConfigQueryWrapper = new LambdaQueryWrapper<>();
        shiftConfigQueryWrapper.eq(org.apache.commons.lang3.StringUtils.isNotBlank(factoryCode), TcShiftConfig::getFactoryCode, factoryCode)
                .eq(TcShiftConfig::getOpenFlag, "1")
                .orderByAsc(TcShiftConfig::getShiftOrder);
        List<TcShiftConfig> shiftConfigs = tcShiftConfigMapper.selectList(shiftConfigQueryWrapper);

        if (shiftConfigs == null || shiftConfigs.isEmpty()) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(stopStartTime);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int timeMinutes = hour * 60 + minute;

        for (TcShiftConfig config : shiftConfigs) {
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
