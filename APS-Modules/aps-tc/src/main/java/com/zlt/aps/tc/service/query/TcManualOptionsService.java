package com.zlt.aps.tc.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcShiftConfig;
import com.zlt.aps.tc.api.domain.vo.TcManualConstructionOptionVo;
import com.zlt.aps.tc.api.domain.vo.TcManualMachineOptionVo;
import com.zlt.aps.tc.api.domain.vo.TcManualOptionsVo;
import com.zlt.aps.tc.api.domain.vo.TcManualShiftOptionVo;
import com.zlt.aps.tc.domain.vo.TcConstructionSidewallRowVo;
import com.zlt.aps.tc.mapper.TcAutoScheduleDataLoadMapper;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.mapper.TcShiftConfigMapper;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧人工插单和转机台选项查询服务。
 *
 * <p>施工胶料、基部胶、口型和工艺字段全部由后端施工资料解析，人工请求只提交胎侧编码和施工版本。</p>
 */
@Service
public class TcManualOptionsService {

    private final TcAutoScheduleDataLoadMapper autoScheduleDataLoadMapper;

    private final TcMachineInfoMapper machineInfoMapper;

    private final TcShiftConfigMapper shiftConfigMapper;

    /**
     * 构造人工操作选项查询服务。
     *
     * @param autoScheduleDataLoadMapper 自动排程数据加载 Mapper
     * @param machineInfoMapper 机台资料 Mapper
     * @param shiftConfigMapper 班次配置 Mapper
     */
    public TcManualOptionsService(TcAutoScheduleDataLoadMapper autoScheduleDataLoadMapper,
                                  TcMachineInfoMapper machineInfoMapper,
                                  TcShiftConfigMapper shiftConfigMapper) {
        this.autoScheduleDataLoadMapper = autoScheduleDataLoadMapper;
        this.machineInfoMapper = machineInfoMapper;
        this.shiftConfigMapper = shiftConfigMapper;
    }

    /**
     * 查询指定工厂、日期的人工操作选项。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 施工、机台和六班选项
     */
    public TcManualOptionsVo listOptions(String factoryCode, Date scheduleDate) {
        this.validateScope(factoryCode, scheduleDate);
        TcManualOptionsVo optionsVo = new TcManualOptionsVo();
        optionsVo.setConstructionList(this.listConstructionOptions(factoryCode));
        optionsVo.setMachineList(this.listMachineOptions(factoryCode));
        optionsVo.setShiftList(this.listShiftOptions(factoryCode, scheduleDate));
        return optionsVo;
    }

    /**
     * 按胎侧编码和施工版本解析可信施工快照。
     *
     * @param factoryCode 工厂编码
     * @param sidewallCode 胎侧编码
     * @param constructionVersion 胎侧施工版本
     * @return 施工快照排程结果模板
     * @throws ServiceException 施工版本不存在或关键字段无效时抛出
     */
    public TcScheduleResult resolveConstruction(String factoryCode, String sidewallCode,
                                                 String constructionVersion) {
        TcManualConstructionOptionVo optionVo = this.listConstructionOptions(factoryCode).stream()
                .filter(item -> Objects.equals(item.getSidewallCode(), sidewallCode))
                .filter(item -> Objects.equals(item.getConstructionVersion(), constructionVersion))
                .findFirst().orElseThrow(() -> new ServiceException(
                        I18nUtil.getMessage("ui.tc.schedule.insert.constructionNotFound")));
        List<String> missingFieldList = this.resolveConstructionMissingFieldList(optionVo);
        if (!missingFieldList.isEmpty()) {
            throw new ServiceException(this.buildConstructionInvalidMessage(optionVo, missingFieldList));
        }
        TcScheduleResult result = new TcScheduleResult();
        result.setFactoryCode(factoryCode);
        result.setSidewallCode(optionVo.getSidewallCode());
        result.setConstructionVersion(optionVo.getConstructionVersion());
        result.setSidewallCraft(optionVo.getSidewallCraft());
        result.setSidewallLength(optionVo.getSidewallLength());
        result.setSidewallWeight(optionVo.getSidewallWeight());
        result.setSidewallWearpRubberWeight(optionVo.getSidewallWearpRubberWeight());
        result.setGlueCode(optionVo.getGlueCode());
        result.setBaseGlueCode(optionVo.getBaseGlueCode());
        result.setWholeGlueCode(optionVo.getWholeGlueCode());
        result.setMouthPlateCode(optionVo.getMouthPlateCode());
        return result;
    }

    /**
     * 按胎侧编码批量解析唯一有效施工快照。
     *
     * <p>Excel 新增行不接收用户指定的施工版本，因此同一胎侧编码必须在当前工厂唯一对应
     * 一个有效胎侧施工版本；不存在或存在多个版本时由调用方整批拒绝导入。</p>
     *
     * @param factoryCode 工厂编码
     * @param sidewallCodeSet 待解析的胎侧编码集合
     * @return 胎侧编码对应的可信施工快照
     * @throws ServiceException 施工不存在、版本不唯一或施工关键字段无效时抛出
     */
    public Map<String, TcScheduleResult> resolveUniqueConstructions(String factoryCode,
                                                                     Set<String> sidewallCodeSet) {
        if (sidewallCodeSet == null || sidewallCodeSet.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<TcManualConstructionOptionVo>> groupedOptionMap = this.listConstructionOptions(factoryCode)
                .stream().filter(item -> sidewallCodeSet.contains(item.getSidewallCode()))
                .collect(Collectors.groupingBy(TcManualConstructionOptionVo::getSidewallCode,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, TcScheduleResult> resultMap = new LinkedHashMap<>();
        for (String sidewallCode : sidewallCodeSet) {
            List<TcManualConstructionOptionVo> optionList = groupedOptionMap.get(sidewallCode);
            if (optionList == null || optionList.isEmpty()) {
                throw new ServiceException(MessageFormat.format(I18nUtil.getMessage(
                        "ui.tc.schedule.insert.sidewallNotFound"), sidewallCode));
            }
            if (optionList.size() != 1) {
                throw new ServiceException(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.constructionAmbiguous"), sidewallCode));
            }
            TcManualConstructionOptionVo optionVo = optionList.get(0);
            resultMap.put(sidewallCode, this.resolveConstruction(factoryCode, sidewallCode,
                    optionVo.getConstructionVersion()));
        }
        return resultMap;
    }

    /**
     * 查询并解析胎侧施工选项。
     *
     * @param factoryCode 工厂编码
     * @return 去重后的施工选项
     */
    private List<TcManualConstructionOptionVo> listConstructionOptions(String factoryCode) {
        List<TcConstructionSidewallRowVo> rowList = this.autoScheduleDataLoadMapper
                .selectManualConstructionOptions(factoryCode);
        if (rowList == null) {
            return Collections.emptyList();
        }
        Map<String, TcManualConstructionOptionVo> optionMap = new LinkedHashMap<>();
        rowList.stream().filter(Objects::nonNull)
                .filter(row -> StringUtils.isNotBlank(row.getSidewallCode())
                        && StringUtils.isNotBlank(row.getSidewallVersion()))
                .map(this::buildConstructionOption)
                .forEach(option -> optionMap.putIfAbsent(
                        option.getSidewallCode() + "|" + option.getConstructionVersion(), option));
        return new ArrayList<>(optionMap.values());
    }

    /**
     * 构造施工选项并按英文逗号拆分主胶和基部胶。
     *
     * @param row 施工查询行
     * @return 施工选项
     */
    private TcManualConstructionOptionVo buildConstructionOption(TcConstructionSidewallRowVo row) {
        List<String> rubberCodeList = StringUtils.isBlank(row.getSidewallRubber())
                ? Collections.emptyList() : Arrays.stream(row.getSidewallRubber().split(","))
                .map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        TcManualConstructionOptionVo optionVo = new TcManualConstructionOptionVo();
        optionVo.setSourceConstructionCode(row.getConstructionCode());
        optionVo.setSourceConstructionVersion(row.getConstructionVersion());
        optionVo.setSidewallCode(row.getSidewallCode());
        optionVo.setConstructionVersion(row.getSidewallVersion());
        optionVo.setSidewallCraft(row.getSidewallCraft());
        optionVo.setSidewallLength(row.getSidewallLength());
        optionVo.setMouthPlateCode(row.getSidewallMouthPlate());
        optionVo.setGlueCode(rubberCodeList.isEmpty() ? null : rubberCodeList.get(0));
        optionVo.setBaseGlueCode(rubberCodeList.size() <= 1 ? null
                : String.join(",", rubberCodeList.subList(1, rubberCodeList.size())));
        optionVo.setWholeGlueCode(String.join(",", rubberCodeList));
        optionVo.setSidewallWeight(row.getSidewallWeight());
        optionVo.setSidewallWearpRubberWeight(row.getSidewallWearpRubberWeight());
        return optionVo;
    }

    /**
     * 收集胎侧人工施工快照缺失的关键字段。
     *
     * @param optionVo 胎侧施工选项
     * @return 已国际化的缺失字段名称
     */
    private List<String> resolveConstructionMissingFieldList(TcManualConstructionOptionVo optionVo) {
        List<String> missingFieldList = new ArrayList<>();
        if (StringUtils.isBlank(optionVo.getGlueCode())) {
            missingFieldList.add(I18nUtil.getMessage("ui.tc.schedule.insert.constructionFieldRubber"));
        }
        if (StringUtils.isBlank(optionVo.getMouthPlateCode())) {
            missingFieldList.add(I18nUtil.getMessage("ui.tc.schedule.insert.constructionFieldMouthPlate"));
        }
        return missingFieldList;
    }

    /**
     * 构造包含来源施工定位信息和缺失字段的胎侧错误提示。
     *
     * @param optionVo 胎侧施工选项
     * @param missingFieldList 已国际化的缺失字段名称
     * @return 已完成国际化参数替换的错误提示
     */
    private String buildConstructionInvalidMessage(TcManualConstructionOptionVo optionVo,
                                                   List<String> missingFieldList) {
        return MessageFormat.format(I18nUtil.getMessage("ui.tc.schedule.insert.constructionInvalid"),
                this.displayValue(optionVo.getSourceConstructionCode()),
                this.displayValue(optionVo.getSourceConstructionVersion()),
                this.displayValue(optionVo.getSidewallCode()),
                this.displayValue(optionVo.getConstructionVersion()),
                String.join(", ", missingFieldList));
    }

    /**
     * 将空白定位字段转换为统一占位符。
     *
     * @param value 原始字段值
     * @return 可直接展示的字段值
     */
    private String displayValue(String value) {
        return StringUtils.isBlank(value) ? "-" : value;
    }

    /**
     * 查询有效机台选项。
     *
     * @param factoryCode 工厂编码
     * @return 机台选项
     */
    private List<TcManualMachineOptionVo> listMachineOptions(String factoryCode) {
        LambdaQueryWrapper<TcMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineInfo::getFactoryCode, factoryCode);
        wrapper.eq(TcMachineInfo::getMachineStatus, "1");
        wrapper.orderByAsc(TcMachineInfo::getMachineCode);
        List<TcMachineInfo> machineInfoList = this.machineInfoMapper.selectList(wrapper);
        if (machineInfoList == null) {
            return Collections.emptyList();
        }
        return machineInfoList.stream().map(machineInfo -> {
            TcManualMachineOptionVo optionVo = new TcManualMachineOptionVo();
            optionVo.setMachineCode(machineInfo.getMachineCode());
            optionVo.setMachineName(machineInfo.getMachineName());
            optionVo.setMaxCapacity(machineInfo.getMaxCapacity());
            optionVo.setOpenShiftCode(machineInfo.getOpenShiftCode());
            return optionVo;
        }).collect(Collectors.toList());
    }

    /**
     * 查询指定排程日六班配置。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 六班配置
     */
    private List<TcManualShiftOptionVo> listShiftOptions(String factoryCode, Date scheduleDate) {
        LambdaQueryWrapper<TcShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcShiftConfig::getFactoryCode, factoryCode);
        wrapper.orderByAsc(TcShiftConfig::getShiftOrder);
        List<TcShiftConfig> shiftConfigList = this.shiftConfigMapper.selectList(wrapper);
        if (shiftConfigList == null) {
            return Collections.emptyList();
        }
        return shiftConfigList.stream().map(shiftConfig -> {
            TcManualShiftOptionVo optionVo = new TcManualShiftOptionVo();
            optionVo.setScheduleDate(scheduleDate);
            optionVo.setShiftOrder(shiftConfig.getShiftOrder());
            optionVo.setShiftCode(shiftConfig.getShiftCode());
            optionVo.setShiftName(shiftConfig.getShiftName());
            optionVo.setOpenFlag(shiftConfig.getOpenFlag());
            return optionVo;
        }).collect(Collectors.toList());
    }

    /**
     * 校验选项查询范围。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     */
    private void validateScope(String factoryCode, Date scheduleDate) {
        if (StringUtils.isBlank(factoryCode) || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.invalidScope"));
        }
    }
}
