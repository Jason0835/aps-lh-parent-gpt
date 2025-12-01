package com.zlt.aps.cx.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.common.CommonCacheService;
import com.zlt.aps.common.CommonQueryCacheService;
import com.zlt.aps.common.CommonRedisService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.enums.HalfComponentCodeEnums;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.config.CxShiftConfig;
import com.zlt.aps.constants.CxEngineConstants;
import com.zlt.aps.constants.CxParamCodeConstants;
import com.zlt.aps.constants.CxPrefixConstants;
import com.zlt.aps.cx.mapper.entity.*;
import com.zlt.aps.cx.service.CxDispatcherLogService;
import com.zlt.aps.cx.service.CxMatchingSpecifyMachineService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.IAutoScheduleLogService;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxCheckConstructionResultDto;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxTransferDeskDTO;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.*;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxGanttVo;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxMachineInfoVo;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxProductConstructionInfoDto;
import com.zlt.aps.cxlh.cx.api.domain.vo.LhAlgorithmScheduleResultDto;
import com.zlt.aps.lh.api.domain.bo.ValidateResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.mapper.LhScheduleResultEntityMapper;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.MdmProductConstructionVO;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * Description: 成型排程结果业务实现类
 *
 * @author 16799
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CxScheduleResultServiceImpl extends AbstractDocService<CxScheduleResult> implements CxScheduleResultService {
    @Autowired
    private CxScheduleResultEntityMapper cxScheduleResultEntityMapper;
    @Autowired
    private MdmMoldingMachineEntityMapper mdmMoldingMachineEntityMapper;
    @Autowired
    private MdmMoldingMachineClsEntityMapper mdmMoldingMachineClsEntityMapper;
    @Autowired
    private MdmMoldingMachineClsBEntityMapper mdmMoldingMachineClsBEntityMapper;
    @Autowired
    private CxMatchingSpecifyMachineService iCxMatchingSpecifyMachineService;
    @Autowired
    private IMdmProductConstructionService mdmProductConstructionService;
    @Autowired
    private CommonRedisService commonRedisService;
    @Autowired
    private MdmProductInfoEntityMapper mdmProductInfoEntityMapper;
    @Autowired
    private CxProductConstructionInfoMapper cxProductConstructionInfoMapper;
    @Autowired
    private AutoScheduleLogEntityMapper autoScheduleLogEntityMapper;
    @Resource
    private CommonQueryCacheService commonQueryCacheService;
    @Autowired
    private ProductMoldingLimitMapper productMoldingLimitMapper;
    @Resource
    private CommonCacheService commonCacheService;
    @Autowired
    private CxSchedulingAlgorithmResultServiceImpl cxSchedulingAlgorithmResultService;
    @Autowired
    private IAutoScheduleLogService autoScheduleLogService;
    @Autowired
    private CxDispatcherLogService cxDispatcherLogService;

    private static Map<String, Field> constructionFieldsMap = null;

    @Autowired
    private LhScheduleResultEntityMapper lhScheduleAdjustEntityMapper;
    @Autowired
    private CxOnlineImportEntityMapper cxOnlineImportEntityMapper;
    @Autowired
    private CxStockEntityMapper cxStockEntityMapper;
    @Autowired
    private CxEmbryoMonthPlanSurplusEntityMapper cxEmbryoMonthPlanSurplusEntityMapper;

    @Override
    public String getDocTypeCode() {
        return "OUT2046";
    }

    /**
     * 导出数据
     *
     * @param cxScheduleResult
     * @return
     */
    @Override
    public List<CxScheduleResult> selectListExportData(QueryWrapper<CxScheduleResult> cxScheduleResult) {
        return cxScheduleResultEntityMapper.selectList(cxScheduleResult);
    }

    /**
     * 获取指定天数的成型排程结果
     *
     * @param scheduleDate 排程天数
     * @return List<CxScheduleResult> 成型排程度结果
     */
    @Override
    public List<CxScheduleResult> selectListByDate(Date scheduleDate) {
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("SCHEDULE_DATE", scheduleDate);
        return cxScheduleResultEntityMapper.selectList(queryWrapper);
    }

    /**
     * 期初数据导入方法
     *
     * @param list         导入集合
     * @param importLogId  日志ID
     * @param scheduleDate 导入日期
     * @return 导入结果
     */
    @Override
    public AjaxResult importData3(List<CxOnlineImport> list, Long importLogId, String scheduleDate) {
        //Step1-初始化成功失败计数器
        int successNum = 0;
        int failureNum = 0;

        //Step1-初始化导入列表/导入错误列表
        List<CxOnlineImport> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();


        //Step3-进行数据必填数据校验/字典/格式校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxOnlineImport docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        if (CollectionUtils.isNotEmpty(list)) {
            //获取list中rq栏位SET集合
            Set<Date> rqSet = list.stream().map(CxOnlineImport::getRq).collect(Collectors.toSet());
            //执行删除
            for (Date rq : rqSet) {
                //更新半部件删除字段
                LambdaUpdateWrapper<CxOnlineImport> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(CxOnlineImport::getRq, rq)
                        .set(CxOnlineImport::getIsDelete, 1);
                cxOnlineImportEntityMapper.update(null, updateWrapper);
            }
        }

        for(int i = 0; i < list.size(); i++) {
            list.get(i).setId((long) (i+1));
            CxOnlineImport item = list.get(i);
            if (item.getWbwc() != null && item.getWbwc()>99999){
                item.setWbwc(0);
            }
            if (item.getZbwc() != null && item.getZbwc()>99999){
                item.setZbwc(0);
            }
            if (item.getSuwei1() != null && item.getSuwei1()>99999){
                item.setSuwei1(0);
            }
            if (item.getWbsw() != null && item.getWbsw()>99999){
                item.setWbsw(0);
            }
            if (item.getSy() != null && item.getSy()>99999){
                item.setSy(0);
            }
            if (item.getZbjh() == null){
                item.setZbjh(0);
            }
            if (item.getWbjh() == null){
                item.setWbjh(0);
            }
            if (StringUtils.isEmpty(item.getSt())){
                item.setSt(null);
            }
            importList.add(item);
            cxOnlineImportEntityMapper.insert(list.get(i));
            successNum++;
        }

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 期初数据导入方法
     *
     * @param list         导入集合
     * @param importLogId  日志ID
     * @param scheduleDate 导入日期
     * @return 导入结果
     */
    @Override
    public AjaxResult importData2(List<CxScheduleResult> list, Long importLogId, String scheduleDate) {
        //Step1-初始化成功失败计数器
        int successNum = 0;
        int failureNum = 0;


        //Step1-初始化导入列表/导入错误列表
        List<CxScheduleResult> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //Step3-进行数据必填数据校验/字典/格式校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }


        //Step2-初始化国际化提示
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
        String scheduleDateError = I18nUtil.getMessage("import.cx.validated.scheduleDate.error");
        String machineNotExist = I18nUtil.getMessage("ui.error.message.column.machineNotExist");
        String constructionNotExist = I18nUtil.getMessage("lh.engine.construction.info.empty.error2");
        String embryoCodeExist = I18nUtil.getMessage("lh.engine.embryoCode.construction.info.empty.error");


        //Step3-进行数据必填数据校验/字典/格式校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        //进行物料施工校验
        QueryWrapper<MdmProductInfo> mdmProductInfoQueryWrapper = new QueryWrapper<>();
        List<MdmProductInfo> mdmProductInfos = mdmProductInfoEntityMapper.selectList(mdmProductInfoQueryWrapper);
        Map<String, List<MdmProductInfo>> mdmProductInfoMap = mdmProductInfos.stream()
                .collect(Collectors.groupingBy(MdmProductInfo::getProductCode));

        //施工信息校验
        List<CxProductConstructionInfo> cxProductConstructionInfoList = cxProductConstructionInfoMapper.selectCxProductConstructionInfoList(new CxProductConstructionInfo());
        Map<String, List<CxProductConstructionInfo>> cxProductConstructionInfoListMap = cxProductConstructionInfoList.stream()
                .collect(Collectors.groupingBy(cxProductConstructionInfo -> cxProductConstructionInfo.getEmbryoVersion() + cxProductConstructionInfo.getEmbryoCode()));

        Date importScheduleDate = DateUtils.parseDate(scheduleDate);
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            if (docEntity.getScheduleDate() == null || docEntity.getScheduleDate().compareTo(importScheduleDate) != 0) {
                docEntity.setId(-999L);
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(scheduleDateError, errorNum, scheduleDate), importErrorLogs);
                continue;
            }

            if (docEntity.getSpecCode() == null || !mdmProductInfoMap.containsKey(docEntity.getSpecCode())) {
                docEntity.setId(-999L);
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(constructionNotExist, errorNum), importErrorLogs);
            } else {
                List<MdmProductInfo> mdmProductInfo = mdmProductInfoMap.get(docEntity.getSpecCode());
                if (!mdmProductInfo.isEmpty()) {
                    for (MdmProductInfo mdmProductInfoEntity : mdmProductInfo) {
                        if (BigDecimalUtils.safeCompare(mdmProductInfoEntity.getProSize(), BigDecimal.valueOf(docEntity.getSpecDimension() == null ? 0 : docEntity.getSpecDimension())) == 0) {
                            docEntity.setSpecDesc(mdmProductInfoEntity.getSpecifications());
                            docEntity.setLhSingleTireTime((double) (mdmProductInfoEntity.getCuringTime() == null ? 0 : mdmProductInfoEntity.getCuringTime()));
                        }
                    }
                }
            }

            if (docEntity.getEmbryoCode() == null || !cxProductConstructionInfoListMap.containsKey(docEntity.getBomDataVersion() + docEntity.getEmbryoCode())) {
                docEntity.setId(-999L);
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(embryoCodeExist, errorNum), importErrorLogs);
            }
        }

        //Step4-获取成型机进行校验
        HashMap<String, CxMachineInfoVo> cxMachineInfoVoArrayList = new HashMap<>();
        QueryWrapper<MdmMoldingMachine> queryMdmMoldingMachineWrapper = new QueryWrapper<>();
        List<MdmMoldingMachine> moldingMachines = mdmMoldingMachineEntityMapper.selectList(queryMdmMoldingMachineWrapper);

        //Step4-获取成型机类型表 MdmMoldingMachineCls,依据类型分组
        QueryWrapper<MdmMoldingMachineCls> queryMdmMoldingMachineClsWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineCls> moldingMachineCls = mdmMoldingMachineClsEntityMapper.selectList(queryMdmMoldingMachineClsWrapper);
        Map<Long, List<MdmMoldingMachineCls>> moldingMachineClassCodeMap = moldingMachineCls.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachineCls::getId));

        //Step4-获取成型机类型子表 MdmMoldingMachineClsB,依据主键ID分组
        QueryWrapper<MdmMoldingMachineClsB> queryMdmMoldingMachineClassWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineClsB> moldingMachineClsB = mdmMoldingMachineClsBEntityMapper.selectList(queryMdmMoldingMachineClassWrapper);
        Map<Long, List<MdmMoldingMachineClsB>> moldingMachineClassItemCodeMap = moldingMachineClsB.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachineClsB::getMoldingMachineClassId));

        //Step4-遍历 moldingMachines，构建 CxMachineInfoVo 对象并保存到上下文中
        moldingMachines.forEach(moldingMachine -> {
            // 初始化 CxMachineInfoVo 对象
            CxMachineInfoVo item = new CxMachineInfoVo();
            // 机台信息
            BeanUtils.copyProperties(moldingMachine, item);
            // 机台类型
            item.setMoldingMachineCls(moldingMachineClassCodeMap.get(item.getMoldingMachineClassId()) == null ? null : moldingMachineClassCodeMap.get(item.getMoldingMachineClassId()).get(0));
            // 机台类型子表
            item.setMoldingMachineClassList(moldingMachineClassItemCodeMap.get(item.getMoldingMachineClassId()) == null ? null : moldingMachineClassItemCodeMap.get(item.getMoldingMachineClassId()));
            cxMachineInfoVoArrayList.put(item.getMoldingMachineCode(), item);
        });

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            if (docEntity.getCxMachineCode() == null || !cxMachineInfoVoArrayList.containsKey(docEntity.getCxMachineCode())) {
                docEntity.setId(-999L);
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(machineNotExist, errorNum), importErrorLogs);
            } else {
                CxMachineInfoVo cxMachineInfoVo = cxMachineInfoVoArrayList.get(docEntity.getCxMachineCode());
                //填充机台类型
                docEntity.setCxMachineType(String.valueOf(cxMachineInfoVo.getMoldingMachineCls().getMouldMethod()));
                //获取机台定额
                if (cxMachineInfoVo.getMoldingMachineClassList() != null) {
                    cxMachineInfoVo.getMoldingMachineClassList().forEach(moldingMachineClassB -> {
                        if (BigDecimalUtils.safeCompare(moldingMachineClassB.getProSize(), docEntity.getSpecDimension() == null ? BigDecimal.ZERO : BigDecimal.valueOf(docEntity.getSpecDimension())) == 0) {
                            docEntity.setClass1MachineQuota((double) (moldingMachineClassB.getProductionQuotaQty() == null ? 0 : moldingMachineClassB.getProductionQuotaQty()));
                            docEntity.setClass2MachineQuota((double) (moldingMachineClassB.getProductionQuotaQty() == null ? 0 : moldingMachineClassB.getProductionQuotaQty()));
                        }
                    });
                }
            }
        }

        //成型自动排程批次号
        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", importScheduleDate);
        String cxBatchNo = commonRedisService.getSequence(CxPrefixConstants.SCHEDULE_BATCH_NO_PREFIX + scheduleDateStr, CxPrefixConstants.CX_BATCH_NO_PREFIX + scheduleDateStr);
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            //批次号
            docEntity.setCxBatchNo(cxBatchNo);
            //半部件删除标识
            docEntity.setDelFlag(0);
            //工单号
            docEntity.setOrderNo(commonRedisService.getSequence(CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleDateStr, CxPrefixConstants.CX_ORDER_NO_PREFIX + scheduleDateStr));
            //生产状态
            docEntity.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_DOING);
            //是否发布
            docEntity.setIsRelease(CxEngineConstants.IS_PUBLISH_NO);
        }


        //2.进行数据库唯一性校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else {
                //todo 如果是存在则更新,则需要自行实现
                failureNum++;
                //数据库已经存在,不允许插入
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (PubUtil.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        //更新半部件删除字段
        LambdaUpdateWrapper<CxScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResult::getScheduleDate, importScheduleDate)
                .set(CxScheduleResult::getDelFlag, 1);
        cxScheduleResultEntityMapper.update(null, updateWrapper);

        //删除排程日期对应的数据
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("SCHEDULE_DATE", importScheduleDate);
        cxScheduleResultEntityMapper.delete(queryWrapper);
        //先删除后插入
        successNum = baseDao.saveBatch(importList);

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }


    /**
     * 导入数据：导入数据方式【期初数据导入，或者线下排程导入】
     *
     * @param list          导入列表
     * @param updateSupport 是否覆盖
     * @param importLogId   导入日志id
     * @return 返回导入结果
     */
    @Override
    public AjaxResult importData(List<CxScheduleResult> list, boolean updateSupport, Long importLogId) {
        return new AjaxResult();
    }


    /**
     * 唯一性校验
     *
     * @param docEntityVO
     * @return
     */
    @Override
    public String checkUnique(CxScheduleResult docEntityVO) {
        // 唯一性判断依据: 根据业务修改
        return UserConstants.UNIQUE;
    }


    /**
     * title：按照日期获取成型排程计划
     *
     * @param scheduleDate 排程日期
     * @param scheduleLog  日志记录
     * @param values 当获取不到排程日期的结果时，数据从这里取（给预排第二天使用） Nick+ 2025-07-03
     * @return List<CxScheduleResult> 排程结果
     */
    @Override
    public List<CxScheduleResult> getScheduleCxScheduleResults(Date scheduleDate, StringBuilder scheduleLog, Collection<CxScheduleResult> values) {
        // 记录开始查询日志
        scheduleLog.append("开始获取成型排程计划，排程日期：").append(scheduleDate).append("\n");

        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("schedule_date", scheduleDate);
        List<CxScheduleResult> cxScheduleResults = cxScheduleResultEntityMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(cxScheduleResults) && CollectionUtils.isNotEmpty(values)){
            cxScheduleResults = new ArrayList<>(values);
        }

        // 记录查询结果数量
        scheduleLog.append("根据排程日期查询到 ").append(cxScheduleResults.size()).append(" 条排程结果\n");

        if (CollectionUtils.isNotEmpty(cxScheduleResults)) {
            for (CxScheduleResult cxScheduleResult : cxScheduleResults) {
                // 处理 class2ModifyQty
                if (cxScheduleResult.getClass2ModifyQty() == null) {
                    cxScheduleResult.setClass2ModifyQty(cxScheduleResult.getClass2PlanQty() == null ? 0 : cxScheduleResult.getClass2PlanQty());
                    scheduleLog.append("排程计划ID：").append(cxScheduleResult.getId())
                            .append("，class3ModifyQty 为空，设置为 class3PlanQty 的值：")
                            .append(cxScheduleResult.getClass2ModifyQty()).append("\n");
                }

                // 处理 class3ModifyQty
                if (cxScheduleResult.getClass3ModifyQty() == null) {
                    cxScheduleResult.setClass3ModifyQty(cxScheduleResult.getClass3PlanQty() == null ? 0 : cxScheduleResult.getClass3PlanQty());
                    scheduleLog.append("排程计划ID：").append(cxScheduleResult.getId())
                            .append("，class3ModifyQty 为空，设置为 class3PlanQty 的值：")
                            .append(cxScheduleResult.getClass3ModifyQty()).append("\n");
                }

                // 处理 class2ModifySort
                if (cxScheduleResult.getClass2ModifySort() == null) {
                    cxScheduleResult.setClass2ModifySort(cxScheduleResult.getClass2Sort() == null ? 0 : cxScheduleResult.getClass2Sort());
                    scheduleLog.append("排程计划ID：").append(cxScheduleResult.getId())
                            .append("，class2ModifySort 为空，设置为 class2Sort 的值：")
                            .append(cxScheduleResult.getClass2ModifySort()).append("\n");
                }

                // 处理 class3ModifySort
                if (cxScheduleResult.getClass3ModifySort() == null) {
                    cxScheduleResult.setClass3ModifySort(cxScheduleResult.getClass3Sort() == null ? 0 : cxScheduleResult.getClass3Sort());
                    scheduleLog.append("排程计划ID：").append(cxScheduleResult.getId())
                            .append("，class3ModifySort 为空，设置为 class3Sort 的值：")
                            .append(cxScheduleResult.getClass3ModifySort()).append("\n");
                }
            }
            scheduleLog.append("成功处理 ").append(cxScheduleResults.size()).append(" 条排程数据\n");
            return cxScheduleResults;
        } else {
            scheduleLog.append("未找到排程结果，返回空列表\n");
            return new ArrayList<>();
        }
    }


    @Override
    public void generateFinalSchedule(Map<String, CxScheduleResult> cxScheduleResultContextMap) {
        List<CxScheduleResult> cxScheduleResults = new ArrayList<>(cxScheduleResultContextMap.values());
        for (CxScheduleResult cxScheduleResult : cxScheduleResults) {
            cxScheduleResultEntityMapper.insert(cxScheduleResult);
        }
    }

    /**
     * 日志分割定义
     */
    private static final int MAX_LOG_LENGTH = 30000;

    @Override
    public void genScheduleLog(String scheduleLog, String cxBatchNo) {
        if (StringUtils.isBlank(scheduleLog)) {
            log.warn("生成日志失败：日志内容为空");
            return;
        }

        // 分割长日志
        List<String> logSegments = splitLongLog(scheduleLog);

        // 批量插入日志记录
        batchInsertLogs(logSegments, cxBatchNo);
    }


    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(long[] ids) {
        return cxScheduleResultEntityMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    @Override
    public CxScheduleResult selectById(Long id) {
        return cxScheduleResultEntityMapper.selectById(id);
    }

    /**
     * 变更计划数量前置校验
     *
     * @param cxScheduleResult 待校验的调量任务（包含班次计划量和机台定额）
     * @return 校验结果（包含通过/失败状态和提示信息）
     */
    @Override
    public ValidateResult changePlanQtyPreCheck(CxScheduleResult cxScheduleResult) {
        // region 1. 参数基础校验
        logger.debug("开始执行计划量变更前置校验，参数：{}", cxScheduleResult);

        // 创建错误信息收集器
        StringBuilder errorMsg = new StringBuilder();

        // 空参数检查
        if (cxScheduleResult == null) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.validate.param.empty.error"));
            logger.warn("参数校验失败：输入参数为空");
            return ValidateResult.error(errorMsg.toString());
        }

        // 关键字段检查
        String machineCode = cxScheduleResult.getCxMachineCode();
        if (StringUtils.isBlank(machineCode)) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.validate.machineCode.empty.error"));
            logger.warn("参数校验失败：机台编码为空");
        }

        // 存在错误立即返回
        if (errorMsg.length() > 0) {
            return ValidateResult.error(errorMsg.toString());
        }
        // endregion

        // region 2. 准备校验上下文
        logger.debug("开始准备校验上下文，机台：{}", machineCode);

        // 初始化提示信息收集器
        StringBuilder tipMsg = new StringBuilder();

        // 深拷贝参数对象（防止污染原始数据）
        CxScheduleResult context = new CxScheduleResult();
        BeanUtils.copyProperties(cxScheduleResult, context);
        logger.debug("创建校验上下文副本完成");
        // endregion

        // region 3. 加载系统配置
        logger.debug("开始加载系统配置参数");

        // 加载成型参数配置（使用LinkedHashMap保持顺序）
        Map<String, CxParams> cxParamsMap = new LinkedHashMap<>();
        commonQueryCacheService.queryCxParams().forEach(p -> cxParamsMap.put(p.getParamCode(), p));
        logger.debug("加载成型参数完成，参数数量：{}", cxParamsMap.size());

        // 初始化班次配置
        CxShiftConfig shiftConfig = CxShiftConfig.of(commonCacheService.getCxShiftSystem(cxParamsMap));
        int shiftStartHour = commonCacheService.getCxShiftSystemStartHour(cxParamsMap);
        shiftConfig.setStartTime(context.getScheduleDate(), shiftStartHour);
        logger.info("班次配置初始化完成，班制类型：{}，基准小时：{}", shiftConfig.getShiftCount(), shiftStartHour);
        // endregion

        // region 4. 班次计划量校验
        List<Integer> validClasses = shiftConfig.getValidClasses();
        logger.debug("开始校验有效班次，班次列表：{}", validClasses);

        for (int classNo : validClasses) {
            // 动态生成字段键名
            final String planQtyKey = String.format("class%dPlanQty", classNo);
            final String quotaKey = String.format("class%dMachineQuota", classNo);

            // 获取当前班次数据
            int currentQty = (int) context.getFieldValueByFieldName(planQtyKey);
            double classQuota = (double) context.getFieldValueByFieldName(quotaKey);
            logger.debug("班次{}校验：计划量={} 定额={}", classNo, currentQty, classQuota);

            // 执行额度校验
            if (currentQty > classQuota) {
                String message = StringUtils.format(
                        I18nUtil.getMessage("cx.engine.changeQty.class" + classNo + "PlanQty.tip"),
                        currentQty,
                        classQuota
                );
                tipMsg.append(message);
                logger.warn("班次{}计划量超限：当前{} > 定额{}", classNo, currentQty, classQuota);
            }
        }
        // endregion

        // region 5. 返回最终结果
        if (tipMsg.length() > 0) {
            logger.info("校验完成，存在提示信息：{}", tipMsg);
            return ValidateResult.success(tipMsg.toString());
        }
        logger.debug("校验通过，无异常提示");
        return ValidateResult.success();
        // endregion
    }

    @Override
    public List<CxScheduleResult> checkScheduleResultUnique(CxScheduleResult cxScheduleResult) {
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("id", cxScheduleResult.getId());
        queryWrapper.eq("schedule_date", cxScheduleResult.getScheduleDate());
        queryWrapper.eq("embryo_code", cxScheduleResult.getEmbryoCode());
        queryWrapper.eq("spec_code", cxScheduleResult.getSpecCode());
        queryWrapper.eq("bom_data_version", cxScheduleResult.getBomDataVersion());
        List<CxScheduleResult> cxScheduleResults = cxScheduleResultEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(cxScheduleResults)) {
            return cxScheduleResults;
        }
        return new ArrayList<>();
    }

    /**
     * 根据机台编号和排程时间查询排程结果
     *
     * @param factoryCode
     * @param machineCode
     * @param scheduleDate
     * @return
     */
    @Override
    public CxScheduleResult getScheduleResultByMachineCodeAndScheduleDate(String factoryCode, String machineCode, Date scheduleDate) {
        LambdaQueryWrapper<CxScheduleResult> query = new LambdaQueryWrapper<>();
        query.eq(CxScheduleResult::getFactoryCode, factoryCode)
                .eq(CxScheduleResult::getLhMachineCode, machineCode)
                .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                .eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        return cxScheduleResultEntityMapper.selectOne(query);
    }

    /**
     * 转机台
     *
     * @param dto
     */
    @Override
    public void changeMachine(CxTransferDeskDTO dto) {
        CxScheduleResult cxScheduleResult = this.selectById(dto.getId());
        //进行转机台赋值
        cxScheduleResult.setIsRelease(cxScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        cxScheduleResult.setRemark("原始机台：" + cxScheduleResult.getCxMachineCode() + ",转入机台：" + dto.getCxMachineCode());
        cxScheduleResult.setCxMachineCode(dto.getCxMachineCode());
        //插入转机台日志
        autoScheduleLogService.insertLhScheduleLog(ApsConstant.APS_STRING_1, cxScheduleResult.getCxBatchNo(), cxScheduleResult.getOrderNo(), "转机台日志",
                logSplit("原始机台：" + cxScheduleResult.getLhMachineCode(), ",转入机台：" + dto.getCxMachineCode(), "操作人员：" + SecurityUtils.getUsername(), "操作时间：" + DateUtils.getTime())); //添加日志
        //排程操作日志
        CxScheduleResult oldSchedule = this.selectById(dto.getId());
        insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, oldSchedule, cxScheduleResult);

        //更新排程
        cxScheduleResultEntityMapper.updateById(cxScheduleResult);
    }

    /**
     * 分割超长日志内容
     */
    private List<String> splitLongLog(String logContent) {
        List<String> segments = new ArrayList<>();

        // 按换行符优先分割
        String[] lines = logContent.split("\\r?\\n");
        StringBuilder currentSegment = new StringBuilder();

        for (String line : lines) {
            // 单行超长处理
            if (line.length() > MAX_LOG_LENGTH) {
                // 先保存已有内容
                if (currentSegment.length() > 0) {
                    segments.add(currentSegment.toString());
                    currentSegment.setLength(0);
                }
                // 强制分割超长行
                segments.addAll(splitByFixedLength(line));
                continue;
            }

            // 正常行处理
            if (currentSegment.length() + line.length() + 1 > MAX_LOG_LENGTH) {
                segments.add(currentSegment.toString());
                currentSegment.setLength(0);
            }
            if (currentSegment.length() > 0) {
                currentSegment.append("\n");
            }
            currentSegment.append(line);
        }

        // 添加最后一段
        if (currentSegment.length() > 0) {
            segments.add(currentSegment.toString());
        }

        return segments;
    }


    /**
     * 按固定长度分割字符串
     */
    private List<String> splitByFixedLength(String text) {
        List<String> parts = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int endIndex = Math.min(index + CxScheduleResultServiceImpl.MAX_LOG_LENGTH, text.length());
            parts.add(text.substring(index, endIndex));
            index = endIndex;
        }
        return parts;
    }


    /**
     * 批量插入日志记录
     */
    private void batchInsertLogs(List<String> logSegments, String cxBatchNo) {
        String dateStr = DateUtils.parseDateToStr("yyyyMMdd", new Date());
        String baseOrderNo = commonRedisService.getSequence(
                CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + dateStr,
                CxPrefixConstants.CX_ORDER_NO_PREFIX + dateStr
        );

        List<AutoScheduleLog> logs = new ArrayList<>();
        for (int i = 0; i < logSegments.size(); i++) {
            AutoScheduleLog log = new AutoScheduleLog();
            log.setProcedureCode(ApsConstant.APS_STRING_1);
            log.setTitle("成型排程过程日志" + (logSegments.size() > 1 ? "（部分" + (i + 1) + "）" : ""));
            log.setBatchNo(cxBatchNo);
            log.setDelFlag(ApsConstant.APS_STRING_0);
            log.setLogDetail(logSegments.get(i));
            // 追加序号
            log.setOrderNo(baseOrderNo + "_" + (i + 1));
            logs.add(log);
            // 批量插入（根据ORM框架调整实现）
            autoScheduleLogEntityMapper.insert(log);
        }
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public Long isReleasingOrTimeoutByIds(Long[] ids) {
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("IS_RELEASE", new Object[]{CxEngineConstants.RELEASING, CxEngineConstants.TIMEOUT_FAILURE});
        queryWrapper.in("ID", ids);
        return cxScheduleResultEntityMapper.selectCount(queryWrapper);
    }

    /**
     * title : 修改排程数量
     *
     * @param cxScheduleResult 调量结果
     * @return 调量结果
     */
    @Override
    public AjaxResult changeQty(CxScheduleResult cxScheduleResult) {
        //唯一性校验
        List<CxScheduleResult> list = checkScheduleResultUnique(cxScheduleResult);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxScheduleResult.uniqueValidate"));
        } else {
            return updateCxScheduleResultForQty(cxScheduleResult);
        }
    }

    /**
     * 更新成型排程计划量（含状态管理和重排逻辑）
     *
     * @param cxScheduleResult 待更新的排程任务（必须包含有效ID）
     * @return 操作结果（成功/失败及提示信息）
     * @throws BusinessException 当任务不存在或数据异常时抛出
     */
    public AjaxResult updateCxScheduleResultForQty(CxScheduleResult cxScheduleResult) {
        // ==================== 1. 初始化校验阶段 ====================
        final long taskId = cxScheduleResult.getId();
        log.info("开始更新排程计划量，任务ID：{}", taskId);

        // 1.2 查询数据库记录
        CxScheduleResult dbRecord = cxScheduleResultEntityMapper.selectById(taskId);
        if (dbRecord == null) {
            log.error("排程任务不存在，ID：{}", taskId);
            return AjaxResult.error(I18nUtil.getMessage("cx.engine.record.not.exist"));
        }

        // 1.3 检查字段变更
        if (!isChange(cxScheduleResult, dbRecord)) {
            log.debug("检测到无有效字段变更，任务ID：{}", taskId);
            return AjaxResult.success();
        }

        // ==================== 2. 状态处理核心逻辑 ====================
        // 2.1 已发布状态处理
        if (ApsConstant.IS_RELEASE.equals(dbRecord.getIsRelease())) {
            log.info("处理已发布任务更新，任务ID：{}", taskId);

            // 2.1.1 班次时间窗口校验
            if (isTimeWindowExpired(cxScheduleResult)) {
                String errorMsg = buildTimeWindowMessage(cxScheduleResult);
                log.warn("时间窗口校验失败：{}", errorMsg);
                return AjaxResult.error(errorMsg);
            }

            // 2.1.2 执行重排操作
            List<CxScheduleResult> allTasks = loadRelatedTasks(cxScheduleResult);
            List<CxScheduleResult> resortedTasks = cxScheduleResultListReSort(allTasks, cxScheduleResult.getScheduleDate());

            // 2.1.3 批量更新任务状态
            updateTasksStatus(resortedTasks);
        }
        // 2.2 未发布状态处理
        else {
            log.debug("处理未发布任务更新，任务ID：{}", taskId);
            //如果是调度员操作，则需要增加操作日志
            insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, null, cxScheduleResult);
            cxScheduleResultEntityMapper.updateById(cxScheduleResult);
        }

        log.info("排程更新操作完成，任务ID：{}", taskId);
        return AjaxResult.success();
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, CxScheduleResult oldSchedule, CxScheduleResult newSchedule) {

        if(oldSchedule == null) {
            //操作前的排程数据
            oldSchedule = this.cxScheduleResultEntityMapper.selectById(newSchedule.getId());
        }
        CxDispatcherLog log = new CxDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        //排程日期
        log.setScheduleDate(newSchedule.getScheduleDate());
        //sap品号
        log.setSapCode(newSchedule.getSapCode());
        //胎胚代码
        log.setEmbryoCode(newSchedule.getEmbryoCode());
        //胎胚版本
        log.setEmbryoVersion(newSchedule.getBomDataVersion());
        //操作前的信息赋值
        log.setBeforeLhMachineCode(oldSchedule.getLhMachineCode());
        log.setBeforeCxMachineCode(oldSchedule.getCxMachineCode());
        log.setBeforeClass1Plan(oldSchedule.getClass1PlanQty());
        log.setBeforeClass2Plan(oldSchedule.getClass2PlanQty());
        log.setBeforeClass3Plan(oldSchedule.getClass3PlanQty());
        log.setBeforeClass4Plan(oldSchedule.getClass4PlanQty());
        log.setBeforeClass5Plan(oldSchedule.getClass5PlanQty());
        //操作后的信息赋值
        log.setAfterLhMachineCode(newSchedule.getLhMachineCode());
        log.setAfterCxMachineCode(newSchedule.getCxMachineCode());
        log.setAfterClass1Plan(newSchedule.getClass1PlanQty());
        log.setAfterClass2Plan(newSchedule.getClass2PlanQty());
        log.setAfterClass3Plan(newSchedule.getClass3PlanQty());
        log.setAfterClass4Plan(newSchedule.getClass4PlanQty());
        log.setAfterClass5Plan(newSchedule.getClass5PlanQty());
        log.setIsDelete(0);
        /** 调用插入日志方法 **/
        cxDispatcherLogService.insertCxDispatcherLog(log);
    }

// ==================== 内部工具方法 ====================

    /**
     * 检查时间窗口是否过期
     */
    private boolean isTimeWindowExpired(CxScheduleResult task) {
        // 获取班次配置
        Map<String, CxParams> paramsConfig = loadCxParamsConfig();
        CxShiftConfig shiftConfig = initShiftConfig(paramsConfig, task.getScheduleDate());

        // 倒序检查各班次
        List<Integer> validClasses = shiftConfig.getValidClasses();
        LocalDateTime now = LocalDateTime.now();

        for (int i = validClasses.size() - 1; i >= 0; i--) {
            int classCode = validClasses.get(i);
            String endTimeKey = String.format("class%dEndTime", classCode);
            Object endTimeValue = task.getFieldValueByFieldName(endTimeKey);

            if (endTimeValue instanceof LocalDateTime) {
                LocalDateTime endTime = (LocalDateTime) endTimeValue;
                if (now.isAfter(endTime)) {
                    log.debug("班次{}时间窗口已过期，结束时间：{}", classCode, endTime);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 构建时间窗口提示消息
     */
    private String buildTimeWindowMessage(CxScheduleResult task) {
        Map<String, CxParams> paramsConfig = loadCxParamsConfig();
        CxShiftConfig shiftConfig = initShiftConfig(paramsConfig, task.getScheduleDate());

        // 查找最近过期的班次
        List<Integer> validClasses = shiftConfig.getValidClasses();
        LocalDateTime now = LocalDateTime.now();

        for (int i = validClasses.size() - 1; i >= 0; i--) {
            int classCode = validClasses.get(i);
            String endTimeKey = String.format("class%dEndTime", classCode);
            Object endTimeValue = task.getFieldValueByFieldName(endTimeKey);

            if (endTimeValue instanceof LocalDateTime) {
                LocalDateTime endTime = (LocalDateTime) endTimeValue;
                if (now.isAfter(endTime)) {
                    return StringUtils.format(
                            I18nUtil.getMessage("cx.engine.change.tl.no.tip"),
                            now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    );
                }
            }
        }
        return I18nUtil.getMessage("cx.engine.timewindow.unknown.error");
    }

    /**
     * 加载关联任务列表
     */
    @Override
    public List<CxScheduleResult> loadRelatedTasks(CxScheduleResult task) {
        QueryWrapper<CxScheduleResult> query = new QueryWrapper<>();
        query.ne("id", task.getId());
        query.eq("schedule_date", task.getScheduleDate());
        query.eq("cx_machine_code", task.getCxMachineCode());

        List<CxScheduleResult> relatedTasks = cxScheduleResultEntityMapper.selectList(query);
        // 加入当前任务
        relatedTasks.add(task);
        return relatedTasks;
    }

    /**
     * 批量更新任务状态
     */
    @Override
    public void updateTasksStatus(List<CxScheduleResult> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            CxScheduleResult task = tasks.get(i);
            task.setPublishSuccessCount(i + 1);
            task.setIsRelease(ApsConstant.NO_RELEASE);
            //如果是调度员操作，则需要增加操作日志
            insetDispatcherLog(ApsConstant.DISPATCHER_OPER_PLAN, null, task);
            cxScheduleResultEntityMapper.updateById(task);
            log.debug("更新任务状态，ID：{}，发布次数：{}", task.getId(), i + 1);
        }
    }

    /**
     * 加载成型参数配置
     */
    private Map<String, CxParams> loadCxParamsConfig() {
        Map<String, CxParams> configMap = new HashMap<>();
        commonQueryCacheService.queryCxParams().forEach(p -> configMap.put(p.getParamCode(), p));
        logger.debug("加载成型参数完成，数量：{}", configMap.size());
        return configMap;
    }

    /**
     * 初始化班次配置
     */
    private CxShiftConfig initShiftConfig(Map<String, CxParams> paramsConfig, Date scheduleDate) {
        CxShiftConfig config = CxShiftConfig.of(commonCacheService.getCxShiftSystem(paramsConfig));
        int startHour = commonCacheService.getCxShiftSystemStartHour(paramsConfig);
        config.setStartTime(scheduleDate, startHour);
        logger.info("初始化班次配置完成，类型：{} 基准时间：{}",
                config.getShiftCount(), startHour);
        return config;
    }


    /**
     * 任务重排核心方法 - 根据欠胎时间和班次产能重新分配生产任务
     *
     * @param cxScheduleResultList 待重排任务列表（自动过滤null元素）
     * @param scheduleDate         排程基准日期（用于班次时间计算）
     * @return 按班次产能分配后的任务列表（包含各任务在各班次的起止时间和计划量）
     * @throws BusinessException 当施工信息缺失时抛出带国际化信息的业务异常
     */
    @Override
    public List<CxScheduleResult> cxScheduleResultListReSort(List<CxScheduleResult> cxScheduleResultList, Date scheduleDate) {
        // ==================== 1. 初始化阶段 ====================
        log.info("开始任务重排处理，原始任务数：{}，排程日期：{}",
                cxScheduleResultList.size(), scheduleDate);

        // 1.1 加载系统参数配置
        Map<String, CxParams> cxParamsMap = new ConcurrentHashMap<>();
        commonQueryCacheService.queryCxParams().forEach(param -> {
            cxParamsMap.put(param.getParamCode(), param);
            log.debug("加载参数配置：{}={}", param.getParamCode(), param.getParamValue());
        });

        // 1.2 初始化班次配置
        CxShiftConfig shiftConfig = CxShiftConfig.of(commonCacheService.getCxShiftSystem(cxParamsMap));
        int startHour = commonCacheService.getCxShiftSystemStartHour(cxParamsMap);
        shiftConfig.setStartTime(scheduleDate, startHour);
        log.info("班次配置初始化完成，班制类型：{}，每日开始时间：{}:00",
                shiftConfig.getShiftCount(), startHour);

        // ==================== 2. 数据准备阶段 ====================
        // 2.1 过滤并排序任务（按欠胎时间升序）
        List<CxScheduleResult> sortedTasks = cxScheduleResultList.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CxScheduleResult::getPreviousTireTime))
                .peek(task -> log.debug("待处理任务：ID={}, 规格={}, 欠胎时间={}",
                        task.getId(), task.getEmbryoCode(), task.getPreviousTireTime()))
                .collect(Collectors.toList());

        // 2.2 准备施工信息查询参数
        List<LhAlgorithmScheduleResultDto> constructionQueries = new ArrayList<>();
        for (CxScheduleResult item : sortedTasks) {
            LhAlgorithmScheduleResultDto queryDto = new LhAlgorithmScheduleResultDto();
            LhScheduleResult lhScheduleResult = new LhScheduleResult();
            lhScheduleResult.setEmbryoCode(item.getEmbryoCode());
            lhScheduleResult.setBomVersion(item.getBomDataVersion());
            queryDto.setLhScheduleResult(lhScheduleResult);
            constructionQueries.add(queryDto);
        }

        // 2.3 查询并分组施工信息
        List<CxProductConstructionInfoDto> constructions = commonQueryCacheService.queryEmbryoCodeInfo(constructionQueries);
        if (CollectionUtils.isEmpty(constructions)) {
            String errorMsg = I18nUtil.getMessage("cx.engine.construction.empty.exception");
            log.error(errorMsg);
            throw new BusinessException(errorMsg);
        }
        Map<String, List<CxProductConstructionInfoDto>> constructionMap = constructions.stream()
                .collect(Collectors.groupingBy(c -> c.getEmbryoCode() + c.getEmbryoVersion()));

        // ==================== 3. 核心重排逻辑 ====================
        List<CxScheduleResult> resultList = new ArrayList<>();
        LocalDateTime currentTime = shiftConfig.parseToDayFirstShiftStartTime();
        CxProductConstructionInfoDto lastConstruction = null;
        // 当前处理班次编号
        int currentShift = 1;
        // 班次内生产顺序
        int shiftSequence = 0;

        // 3.1 遍历处理每个任务
        for (CxScheduleResult originTask : sortedTasks) {
            // 深拷贝任务对象
            CxScheduleResult task = new CxScheduleResult();
            BeanUtils.copyProperties(originTask, task);
            resultList.add(task);

            log.info("开始处理任务[{}]，规格[{}]，计划量[{}]",
                    task.getId(), task.getEmbryoCode(), task.getProductNum());

            // 3.2 处理换型时间
            String constructionKey = task.getEmbryoCode() + task.getBomDataVersion();
            CxProductConstructionInfoDto currentConstruction = constructionMap.get(constructionKey).get(0);
            if (lastConstruction != null && currentConstruction != null) {
                double changeoverHours = cxSchedulingAlgorithmResultService.changeSpecTime(
                        currentConstruction, lastConstruction, Integer.parseInt(task.getCxMachineType()));
                currentTime = currentTime.plusHours((long) changeoverHours);
                log.info("规格变更[{}→{}]，增加换型时间：{}小时",
                        lastConstruction.getEmbryoCode(), currentConstruction.getEmbryoCode(), changeoverHours);
            }
            lastConstruction = currentConstruction;

            // 3.3 班次产能分配
            double remainingQty = task.getProductNum();
            for (int shiftNo : shiftConfig.getValidClasses()) {
                // 跳过非当前班次（currentShift会在循环内递增）
                if (shiftNo != currentShift) {
                    continue;
                }

                // 3.3.1 获取班次结束时间
                Map<String, String> shiftTime = shiftConfig.getShiftTimeByString("class" + shiftNo);
                LocalDateTime shiftEnd = LocalDateTime.parse(
                        shiftTime.get("endTime"),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // 3.3.2 计算可用产能
                Duration availableDuration = Duration.between(currentTime, shiftEnd);
                if (availableDuration.isNegative() || availableDuration.isZero()) {
                    log.warn("班次{}时间窗口已结束，跳过分配", shiftNo);
                    currentShift++;
                    continue;
                }

                Double shiftQuota = (Double) task.getFieldValueByFieldName("class" + shiftNo + "MachineQuota");
                BigDecimal availableSeconds = BigDecimal.valueOf(availableDuration.getSeconds());

                // 理论产量 = 定额 * (可用时间/班次总时间)
                BigDecimal theoreticalQty = BigDecimal.valueOf(shiftQuota)
                        .multiply(availableSeconds)
                        .divide(BigDecimal.valueOf(shiftConfig.getShiftDuration() * 3600L), 0, RoundingMode.DOWN);

                // 实际分配量（取理论产量和剩余量的较小值）
                BigDecimal actualQty = theoreticalQty.min(BigDecimal.valueOf(remainingQty));
                BigDecimal productionTime;

                if (actualQty.compareTo(theoreticalQty) < 0) {
                    // 当分配量小于理论产量时，按实际产量计算耗时
                    productionTime = actualQty
                            .multiply(BigDecimal.valueOf(shiftConfig.getShiftDuration() * 3600L))
                            .divide(BigDecimal.valueOf(shiftQuota), 8, RoundingMode.UP);
                } else {
                    // 否则使用全部可用时间
                    productionTime = availableSeconds;
                }

                log.debug("班次{}产能分配：定额={} 可用时间={}s 分配量={} 耗时={}s",
                        shiftNo, shiftQuota, availableSeconds, actualQty, productionTime);

                // 3.3.3 更新任务数据
                String shiftPrefix = "class" + shiftNo;
                task.setFieldValueByFieldName(shiftPrefix + "StartTime", convertToDate(currentTime));
                currentTime = currentTime.plusSeconds(productionTime.longValue());
                task.setFieldValueByFieldName(shiftPrefix + "EndTime", convertToDate(currentTime));
                task.setFieldValueByFieldName(shiftPrefix + "PlanQty", actualQty);

                // 更新班次顺序号
                shiftSequence = shiftSequence + 1;
                task.setFieldValueByFieldName(shiftPrefix + "Sort", shiftSequence);

                log.debug("班次{}顺序号更新为{}", shiftNo, shiftSequence);

                // 更新剩余量
                remainingQty -= actualQty.doubleValue();
                if (remainingQty <= 0) {
                    log.debug("任务[{}]分配完成，剩余量清零", task.getId());
                    break;
                }

                currentShift++; // 移动到下一个班次
            }
        }

        log.info("任务重排完成，共处理{}个任务", resultList.size());
        return resultList;
    }


    private boolean isChange(CxScheduleResult cxScheduleResult, CxScheduleResult scheduleResult) {
        boolean flag = compare(scheduleResult.getClass1PlanQty(), cxScheduleResult.getClass1PlanQty());
        flag = flag && compare(scheduleResult.getClass2PlanQty(), cxScheduleResult.getClass2PlanQty());
        flag = flag && compare(scheduleResult.getClass3PlanQty(), cxScheduleResult.getClass3PlanQty());
        flag = flag && compare(scheduleResult.getClass4PlanQty(), cxScheduleResult.getClass4PlanQty());
        flag = flag && compare(scheduleResult.getClass5PlanQty(), cxScheduleResult.getClass5PlanQty());
        flag = flag && compare(scheduleResult.getClass6PlanQty(), cxScheduleResult.getClass6PlanQty());
        if (!flag) {
            cxScheduleResult.setIsRelease(cxScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        }
        return flag;
    }

    public boolean compare(Integer d1, Integer d2) {
        return (Objects.equals(d1, d2));
    }


    /**
     * title : 修改排程结果
     *
     * @param cxScheduleResult 修改结果
     * @return 修改结果
     */
    @Override
    public AjaxResult edit(CxScheduleResult cxScheduleResult) {
        int result = cxScheduleResultEntityMapper.updateById(cxScheduleResult);
        if (result > 0) {
            return AjaxResult.success();
        }else {
            return AjaxResult.error();
        }
    }

    /**
     * title ：查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public Long isReleasingOrTimeoutByDate(Date scheduleDate) {
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("IS_RELEASE", new Object[]{CxEngineConstants.RELEASING, CxEngineConstants.TIMEOUT_FAILURE});
        queryWrapper.in("SCHEDULE_DATE", scheduleDate);
        return cxScheduleResultEntityMapper.selectCount(queryWrapper);
    }

    /**
     * title ：插单排程结果
     *
     * @param cxScheduleResult 排程结果
     * @return 排程结果
     */
    @Override
    public AjaxResult add(CxScheduleResult cxScheduleResult) {
        cxScheduleResult.setCxBatchNo(cxSchedulingAlgorithmResultService.generateBatchNumber(cxScheduleResult.getScheduleDate()));
        cxScheduleResult.setDelFlag(0);
        String scheduleStr = DateUtils.parseDateToStr("yyyyMMdd", cxScheduleResult.getScheduleDate());
        cxScheduleResult.setOrderNo(commonRedisService.getSequence(
                CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX + scheduleStr,
                CxPrefixConstants.CX_ORDER_NO_PREFIX + scheduleStr));
        cxScheduleResult.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_DOING);
        cxScheduleResult.setIsRelease(CxEngineConstants.IS_PUBLISH_NO);
        cxScheduleResult.setFactoryCode(SecurityUtils.getUserCurrentFactory());
        cxScheduleResult.setPublishSuccessCount(0);
 //       insertPreCheck(cxScheduleResult);

        // 插入排程结果
        int insertCount = cxScheduleResultEntityMapper.insert(cxScheduleResult);
        insetDispatcherLogInsertOrder(ApsConstant.DISPATCHER_OPER_INSERT_ORDER, new ArrayList<>() ,cxScheduleResult);
        if (insertCount > 0) {
            return AjaxResult.success();
        }
        return AjaxResult.error();
    }

    /**
     * 插单排程结果预检查
     * 1. 参数基础校验（非空、有效性）
     * 2. 成型机台可用性校验
     * 3. 施工基础信息校验
     * 4. 月度计划余量校验
     * 5. 机台特殊作业限制校验
     * 6. 设备特性匹配校验
     *
     * @param cxScheduleResult 插单排程结果
     * @return 检查结果（包含错误信息或提示信息）
     */
    @Override
    public ValidateResult insertPreCheck(CxScheduleResult cxScheduleResult) {
        // 日志记录入参
        log.info("[插单预检] 开始处理插单预检请求，参数: {}", JSON.toJSONString(cxScheduleResult));

        /*----------- 1. 参数基础校验 -----------*/
        if (cxScheduleResult == null) {
            log.error("[插单预检] 参数不能为空");
            return ValidateResult.error(I18nUtil.getMessage("cx.engine.insert.param.empty.error"));
        }

        StringBuilder errorMsg = new StringBuilder();
        String embryoCode = cxScheduleResult.getEmbryoCode();
        String machineCode = cxScheduleResult.getCxMachineCode();
        String bomDataVersion = cxScheduleResult.getBomDataVersion();

        // 非空校验（显式校验关键字段）
        if (cxScheduleResult.getScheduleDate() == null) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.scheduleDate.empty.error"));
            log.warn("[插单预检] 排程日期为空");
        }
        if (StringUtils.isEmpty(cxScheduleResult.getSpecCode())) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.sapCode.empty.error"));
            log.warn("[插单预检] 规格代码为空");
        }
        if (StringUtils.isEmpty(embryoCode)) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.embryoCode.empty.error"));
            log.warn("[插单预检] 胎胚代码为空");
        }
        if (StringUtils.isEmpty(bomDataVersion)) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.bomDataVersion.empty.error"));
            log.warn("[插单预检] Bom版本为空");
        }
        if (StringUtils.isEmpty(machineCode)) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.machineCode.empty.error"));
            log.warn("[插单预检] 成型机台编号为空");
        }

        // 基础校验不通过直接返回
        if (errorMsg.length() > 0) {
            log.error("[插单预检] 基础校验失败: {}", errorMsg);
            return ValidateResult.error(errorMsg.toString());
        }

        /*----------- 2. 成型机台可用性校验 -----------*/
        // 获取可用机台信息（按日期过滤）
        Date scheduleDate = cxScheduleResult.getScheduleDate();
        Map<String, CxMachineInfoVo> availableMachines = iCxMatchingSpecifyMachineService.getAvailableMoldingMachine(scheduleDate);
        if (MapUtils.isEmpty(availableMachines)) {
            log.error("[插单预检] 没有可用的成型机台，日期: {}", scheduleDate);
            return ValidateResult.error(I18nUtil.getMessage("cx.engine.machine.not.exsit"));
        }

        CxMachineInfoVo machineInfo = availableMachines.get(machineCode);
        if (machineInfo == null) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.machine.not.exsit"));
            log.error("[插单预检] 指定机台不可用，机台编号: {}", machineCode);
            return ValidateResult.error(errorMsg.toString());
        }
        log.info("[插单预检] 机台{}状态校验通过", machineCode);

        /*----------- 3. 施工基础信息校验 -----------*/
        // 构造施工信息查询参数
        LhAlgorithmScheduleResultDto queryDto = new LhAlgorithmScheduleResultDto();
        LhScheduleResult lhScheduleResult = new LhScheduleResult();
        lhScheduleResult.setEmbryoCode(embryoCode);
        lhScheduleResult.setBomVersion(bomDataVersion);
        queryDto.setLhScheduleResult(lhScheduleResult);

        // 查询施工信息并按胎胚+Bom版本分组
        List<CxProductConstructionInfoDto> constructionInfos = commonQueryCacheService.queryEmbryoCodeInfo(Collections.singletonList(queryDto));
        if (CollectionUtils.isEmpty(constructionInfos)) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.construction.empty.exception"));
            log.error("[插单预检] 未查询到施工基础信息，胎胚:{} Bom版本:{}", embryoCode, bomDataVersion);
            return ValidateResult.error(errorMsg.toString());
        }

        // 构建分组键并验证是否存在匹配施工信息
        String constructionKey = GenerageMapKeyUtils.createMapKey(embryoCode, bomDataVersion);
        Map<String, List<CxProductConstructionInfoDto>> constructionGroup = constructionInfos.stream()
                .collect(Collectors.groupingBy(c -> c.getEmbryoCode() + c.getEmbryoVersion()));
        if (!constructionGroup.containsKey(constructionKey)) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.construction.embryo.not.exist"));
            log.error("[插单预检] 无匹配施工信息，胎胚:{} Bom版本:{}", embryoCode, bomDataVersion);
            return ValidateResult.error(errorMsg.toString());
        }

        // 获取施工详情并校验关键参数
        CxProductConstructionInfoDto constructionInfo = constructionGroup.get(constructionKey).get(0);
        if (constructionInfo.getDimension() == null) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.construction.dimension.empty.error"));
            log.error("[插单预检] 施工信息寸口为空");
            return ValidateResult.error(errorMsg.toString());
        }

        /*----------- 4. 计划量校验 -----------*/
        int totalPlanQty = insertTotalPlanQty(cxScheduleResult);
        if (totalPlanQty <= 0) {
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.planQty.limit.error"));
            log.error("[插单预检] 计划总量不合法: {}", totalPlanQty);
            return ValidateResult.error(errorMsg.toString());
        }

        /*----------- 5. 月度计划余量校验 -----------*/
        StringBuilder tipMsg = new StringBuilder();
        List<CxEmbryoMonthPlanSurplus> monthSurplusList = commonQueryCacheService.getMonthRemainQtyList(scheduleDate);
        CxEmbryoMonthPlanSurplus monthSurplus = findMonthSurplus(monthSurplusList, cxScheduleResult);

        if (monthSurplus != null) {
            // 计算已排程总量
            int scheduledQty = calculateScheduledQty(cxScheduleResult);
            int remainQty = monthSurplus.getMonthRemainQty();

            // 余量不足提示
            if (totalPlanQty + scheduledQty > remainQty) {
                String msg = StringUtils.format(I18nUtil.getMessage("cx.engine.remain.out.tip"),
                        remainQty, scheduledQty, totalPlanQty, (totalPlanQty + scheduledQty - remainQty));
                tipMsg.append(msg);
                log.warn("[插单预检] 月度余量不足，当前余量:{} 已排量:{} 本次计划:{}", remainQty, scheduledQty, totalPlanQty);
            }
        }

        /*----------- 6. 机台作业限制校验 -----------*/
        // 查询机台限制配置
        List<ProductMoldingLimit> limits = productMoldingLimitMapper.selectList(new QueryWrapper<ProductMoldingLimit>()
                .eq("embryo_code", embryoCode)
                .eq("sap_code", cxScheduleResult.getSapCode()));

        // 分离限制机台和禁用机台
        Set<String> limitMachines = new HashSet<>();
        Set<String> forbiddenMachines = new HashSet<>();
        limits.forEach(limit -> {
            if (CxEngineConstants.SPECIFY_JOB_TYPE_YES.equals(limit.getJobType())) {
                limitMachines.add(limit.getMachineCode());
            } else if (CxEngineConstants.SPECIFY_JOB_TYPE_NO.equals(limit.getJobType())) {
                forbiddenMachines.add(limit.getMachineCode());
            }
        });

        // 机台限制检查
        if (!limitMachines.isEmpty() && !limitMachines.contains(machineCode)) {
            String msg = StringUtils.format(I18nUtil.getMessage("cx.engine.change.spefifyMachine.onlyUse.tip"), limitMachines);
            tipMsg.append(msg);
            log.warn("[插单预检] 机台{}不在定点机台列表{}中", machineCode, limitMachines);
        }
        if (forbiddenMachines.contains(machineCode)) {
            tipMsg.append(I18nUtil.getMessage("cx.engine.change.spefifyMachine.canNotUse.tip"));
            log.warn("[插单预检] 机台{}在禁用列表中", machineCode);
        }

        /*----------- 7. 设备特性匹配校验 -----------*/
        MdmMoldingMachineCls machineCls = machineInfo.getMoldingMachineCls();
        Double requiredDimension = constructionInfo.getDimension();
        String requiredDrum = constructionInfo.getMoldingDrum();

        // 模具方法校验
        MdmProductConstructionVO productConstruction = getProductConstruction(cxScheduleResult.getSapCode()+"_"+cxScheduleResult.getSpecCode());
        String requiredMouldMethod = productConstruction.getMouldMethod();
        if (machineCls == null || !String.valueOf(machineCls.getMouldMethod()).equals(requiredMouldMethod)) {
            String msg = StringUtils.format(I18nUtil.getMessage("cx.engine.change.mouldMethod.no.tip"),
                    machineCode, machineCls != null ? machineCls.getMouldMethod() : "null", requiredMouldMethod);
            tipMsg.append(msg);
            log.warn("[插单预检] 模具方法不匹配，机台方法:{} 需求方法:{}",
                    machineCls != null ? machineCls.getMouldMethod() : "null", requiredMouldMethod);
        }

        // 成型鼓校验
//        if (!machineInfo.getMoldingDrum().equals(requiredDrum)) {
//            String msg = StringUtils.format(I18nUtil.getMessage("cx.engine.change.moldingDrum.no.tip"),
//                    machineCode, machineInfo.getMoldingDrum(), requiredDrum);
//            tipMsg.append(msg);
//            log.warn("[插单预检] 成型鼓不匹配，机台鼓型:{} 需求鼓型:{}", machineInfo.getMoldingDrum(), requiredDrum);
//        }

        // 尺寸范围校验
        BigDecimal machineMinSize = machineInfo.getMinSize();
        BigDecimal machineMaxSize = machineInfo.getMaxSize();
        if (machineMinSize.compareTo(BigDecimal.valueOf(requiredDimension)) > 0) {
            String msg = StringUtils.format(I18nUtil.getMessage("cx.engine.change.proSizeMin.no.tip"),
                    machineCode, machineMinSize, requiredDimension);
            tipMsg.append(msg);
            log.warn("[插单预检] 尺寸小于机台最小值，机台最小值:{} 需求尺寸:{}", machineMinSize, requiredDimension);
        }
        if (machineMaxSize.compareTo(BigDecimal.valueOf(requiredDimension)) < 0) {
            String msg = StringUtils.format(I18nUtil.getMessage("cx.engine.change.proSizeMax.no.tip"),
                    machineCode, machineMaxSize, requiredDimension);
            tipMsg.append(msg);
            log.warn("[插单预检] 尺寸超过机台最大值，机台最大值:{} 需求尺寸:{}", machineMaxSize, requiredDimension);
        }

        //提示各个有计划的班次可安排最大的计划量
        validateClassShiftPlanQty(machineInfo, cxScheduleResult, tipMsg, constructionInfo);

        /*----------- 8. 返回最终结果 -----------*/
        if (tipMsg.length() > 0) {
            log.info("[插单预检] 检查通过，存在提示信息: {}", tipMsg);
            return ValidateResult.success(tipMsg.toString());
        } else {
            log.info("[插单预检] 检查通过，无异常");
            return ValidateResult.success();
        }
    }

    private void validateClassShiftPlanQty(CxMachineInfoVo machineInfo, CxScheduleResult cxScheduleResult, StringBuilder tipMsg, CxProductConstructionInfoDto constructionInfo) {
        logger.debug("开始验证班次计划量，机台编码：{}，排程日期：{}", machineInfo.getMoldingMachineCode(), cxScheduleResult.getScheduleDate());

        // 1. 查询当前机台当天的所有成型排程任务
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("schedule_date", cxScheduleResult.getScheduleDate());
        queryWrapper.eq("cx_machine_code", cxScheduleResult.getCxMachineCode());
        List<CxScheduleResult> machineTaskList = cxScheduleResultEntityMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(machineTaskList)) {
            logger.info("机台{}在{}当天无排程任务，跳过校验", cxScheduleResult.getCxMachineCode(), cxScheduleResult.getScheduleDate());
            machineTaskList = new ArrayList<>();
        }
        logger.debug("查询到机台{}当天排程任务数：{}", cxScheduleResult.getCxMachineCode(), machineTaskList.size());

        // 2. 按班次设置计划量提示语（从一班开始处理）
        setClassShiftTip(machineInfo, cxScheduleResult, machineTaskList, tipMsg, constructionInfo);
    }

    private void setClassShiftTip(CxMachineInfoVo machineInfo, CxScheduleResult cxScheduleResult,
                                  List<CxScheduleResult> machineTaskList, StringBuilder tipMsg,
                                  CxProductConstructionInfoDto constructionInfo) {
        logger.debug("初始化班次配置参数");

        // 1. 加载成型参数配置
        Map<String, CxParams> cxParamsMap = new HashMap<>();
        commonQueryCacheService.queryCxParams().forEach(item -> cxParamsMap.put(item.getParamCode(), item));
        logger.debug("加载成型参数配置项数：{}", cxParamsMap.size());

        // 2. 初始化班次配置（班制、开始时间）
        CxShiftConfig cxShiftConfig = CxShiftConfig.of(commonCacheService.getCxShiftSystem(cxParamsMap));
        int startHour = commonCacheService.getCxShiftSystemStartHour(cxParamsMap);
        cxShiftConfig.setStartTime(cxScheduleResult.getScheduleDate(), startHour);
        logger.info("班次配置初始化完成，班制类型：{}，开始时间基准小时：{}", cxShiftConfig.getShiftCount(), startHour);

        // 3. 遍历所有有效班次进行处理
        List<Integer> validClasses = cxShiftConfig.getValidClasses();
        logger.debug("开始处理有效班次，班次列表：{}", validClasses);
        for (int classCode : validClasses) {
            logger.debug("正在处理班次：{}", classCode);
            setShiftPlanQtyTip(machineInfo, cxScheduleResult, machineTaskList, classCode, tipMsg, cxShiftConfig, constructionInfo);
        }
    }

    private void setShiftPlanQtyTip(CxMachineInfoVo machineInfo, CxScheduleResult cxScheduleResult, List<CxScheduleResult> machineTaskList, int classCode, StringBuilder tipMsg, CxShiftConfig cxShiftConfig, CxProductConstructionInfoDto constructionInfo) {
        // 0. 初始化班次相关字段键名
        final String planQtyKey = String.format("class%dPlanQty", classCode);
        final String machineQuotaKey = String.format("class%dMachineQuota", classCode);
        final String sortKey = String.format("class%dSort", classCode);
        final String classStartTimeKey = String.format("class%dStartTime", classCode);
        final String classEndTimeKey = String.format("class%dEndTime", classCode);
        // 1. 获取当前班次的最大排序任务
        Optional<CxScheduleResult> lastTaskOpt = machineTaskList.stream()
                .filter(task -> task.getFieldValueByFieldName(planQtyKey) != null && (int) task.getFieldValueByFieldName(planQtyKey) > 0)
                .filter(task -> task.getFieldValueByFieldName(sortKey) != null)
                .max(Comparator.comparing(task -> (int) task.getFieldValueByFieldName(sortKey)));

        if (lastTaskOpt.isPresent()) {
            // 获取最后一个规格的生产结束时间
            CxScheduleResult lastTask = lastTaskOpt.orElseGet(CxScheduleResult::new);
            Date lastEndTime = convertToDate(lastTask.getFieldValueByFieldName(classEndTimeKey));
            logger.debug("班次{}最后一个任务[{}]结束时间：{}", classCode, lastTask.getId(), lastEndTime);
            // 3. 获取机台班次定额参数
            int shiftQuota = (int) lastTask.getFieldValueByFieldName(machineQuotaKey);
            logger.debug("班次{}机台定额：{}", classCode, shiftQuota);


            // 4. 计算换工装时间（小时转秒）
            double changeSpecTimeSec = calculateChangeSpecTime(lastTask, constructionInfo) * 3600;
            logger.debug("换工装时间计算完成，耗时：{}秒", changeSpecTimeSec);


            // 5. 计算本班次剩余可用时间
            LocalDateTime availableStart = LocalDateTime.ofInstant(lastEndTime.toInstant(), ZoneId.systemDefault())
                    .plusSeconds((long) changeSpecTimeSec);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime shiftEnd = LocalDateTime.parse(cxShiftConfig.getShiftTimeByString("class" + classCode).get("endTime"), formatter);

            //将开始时间结束时间赋值给任务
            cxScheduleResult.setFieldValueByFieldName(classStartTimeKey, availableStart);
            cxScheduleResult.setFieldValueByFieldName(classEndTimeKey, shiftEnd);
            cxScheduleResult.setFieldValueByFieldName(machineQuotaKey, shiftQuota);
            cxScheduleResult.setCxMachineQty(lastTask.getCxMachineQty());

            cxScheduleResult.setFieldValueByFieldName(sortKey, (int) lastTask.getFieldValueByFieldName(sortKey) + 1);
            Duration availableDuration = Duration.between(availableStart, shiftEnd);
            long availableSeconds = availableDuration.getSeconds();

            logger.debug("班次{}可用生产时间区间：{} - {} (剩余{}秒)",
                    classCode, availableStart, shiftEnd, availableSeconds);


            // 计算理论产量 = 班次定额  / 班次时长  * 可用时长
            BigDecimal totalShiftSec = BigDecimal.valueOf(cxShiftConfig.getShiftDuration() * 3600L);
            BigDecimal availableSec = BigDecimal.valueOf(availableSeconds);
            BigDecimal theoreticalQty = availableSec.multiply(BigDecimal.valueOf(shiftQuota))
                    .divide(totalShiftSec, 0, RoundingMode.DOWN);
            int currentPlanQty = (int) cxScheduleResult.getFieldValueByFieldName(planQtyKey);
            logger.debug("理论可排产量：{}，当前计划量：{}", theoreticalQty, currentPlanQty);

            // 7. 校验计划量并生成提示
            if (currentPlanQty > shiftQuota) {
                // 超过班次总定额
                tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + classCode + "PlanQty.tip"), shiftQuota));
                logger.warn("计划量超限：班次{}计划量{} > 定额{}", classCode, currentPlanQty, shiftQuota);
            } else if (availableSeconds <= 0 || availableSeconds <= changeSpecTimeSec) {
                // 无可用时间
                tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + classCode + "PlanQty.tip"), 0));
                logger.warn("无可用时间：班次{}剩余时间{}秒，换装耗时{}秒", classCode, availableSeconds, changeSpecTimeSec);
            } else if (theoreticalQty.compareTo(BigDecimal.valueOf(currentPlanQty)) < 0) {
                // 超过理论产量
                tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + classCode + "PlanQty.tip"), theoreticalQty));
                logger.warn("计划量超限：班次{}计划量{} > 理论产量{}", classCode, currentPlanQty, theoreticalQty);
            }
        } else {
            // 空机台依据寸口获取定额
            LhAlgorithmScheduleResultDto task = new LhAlgorithmScheduleResultDto();
            int shiftQuota = cxSchedulingAlgorithmResultService.getShiftQuota(machineInfo, classCode, constructionInfo, task);
            int currentPlanQty = (int) cxScheduleResult.getFieldValueByFieldName(planQtyKey);


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime availableStart = LocalDateTime.parse(cxShiftConfig.getShiftTimeByString("class" + classCode).get("startTime"), formatter);
            LocalDateTime shiftEnd = LocalDateTime.parse(cxShiftConfig.getShiftTimeByString("class" + classCode).get("endTime"), formatter);



            cxScheduleResult.setFieldValueByFieldName(classStartTimeKey, availableStart);
            cxScheduleResult.setFieldValueByFieldName(classEndTimeKey, shiftEnd);
            cxScheduleResult.setFieldValueByFieldName(machineQuotaKey, shiftQuota);
            cxScheduleResult.setCxMachineQty(shiftQuota);

            cxScheduleResult.setFieldValueByFieldName(sortKey, 1);
            // 空机台提示
            if (currentPlanQty > shiftQuota) {
                // 超过班次总定额
                tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + classCode + "PlanQty.tip"), shiftQuota));
                logger.warn("计划量超限：班次{}计划量{} > 定额{}", classCode, currentPlanQty, shiftQuota);
            }
        }
    }


    /**
     * 计算换工装时间（小时）
     */
    private double calculateChangeSpecTime(CxScheduleResult lastTask, CxProductConstructionInfoDto newConstruction) {
        LhAlgorithmScheduleResultDto queryDto = new LhAlgorithmScheduleResultDto();
        LhScheduleResult lhResult = new LhScheduleResult();
        lhResult.setEmbryoCode(lastTask.getEmbryoCode());
        lhResult.setBomVersion(lastTask.getBomDataVersion());
        queryDto.setLhScheduleResult(lhResult);

        List<CxProductConstructionInfoDto> constructions = commonQueryCacheService.queryEmbryoCodeInfo(Collections.singletonList(queryDto));
        CxProductConstructionInfoDto lastSpec = CollectionUtils.isEmpty(constructions) ? new CxProductConstructionInfoDto() : constructions.get(0);

        return cxSchedulingAlgorithmResultService.changeSpecTime(lastSpec, newConstruction, Integer.parseInt(lastTask.getCxMachineType()));
    }


    /**
     * 时间类型转换
     */
    private Date convertToDate(Object timeValue) {
        if (timeValue instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) timeValue).atZone(ZoneId.systemDefault()).toInstant());
        } else if (timeValue instanceof Date) {
            return (Date) timeValue;
        }
        throw new IllegalArgumentException("不支持的时间类型：" + timeValue.getClass());
    }

    /**
     * title：辅助方法：查找月度余量
     *
     * @param list   月度余量列表
     * @param result 插单排程结果
     * @return 月度余量
     */
    private CxEmbryoMonthPlanSurplus findMonthSurplus(List<CxEmbryoMonthPlanSurplus> list, CxScheduleResult result) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        return list.stream()
                .filter(item -> item.getMaterialCode().equals(result.getEmbryoCode())
                        && item.getBomDataVersion().equals(result.getBomDataVersion()))
                .findFirst()
                .orElse(null);
    }

    /**
     * title：插单辅助方法：计算已排程总量
     *
     * @param result 插单排程结果
     * @return 已排程总量
     */
    private int calculateScheduledQty(CxScheduleResult result) {
        List<CxScheduleResult> existResults = cxScheduleResultEntityMapper.selectList(new QueryWrapper<CxScheduleResult>()
                .eq("schedule_date", result.getScheduleDate())
                .eq("embryo_code", result.getEmbryoCode())
                .eq("bom_data_version", result.getBomDataVersion()));

        return existResults.stream()
                .mapToInt(r -> r.getClass1PlanQty() + r.getClass2PlanQty() + r.getClass3PlanQty()
                        + r.getClass4PlanQty() + r.getClass5PlanQty() + r.getClass6PlanQty())
                .sum();
    }

    /**
     * title：插单辅助方法：获取产品施工关系
     *
     * @param specCode 规格代码
     * @return 产品施工关系
     */
    private MdmProductConstructionVO getProductConstruction(String specCode) {
        List<MdmProductConstructionVO> constructions = mdmProductConstructionService.queryByFactoryCodeAndSpecCodes(
                SecurityUtils.getUserCurrentFactory(), Collections.singleton(specCode));
        if (CollectionUtils.isEmpty(constructions)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.constructionRelationshipNotFound"));
        }
        // 假设每个规格对应唯一施工关系
        return constructions.get(0);
    }

    /**
     * title：插单辅助方法：获取插单的计划总任务量
     *
     * @param cxScheduleResult 排程计划
     * @return 计划总量
     */
    public Integer insertTotalPlanQty(CxScheduleResult cxScheduleResult) {
        Integer totalPlanQty = 0;
        if (cxScheduleResult.getClass1PlanQty() == null) {
            cxScheduleResult.setClass1PlanQty(0);
        }
        totalPlanQty += cxScheduleResult.getClass1PlanQty();
        if (cxScheduleResult.getClass2PlanQty() == null) {
            cxScheduleResult.setClass2PlanQty(0);
        }
        totalPlanQty += cxScheduleResult.getClass2PlanQty();
        if (cxScheduleResult.getClass3PlanQty() == null) {
            cxScheduleResult.setClass3PlanQty(0);
        }
        totalPlanQty += cxScheduleResult.getClass3PlanQty();
        if (cxScheduleResult.getClass4PlanQty() == null) {
            cxScheduleResult.setClass4PlanQty(0);
        }
        totalPlanQty += cxScheduleResult.getClass4PlanQty();
        if (cxScheduleResult.getClass5PlanQty() == null) {
            cxScheduleResult.setClass5PlanQty(0);
        }
        totalPlanQty += cxScheduleResult.getClass5PlanQty();
        if (cxScheduleResult.getClass6PlanQty() == null) {
            cxScheduleResult.setClass6PlanQty(0);
        }
        totalPlanQty += cxScheduleResult.getClass6PlanQty();
        return totalPlanQty;
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<CxScheduleResult> scheduleResults, CxScheduleResult newSchedule) {
        List<CxScheduleResult> scheduleResultList = this.checkScheduleResultUnique(newSchedule);
        CxDispatcherLog log = new CxDispatcherLog();
        //基础信息赋值
        //log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        //排程日期
        log.setScheduleDate(newSchedule.getScheduleDate());
        //sap品号
        log.setSapCode(newSchedule.getSpecCode());
        //胎胚代码
        log.setEmbryoCode(newSchedule.getEmbryoCode());
        //胎胚版本
        log.setEmbryoVersion(newSchedule.getBomDataVersion());
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (com.alibaba.nacos.common.utils.CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<CxScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(CxScheduleResult::getCreateTime));
            if (max.isPresent()) {
                CxScheduleResult scheduleResult = max.get();
                log.setBeforeCxMachineCode(scheduleResult.getCxMachineCode());
                log.setBeforeLhMachineCode(scheduleResult.getLhMachineCode());
                log.setBeforeClass1Plan(scheduleResult.getClass1PlanQty());
                log.setBeforeClass2Plan(scheduleResult.getClass2PlanQty());
                log.setBeforeClass3Plan(scheduleResult.getClass3PlanQty());
                log.setBeforeClass4Plan(scheduleResult.getClass4PlanQty());
                log.setBeforeClass5Plan(scheduleResult.getClass5PlanQty());
            }
        }
        //操作后的信息赋值
        log.setAfterLhMachineCode(newSchedule.getLhMachineCode());
        log.setAfterCxMachineCode(newSchedule.getCxMachineCode());
        log.setAfterClass1Plan(newSchedule.getClass1PlanQty());
        log.setAfterClass2Plan(newSchedule.getClass2PlanQty());
        log.setAfterClass3Plan(newSchedule.getClass3PlanQty());
        log.setAfterClass4Plan(newSchedule.getClass4PlanQty());
        log.setAfterClass5Plan(newSchedule.getClass5PlanQty());
        log.setIsDelete(0);
        /* 调用插入日志方法 **/
        cxDispatcherLogService.insertCxDispatcherLog(log);
    }

    @Override
    public MdmProductConstructionVO getBomData(CxScheduleResult cxScheduleResult) {
        return this.getProductConstruction(cxScheduleResult.getSapCode()+"_"+cxScheduleResult.getSpecCode());
    }

    @Override
    public Long isPublishByIds(Long[] ids) {
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("IS_RELEASE", ApsConstant.NO_RELEASE);
        queryWrapper.in("id", ids);
        return cxScheduleResultEntityMapper.selectCount(queryWrapper);
    }

    @Override
    public String removeResultCheck(Long[] ids, List<CxScheduleResult> finalList) {
        if(StringUtils.isEmpty(ids)){
            throw new IllegalArgumentException(I18nUtil.getMessage("cx.schedule.result.remove.error.params"));
        }
        List<CxScheduleResult> removeList=cxScheduleResultEntityMapper.selectBatchIds(Arrays.asList(ids));
        if(StringUtils.isEmpty(removeList)){
            throw new IllegalArgumentException(I18nUtil.getMessage("cx.schedule.result.remove.error.params"));
        }
        StringBuilder errorLog=new StringBuilder();

        //遍历进行校验提醒
        for(CxScheduleResult cxScheduleResult:removeList){
            //发布状态
            String isRelease=cxScheduleResult.getIsRelease();
            if(CxEngineConstants.IS_PUBLISH_YES.equals(isRelease)&&StringUtils.isNotEmpty(isRelease)){
                errorLog.append(StringUtils.format(I18nUtil.getMessage("cx.schedule.result.remove.isPublish.yes"),cxScheduleResult.getOrderNo()));
                continue;
            }
            finalList.add(cxScheduleResult);
        }
        return errorLog.toString();
    }

    @Override
    public int removeCxSecheduleResultByList(Long[] ids, List<CxScheduleResult> removeList) {
        //删除成型排程结果表
        return cxScheduleResultEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 更改发布状态
     *
     * @param entity 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(CxScheduleResult entity) {
        //更新半部件删除字段
        LambdaUpdateWrapper<CxScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResult::getScheduleDate, entity.getScheduleDate())
                .set(CxScheduleResult::getIsRelease, entity.getIsRelease());
        return cxScheduleResultEntityMapper.update(null, updateWrapper);
    }


    /**
     * 更新指定相关数据记录的发布状态
     *
     * @param ids         排程ID列表
     * @param status      更新的状态
     */
    @Override
    public int updateRelaseStatus(long[] ids, String status) {
        // 将 long[] 转换为 Long[]
        Long[] idArray = Arrays.stream(ids).boxed().toArray(Long[]::new);
        // 调用 batchUpdate 方法
        return cxScheduleResultEntityMapper.batchUpdate(idArray, status);
    }


    /**
     * 验证选中记录的施工信息
     *
     * @param ids
     * @return
     */
    @Override
    public String validateConstructionByIds(Long[] ids) {
        //Joran 2022-03-16 从成型参数中获取开关如果是Y则进行验证，否则直接返回成功start
        Map<String, String> cxParams = commonCacheService.loadCxParamsMap();
        String switchConfig = CxEngineConstants.NO;
        if (cxParams.containsKey(CxParamCodeConstants.VALIDATE_CONSTRUCTION_SWITCH)) {
            switchConfig = cxParams.get(CxParamCodeConstants.VALIDATE_CONSTRUCTION_SWITCH);
        }
        //Joran 2022-03-16 从成型参数中获取开关如果是Y则进行验证，否则直接返回成功end
        StringBuilder msg = new StringBuilder();
        //用于存储已经校验过的数据防止重复校验
        Set<String> existsKey = new TreeSet<>();
        if (CxEngineConstants.YES.equals(switchConfig)) {
            List<CxScheduleResult> validateResultList = cxScheduleResultEntityMapper.selectRemoveList(ids);
            if (StringUtils.isNotEmpty(validateResultList)) {
                for (CxScheduleResult cxScheduleResult : validateResultList) {
                    String embryoCode = cxScheduleResult.getEmbryoCode();
                    String bomDataVersion = cxScheduleResult.getBomDataVersion();
                    String key = GenerageMapKeyUtils.createMapKey(embryoCode, bomDataVersion);
                    if (existsKey.contains(key)) {
                        log.debug("当前键值：" + key + ",已经验证过。不重复校验");
                        continue;
                    }
                    String errorMsg = this.checkConstruction(embryoCode, bomDataVersion);
                    if (StringUtils.isNotEmpty(errorMsg)) {
                        if (StringUtils.isEmpty(msg)) {
                            msg.append(errorMsg);
                        } else {
                            msg.append("<br/>").append(errorMsg);
                        }
                    }
                    existsKey.add(key);
                }
            }
        }

        return msg.toString();
    }

    public String checkConstruction(String embryoCode, String bomVersion) {
        if (StringUtils.isEmpty(embryoCode) || StringUtils.isEmpty(bomVersion)) {
            String msgTemplate = I18nUtil.getMessage("ui.data.column.construction.check.codeAndVersion.notExists");
            return StringUtils.format(msgTemplate, embryoCode, bomVersion);
        }
        List<CxProductConstructionInfo> constructionList = cxProductConstructionInfoMapper
                .getCheckConstructionInfo(embryoCode, bomVersion);
        CxProductConstructionInfo construction = CollectionUtils.isEmpty(constructionList) ? null : constructionList.get(0);
        if (construction == null) {
            String msgTemplate = I18nUtil.getMessage("ui.data.column.construction.check.codeAndVersion.notExists");
            return StringUtils.format(msgTemplate, embryoCode, bomVersion);
        }
        // 讲施工栏位缓存到map中
        this.initConstructionFieldsMap();
        CxCheckConstructionResultDto result = this.checkProductConstruction(construction);
        if (StringUtils.isNotEmpty(result.getErrorMessage())) {
            String msgTemplate = I18nUtil.getMessage("ui.data.column.construction.check.codeAndVersion.error");
            return StringUtils.format(msgTemplate, embryoCode, bomVersion, result.getErrorMessage());
        }
        return "";
    }

    /**
     * 检查施工信息
     *
     * @param construction
     * @return
     */
    private CxCheckConstructionResultDto checkProductConstruction(CxProductConstructionInfo construction) {
        CxCheckConstructionResultDto result = new CxCheckConstructionResultDto();
        result.setEmbryoCode(construction.getEmbryoCode());
        result.setEmbryoVersion(construction.getEmbryoVersion());
        result.setSapCode(construction.getSapCode());
        // 异常信息
        StringBuffer errorMessage = new StringBuffer();
        // 先判断施工是否存在
        if (construction.getId() == null) {
            // 如果施工不存在，则直接反馈异常信息
            errorMessage.append(I18nUtil.getMessage("ui.data.column.construction.check.construction.notExists"));
        } else {
            // 判断以下所有栏位是否有缺失
            this.checkField("sapCode", construction, errorMessage);
            this.checkField("embryoCode", construction, errorMessage);
            this.checkField("specDesc", construction, errorMessage);
            this.checkField("noseWidth", construction, errorMessage);
            if (construction.getEmbryoCode().startsWith("E")) {
                // 如果胎胚是二次发，则扣圈盘直径必须不为空
                this.checkField("flipDiscDiameter", construction, errorMessage);
            }
            this.checkField("treadCode", construction, errorMessage);
            this.checkField("treadSap", construction, errorMessage);
            this.checkField("treadVersion", construction, errorMessage);
            this.checkField("treadRubberCategory", construction, errorMessage);
            this.checkField("treadMouthPlate", construction, errorMessage);
            this.checkField("treadShoulderLength", construction, errorMessage);
            this.checkField("sidewallCode", construction, errorMessage);
            this.checkField("sidewallSap", construction, errorMessage);
            this.checkField("sidewallVersion", construction, errorMessage);
            this.checkField("sidewallRubber", construction, errorMessage);
            this.checkField("sidewallMouthPlate", construction, errorMessage);
            this.checkField("sidewallLength", construction, errorMessage);
            this.checkField("insideCode", construction, errorMessage);
            this.checkField("insideSap", construction, errorMessage);
            this.checkField("insideVersion", construction, errorMessage);
            this.checkField("insideRubber", construction, errorMessage);
            this.checkField("tireRingCode", construction, errorMessage);
            this.checkField("tireRingSap", construction, errorMessage);
            this.checkField("tireRingVersion", construction, errorMessage);
            this.checkField("apexCode", construction, errorMessage);
            this.checkField("hexagonRubberCode", construction, errorMessage);
            this.checkField("hexagonMouthPlate", construction, errorMessage);
            this.checkField("beadCode", construction, errorMessage);
            this.checkField("beadSap", construction, errorMessage);
            this.checkField("beadVersion", construction, errorMessage);
            this.checkField("beadType", construction, errorMessage);
            this.checkField("beadArrange", construction, errorMessage);
            this.checkField("fitDrumPerimeter", construction, errorMessage);
            this.checkField("beltCuttingAngle", construction, errorMessage);
            this.checkField("beltCode1", construction, errorMessage);
            this.checkField("beltSap1", construction, errorMessage);
            this.checkField("belt1Version", construction, errorMessage);
            this.checkField("beltCraft1", construction, errorMessage);
            this.checkField("beltCode2", construction, errorMessage);
            this.checkField("beltSap2", construction, errorMessage);
            this.checkField("belt2Version", construction, errorMessage);
            this.checkField("beltCraft2", construction, errorMessage);
            this.checkField("articleCrownSpec", construction, errorMessage);
            this.checkField("articleCrownSap", construction, errorMessage);
            this.checkField("articleCrownVersion", construction, errorMessage);
            this.checkField("tireFabricCode1", construction, errorMessage);
            this.checkField("tireFabricSap1", construction, errorMessage);
            this.checkField("tireFabric1Version", construction, errorMessage);
            this.checkField("tireFabricCraft1", construction, errorMessage);
            if (StringUtils.isNotEmpty(construction.getTireFabricCode2())) {
                // 如果有2号胎体布，则必须有2号胎胚布工艺
                this.checkField("tireFabricCraft2", construction, errorMessage);
            }
            if (StringUtils.isNotEmpty(construction.getTireFabricCode3())) {
                // 如果有3号胎体布，则必须有3号胎胚布工艺
                this.checkField("tireFabricCraft3", construction, errorMessage);
            }
            this.checkField("cordSpec", construction, errorMessage);
            this.checkField("cordSap", construction, errorMessage);
            this.checkField("cordVersion", construction, errorMessage);
            this.checkField("originalLineCode", construction, errorMessage);
            this.checkField("delFlag", construction, errorMessage);
            this.checkField("dimension", construction, errorMessage);
            this.checkField("sectionWidth", construction, errorMessage);
            this.checkField("hexagonRubberDimension", construction, errorMessage);
            this.checkField("productionStage", construction, errorMessage);
            // 移除最后一个逗号
            if (errorMessage.length() > 0) {
                errorMessage.setLength(errorMessage.length() - 1);
            }
        }
        result.setErrorMessage(errorMessage.toString());
        return result;
    }

    /**
     * 检查施工栏位，本栏位有错的换需要将其拼接到错误信息中
     *
     * @param columnName   栏位名称
     * @param construction 施工信息
     * @param errorMessage 错误信息
     */
    private void checkField(String columnName, CxProductConstructionInfo construction, StringBuffer errorMessage) {
        // 错误栏位
        String errorFeild = null;
        // 取出缓存的栏位field
        Field field = constructionFieldsMap.get(columnName);
        if (field != null) {
            // 执行栏位的getter方法，获取到单位值
            Object val = ReflectUtils.invokeGetter(construction, field.getName());
            if (val == null) {
                // 如果值为空，则该栏位为错位u栏位
                Excel annotation = field.getAnnotation(Excel.class);
                if (annotation != null && StringUtils.isNotBlank(annotation.name())) {
                    errorFeild = I18nUtil.getMessage(annotation.name());
                }
            }
        }
        // 如果有错误栏位，则讲其拼接至错误信息中
        if (StringUtils.isNotEmpty(errorFeild)) {
            errorMessage.append(errorFeild).append("，");
        }
    }

    /**
     * 获取施工表
     *
     * @return
     */
    private synchronized void initConstructionFieldsMap() {
        if (constructionFieldsMap == null) {
            constructionFieldsMap = new ConcurrentHashMap<>(16);
            Field[] allFields = CxProductConstructionInfo.class.getDeclaredFields();
            for (int col = 0; col < allFields.length; col++) {
                Field field = allFields[col];
                Excel attr = field.getAnnotation(Excel.class);
                if (attr != null) {
                    constructionFieldsMap.put(field.getName(), field);
                }
            }
        }
    }

    /**
     * 验证列表中如果存在施工版本为空给出错误提示
     *
     * @param cxScheduleResultList
     * @return
     */
    @Override
    public String checkBomDataVersion(List<CxScheduleResult> cxScheduleResultList) {
        StringBuilder errorMsg = new StringBuilder();
        if (StringUtils.isNotEmpty(cxScheduleResultList)) {
            for (CxScheduleResult cxScheduleResult : cxScheduleResultList) {
                if (StringUtils.isEmpty(cxScheduleResult.getBomDataVersion())) {
                    errorMsg.append(StringUtils.format(I18nUtil.getMessage("cx.publish.bomDataVersion.empty.error"), cxScheduleResult.getSapCode(), cxScheduleResult.getEmbryoCode(), cxScheduleResult.getCxMachineName())).append("<br/>");
                    break;
                }
            }
        }
        return errorMsg.toString();
    }

    /**
     * 排程发布
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult publish(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode) {
        String language = I18nUtil.getLocaleFromRedis().getLanguage();

        //数据同步,发起通知
        cxScheduleResultEntityMapper.deployScheduleToMes(dataVersion, ids, factoryCode, companyCode, language);
        //保存发布记录，更新发布状态
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_CX);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        //Joran 2022-03-09记录发布对应的数据版本号
        record.setDataVersion(dataVersion);
        cxScheduleResultEntityMapper.insertPublishRecord(record);
        //Joran 2021-10-12 排程发布更新投产表状态，再进行排程更新
        schedulePublish(ids, ApsConstant.RELEASING);
        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
    }


    public AjaxResult parseCxScheduleResult2(CxScheduleResult cxScheduleResult) {
        // 获取传入日期和前一天的日期
        Date scheduleDate = cxScheduleResult.getScheduleDate();
        LocalDate localDate = scheduleDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate previousDate = localDate.minusDays(1);

        // 查询前一天数据并分组
        QueryWrapper<CxOnlineImport> lastQueryWrapper = new QueryWrapper<>();
        lastQueryWrapper.eq("rq", previousDate);
        lastQueryWrapper.isNotNull("st");
        List<CxOnlineImport> cxOnlineImportList2 = cxOnlineImportEntityMapper.selectList(lastQueryWrapper);
        // 填充缺失的机台和数量
        String currentMachineCode = "";
        int currentCnt = 0;
        for (CxOnlineImport item : cxOnlineImportList2) {
            if (StringUtils.isNotBlank(item.getJt())) {
                currentMachineCode = StringUtils.trimToEmpty(item.getJt());
                currentCnt = item.getCn() != null ? item.getCn() : 0;
            } else {
                item.setJt(currentMachineCode);
                item.setCn(currentCnt);
            }
        }

        Map<String, CxOnlineImport> previousDayDataMap = cxOnlineImportList2
                .stream()
                .collect(Collectors.toMap(
                        item -> StringUtils.trimToEmpty(item.getJt()) + "_" + StringUtils.trimToEmpty(item.getSt()),
                        item -> item,
                        // 如果有重复键，保留第一个
                        (existing, replacement) -> existing
                ));


        // 查询当天数据
        QueryWrapper<CxOnlineImport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("rq", cxScheduleResult.getScheduleDate());
        queryWrapper.isNotNull("st");
        List<CxOnlineImport> cxOnlineImportList = cxOnlineImportEntityMapper.selectList(queryWrapper)
                .stream()
                .sorted(Comparator.comparing(CxOnlineImport::getId))
                .collect(Collectors.toList());

        // 填充缺失的机台和数量
        currentMachineCode = "";
        currentCnt = 0;
        for (CxOnlineImport item : cxOnlineImportList) {
            if (StringUtils.isNotBlank(item.getJt())) {
                currentMachineCode = StringUtils.trimToEmpty(item.getJt());
                currentCnt = item.getCn() != null ? item.getCn() : 0;
            } else {
                item.setJt(currentMachineCode);
                item.setCn(currentCnt);
            }
        }

        // 生成计划
        for (CxOnlineImport cxOnlineImport : cxOnlineImportList) {
            CxScheduleResult scheduleResult = new CxScheduleResult();
            // 设置基本属性（去掉空格）
            scheduleResult.setScheduleDate(cxOnlineImport.getRq());
            scheduleResult.setCxMachineCode(StringUtils.trimToEmpty(cxOnlineImport.getJt()));
            scheduleResult.setFactoryCode("AH01");
            scheduleResult.setBomDataVersion("1");
            scheduleResult.setEmbryoCode(StringUtils.trimToEmpty(cxOnlineImport.getSt()));
            scheduleResult.setCxMachineQty(cxOnlineImport.getCn() != null ? cxOnlineImport.getCn() : 0);
            scheduleResult.setDelFlag(0);

            // 处理前一天的数据
            String key = StringUtils.trimToEmpty(cxOnlineImport.getJt()) + "_" +
                    StringUtils.trimToEmpty(cxOnlineImport.getSt());
            if (previousDayDataMap.containsKey(key)) {
                CxOnlineImport prevData = previousDayDataMap.get(key);
                scheduleResult.setClass3PlannedQty(prevData.getZbjh() != null ? prevData.getZbjh() : 0);
                scheduleResult.setClass1PlanQty(prevData.getZbjh() != null ? prevData.getZbjh() : 0);
                scheduleResult.setClass2PlanQty(prevData.getWbjh() != null ? prevData.getWbjh() : 0);

                // 检查结束标志
                boolean isFinished = (StringUtils.isNotBlank(prevData.getMiaoshi1()) &&
                        StringUtils.trimToEmpty(prevData.getMiaoshi1()).contains("结束")) ||
                        (StringUtils.isNotBlank(prevData.getWbfl()) &&
                                StringUtils.trimToEmpty(prevData.getWbfl()).contains("结束"));
                if (isFinished) {
                    scheduleResult.setRemark("结束");
                }
            } else {
                scheduleResult.setClass1PlanQty(0);
                scheduleResult.setClass2PlanQty(0);
            }

            // 设置当天数据
            scheduleResult.setClass3PlanQty(cxOnlineImport.getZbjh() != null ? cxOnlineImport.getZbjh() : 0);
            scheduleResult.setClass4PlanQty(cxOnlineImport.getWbjh() != null ? cxOnlineImport.getWbjh() : 0);
            scheduleResult.setClass5PlanQty(0);
            scheduleResult.setClass6PlanQty(0);

            // 检查当天结束标志
            boolean isTodayFinished = (StringUtils.isNotBlank(cxOnlineImport.getMiaoshi1()) &&
                    StringUtils.trimToEmpty(cxOnlineImport.getMiaoshi1()).contains("结束")) ||
                    (StringUtils.isNotBlank(cxOnlineImport.getWbfl()) &&
                            StringUtils.trimToEmpty(cxOnlineImport.getWbfl()).contains("结束"));
            if (isTodayFinished && StringUtils.isBlank(scheduleResult.getRemark())) {
                scheduleResult.setRemark("结束");
            }

            cxScheduleResultEntityMapper.insert(scheduleResult);
        }

        return AjaxResult.success("解析成功");
    }



    @Override
    public AjaxResult parseCxScheduleResult(CxScheduleResult cxScheduleResult) {
        // 获取传入日期和前一天的日期
        Date scheduleDate = cxScheduleResult.getScheduleDate();
        // 转换为 LocalDate
        LocalDate localDate = scheduleDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();


        QueryWrapper<CxOnlineImport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("rq",cxScheduleResult.getScheduleDate());
        queryWrapper.isNotNull("st");


        List<CxOnlineImport> cxOnlineImportList = cxOnlineImportEntityMapper.selectList(queryWrapper);
        cxOnlineImportList = cxOnlineImportList.stream().collect(Collectors.toList());

        //依据日序升序排列
        cxOnlineImportList.sort(new Comparator<CxOnlineImport>() {
            @Override
            public int compare(CxOnlineImport o1, CxOnlineImport o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });

        String machineCode = "";
        int cnt = 0;
        double ck = 0;

        for (CxOnlineImport cxOnlineImport : cxOnlineImportList) {
            if (StringUtils.isNotEmpty(cxOnlineImport.getJt()) && StringUtils.isNotEmpty(cxOnlineImport.getCkfw())){
                machineCode = cxOnlineImport.getJt();
                cnt = cxOnlineImport.getCn() == null ? 0 : cxOnlineImport.getCn();
                String str = cxOnlineImport.getCkfw();
                String[] parts = str.split("/");
                String firstNumber = parts[0];
                ck = firstNumber == null ? 0 : Double.parseDouble(firstNumber);
                cxOnlineImport.setCkfw(String.valueOf(ck));
            }else if (StringUtils.isEmpty(cxOnlineImport.getJt())){
                cxOnlineImport.setJt(machineCode);
                cxOnlineImport.setCn(cnt);
                cxOnlineImport.setCkfw(String.valueOf(ck));
            }
        }

        //先删除后插入
        QueryWrapper<CxScheduleResult> operation = new QueryWrapper<>();
        operation.eq("schedule_date",cxScheduleResult.getScheduleDate());
        cxScheduleResultEntityMapper.delete(operation);

        //删除库存
        QueryWrapper<CxStock> stockQueryWrapper = new QueryWrapper<>();
        stockQueryWrapper.eq("STOCK_DATE",cxScheduleResult.getScheduleDate());
        cxStockEntityMapper.delete(stockQueryWrapper);

        //删除月度剩余量
        QueryWrapper<CxEmbryoMonthPlanSurplus> cxEmbryoMonthPlanSurplusQueryWrapper = new QueryWrapper<>();
        cxEmbryoMonthPlanSurplusQueryWrapper.eq("year",localDate.getYear());
        cxEmbryoMonthPlanSurplusQueryWrapper.eq("month",localDate.getMonthValue());
        cxEmbryoMonthPlanSurplusEntityMapper.delete(cxEmbryoMonthPlanSurplusQueryWrapper);



        //1.生成指定日期的成型计划
        for (CxOnlineImport cxOnlineImport : cxOnlineImportList) {
            CxScheduleResult scheduleResult = new CxScheduleResult();
            scheduleResult.setScheduleDate(cxOnlineImport.getRq());
            //将时间格式化为yyyyMMdd
            // 不足3位前面补0
            String idStr = String.format("%03d", cxOnlineImport.getId());
            scheduleResult.setOrderNo("CXGD" + DateUtils.parseDateToStr("yyyyMMdd", scheduleResult.getScheduleDate()) + idStr);

            scheduleResult.setCxMachineCode(cxOnlineImport.getJt());
            scheduleResult.setFactoryCode("AH01");
            scheduleResult.setBomDataVersion("1");
            scheduleResult.setSapCode(cxOnlineImport.getSpec());
            scheduleResult.setSpecCode(cxOnlineImport.getSt());
            scheduleResult.setEmbryoCode(cxOnlineImport.getSt());
            scheduleResult.setCxMachineQty(cxOnlineImport.getCn());
            scheduleResult.setSpecDesc(cxOnlineImport.getSpec());
            scheduleResult.setLhMachineQty(Double.valueOf(cxOnlineImport.getZs() == null ? 0 : cxOnlineImport.getZs()));

            scheduleResult.setRemark("调整后的线上计划");
            scheduleResult.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_DOING);
            scheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleResult.setLhMachineQty((double) (cxOnlineImport.getZs() == null ? 1 : cxOnlineImport.getZs() * 2));
            scheduleResult.setTotalStock(cxOnlineImport.getBt());
            if (cxOnlineImport.getCxhj() != null) {
                scheduleResult.setProductNum((double) (cxOnlineImport.getCxhj() == null ? 0 : cxOnlineImport.getCxhj()));
            }

            // Parse and set R value
            Integer rValue = parseRValueFromSpec(cxOnlineImport.getSpec());
            if (rValue != null) {
                // Assuming you have this field
                scheduleResult.setSpecDimension(Double.valueOf(rValue));
            }

            //scheduleResult.setSpecDimension(Double.valueOf(cxOnlineImport.getCkfw()));

            scheduleResult.setClass1PlanQty(cxOnlineImport.getZbjh() != null ? cxOnlineImport.getZbjh() : 0);
            scheduleResult.setClass1FinishQty(cxOnlineImport.getZbwc()  != null ? cxOnlineImport.getZbwc() : 0);
            if (cxOnlineImport.getZbwc() != null && cxOnlineImport.getZbjh() != null && cxOnlineImport.getZbjh() != 0) {
                scheduleResult.setClass1FinishRate(cxOnlineImport.getZbwc() / cxOnlineImport.getZbjh());
            }
            scheduleResult.setClass1Sort(cxOnlineImport.getSuwei1());
            scheduleResult.setClass1Analysis(cxOnlineImport.getMiaoshi1());

            scheduleResult.setClass2PlanQty(cxOnlineImport.getWbjh()  != null ? cxOnlineImport.getWbjh() : 0);
            scheduleResult.setClass2ModifyQty(cxOnlineImport.getWbjh() != null ? cxOnlineImport.getWbjh() : 0);
            scheduleResult.setClass2FinishQty(cxOnlineImport.getWbwc());
            if (cxOnlineImport.getWbjh() != null && cxOnlineImport.getWbwc() != null && cxOnlineImport.getWbjh() != 0 ) {
                scheduleResult.setClass2FinishRate(cxOnlineImport.getWbwc() / cxOnlineImport.getWbjh());
            }
            scheduleResult.setClass2Sort(cxOnlineImport.getWbsw());
            scheduleResult.setClass2ModifySort(cxOnlineImport.getWbsw());
            scheduleResult.setClass2Analysis(cxOnlineImport.getWbfl());

            scheduleResult.setClass3PlanQty(0);
            scheduleResult.setClass4PlanQty(0);
            scheduleResult.setClass4Analysis("调整后,线上仅有一天计划,故计划为0");
            scheduleResult.setClass5PlanQty(0);
            scheduleResult.setClass5Analysis("调整后,线上仅有一天计划,故计划为0");
            scheduleResult.setClass6PlanQty(0);
            cxScheduleResultEntityMapper.insert(scheduleResult);

            /**
             * 生成库存
             */
            CxStock stock = new CxStock();
            stock.setStockNum(cxOnlineImport.getBt());
            stock.setScheduleUseStock(Long.valueOf(cxOnlineImport.getJb() == null ? 0:cxOnlineImport.getJb()));
            stock.setEmbryoCode(cxOnlineImport.getSt());
            stock.setStockDate(cxOnlineImport.getRq());
            cxStockEntityMapper.insert(stock);

            /**
             * 生成月度剩余量
             */

            CxEmbryoMonthPlanSurplus surplus = new CxEmbryoMonthPlanSurplus();
            if (cxOnlineImport.getWbjh() == null){
                cxOnlineImport.setWbjh(0);
            }
            if (cxOnlineImport.getZbjh() == null){
                cxOnlineImport.setZbjh(0);
            }
            if (cxOnlineImport.getSy() == null){
                cxOnlineImport.setSy(0);
            }
            surplus.setMonthRemainQty(cxOnlineImport.getSy() - cxOnlineImport.getZbjh() - cxOnlineImport.getWbjh());
            surplus.setFactoryCode("AH01");
            surplus.setMaterialCode(cxOnlineImport.getSt());
            surplus.setYear(String.valueOf(localDate.getYear()));
            surplus.setMonth(String.valueOf(localDate.getMonthValue()));
            surplus.setBomDataVersion("1");
            cxEmbryoMonthPlanSurplusEntityMapper.insert(surplus);
        }

        return AjaxResult.success("解析成功");
    }


    public List<CxOnlineImport> genXcScheduleResult(CxScheduleResult cxScheduleResult) {
        // 获取传入日期和前一天的日期
        Date scheduleDate = cxScheduleResult.getScheduleDate();

        QueryWrapper<CxOnlineImport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("rq", cxScheduleResult.getScheduleDate());
        List<CxOnlineImport> cxOnlineImportList = cxOnlineImportEntityMapper.selectList(queryWrapper);
        cxOnlineImportList = cxOnlineImportList.stream().collect(Collectors.toList());

        //依据日序升序排列
        cxOnlineImportList.sort(new Comparator<CxOnlineImport>() {
            @Override
            public int compare(CxOnlineImport o1, CxOnlineImport o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });

        String machineCode = "";
        int cnt = 0;
        for (CxOnlineImport cxOnlineImport : cxOnlineImportList) {
            if (StringUtils.isNotEmpty(cxOnlineImport.getJt())) {
                machineCode = cxOnlineImport.getJt();
                cnt = cxOnlineImport.getCn() == null ? 0 : cxOnlineImport.getCn();
            } else if (StringUtils.isEmpty(cxOnlineImport.getJt())) {
                cxOnlineImport.setJt(machineCode);
                cxOnlineImport.setCn(cnt);
            }
        }


        // 获取全部成型排程,按机台分组
        QueryWrapper<CxScheduleResult> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("schedule_date", cxScheduleResult.getScheduleDate());
        List<CxScheduleResult> cxScheduleResultList = cxScheduleResultEntityMapper.selectList(queryWrapper2);

        //按照机台+胎胚分组
        Map<String, List<CxScheduleResult>> cxScheduleResultMap = cxScheduleResultList.stream()
                .collect(Collectors.groupingBy(CxScheduleResult::getCxMachineCode));

        //按照机台+胎胚分组
        Map<String, List<CxScheduleResult>> cxScheduleResult2Map = cxScheduleResultList.stream()
                .collect(Collectors.groupingBy(item -> item.getCxMachineCode() + "_" + item.getEmbryoCode()));


        //按照机台+胎胚分组
        Map<String, List<CxOnlineImport>> CxOnlineImportMap = cxOnlineImportList.stream()
                .collect(Collectors.groupingBy(item -> item.getJt() + "_" + item.getSt()));

        //遍历填充
        String lastjt = null;
        Long lastId = null;
        List<CxOnlineImport> toAddList = new ArrayList<>();
        for (CxOnlineImport cxOnlineImport : cxOnlineImportList) {
            if (StringUtils.isNotEmpty(cxOnlineImport.getJt())) {
                if (StringUtils.isNotEmpty(lastjt) && !lastjt.equals(cxOnlineImport.getJt())) {
                    List<CxScheduleResult> lastList = cxScheduleResultMap.get(lastjt);
                    if (lastList != null) {
                        for (CxScheduleResult item : lastList) {
                            String key = item.getCxMachineCode() + "_" + item.getEmbryoCode();
                            if (!CxOnlineImportMap.containsKey(key)) {
                                CxOnlineImport insert = new CxOnlineImport();
                                insert.setJt(lastjt);
                                insert.setId(lastId);
                                insert.setSt(item.getEmbryoCode());
                                insert.setSpec("新♥转移规格");
                                insert.setSuwei1(item.getClass1Sort());
                                insert.setWbsw(item.getClass2Sort());
                                insert.setWbjh(item.getClass2PlanQty());
                                insert.setZbjh(item.getClass1PlanQty());
                                insert.setWbfl(item.getClass2Analysis());
                                insert.setMiaoshi1(item.getClass1Analysis());
                                // 先收集，不直接修改原列表
                                toAddList.add(insert);
                            }
                        }
                    }
                }
            }

            lastjt = cxOnlineImport.getJt();
            lastId = cxOnlineImport.getId();

            String key = cxOnlineImport.getJt() + "_" + cxOnlineImport.getSt();
            if (cxScheduleResult2Map.containsKey(key)) {
                List<CxScheduleResult> cxScheduleResultList2 = cxScheduleResult2Map.get(key);
                if (cxScheduleResultList2 != null) {
                    CxScheduleResult item = cxScheduleResultList2.get(0);
                    cxOnlineImport.setSuwei1(item.getClass1Sort());
                    cxOnlineImport.setWbsw(item.getClass2Sort());
                    cxOnlineImport.setWbjh(item.getClass2PlanQty());
                    cxOnlineImport.setZbjh(item.getClass1PlanQty());
                    cxOnlineImport.setWbfl(item.getClass2Analysis());
                    cxOnlineImport.setMiaoshi1(item.getClass1Analysis());
                    item.setIsDelete(1);
                }else {
                    cxOnlineImport.setSuwei1(null);
                    cxOnlineImport.setWbsw(null);
                    cxOnlineImport.setWbjh(null);
                    cxOnlineImport.setZbjh(null);
                    cxOnlineImport.setWbfl(null);
                    cxOnlineImport.setMiaoshi1(null);
                }
            }else {
                cxOnlineImport.setSuwei1(null);
                cxOnlineImport.setWbsw(null);
                cxOnlineImport.setWbjh(null);
                cxOnlineImport.setZbjh(null);
                cxOnlineImport.setWbfl(null);
                cxOnlineImport.setMiaoshi1(null);
            }
        }

        // 最后统一添加
        cxOnlineImportList.addAll(toAddList);

        //依据日序升序排列
        cxOnlineImportList.sort(new Comparator<CxOnlineImport>() {
            @Override
            public int compare(CxOnlineImport o1, CxOnlineImport o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });

        return cxOnlineImportList;
    }

    /**
     * 排程发布后更新状态及投产状态表状态更新
     *
     * @param ids    要发布的ID
     * @param status 发布状态
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int schedulePublish(long[] ids, String status) {
        // 将 long[] 转换为 Long[]
        Long[] idArray = Arrays.stream(ids).boxed().toArray(Long[]::new);
        // 调用 batchUpdate 方法
        return cxScheduleResultEntityMapper.batchUpdate(idArray, status);
    }


    private Integer parseRValueFromSpec(String spec) {
        if (StringUtils.isEmpty(spec)) {
            return null;
        }

        // Pattern to match R followed by exactly 2 digits
        Pattern pattern = Pattern.compile("R(\\d{2})");
        Matcher matcher = pattern.matcher(spec);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.error("Failed to parse R value from spec: " + spec, e);
            }
        }
        return null;
    }

    /**
     * 查询成型机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @Override
    public AjaxResult selectMachineGantt(CxGanttVo queryVO) {
        List<CxGanttVo> newGanteList = new ArrayList<>();
        List<CxGanttVo> cxScheduleResultList = cxScheduleResultEntityMapper.getCxGanttData(queryVO);
        if (CollectionUtils.isNotEmpty(cxScheduleResultList)) {
            for (CxGanttVo ganttVo : cxScheduleResultList) {
                //构造开始日、结束日、开始时刻、结束时刻、起点位置、时差;
                String scheduleDay = DateUtils.getDay(ganttVo.getScheduleDate()) + "";
                String startDay = DateUtils.getDay(ganttVo.getStartDate()) + "";
                String endDay = DateUtils.getDay(ganttVo.getEndDate()) + "";
                int startHours = DateUtils.getHour(ganttVo.getStartDate());
                int endHours = DateUtils.getHour(ganttVo.getEndDate());
                int dayInterval = DateUtils.getDayInterval(ganttVo.getEndDate(), ganttVo.getStartDate());
                int dayInterval2 = DateUtils.getDayInterval(ganttVo.getScheduleDate(), ganttVo.getStartDate());

                //计算以下三个值，用户画甘特图
                //算起点位置：后端给24小时制的起始时刻
                //算长条宽度：小时差*25：(endHour-startHour+1)*25，后端给时差;
                //算margin-left宽度：固定值*天数，不用后端给

                if (dayInterval2 > 0) {
                    //起始日期在排程日期前
                    ganttVo.setHourStart(startHours);
                } else if (dayInterval2 == 0) {
                    //起始日期就是排程日期
                    ganttVo.setHourStart(startHours + 24);
                } else {
                    //起始日期在排程日期后
                    ganttVo.setHourStart(startHours + 48);
                }

                //跨天存在前一天数据
                if (!startDay.equals(endDay) && scheduleDay.equals(endDay)) {
                    ganttVo.setHourInterval(24 - startHours + endHours);
                    //跨多天
                    if (dayInterval > 1) {
                        ganttVo.setHourInterval(24 - startHours + 24 * (dayInterval - 1) + endHours);
                    }
                } else if (!startDay.equals(endDay)) {
                    ganttVo.setHourInterval(24 - startHours + endHours);
                    //跨多天
                    if (dayInterval > 1) {
                        ganttVo.setHourInterval(24 - startHours + 24 * (dayInterval - 1) + endHours);
                    }
                } else {
                    ganttVo.setHourInterval(endHours - startHours);
                }

                ganttVo.setStartDay(startDay);
                ganttVo.setEndDay(endDay);
                ganttVo.setStartHour(startHours + "");
                ganttVo.setEndHour(endHours + "");
                newGanteList.add(ganttVo);
            }
        }
        return AjaxResult.success(newGanteList);
    }

    /**
     * 成型触发反向修改硫化计划
     *
     * @param cxScheduleResult 查询参数
     * @return 结果
     */
    @Override
    public AjaxResult updateLhScheduleResult(CxScheduleResult cxScheduleResult) {
        StringBuilder msg = new StringBuilder();
        //1.根据id查成型排查
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        if (cxScheduleResult.getIds() != null) {
            queryWrapper.in("id", cxScheduleResult.getIds());
        }
        List<CxScheduleResult> list = cxScheduleResultEntityMapper.selectList(queryWrapper);

        for (CxScheduleResult item : list) {
            //获取成型今日计划量
            int todayPlanQty = (item.getClass1PlanQty() == null ? 0 :  item.getClass1PlanQty()) + (item.getTotalStock() == null ? 0 : item.getTotalStock()) ;
            // 汇总成型计划想关联的硫化ids
            if (item.getLhScheduleIds() != null) {
                String[] ids = item.getLhScheduleIds().split(",");
                QueryWrapper<LhScheduleResult> lhScheduleResultQueryWrapper = new QueryWrapper<>();
                lhScheduleResultQueryWrapper.in("id", ids);
                List<LhScheduleResult> lhScheduleResultList = lhScheduleAdjustEntityMapper.selectList(lhScheduleResultQueryWrapper);

                int lhTodayPlanQty = 0;
                //将硫化计划量调整
                for (LhScheduleResult lhScheduleResult : lhScheduleResultList) {
                   lhTodayPlanQty = lhTodayPlanQty + (lhScheduleResult.getClass1PlanQty() == null ? 0 : lhScheduleResult.getClass1PlanQty());
                }

                //计算硫化产量 - {成型早班+库存}
                int lhTodayQty = lhTodayPlanQty - todayPlanQty;

                //更新硫化计划
                if (lhTodayQty > 0) {
                    for (LhScheduleResult lhScheduleResult : lhScheduleResultList) {
                        lhScheduleResult.setClass1PlanQty(lhScheduleResult.getClass1PlanQty() - lhTodayPlanQty/lhScheduleResultList.size());
                        lhScheduleAdjustEntityMapper.updateById(lhScheduleResult);
                    }
                }else {
                    msg.append("胎胚【").append(item.getEmbryoCode()).append("】")
                            .append("今日库存")
                            .append(item.getTotalStock())
                            .append("，今日早班计划量")
                            .append(item.getClass1PlanQty())
                            .append("，今日计划量满足硫化，无需调整硫化计划！\n");
                }
            }
        }
        if (msg.length() > 0){
            return AjaxResult.error(msg.toString());
        }
        return AjaxResult.success();
    }

    /**
     * 校验施工切换版本是否还有剩余旧版本半部件的库存
     *
     * @param embryoCode   施工代号
     * @param oldVersion   旧版本
     * @param newVersion   新版本
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult checkConsOldVersionStock(String embryoCode, String oldVersion, String newVersion, Date scheduleDate) {
        // 查询新旧版本施工，区分哪些半部件有变动，有变动的，才需要做下面的校验
        List<CxProductConstructionInfo> consList = cxScheduleResultEntityMapper.selectOldNewConstruction(embryoCode, oldVersion, newVersion);
        CxProductConstructionInfo oldCons = null;
        CxProductConstructionInfo newCons = null;
        for (CxProductConstructionInfo constructionInfo : consList) {
            if (oldVersion.equals(constructionInfo.getEmbryoVersion())) {
                oldCons = constructionInfo;
                continue;
            }

            if (newVersion.equals(constructionInfo.getEmbryoVersion())) {
                newCons = constructionInfo;
            }
        }
        // 有一个为空，表示未找到对应版本的施工数据
        if (Objects.isNull(oldCons) || Objects.isNull(newCons)) {
            return AjaxResult.error("未找到对应施工版本的数据");
        }
        // 1.查询新旧版本不同的半部件
        // 胎面不一致，校验旧库存
        String result = checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.TM);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.TC);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.NC);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.CD15_1);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.CD15_2);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.CD90);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.XWYY);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.GDYY);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.GSQ);
        result += checkConsByPartFieldName(scheduleDate, oldCons, newCons, HalfComponentCodeEnums.TQ);
        if (StringUtils.isNotBlank(result)) {
            return AjaxResult.error(result);
        }
        return AjaxResult.success();
    }

    private String checkConsByPartFieldName(Date scheduleDate, CxProductConstructionInfo oldCons, CxProductConstructionInfo newCons, HalfComponentCodeEnums halfComponentCodeEnums) {
        String partFieldName = halfComponentCodeEnums.getFieldName();
        Object oldFieldValue = ReflectUtils.getFieldValue(oldCons, partFieldName);
        Object newFieldValue = ReflectUtils.getFieldValue(newCons, partFieldName);
        String msg = "";
        if (!oldFieldValue.equals(newFieldValue)) {
            // 2.如果当天成型是否有其他胎胚共用旧版本的半部件，如果没有给予提示，有则直接返回
            List<CxScheduleResult> existSameEmbryoCodeCxList = cxScheduleResultEntityMapper.selectByEmbryoCodeAndScheduleDate(oldFieldValue, scheduleDate, partFieldName);
            if (CollectionUtils.isNotEmpty(existSameEmbryoCodeCxList)) {
                // 有共用胎胚，不提示
                return msg;
            }
            // 4.查询对应库存量是否大于0，如果大于0就给予提示，“哪个半部件还有库存，不能切换版本”
            BigDecimal stockQty = cxScheduleResultEntityMapper.selectByPartFieldNameAndScheduleDate(oldFieldValue, scheduleDate, partFieldName);
            if (BigDecimal.ZERO.compareTo(stockQty) <= 0) {
                return halfComponentCodeEnums.getProductName() + "【" + oldFieldValue + "】还有库存，不能切换版本";
            }
        }
        return msg;
    }
}





