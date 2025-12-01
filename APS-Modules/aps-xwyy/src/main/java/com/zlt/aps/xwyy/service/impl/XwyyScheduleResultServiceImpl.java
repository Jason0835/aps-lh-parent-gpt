package com.zlt.aps.xwyy.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.enums.HalfComponentFinishTableEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.CxTCd15BigRoll;
import com.zlt.aps.common.engine.domain.CxTCd90BigRoll;
import com.zlt.aps.common.engine.domain.CxTCd90Params;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.common.engine.service.CxTCd15BigRollService;
import com.zlt.aps.common.engine.service.CxTCd90BigRollService;
import com.zlt.aps.common.engine.service.CxTCd90ParamsService;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.service.impl.BaseFinishQtyImportService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyParamsDto;
import com.zlt.aps.gdyy.api.domain.entity.GdyyOriginalLineSpec;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.gdyy.engine.service.GdyyEngineService;
import com.zlt.aps.gdyy.entity.GdyyParams;
import com.zlt.aps.gdyy.mapper.GdyyOriginalLineSpecEntityMapper;
import com.zlt.aps.gdyy.mapper.GdyyParamsMapper;
import com.zlt.aps.gdyy.mapper.GdyyScheduleResultMapper;
import com.zlt.aps.gdyy.mapper.GdyyStockMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyParamsDto;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.api.domain.entity.*;
import com.zlt.aps.xwyy.api.domain.vo.HalfYyExportDataVo;
import com.zlt.aps.xwyy.common.handle.XwyySyncDataHandle;
import com.zlt.aps.xwyy.engine.service.XwyyEngineService;
import com.zlt.aps.xwyy.entity.XwyyParams;
import com.zlt.aps.xwyy.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.mapper.XwyyOriginalLineSpecMapper;
import com.zlt.aps.xwyy.mapper.XwyyParamsMapper;
import com.zlt.aps.xwyy.mapper.XwyyScheduleResultMapper;
import com.zlt.aps.xwyy.mapper.XwyyStockMapper;
import com.zlt.aps.xwyy.service.XwyyBigRollRubberCarRelationService;
import com.zlt.aps.xwyy.service.XwyyDispatcherLogService;
import com.zlt.aps.xwyy.service.XwyyMachineInfoService;
import com.zlt.aps.xwyy.service.XwyyScheduleResultService;
import com.zlt.aps.xwyy.vo.XwyyScheduleOriginalSumPlanVo;
import com.zlt.sync.povo.SyncParamsVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 纤维压延排程结果Service业务层处理
 *
 * @author chen
 * @date 2021-07-06
 */
@Service
public class XwyyScheduleResultServiceImpl extends ServiceImpl<XwyyScheduleResultMapper, XwyyScheduleResult> implements XwyyScheduleResultService {
    @Autowired
    private XwyyScheduleResultMapper xwyyScheduleResultMapper;

    @Autowired
    private XwyyMachineInfoService xwyyMachineInfoService;

    @Autowired
    private XwyyEngineService xwyyEngineService;

    @Resource
    private XwyySyncDataHandle xwyySyncDataHandle;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${excelModelPath}")
    private String excelModelPath;

    @Autowired
    private FactoryService factoryService;

    @Autowired
    private CxTCd90BigRollService cd90BigRollService;

    @Autowired
    private CxTCd90ParamsService cd90ParamsService;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private XwyyDispatcherLogService xwyyDispatcherLogService;

    @Autowired
    private XwyyOriginalLineSpecMapper xwyyOriginalLineSpecMapper;

    @Autowired
    private XwyyBigRollRubberCarRelationService xwyyBigRollRubberCarRelationService;

    @Autowired
    private BaseFinishQtyImportService baseFinishQtyImportService;

    @Autowired
    private GdyyScheduleResultMapper gdyyScheduleResultMapper;

    @Autowired
    private CxTCd15BigRollService cd15BigRollService;

    @Autowired
    private XwyyParamsMapper xwyyParamsMapper;

    @Autowired
    private GdyyParamsMapper gdyyParamsMapper;

    @Autowired
    private GdyyEngineService gdyyEngineService;

    /**
     * 查询纤维压延排程结果信息维护列表
     *
     * @param scheduleResult 纤维压延排程结果信息维护
     * @return 纤维压延排程结果信息维护集合
     */
    @Override
    public List<XwyyScheduleResultDto> selectScheduleResultList(XwyyScheduleResult scheduleResult) {
        List<XwyyScheduleResultDto> list = xwyyScheduleResultMapper.selectScheduleResultList(scheduleResult);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        Map<String, Long> originalLineNameCountMap = list.stream().collect(Collectors.groupingBy(XwyyScheduleResultDto::getOriginalLineName, Collectors.counting()));
        Map<String, Long> originalLineNameBrandCountMap = list.stream().collect(Collectors.groupingBy(item -> item.getOriginalLineName() + item.getOriginalBrand(), Collectors.counting()));
        // 大卷信息
        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(CollectionUtil.propertiesToList(list, XwyyScheduleResultDto::getBigRollCode));
        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
        // 大卷默认信息
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        Map<String, String> originalLineNameMap = new HashMap<>();
        Map<String, String> originalLineNameBrandMap = new HashMap<>();
        List<XwyyMachineInfo> machineInfoList = xwyyMachineInfoService.selectMachineInfoList(new XwyyMachineInfo());
        Map<Long, XwyyMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(XwyyMachineInfo::getId, Function.identity(), (s1, s2) -> s1));
        // 分别计算纤维压延当日合计个数
        for (XwyyScheduleResultDto dto : list) {
            String originalLineName = dto.getOriginalLineName();
            if (!originalLineNameMap.containsKey(originalLineName)) {
                originalLineNameMap.put(originalLineName, dto.getBigRollCode());
            } else {
                String bigRollCode = originalLineNameMap.getOrDefault(originalLineName, "");
                originalLineNameMap.put(originalLineName, bigRollCode.compareTo(dto.getBigRollCode()) > 0 ? bigRollCode : dto.getBigRollCode());
            }
            String brandKey = dto.getOriginalLineName() + dto.getOriginalBrand();
            if (!originalLineNameBrandMap.containsKey(brandKey)) {
                originalLineNameBrandMap.put(brandKey, dto.getBigRollCode());
            } else {
                String bigRollCodeBrand = originalLineNameBrandMap.getOrDefault(brandKey, "");
                originalLineNameBrandMap.put(brandKey, bigRollCodeBrand.compareTo(dto.getBigRollCode()) > 0 ? bigRollCodeBrand : dto.getBigRollCode());
            }
            CxTCd90BigRoll cd90BigRoll = cd90BigRollMap.get(dto.getBigRollCode());
            BigDecimal xwyyNum = BigDecimal.ZERO;
//            BigDecimal dayPlanQtyNum = BigDecimal.ZERO;
//            BigDecimal nightPlanQtyNum = BigDecimal.ZERO;
            dto.setActClothLength(BigDecimal.ZERO);
            if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null && !cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
                xwyyNum = BigDecimal.valueOf(dto.getDailyTotalQty()).divide(cd90BigRoll.getActClothLength(), 1, BigDecimal.ROUND_UP);
//                dayPlanQtyNum = BigDecimal.valueOf(dto.getDayPlanQty()).divide(cd90BigRoll.getActClothLength(), 1, BigDecimal.ROUND_UP);
//                nightPlanQtyNum = BigDecimal.valueOf(dto.getNightPlanQty()).divide(cd90BigRoll.getActClothLength(), 1, BigDecimal.ROUND_UP);
                dto.setActClothLength(cd90BigRoll.getActClothLength());
            } else if (cd90Params != null && StringUtils.isNotBlank(cd90Params.getParamValue()) && !Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
                xwyyNum = BigDecimal.valueOf(dto.getDailyTotalQty()).divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 1, BigDecimal.ROUND_UP);
//                dayPlanQtyNum = BigDecimal.valueOf(dto.getDayPlanQty()).divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 1, BigDecimal.ROUND_UP);
//                nightPlanQtyNum = BigDecimal.valueOf(dto.getNightPlanQty()).divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 1, BigDecimal.ROUND_UP);
                dto.setActClothLength(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())));
            }
            dto.setDailyTotalQtyNum(xwyyNum.doubleValue());
//            dto.setDayPlanQtyNum(dayPlanQtyNum.doubleValue());
//            dto.setNightPlanQtyNum(nightPlanQtyNum.doubleValue());
        }
        for (XwyyScheduleResultDto item : list) {
            String originalLineName = item.getOriginalLineName();
            String maxBigRollCode = originalLineNameMap.get(originalLineName);
            String brandKey = originalLineName + item.getOriginalBrand();
            String maxBigRollCodeBrand = originalLineNameBrandMap.get(brandKey);
            if (originalLineNameCountMap.get(originalLineName) > 1) {
                item.setMaxBigRollCode(maxBigRollCode);
            }
            if (originalLineNameBrandCountMap.get(brandKey) > 1) {
                item.setMaxBigRollCodeBrand(maxBigRollCodeBrand);
            }
            String machineIdStr = item.getMachineId();
            if (StringUtils.isNotBlank(machineIdStr)) {
                List<String> machineNameList = new ArrayList<>();
                String[] machineIdArr = machineIdStr.split(",");
                for (String machineId : machineIdArr) {
                    Long key = null;
                    try {
                        key = Long.valueOf(machineId);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (machineInfoMap.containsKey(key)) {
                        XwyyMachineInfo machineInfo = machineInfoMap.get(key);
                        machineNameList.add(machineInfo.getMachineName());
                    }
                }
                item.setMachineName(String.join(",", machineNameList));
            }
        }
        return list;
    }

    /**
     * 查询纤维压延排程结果信息维护列表
     *
     * @param id 要查询的id
     * @return 纤维压延排程结果信息维护集合
     */
    @Override
    public XwyyScheduleResultDto selectScheduleResultById(Long id) {
        XwyyScheduleResultDto resultDto = xwyyScheduleResultMapper.selectScheduleResultById(id);
        // 大卷信息
        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(Collections.singletonList(resultDto.getBigRollCode()));
        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
        // 大卷默认信息
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        CxTCd90BigRoll cd90BigRoll = cd90BigRollMap.get(resultDto.getBigRollCode());
        BigDecimal xwyyNum = BigDecimal.ZERO;
        resultDto.setActClothLength(BigDecimal.ZERO);
        if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null && !cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
            xwyyNum = BigDecimal.valueOf(resultDto.getDailyTotalQty()).divide(cd90BigRoll.getActClothLength(), 1, BigDecimal.ROUND_UP);
            resultDto.setActClothLength(cd90BigRoll.getActClothLength());
        } else if (cd90Params != null && StringUtils.isNotBlank(cd90Params.getParamValue()) && !Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
            xwyyNum = BigDecimal.valueOf(resultDto.getDailyTotalQty()).divide(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())), 1, BigDecimal.ROUND_UP);
            resultDto.setActClothLength(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())));
        }
        List<XwyyOriginalLineSpec> lineSpecList = xwyyOriginalLineSpecMapper.listOriginalLineSpec(new XwyyOriginalLineSpec());
        Map<String, String> lineSpecCodeMap = lineSpecList.stream().collect(Collectors.toMap(XwyyOriginalLineSpec::getOriginalLineCode, XwyyOriginalLineSpec::getOriginalLineLength));
        resultDto.setOriginalLineLength(lineSpecCodeMap.getOrDefault(resultDto.getOriginalLineCode(), ""));
        resultDto.setDailyTotalQtyNum(xwyyNum.doubleValue());
        return resultDto;
    }

    /**
     * 保存纤维压延排程结果信息维护
     *
     * @param scheduleResult 纤维压延排程结果信息维护
     */
    @Override
    public void saveScheduleResult(XwyyScheduleResult scheduleResult) {
        // 先无论是否有共用原线规格、原线品牌、胶料车数，都先更新为0，再更新为正确原线卷数、原线品牌个数
        scheduleResult.setOriginalLineQtyNum("0");
        scheduleResult.setOriginalBrandNum(BigDecimal.ZERO);
        // 校验字段是否修改，修改则改状态为未发布
        if (scheduleResult.getId() != null) {
            boolean flag;
            XwyyScheduleResultDto resultDto = xwyyScheduleResultMapper.selectScheduleResultById(scheduleResult.getId());
            flag = compareFields(scheduleResult, resultDto);
            if (!flag) {
                scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
            scheduleResult.setBaseVale(scheduleResult.getId());
            saveOrUpdate(scheduleResult);
        } else {
            // 插单操作，直接调用引擎插单接口
            scheduleResult.setBaseVale(null);
            XwyyScheduleResultDto scheduleResultDto = new XwyyScheduleResultDto();
            BeanUtils.copyProperties(scheduleResult, scheduleResultDto);
            List<XwyyScheduleResult> scheduleResults = xwyyScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
            xwyyEngineService.insertXwyyOrder(scheduleResultDto);
            scheduleResult.setMaxBigRollCode(scheduleResultDto.getMaxBigRollCode());
            scheduleResult.setOriginalLineLength(scheduleResultDto.getOriginalLineLength());
            scheduleResult.setOriginalLineCode(scheduleResultDto.getOriginalLineCode());
            scheduleResult.setId(scheduleResultDto.getId());
            this.insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, scheduleResults, scheduleResult);
        }
        updateOriginalLineQtyNumAndOriginalBrandNum(scheduleResult);
        updateRubberCarNumber(scheduleResult);
    }

    @Autowired
    private GdyyOriginalLineSpecEntityMapper gdyyOriginalLineSpecEntityMapper;
    @Autowired
    private XwyyStockMapper xwyyStockMapper;

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType    操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, XwyyScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        XwyyScheduleResultDto oldSchedule = this.xwyyScheduleResultMapper.selectScheduleResultById(newSchedule.getId());  //操作前的排程数据
        XwyyDispatcherLog log = new XwyyDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getBigRollCode());    //帘布大卷代码
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineId());
        log.setBeforeDayPlan(oldSchedule.getDayPlanQty());
        log.setBeforeNightPlan(oldSchedule.getNightPlanQty());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        /** 调用插入日志方法 **/
        xwyyDispatcherLogService.insertXwyyDispatcherLog(log);
    }
    @Autowired
    private GdyyStockMapper gdyyStockMapper;

    /**
     * 更新原线个数和原线品牌个数
     *
     * @param scheduleResult 要更改的记录
     *                       (必要参数：排程日期、原线规格编号、原线长度、多规格共用要更新的帘布大卷代号、帘布大卷编号、原线品牌)
     */
    private void updateOriginalLineQtyNumAndOriginalBrandNum(XwyyScheduleResult scheduleResult) {
        // 更新计划个数
        // 大卷信息
        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(Collections.singletonList(scheduleResult.getBigRollCode()));
        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
        // 大卷默认信息
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        CxTCd90BigRoll cd90BigRoll = cd90BigRollMap.get(scheduleResult.getBigRollCode());
        scheduleResult.setActClothLength(BigDecimal.ZERO);
        if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null && !cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
            scheduleResult.setActClothLength(cd90BigRoll.getActClothLength());
        } else if (cd90Params != null && StringUtils.isNotBlank(cd90Params.getParamValue()) && !Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
            scheduleResult.setActClothLength(BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue())));
        }
        Double dayPlanQty = scheduleResult.getDayPlanQty() == null ? 0D : scheduleResult.getDayPlanQty();
        double dayPlanQtyNum = BigDecimalUtil.div(dayPlanQty, scheduleResult.getActClothLength().doubleValue());
        scheduleResult.setDayPlanQtyNum(BigDecimalUtil.roundUp(dayPlanQtyNum, 1));
        Double nightPlanQty = scheduleResult.getNightPlanQty() == null ? 0D : scheduleResult.getNightPlanQty();
        double nightPlanQtyNum = BigDecimalUtil.div(nightPlanQty, scheduleResult.getActClothLength().doubleValue());
        scheduleResult.setNightPlanQtyNum(BigDecimalUtil.roundUp(nightPlanQtyNum, 1));
        LambdaUpdateWrapper<XwyyScheduleResult> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(XwyyScheduleResult::getId, scheduleResult.getId())
                .set(XwyyScheduleResult::getDayPlanQtyNum, scheduleResult.getDayPlanQtyNum())
                .set(XwyyScheduleResult::getNightPlanQtyNum, scheduleResult.getNightPlanQtyNum());
        xwyyScheduleResultMapper.update(null, wrapper);

        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleResult.getScheduleDate());
        params.put("originalLineCode", scheduleResult.getOriginalLineCode());
        Double planSum = xwyyScheduleResultMapper.selectPlanSumByParams(params);
        // 计算原线卷数，结果保留1位小数，向上进位
        String originalLineLength = scheduleResult.getOriginalLineLength();
        if (StringUtils.isBlank(originalLineLength) || BigDecimal.ZERO.toString().equals(originalLineLength)) {
            originalLineLength = "1";
        }
        BigDecimal originalLineQtyNum = new BigDecimal(String.valueOf(planSum))
                .divide(new BigDecimal(originalLineLength), 1, RoundingMode.UP);
        params.put("bigRollCode", StringUtils.isBlank(scheduleResult.getMaxBigRollCode()) ? scheduleResult.getBigRollCode() : scheduleResult.getMaxBigRollCode());
        params.put("originalLineQtyNum", originalLineQtyNum);
        xwyyScheduleResultMapper.updateOriginalLineQtyNumByParams(params);
        String originalBrand = scheduleResult.getOriginalBrand();
        if (StringUtils.isNotBlank(originalBrand)) {
            params.put("bigRollCode", StringUtils.isBlank(scheduleResult.getMaxBigRollCodeBrand()) ? scheduleResult.getBigRollCode() : scheduleResult.getMaxBigRollCodeBrand());
            params.put("originalBrand", originalBrand);
            Double brandPlanSum = xwyyScheduleResultMapper.selectPlanSumByParams(params);
            BigDecimal originalBrandNum = new BigDecimal(String.valueOf(brandPlanSum))
                    .divide(new BigDecimal(originalLineLength), 1, RoundingMode.UP);
            params.put("originalBrandNum", originalBrandNum);
            xwyyScheduleResultMapper.updateOriginalBrandNumByParams(params);
        }
    }

    /**
     * 保存钢丝圈排程结果选机台信息
     *
     * @param scheduleResult 钢丝圈排程结果信息
     */
    @Override
    public void chooseMachine(XwyyScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);

        // 调用引擎确认机台接口，根据损耗率重新计算计划量
        XwyyScheduleResultDto scheduleResultDto = new XwyyScheduleResultDto();
        BeanUtils.copyProperties(scheduleResult, scheduleResultDto);
        xwyyEngineService.confirmXwyyMachine(scheduleResultDto);
        scheduleResult.setDayPlanQty(scheduleResultDto.getDayPlanQty());
        scheduleResult.setNightPlanQty(scheduleResultDto.getNightPlanQty());
        scheduleResult.setTotalPlan(scheduleResultDto.getTotalPlan());

        saveOrUpdate(scheduleResult);
    }

    /**
     * 批量删除纤维压延排程结果信息维护
     *
     * @param ids 需要删除的纤维压延排程结果信息维护ID
     */
    @Override
    public void deleteScheduleResultByIds(Long[] ids, List<XwyyScheduleResult> list) {
        xwyyScheduleResultMapper.deleteByIds(Arrays.stream(ids)
                .collect(Collectors.toList()));
        // 查询所有要删除的记录的原线名称(不重复)，拿到原线名称对应的最大帘布大卷代号
        Date scheduleDate = null;
        Set<String> originalLineCodeList = list.stream().map(XwyyScheduleResult::getOriginalLineCode).collect(Collectors.toSet());
        if (list.size() > 0) {
            scheduleDate = list.get(0).getScheduleDate();
        }
        updateOriginalLineQtyNumAndBrandNum(scheduleDate, originalLineCodeList);
    }

    /**
     * 更新胶料车数
     *
     * @param scheduleResult 参数
     */
    private void updateRubberCarNumber(XwyyScheduleResult scheduleResult) {
        XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation = new XwyyBigRollRubberCarRelation();
        xwyyBigRollRubberCarRelation.setBigRollCode(scheduleResult.getBigRollCode());
        XwyyBigRollRubberCarRelation relation = xwyyBigRollRubberCarRelationService.selectByBigRollCode(xwyyBigRollRubberCarRelation);
        if (relation != null) {
            Double dayPlanQtyNum = scheduleResult.getDayPlanQtyNum() == null ? 0D : scheduleResult.getDayPlanQtyNum();
            Double nightPlanQtyNum = scheduleResult.getNightPlanQtyNum() == null ? 0D : scheduleResult.getNightPlanQtyNum();
            BigDecimal rubberCarNumber = BigDecimal.valueOf(dayPlanQtyNum + nightPlanQtyNum).multiply(relation.getCarNumber());
            // 保留一位小数向上取整
            rubberCarNumber = rubberCarNumber.setScale(1, RoundingMode.UP);
            scheduleResult.setRubberCarNumber(rubberCarNumber);
        } else {
            scheduleResult.setRubberCarNumber(null);
        }
        if (ObjectUtils.allNotNull(scheduleResult.getRubberCarNumber(), scheduleResult.getId())) {
            xwyyScheduleResultMapper.updateRubberCarNumber(scheduleResult);
        }
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<XwyyScheduleResult> scheduleResults, XwyyScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<XwyyScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        XwyyDispatcherLog log = new XwyyDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getBigRollCode());
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<XwyyScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(XwyyScheduleResult::getCreateTime));
            if (max.isPresent()) {
                XwyyScheduleResult scheduleResult = max.get();
                log.setBeforeMachineId(scheduleResult.getMachineId());
                log.setBeforeDayPlan(scheduleResult.getDayPlanQty());
                log.setBeforeNightPlan(scheduleResult.getNightPlanQty());
            }
        }
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        /* 调用插入日志方法 **/
        xwyyDispatcherLogService.insertXwyyDispatcherLog(log);
    }

    /**
     * 根据排程日期和代码查询排程结果
     *
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<XwyyScheduleResult> selectByScheduleDateAndCode(XwyyScheduleResult scheduleResult) {
        return xwyyScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
    }

    /**
     * 发布排程结果
     *
     * @param scheduleResult 排程日期
     * @param ids            要发布的排程结果id
     * @param dataVersion    数据同步版本
     */
    @Override
    public void publish(XwyyScheduleResult scheduleResult, long[] ids, String dataVersion) {
        // 保存发布日志
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_XWYY);
        record.setScheduleDate(scheduleResult.getScheduleDate());
        record.setPublishStatus(ApsConstant.IS_RELEASE);
        record.setDataVersion(dataVersion);
        xwyyScheduleResultMapper.insertPublishRecord(record);
        this.deployXwyyScheduleToMid(scheduleResult.getScheduleDate(), ids, dataVersion);

        if (ids == null || ids.length == 0) {
            // 设置更新人和更新时间
            scheduleResult.setBaseVale(0L);
            scheduleResult.setIsRelease(ApsConstant.RELEASING);
            xwyyScheduleResultMapper.publishAll(scheduleResult);
        } else {
            // ids不为空，发布指定记录，需求暂未变更，变更后测试
            xwyyScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                    .boxed().collect(Collectors.toList()), ApsConstant.RELEASING);
        }
    }

    /**
     * 把排程数据发布到中间库
     *
     * @param scheduleDate 排程日期
     * @param ids          排程id
     */
    private void deployXwyyScheduleToMid(Date scheduleDate, long[] ids, String dataVersion) {
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
        // 把排程数据同步到接口中间库中
        xwyyScheduleResultMapper.deployXwyyScheduleToMid(dataVersion, scheduleDate, ids, factoryCode, companyCode,
                DateUtils.getNowDate());
    }


    /**
     * 给mes发送排程下发通知
     *
     * @param scheduleDate 排产日
     * @param dataVersion  数据版本
     */
    public void publishNoticeMes(Date scheduleDate, String dataVersion, int rowCount) {
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
        //数据同步到中间库后，往mq中发送消息通知MES去取数据
        SyncParamsVO syncParamsVO = new SyncParamsVO();
        syncParamsVO.setSyncKey(ApsConstant.XWYY_DEPLOY_SYNC_KEY);
        syncParamsVO.setDataVersion(dataVersion);
        // 请求参数
        JSONObject params = new JSONObject();
        params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
        params.put("rowCount", rowCount);
        syncParamsVO.setParams(params);
        syncParamsVO.setFactoryCode(factoryCode);
        syncParamsVO.setCompanyCode(companyCode);
        xwyySyncDataHandle.syncNotice(syncParamsVO);  //往消息队列发送消息
    }

    /**
     * 更新指定相关数据记录的发布状态
     *
     * @param dataVersion 数据版本
     * @param ids         排程ID列表
     * @param status      更新的状态
     */
    @Override
    public void updateRelaseStatus(String dataVersion, long[] ids, String status) {
        xwyyScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                .boxed().collect(Collectors.toList()), status);
        xwyyScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_XWYY);
        record.setScheduleDate(scheduleDate);
        return xwyyScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     *
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    @Override
    public Boolean checkUnique(XwyyScheduleResult scheduleResult) {
        return xwyyScheduleResultMapper.checkUnique(scheduleResult) == 0;
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list         要导入数据
     * @param importLogId  导入日志id
     * @param scheduleDate 排程日期
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<XwyyScheduleResultDto> list, Long importLogId, Date scheduleDate) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<XwyyScheduleResultDto> importList = new ArrayList<>();

        try {
            //将机台名称转为机台code
            XwyyMachineInfo xwyyMachineInfo = new XwyyMachineInfo();
            xwyyMachineInfo.setStatus("0");
            List<XwyyMachineInfo> machineInfoList = xwyyMachineInfoService.selectMachineInfoList(xwyyMachineInfo);
            if (CollectionUtils.isEmpty(machineInfoList)) {
                // 未查询到机台信息
                String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
                addImportErrorLog(importLogId, null, message, importErrorLogs);
                return AjaxResult.error(message, importErrorLogs);
            }

            //根据机台名称去重
            TreeSet<XwyyMachineInfo> treeSet = new TreeSet<XwyyMachineInfo>(new Comparator<XwyyMachineInfo>() {
                @Override
                public int compare(XwyyMachineInfo o1, XwyyMachineInfo o2) {
                    return o1.getMachineName().compareTo(o2.getMachineName());
                }
            });
            treeSet.addAll(machineInfoList);
            machineInfoList = new ArrayList<>(treeSet);


            Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(XwyyMachineInfo::getMachineName, XwyyMachineInfo::getId));
            //按业务主键分组
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> item.getBigRollCode() + item.getMachineId(), Collectors.counting()));

            //将机台代码转换为机台id，并做校验
            for (int i = 0; i < list.size(); i++) {
                XwyyScheduleResultDto scheduleResultDto = list.get(i);
                scheduleResultDto.setDataSource("2");
                scheduleResultDto.setScheduleDate(scheduleDate);

                int errorNum = i + 3;
                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, scheduleResultDto);

                // 需要在机台id转换前校验
                if (groupMap.get(scheduleResultDto.getBigRollCode() + scheduleResultDto.getMachineId()) > 1) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                    String columnName = I18nUtil.getMessage("ui.data.column.xwyy.scheduleResult.bigRollCode");
                    String columnName2 = I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine");
                    message = String.format(message, columnName + "+" + columnName2);
                    addImportErrorLog(importLogId, i + 3, message, importErrorLogs);
                    continue;
                }

                // 机台code 转为机台id
                if (scheduleResultDto.getMachineId() != null && scheduleResultDto.getMachineId().indexOf(",") > 0) {
                    String message = I18nUtil.getMessage("ui.data.column.machine.produceLineValidate");
                    message = String.format(message, i + 3, I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine"));
                    addImportErrorLog(importLogId, i + 3, message, validated);
                }
                if (machineCodeMap.get(scheduleResultDto.getMachineId()) == null) {
                    addImportErrorLog(importLogId, i + 3, I18nUtil.getMessage("ui.error.message.column.produceLineNotExist"), validated);
                }

//                XwyyScheduleResult scheduleResult = new XwyyScheduleResult();
//                BeanUtils.copyProperties(scheduleResultDto, scheduleResult);
//                int unique = xwyyScheduleResultMapper.checkUnique(scheduleResult);
//                if (unique != 0) {
//                    // 唯一性校验不通过
//                    String message = StringUtils.format(I18nUtil.getMessage("import.error.message.scheduleResult.unique"), errorNum);
//                    addImportErrorLog(importLogId, errorNum, message, validated);
//                }

                if (CollectionUtils.isNotEmpty(validated)) {
                    failureNum++;
                    importErrorLogs.addAll(validated);
                } else {
                    successNum++;
                    scheduleResultDto.setMachineId(machineCodeMap.get(scheduleResultDto.getMachineId()) + "");
                    scheduleResultDto.setBaseVale(null);
                    scheduleResultDto.setDataSource(EngineConstants.SCHEDULE_DATA_SOURCE_IMPORT);
                    importList.add(scheduleResultDto);
                }

                System.out.println("--------------------------");
                System.out.println(scheduleResultDto);
                System.out.println("--------------------------");
            }

            // 调用引擎导入,传入 importList
            if (!importList.isEmpty()) {
                // 如果引擎导入失败，会将失败日志返回
                List<ImportErrorLog> engineImportErrorLogs = xwyyEngineService.batchSaveXwyySchedule(scheduleDate,
                        importList);
                // 如果有记录导入失败，则需要合并失败日志
                if (!engineImportErrorLogs.isEmpty()) {
                    engineImportErrorLogs.stream().forEach(v -> v.setImportLogId(importLogId));
                    importErrorLogs.addAll(engineImportErrorLogs);
                    successNum -= engineImportErrorLogs.size();
                    failureNum += engineImportErrorLogs.size();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
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

    /**
     * 设置单元格样式
     *
     * @param row       单元行对象
     * @param cellNum   列数
     * @param cellStyle 样式
     */
    private void setCellStyle(Row row, int cellNum, CellStyle cellStyle) {
        for (int i = 0; i < cellNum; i++) {
            row.getCell(i).setCellStyle(cellStyle);
        }
    }

    /**
     * 比较两对象属性值是否相同
     *
     * @param scheduleResult 前端传入对象
     * @param resultDto      查询结果对象
     * @return 是否相同
     */
    private boolean compareFields(XwyyScheduleResult scheduleResult, XwyyScheduleResultDto resultDto) {
        boolean result = compare(resultDto.getDayPlanQty(), scheduleResult.getDayPlanQty());
        result = result && compare(resultDto.getNightPlanQty(), scheduleResult.getNightPlanQty());
        result = result && compare(resultDto.getDayOut(), scheduleResult.getDayOut());
        result = result && compare(resultDto.getMachineId(), scheduleResult.getMachineId());
        result = result && compare(resultDto.getFac2Class1Plan(), scheduleResult.getFac2Class1Plan());
        result = result && compare(resultDto.getFac2Class2Plan(), scheduleResult.getFac2Class2Plan());
        result = result && compare(resultDto.getFac2Class3Plan(), scheduleResult.getFac2Class3Plan());
        result = result && compare(resultDto.getFac5Class1Plan(), scheduleResult.getFac5Class1Plan());
        result = result && compare(resultDto.getFac5Class2Plan(), scheduleResult.getFac5Class2Plan());
        result = result && compare(resultDto.getFac5Class3Plan(), scheduleResult.getFac5Class3Plan());
        result = result && compare(resultDto.getDayHandAnalysis(), scheduleResult.getDayHandAnalysis());
        result = result && compare(resultDto.getNightHandAnalysis(), scheduleResult.getNightHandAnalysis());
        result = result && compare(resultDto.getRemark(), scheduleResult.getRemark());
        return result;
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        return xwyyScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(long[] ids) {
        return xwyyScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(XwyyScheduleResult entity) {
        return xwyyScheduleResultMapper.changeReleaseStatus(entity);
    }

    @Override
    public int checkXwyyCodeExist(XwyyScheduleResultDto dto) {
        return xwyyScheduleResultMapper.checkXwyyCodeExist(dto);
    }

    @Override
    public int isPublishByIds(Long[] ids) {
        return xwyyScheduleResultMapper.isPublishByIds(ids);
    }

    @Override
    public List<XwyyScheduleResultDto> selectByIds(List<Long> ids2) {
        return xwyyScheduleResultMapper.selectByIds(ids2);
    }

    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    public boolean compare(Double d1, Double d2) {
        d1 = ObjectUtils.isEmpty(d1) ? 0D : d1;
        d2 = ObjectUtils.isEmpty(d2) ? 0D : d2;
        return d1.equals(d2);
    }

    /**
     * 根据帘布大卷代号获取帘线大卷标准长度
     *
     * @param bigRollCode 帘布大卷代号
     * @return 帘线大卷标准长度
     */
    @Override
    public BigDecimal getActClothLength(String bigRollCode) {
        // 大卷信息
        List<CxTCd90BigRoll> cd90BigRollList = cd90BigRollService.getByCrodSpecList(Collections.singletonList(bigRollCode));
        Map<String, CxTCd90BigRoll> cd90BigRollMap = CollectionUtil.toMap(cd90BigRollList, CxTCd90BigRoll::getBigRollCode);
        // 大卷默认信息
        CxTCd90Params cd90Params = cd90ParamsService.getByParamCode(EngineConstants.STANDARD_SIZE);
        CxTCd90BigRoll cd90BigRoll = cd90BigRollMap.get(bigRollCode);
        BigDecimal actClothLength = BigDecimal.ZERO;
        if (cd90BigRoll != null && cd90BigRoll.getActClothLength() != null && !cd90BigRoll.getActClothLength().equals(BigDecimal.ZERO)) {
            actClothLength = cd90BigRoll.getActClothLength();
        } else if (cd90Params != null && StringUtils.isNotBlank(cd90Params.getParamValue()) && !Double.valueOf(cd90Params.getParamValue()).equals(0d)) {
            actClothLength = BigDecimal.valueOf(Double.parseDouble(cd90Params.getParamValue()));
        }
        return actClothLength;
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Override
    public int combinationMiddleAndNight(long[] ids, String classifiedShift) {
        Map<String, Object> map = new HashMap<>();
        map.put("classifiedShift", classifiedShift);
        map.put("ids", ids);
        return xwyyScheduleResultMapper.combinationMiddleAndNight(map);
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list        要导入数据
     * @param importLogId 导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importFinishQty(List<XwyyDayFinishQty> list, Long importLogId) {
        return baseFinishQtyImportService.importFinishQty(list, importLogId, HalfComponentFinishTableEnum.XWYY);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult getSummaryVo(XwyyScheduleResultDto scheduleResult) {
        ScheduleSummaryVo summaryVo = xwyyScheduleResultMapper.getSummaryVo(scheduleResult);
        if (summaryVo == null) {
            summaryVo = new ScheduleSummaryVo();
            summaryVo.setScheduleDate(scheduleResult.getScheduleDate());
        }
        ScheduleSummaryVo lastDayPlanQtySummaryVo = xwyyScheduleResultMapper.getLastDayPlanQty(scheduleResult);
        Double lastDayPlanQty = null;
        if (lastDayPlanQtySummaryVo != null) {
            lastDayPlanQty = lastDayPlanQtySummaryVo.getNightPlanQty();
            summaryVo.setLastDayPlanQty(lastDayPlanQty);
        }
        ScheduleSummaryVo cxConsumeSummaryVo = null;
        Double cxConsumeQty = null;
        if (StringUtils.isBlank(scheduleResult.getIsRelease()) && StringUtils.isBlank(scheduleResult.getMachineId())) {
            cxConsumeSummaryVo = xwyyScheduleResultMapper.getCxConsume(scheduleResult);
        }
        if (cxConsumeSummaryVo != null) {
            cxConsumeQty = cxConsumeSummaryVo.getCxConsumeQty();
            summaryVo.setCxConsumeQty(cxConsumeQty);
        }
        // 理论交班库存计算,理论交班库存 = 库存 + 昨日早班 + 夜班 - 成型消耗量
        Double stockQty = ObjectUtils.defaultIfNull(summaryVo.getStockQty(), 0D);
        Double nightPlanQty = ObjectUtils.defaultIfNull(summaryVo.getNightPlanQty(), 0D);
        if (lastDayPlanQty != null && cxConsumeQty != null) {
            summaryVo.setTheoreticClassStockQty(stockQty + lastDayPlanQty + nightPlanQty - cxConsumeQty);
        }
        return AjaxResult.success(summaryVo);
    }

    /**
     * 根据已删除的原线名称更新成对应的原线卷数和原线品牌个数
     *
     * @param scheduleDate         排程日期
     * @param originalLineCodeList 已删除的原线名称(不重复)
     */
    private void updateOriginalLineQtyNumAndBrandNum(Date scheduleDate, Set<String> originalLineCodeList) {
        // 更新所有原线名称和已删除的记录一样的原线卷数和原线品牌个数
        List<XwyyScheduleOriginalSumPlanVo> sumPlanByOriginalLineCode = xwyyScheduleResultMapper.selectSumPlanByOriginalLineCode(scheduleDate, originalLineCodeList);
        List<XwyyScheduleOriginalSumPlanVo> sumPlanByOriginalBrand = xwyyScheduleResultMapper.selectSumPlanByOriginalBrand(scheduleDate, originalLineCodeList);
        // 更新原线卷数
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        for (XwyyScheduleOriginalSumPlanVo planVo : sumPlanByOriginalLineCode) {
            params.put("bigRollCode", planVo.getBigRollCode());
            params.put("originalLineCode", planVo.getOriginalLineCode());
            // 计算原线卷数，结果保留1位小数，向上进位,原线长度如果为空，则除以1
            BigDecimal originalLineQtyNum = BigDecimal.valueOf(planVo.getSumPlan())
                    .divide(new BigDecimal(planVo.getOriginalLineLength()), 1, RoundingMode.UP);
            params.put("originalLineQtyNum", originalLineQtyNum);
            xwyyScheduleResultMapper.updateOriginalLineQtyNumByParams(params);
        }
        // 更新原线品牌个数,原线长度如果为空，则除以1
        for (XwyyScheduleOriginalSumPlanVo planVo : sumPlanByOriginalBrand) {
            params.put("bigRollCode", planVo.getBigRollCode());
            params.put("originalLineCode", planVo.getOriginalLineCode());
            BigDecimal originalBrandNum = BigDecimal.valueOf(planVo.getSumPlan())
                    .divide(new BigDecimal(planVo.getOriginalLineLength()), 1, RoundingMode.UP);
            params.put("originalBrandNum", originalBrandNum);
            String originalBrand = planVo.getOriginalBrand();
            params.put("originalBrand", originalBrand);
            if (StringUtils.isBlank(originalBrand)) {
                params.put("originalBrandNum", 0);
            }
            xwyyScheduleResultMapper.updateOriginalBrandNumByParams(params);
        }
    }

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    @Override
    public byte[] export(List<XwyyScheduleResultDto> list) {
        XwyyScheduleResultDto summarySchedule = this.summaryExport(list);  //给导出的数据增加汇总行
        // 按用户语言读取模板
        Locale lang = ServletUtils.getUserLang();
        InputStream inputStream = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(lang) || lang == null) {
            // 中文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "xwyyScheduleResult.xlsx");
        } else if (Locale.US.equals(lang)) {
            // 英文
            inputStream = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + "xwyyScheduleResult_en.xlsx");
        }
        Workbook webBook = ExcelUtils.readExcel(inputStream);
        CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
        DataFormat format = webBook.createDataFormat();
        cellStyle.setDataFormat(format.getFormat("[=0]\"\""));  //导出的单元格如果值为0，则显示空白
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            // 添加导出生产线
            List<XwyyMachineInfo> machineInfos = xwyyMachineInfoService.selectMachineInfoList(new XwyyMachineInfo());
            Map<String, String> map = null;
            if (CollectionUtils.isNotEmpty(machineInfos)) {
                map = machineInfos.stream().collect(Collectors.toMap(item -> item.getId() + "", XwyyMachineInfo::getMachineName));
            }
            Sheet sheet = webBook.getSheetAt(0);
            int month = DateUtil.getMonth(list.get(0).getScheduleDate());
            int day = DateUtil.getDay(list.get(0).getScheduleDate());
            Row row1 = sheet.getRow(0);

            BigDecimal midPlan = new BigDecimal(summarySchedule.getDayPlanQty());
            BigDecimal nightPlan = new BigDecimal(summarySchedule.getNightPlanQty());

            for (int i = 0; i < list.size(); i++) {
                int cellNum = 0;
                XwyyScheduleResultDto scheduleResult = list.get(i);
                Row row = sheet.createRow(i + 2);
//                row.createCell(cellNum++).setCellValue(DateFormatUtils.format(scheduleResult.getScheduleDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(scheduleResult.getBigRollCode() == null ? "" : scheduleResult.getBigRollCode());
                row.createCell(cellNum++).setCellValue(scheduleResult.getOriginalLineName() == null ? "" : scheduleResult.getOriginalLineName());
                row.createCell(cellNum++).setCellValue(scheduleResult.getOriginalLineQtyNum() == null ? "" : scheduleResult.getOriginalLineQtyNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getOriginalBrand() == null ? "" : scheduleResult.getOriginalBrand());
                row.createCell(cellNum++).setCellValue(scheduleResult.getOriginalBrandNum() == null ? BigDecimal.ZERO.doubleValue() : scheduleResult.getOriginalBrandNum().doubleValue());
                StringBuilder produceLine = new StringBuilder();
                if (StringUtils.isNotEmpty(scheduleResult.getMachineId()) && map != null) {
                    String[] aa = scheduleResult.getMachineId().split(",");
                    for (String a : aa) {
                        produceLine.append(map.get(a)).append(",");
                    }
                }
                if (StringUtils.isNotEmpty(produceLine.toString())) {
                    produceLine = new StringBuilder(produceLine.substring(0, produceLine.length() - 1));
                }
                row.createCell(cellNum++).setCellValue(produceLine.toString());
                row.createCell(cellNum++).setCellValue(scheduleResult.getMonthPlanOs() == null ? 0 : Double.parseDouble(scheduleResult.getMonthPlanOs()));
                row.createCell(cellNum++).setCellValue(scheduleResult.getSupplyTime() == null ? 0 : scheduleResult.getSupplyTime());
                row.createCell(cellNum++).setCellValue(scheduleResult.getYesStock() == null ? 0 : scheduleResult.getYesStock());
                row.createCell(cellNum++).setCellValue(scheduleResult.getTodayStock() == null ? 0 : scheduleResult.getTodayStock());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayUsed() == null ? 0 : scheduleResult.getDayUsed());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQty() == null ? 0 : scheduleResult.getDailyTotalQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDailyTotalQtyNum() == null ? 0d : scheduleResult.getDailyTotalQtyNum());
                //胶料号
                row.createCell(cellNum++).setCellValue(scheduleResult.getRubberCode());
                //胶料车数
                row.createCell(cellNum++).setCellValue(scheduleResult.getRubberCarNumber() == null ? BigDecimal.ZERO.doubleValue() : scheduleResult.getRubberCarNumber().doubleValue());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayPlanQty() == null ? 0 : scheduleResult.getDayPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayPlanQtyNum() == null ? 0d : scheduleResult.getDayPlanQtyNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getDayFinishQty() == null ? 0 : scheduleResult.getDayFinishQty());
                String sysAnaly = scheduleResult.getDaySysAnalysis();
                String handAnaly = scheduleResult.getDayHandAnalysis();
                String anly = "";
                if (StringUtils.isNotEmpty(sysAnaly)) {
                    anly = anly + sysAnaly;
                }
                if (StringUtils.isNotEmpty(handAnaly)) {
                    if (StringUtils.isNotEmpty(anly)) {
                        anly = anly + "," + handAnaly;
                    } else {
                        anly = handAnaly;
                    }
                }
                row.createCell(cellNum++).setCellValue(anly);
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightPlanQty() == null ? 0 : scheduleResult.getNightPlanQty());
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightPlanQtyNum() == null ? 0d : scheduleResult.getNightPlanQtyNum());
                row.createCell(cellNum++).setCellValue(scheduleResult.getNightFinishQty() == null ? 0 : scheduleResult.getNightFinishQty());
                String nightSysAnaly = scheduleResult.getNightSysAnalysis();
                String nightHandAnaly = scheduleResult.getNightHandAnalysis();
                String nightAnly = "";
                if (StringUtils.isNotEmpty(nightSysAnaly)) {
                    nightAnly = nightAnly + nightSysAnaly;
                }
                if (StringUtils.isNotEmpty(nightHandAnaly)) {
                    if (StringUtils.isNotEmpty(nightAnly)) {
                        nightAnly = nightAnly + "," + nightHandAnaly;
                    } else {
                        nightAnly = nightHandAnaly;
                    }
                }
                row.createCell(cellNum++).setCellValue(nightAnly);
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass1Plan() == null ? 0 : scheduleResult.getCxClass1Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass2Plan() == null ? 0 : scheduleResult.getCxClass2Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass3Plan() == null ? 0 : scheduleResult.getCxClass3Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass4Plan() == null ? 0 : scheduleResult.getCxClass4Plan());
                row.createCell(cellNum++).setCellValue(scheduleResult.getCxClass5Plan() == null ? 0 : scheduleResult.getCxClass5Plan());
                row.createCell(cellNum).setCellValue(scheduleResult.getRemark() == null ? "" : scheduleResult.getRemark());
                setCellStyle(row, row.getPhysicalNumberOfCells(), cellStyle);
            }

            //重置表头基本信息
            String dateStr = "";
            if ("zh_CN".equals(lang.toString())) {
                dateStr = DateUtils.parseDateToStr("MM月dd日", list.get(0).getScheduleDate());
            } else {
                String monthStr = month + "";
                String dayStr = day + "";
                if (monthStr.length() <= 1) {
                    monthStr = "0" + month;
                }
                if (dayStr.length() <= 1) {
                    dayStr = "0" + day;
                }
                dateStr = DateUtil.getEngMonthDay(monthStr + dayStr) + " ";
            }
            String baseInfo = I18nUtil.getMessage("ui.data.column.scheduleResult.xwyy.baseInfo");
            String class1Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.heji.zhongban");
            String class2Plan = I18nUtil.getMessage("ui.data.column.scheduleResult.heji.yeban");
            String totalQty = I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty");
            String planInfo = '：' + class1Plan + '：' + midPlan.setScale(0, BigDecimal.ROUND_HALF_UP) + '，' + class2Plan + '：' + nightPlan.setScale(0, BigDecimal.ROUND_HALF_UP) + '，' + totalQty + '：' + (midPlan.add(nightPlan)).setScale(0, BigDecimal.ROUND_HALF_UP);
            baseInfo = dateStr + baseInfo + planInfo;
            Cell cell0 = sheet.getRow(0).getCell(0);
            CellStyle cellStyle0 = cell0.getCellStyle();
            cell0.setCellValue(baseInfo);
            cell0.setCellStyle(cellStyle0);

        }
        //写出字节流
        ByteArrayOutputStream out = null;
        byte[] data = null;
        try {
            out = new ByteArrayOutputStream();
            webBook.write(out);
            data = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert out != null;
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    /**
     * 给导出的数据增加汇总行
     *
     * @param list
     */
    private XwyyScheduleResultDto summaryExport(List<XwyyScheduleResultDto> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        XwyyScheduleResultDto summary = new XwyyScheduleResultDto();
        summary.setBigRollCode(I18nUtil.getMessage("ui.data.column.scheduleResult.totalQty"));
        summary.setDayPlanQty(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getDayPlanQty())).sum());
        summary.setDayPlanQtyNum(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getDayPlanQtyNum())).sum());
        summary.setDayFinishQty(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getDayFinishQty())).sum());
        summary.setNightPlanQty(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getNightPlanQty())).sum());
        summary.setNightPlanQtyNum(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getNightPlanQtyNum())).sum());
        summary.setNightFinishQty(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getNightFinishQty())).sum());
        summary.setDailyTotalQty(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getDailyTotalQty())).sum());
        summary.setDailyTotalQtyNum(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getDailyTotalQtyNum())).sum());

        summary.setCxClass1Plan(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getCxClass1Plan())).sum());
        summary.setCxClass2Plan(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getCxClass2Plan())).sum());
        summary.setCxClass3Plan(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getCxClass3Plan())).sum());
        summary.setCxClass4Plan(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getCxClass4Plan())).sum());
        summary.setCxClass5Plan(list.stream().mapToDouble(r -> getDoubleOrDefault(r.getCxClass5Plan())).sum());
        list.add(summary);
        return summary;
    }

    /**
     * 将排程数据导出到文件
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override

    public List<HalfYyExportDataVo> exportDataToList(Date scheduleDate) {
        int addDay = 15;
        List<Integer> addDayList = new ArrayList<>();
        for (int i = 0; i < addDay; i++) {
            addDayList.add(i);
        }
        XwyyScheduleResult xwyyScheduleResult = new XwyyScheduleResult();
        xwyyScheduleResult.setScheduleDate(scheduleDate);
        List<HalfYyExportDataVo> halfYyExportDataVos = xwyyScheduleResultMapper.selectYyResultList(xwyyScheduleResult, addDayList);
        if (CollectionUtils.isNotEmpty(halfYyExportDataVos)) {
            // 取纤维、钢带原线规格长度
            LambdaQueryWrapper<XwyyOriginalLineSpec> xwyyQueryWrapper = new LambdaQueryWrapper<>();
            xwyyQueryWrapper.eq(ApsBaseEntity::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
            List<XwyyOriginalLineSpec> xwyyOriginalLineSpecList = xwyyOriginalLineSpecMapper.selectList(xwyyQueryWrapper);
            Map<String, String> xwyyOriginalLineLengthMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(xwyyOriginalLineSpecList)) {
                xwyyOriginalLineLengthMap = xwyyOriginalLineSpecList.stream().collect(Collectors.toMap(XwyyOriginalLineSpec::getOriginalLineCode, XwyyOriginalLineSpec::getOriginalLineLength));
            }
            LambdaQueryWrapper<GdyyOriginalLineSpec> gdyyQueryWrapper = new LambdaQueryWrapper<>();
            gdyyQueryWrapper.eq(ApsBaseEntity::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
            List<GdyyOriginalLineSpec> gdyyOriginalLineSpecList = gdyyOriginalLineSpecEntityMapper.selectList(gdyyQueryWrapper);
            Map<String, String> gdyyOriginalLineLengthMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(gdyyOriginalLineSpecList)) {
                gdyyOriginalLineLengthMap = gdyyOriginalLineSpecList.stream().collect(Collectors.toMap(GdyyOriginalLineSpec::getOriginalLineCode, GdyyOriginalLineSpec::getOriginalLineLength));
            }
            // 取纤维、钢带库存数量
//            String startTime = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, DateUtils.addDays(scheduleDate, -1));
            String endTime = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate);
            XwyyStock xwyyStock = new XwyyStock();
            xwyyStock.setStartTime(endTime);
            xwyyStock.setEndTime(endTime);
            List<XwyyStock> xwyyStockList = xwyyStockMapper.selectStockList(xwyyStock);
            Map<String, BigDecimal> xwyyStockMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(xwyyStockList)) {
                xwyyStockMap = xwyyStockList.stream().collect(Collectors.toMap(XwyyStock::getMaterialCode,
                        item -> BigDecimalUtils.add(
                                ObjectUtils.defaultIfNull(item.getStockNum(), BigDecimal.ZERO),
                                ObjectUtils.defaultIfNull(item.getModifyNum(), BigDecimal.ZERO),
                                ObjectUtils.defaultIfNull(item.getBadNum(), BigDecimal.ZERO)
                        )));
            }
            GdyyStock gdyyStock = new GdyyStock();
            gdyyStock.setStartTime(endTime);
            gdyyStock.setEndTime(endTime);
            List<GdyyStock> gdyyStockList = gdyyStockMapper.selectStockList(gdyyStock);
            Map<String, BigDecimal> gdyyStockMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(gdyyStockList)) {
                gdyyStockMap = gdyyStockList.stream().collect(Collectors.toMap(GdyyStock::getMaterialCode,
                        item -> BigDecimalUtils.add(item.getStockNum(), item.getModifyNum(), item.getBadNum())));
            }
            for (HalfYyExportDataVo halfYyExportDataVo : halfYyExportDataVos) {
                String code = halfYyExportDataVo.getCode();
                if (xwyyStockMap.containsKey(code)) {
                    halfYyExportDataVo.setStockQty(xwyyStockMap.get(code).doubleValue());
                }
                if (gdyyStockMap.containsKey(code)) {
                    halfYyExportDataVo.setStockQty(gdyyStockMap.get(code).doubleValue());
                }
                if (xwyyOriginalLineLengthMap.containsKey(code)) {
                    halfYyExportDataVo.setOriLineLength(Double.parseDouble(xwyyOriginalLineLengthMap.get(code)));
                }
                if (gdyyOriginalLineLengthMap.containsKey(code)) {
                    halfYyExportDataVo.setOriLineLength(Double.parseDouble(gdyyOriginalLineLengthMap.get(code)));
                }
            }
        }
        return halfYyExportDataVos;
    }

    /**
     * 将线下排程模板的昨日计划、昨日库存，导入到系统
     *
     * @param list 要导入的数据
     * @return 结果
     */
    @Override
    public AjaxResult importExcelToLastDayPlanAndStock(List<HalfYyExportDataVo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.success();
        }
        Date scheduleDate = list.get(0).getScheduleDate();
        // 纤维大卷长度配置
        Map<String, BigDecimal> xwyyBigRollMap = cd90BigRollService.getByParams(new CxTCd90BigRoll()).stream()
                .collect(Collectors.toMap(CxTCd90BigRoll::getBigRollCode, CxTCd90BigRoll::getActClothLength));
        BigDecimal xwyyStandardSize = new BigDecimal(500);
        XwyyParams xwyyParams = new XwyyParams();
        xwyyParams.setParamCode("STANDARD_SIZE");
        List<XwyyParamsDto> xwyyParamsDtos = xwyyParamsMapper.listParams(xwyyParams);
        if (CollectionUtils.isNotEmpty(xwyyParamsDtos)) {
            String paramValue = xwyyParamsDtos.get(0).getParamValue();
            xwyyStandardSize = new BigDecimal(paramValue);
        }
        // 钢带大卷长度配置
        Map<String, BigDecimal> gdyyBigRollMap = cd15BigRollService.getByParams(new CxTCd15BigRoll()).stream()
                .collect(Collectors.toMap(CxTCd15BigRoll::getBigRollCode, CxTCd15BigRoll::getActClothLength));
        BigDecimal gdyyStandardSize = new BigDecimal(500);
        GdyyParams gdyyParams = new GdyyParams();
        gdyyParams.setParamCode("STANDARD_SIZE");
        List<GdyyParamsDto> gdyyParamsDtos = gdyyParamsMapper.listParams(gdyyParams);
        if (CollectionUtils.isNotEmpty(gdyyParamsDtos)) {
            String paramValue = gdyyParamsDtos.get(0).getParamValue();
            gdyyStandardSize = new BigDecimal(paramValue);
        }
        // 更新库存，排程日期等于Excel日期
        List<HalfYyExportDataVo> xwyyStockList = new ArrayList<>();
        List<HalfYyExportDataVo> gdyyStockList = new ArrayList<>();
        List<HalfYyExportDataVo> xwyyResultList = new ArrayList<>();
        List<HalfYyExportDataVo> gdyyResultList = new ArrayList<>();
        for (HalfYyExportDataVo halfYyExportDataVo : list) {
            if (halfYyExportDataVo.getCode().startsWith(("SD"))) {
                if (halfYyExportDataVo.getStockQty() != null && halfYyExportDataVo.getStockQty() > 0) {
                    gdyyStockList.add(halfYyExportDataVo);
                }
                // 大卷长度
                BigDecimal clothLength = gdyyBigRollMap.getOrDefault(halfYyExportDataVo.getCode(), gdyyStandardSize);
                halfYyExportDataVo.setStandSize(clothLength);
                gdyyResultList.add(halfYyExportDataVo);
            } else {
                if (halfYyExportDataVo.getStockQty() != null && halfYyExportDataVo.getStockQty() > 0) {
                    xwyyStockList.add(halfYyExportDataVo);
                }
                // 大卷长度
                BigDecimal clothLength = xwyyBigRollMap.getOrDefault(halfYyExportDataVo.getCode(), xwyyStandardSize);
                halfYyExportDataVo.setStandSize(clothLength);
                xwyyResultList.add(halfYyExportDataVo);
            }
        }
        // 更新库存信息，库存日期等于Excel日期-1天
        Date subDate = DateUtils.addDays(scheduleDate, -1);
        xwyyStockMapper.deleteStockByDate(subDate);
        xwyyStockMapper.insertBatch(xwyyStockList, subDate);
        gdyyStockMapper.deleteStockByDate(subDate);
        xwyyStockMapper.insertBatchToGdyy(gdyyStockList, subDate);

        int flag = 0;
        LambdaQueryWrapper<XwyyParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XwyyParams::getParamCode, "XWYY_IMPORT_FLAG");
        XwyyParams importFlagParams = xwyyParamsMapper.selectOne(wrapper);
        if (importFlagParams != null) {
            flag = Integer.parseInt(importFlagParams.getParamValue());
        }

        if (flag == 0) {
            xwyyScheduleResultMapper.batchUpdateToLastDayForXwyy(scheduleDate, xwyyResultList);
            xwyyScheduleResultMapper.batchUpdateToLastDayForGdyy(scheduleDate, gdyyResultList);
        } else {
            LambdaQueryWrapper<XwyyScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XwyyScheduleResult::getScheduleDate, scheduleDate);
            xwyyScheduleResultMapper.delete(queryWrapper);
            xwyyScheduleResultMapper.batchInsertToLastDayForXwyy(scheduleDate, xwyyResultList);
            xwyyScheduleResultMapper.deleteGdyyScheduleResultByScheduleDate(scheduleDate);
            xwyyScheduleResultMapper.batchInsertToLastDayForGdyy(scheduleDate, gdyyResultList);
            xwyyEngineService.batchUpdateBatchNoAndOrderNo(scheduleDate);
            gdyyEngineService.batchUpdateBatchNoAndOrderNo(scheduleDate);
        }
        return AjaxResult.success();
    }
}
