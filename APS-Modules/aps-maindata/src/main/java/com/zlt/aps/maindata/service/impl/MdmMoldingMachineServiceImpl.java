package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.domain.entity.CxScheduleResultSearchVo;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IMdmMoldingMachineService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineCls;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineClsB;
import com.zlt.aps.monthplan.api.domain.vo.BaseMoldingMachineInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineProNumVo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachineServiceImpl.java
 * 描    述：MdmMoldingMachineServiceImpl基础数据-成型机档案业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-18
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmMoldingMachineServiceImpl extends AbstractDocService<MdmMoldingMachine> implements IMdmMoldingMachineService {

    @Autowired
    private MdmMoldingMachineEntityMapper entityMapper;
    @Autowired
    private MdmMoldingMachineClsEntityMapper moldingMachineClsEntityMapper;
    @Autowired
    private MdmMoldingMachineClsBEntityMapper mdmMoldingMachineClsBEntityMapper;
    @Autowired
    private MdmMoldingMachineStatusEntityMapper mdmMoldingMachineStatusEntityMapper;
    @Autowired
    private MdmMoldingMachineClsEntityMapper mdmMoldingMachineClsEntityMapper;
    @Autowired
    private CxScheduleResultSearchVoEntityMapper cxScheduleResultSearchVoEntityMapper;

    /**
     * 机台类型：一次法
     */
    public static final Integer MACHINE_TYPE_ONCE = 1;


    /**
     * 机台类型：二次法
     */
    public static final Integer MACHINE_TYPE_TWICE = 2;

    /**
     * 班制
     */
    public static final BigDecimal SHIFT_TYPE = BigDecimal.valueOf(2);


    @Override
    protected String getDocTypeCode() {
        return "0116";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0116");
        return sysDocType;
    }

    /**
     * 根据工厂编码和机台编码获取成型机信息
     *
     * @param factoryCode
     * @param machineCode
     * @return
     */
    @Override
    public MdmMoldingMachine getMoldingMachineByMachineCode(String factoryCode, String machineCode) {
        LambdaQueryWrapper<MdmMoldingMachine> query = new LambdaQueryWrapper<>();
        query.eq(MdmMoldingMachine::getFactoryCode, factoryCode)
                .eq(MdmMoldingMachine::getMoldingMachineCode, machineCode)
                .eq(MdmMoldingMachine::getIsDelete, ApsConstant.APS_YES_NO_0);
        return entityMapper.selectOne(query);
    }

    @Override
    public List<MdmMoldingMachineProNumVo> getMoldingMachineProNum(String factoryCode) {
        // Step 1: 获取成型机类型表 MdmMoldingMachineCls
        QueryWrapper<MdmMoldingMachineCls> queryMdmMoldingMachineClsWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineCls> moldingMachineCls = mdmMoldingMachineClsEntityMapper.selectList(queryMdmMoldingMachineClsWrapper);
        // 依据类型分组
        Map<Long, List<MdmMoldingMachineCls>> moldingMachineClassCodeMap = moldingMachineCls.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachineCls::getId));

        // Step 2: 获取成型机信息表 MdmMoldingMachine
        QueryWrapper<MdmMoldingMachine> queryMdmMoldingMachineWrapper = new QueryWrapper<>();
        List<MdmMoldingMachine> moldingMachines = entityMapper.selectList(queryMdmMoldingMachineWrapper);

        for (MdmMoldingMachine vo : moldingMachines) {
            //填充成型法
            if (moldingMachineClassCodeMap.containsKey(vo.getMoldingMachineClassId())) {
                vo.setMouldMethod(moldingMachineClassCodeMap.get(vo.getMoldingMachineClassId()).get(0).getMouldMethod());
            }
        }

        // Step 2.2: 依据成型法区分：一次法数量，二次法数量
        Map<Integer, List<MdmMoldingMachine>> machineNumMap = moldingMachines.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachine::getMouldMethod));

        // Step 3: 获取成型机类型子表 MdmMoldingMachineClsB
        QueryWrapper<MdmMoldingMachineClsB> queryMdmMoldingMachineClassWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineClsB> moldingMachineClsB = mdmMoldingMachineClsBEntityMapper.selectList(queryMdmMoldingMachineClassWrapper);
        // 已经类型分组
        Map<Long, List<MdmMoldingMachineClsB>> moldingMachineClassItemCodeMap = moldingMachineClsB.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachineClsB::getMoldingMachineClassId));

        // Step 遍历汇总数据
        List<MdmMoldingMachineProNumVo> resultList = new ArrayList<>();
        Map<String, MdmMoldingMachineProNumVo> resultMap = new HashMap<>();
        for (MdmMoldingMachine vo : moldingMachines) {
            // 初始化或获取MdmMoldingMachineClsBs列表
            if (moldingMachineClassItemCodeMap.containsKey(vo.getMoldingMachineClassId())) {
                vo.setMdmMoldingMachineClsBs(moldingMachineClassItemCodeMap.get(vo.getMoldingMachineClassId()));
            } else {
                // 防止NPE
                vo.setMdmMoldingMachineClsBs(new ArrayList<>());
            }

            //计算结果
            for (MdmMoldingMachineClsB item : vo.getMdmMoldingMachineClsBs()) {
                String key = item.getProSize().toString() + vo.getMouldMethod();
                if (resultMap.containsKey(key)) {
                    MdmMoldingMachineProNumVo result = resultMap.get(key);
                    result.setQuota(result.getQuota().add(BigDecimal.valueOf(item.getProductionQuotaQty()).multiply(SHIFT_TYPE)));
                    result.setProNum(result.getProNum() + 1);
                    result.setAverageQuota(result.getQuota().divide(BigDecimal.valueOf(result.getProNum()), 2, RoundingMode.HALF_UP));
                    resultMap.put(key, result);
                } else {
                    MdmMoldingMachineProNumVo result = new MdmMoldingMachineProNumVo();
                    result.setProSize(item.getProSize());
                    result.setMouldMethod(vo.getMouldMethod());
                    result.setProNum(1);
                    result.setQuota(BigDecimal.valueOf(item.getProductionQuotaQty()).multiply(SHIFT_TYPE));
                    result.setAverageQuota(result.getQuota().divide(BigDecimal.valueOf(result.getProNum()), 2, RoundingMode.HALF_UP));
                    result.setMachineSumNum(machineNumMap.get(vo.getMouldMethod()).size());
                    resultMap.put(key, result);
                }
            }
        }

        // 修复类型转换问题
        return new ArrayList<>(resultMap.values());
    }

    /**
     * 获取指定日期成型排程使用的成型机信息
     */
    @Override
    public List<BaseMoldingMachineInfoVo> getCurrencyMachineInfo(CxScheduleResultSearchVo cxScheduleResultSearchVo) {
        List<BaseMoldingMachineInfoVo> baseMoldingMachineInfoVoList = new ArrayList<>();
        // Step 0: 检查传参
        if (cxScheduleResultSearchVo.getScheduleDate() == null) {
            throw new ServiceException("排程日期必填！");
        }

        // Step 1: 获取成型排程结果
        QueryWrapper<CxScheduleResultSearchVo> cxScheduleResultSearchVoQueryWrapper = new QueryWrapper<>();
        cxScheduleResultSearchVoQueryWrapper.eq("schedule_date", cxScheduleResultSearchVo.getScheduleDate());
        List<CxScheduleResultSearchVo> cxScheduleResultSearchVoList = cxScheduleResultSearchVoEntityMapper.selectList(cxScheduleResultSearchVoQueryWrapper);

        // 已经类型分组
        Map<String, List<CxScheduleResultSearchVo>> cxScheduleResultSearchVoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(cxScheduleResultSearchVoList)) {
            cxScheduleResultSearchVoMap = cxScheduleResultSearchVoList.stream()
                    .collect(Collectors.groupingBy(CxScheduleResultSearchVo::getCxMachineCode));
        }

        // Step 2: 获取成型机类型表 MdmMoldingMachineCls
        QueryWrapper<MdmMoldingMachineCls> queryMdmMoldingMachineClsWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineCls> moldingMachineCls = mdmMoldingMachineClsEntityMapper.selectList(queryMdmMoldingMachineClsWrapper);
        // 依据类型分组
        Map<Long, List<MdmMoldingMachineCls>> moldingMachineClassCodeMap = moldingMachineCls.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachineCls::getId));

        // Step 2: 获取成型机信息表 MdmMoldingMachine
        QueryWrapper<MdmMoldingMachine> queryMdmMoldingMachineWrapper = new QueryWrapper<>();
        queryMdmMoldingMachineWrapper.eq("MONTH_PLAN_STATUS", ApsConstant.APS_YES_NO_0);
        List<MdmMoldingMachine> moldingMachines = entityMapper.selectList(queryMdmMoldingMachineWrapper);
        // 依据成型机分组
        // Step 2.2: 依据成型法区分：一次法数量，二次法数量
        Map<String, List<MdmMoldingMachine>> machineNumMap = moldingMachines.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachine::getMoldingMachineCode));

        // Step 3: 获取成型机类型子表 MdmMoldingMachineClsB
        QueryWrapper<MdmMoldingMachineClsB> queryMdmMoldingMachineClassWrapper = new QueryWrapper<>();
        List<MdmMoldingMachineClsB> moldingMachineClsB = mdmMoldingMachineClsBEntityMapper.selectList(queryMdmMoldingMachineClassWrapper);
        // 已经类型分组
        Map<Long, List<MdmMoldingMachineClsB>> moldingMachineClassItemCodeMap = moldingMachineClsB.stream()
                .collect(Collectors.groupingBy(MdmMoldingMachineClsB::getMoldingMachineClassId));

        // Step 3: 生成数据
        for (String machine : machineNumMap.keySet()){
            BaseMoldingMachineInfoVo baseMoldingMachineInfoVo = new BaseMoldingMachineInfoVo();
            //成型机编号
            baseMoldingMachineInfoVo.setMoldingMachineCode(machine);

            MdmMoldingMachine mdmMoldingMachine = null;
            if (machineNumMap.containsKey(machine)) {
                mdmMoldingMachine = machineNumMap.get(machine).get(0);
            } else {
                throw new ServiceException("未找到成型机！");
            }

            baseMoldingMachineInfoVo.setMoldingMachineClsType(mdmMoldingMachine.getMoldingMachineClassId());

            if (moldingMachineClassCodeMap.containsKey(mdmMoldingMachine.getMoldingMachineClassId())) {
                MdmMoldingMachineCls mdmMoldingMachineCls = moldingMachineClassCodeMap.get(mdmMoldingMachine.getMoldingMachineClassId()).get(0);
                //成型机类型
                baseMoldingMachineInfoVo.setMouldMethod(String.valueOf(mdmMoldingMachineCls.getMouldMethod()));

                //成型机类型名称
                baseMoldingMachineInfoVo.setMoldingMachineClsName(mdmMoldingMachineCls.getMoldingMachineClassName());

                if (moldingMachineClassItemCodeMap.containsKey(mdmMoldingMachine.getMoldingMachineClassId())) {
                    List<MdmMoldingMachineClsB> values = moldingMachineClassItemCodeMap.get(mdmMoldingMachine.getMoldingMachineClassId());
                    //获取寸口天产能定额
                    Map<BigDecimal, Long> proSizeQuotaQtyMap = new HashMap<>();
                    for (MdmMoldingMachineClsB mdmMoldingMachineClsB : values) {
                        //只根据成型机配置的寸口上下限制进行配置寸口产能
                        if (mdmMoldingMachineClsB.getProSize().compareTo(mdmMoldingMachine.getMaxSize())>0){
                            continue;
                        }
                        if (mdmMoldingMachineClsB.getProSize().compareTo(mdmMoldingMachine.getMinSize())<0){
                            continue;
                        }

                        proSizeQuotaQtyMap.put(mdmMoldingMachineClsB.getProSize(), SHIFT_TYPE.multiply(BigDecimal.valueOf(mdmMoldingMachineClsB.getProductionQuotaQty())).longValue());
                    }
                    baseMoldingMachineInfoVo.setProSizeQuotaQtyMap(proSizeQuotaQtyMap);

                    //获取寸口天硫化机配比
                    Map<BigDecimal, BigDecimal> proSizeSulfurizationMachineRatioMap = new HashMap<>();
                    for (MdmMoldingMachineClsB mdmMoldingMachineClsB : values) {
                        //只根据成型机配置的寸口上下限制进行配置寸口产能
                        if (mdmMoldingMachineClsB.getProSize().compareTo(mdmMoldingMachine.getMaxSize())>0){
                            continue;
                        }
                        if (mdmMoldingMachineClsB.getProSize().compareTo(mdmMoldingMachine.getMinSize())<0){
                            continue;
                        }

                        proSizeSulfurizationMachineRatioMap.put(mdmMoldingMachineClsB.getProSize(), mdmMoldingMachineClsB.getMoldingSulfurizationRatio());
                    }
                    baseMoldingMachineInfoVo.setMoldingMachineProSizeSulfurizationMachineMap(proSizeSulfurizationMachineRatioMap);
                } else {
                    throw new ServiceException("未找到成型机类型子表！");
                }
            }

            //胎体布层数
            baseMoldingMachineInfoVo.setCarcassClothType(mdmMoldingMachine.getCarcassClothType());

            // 获取当前机台的所有计划
            List<CxScheduleResultSearchVo> cxScheduleResultSearchVos = cxScheduleResultSearchVoMap.get(machine);

            // 当前寸口
            if (cxScheduleResultSearchVos != null && !cxScheduleResultSearchVos.isEmpty()) {

                baseMoldingMachineInfoVo.setCurrentProSize(BigDecimal.valueOf(cxScheduleResultSearchVos.get(0).getSpecDimension())
                        .setScale(2, RoundingMode.HALF_UP));

                // 当前排程的1班规格个数
                int class1MaxSort = 0;
                for (CxScheduleResultSearchVo item:cxScheduleResultSearchVos) {
                   if (item.getClass1Sort() != null && item.getClass1Sort() > class1MaxSort)  {
                       class1MaxSort = item.getClass1Sort();
                   }
                }

                // 当前排程的2班规格个数
                int class2MaxSort = 0;
                for (CxScheduleResultSearchVo item:cxScheduleResultSearchVos) {
                    if (item.getClass2Sort() != null && item.getClass2Sort() > class2MaxSort)  {
                        class2MaxSort = item.getClass2Sort();
                    }

                    //续作规格不能多算一次
                    if (item.getClass2Sort() != null && 1 == item.getClass2Sort()  && item.getClass1Sort() != null &&  item.getClass1Sort() == class1MaxSort)  {
                        class2MaxSort = class2MaxSort -1;
                    }
                }

                // 换工装次数
                baseMoldingMachineInfoVo.setCurrentEmbryoCodeNumber((class1MaxSort == 0 ? 0  : class1MaxSort - 1) + (class2MaxSort == 0 ? 0  : class2MaxSort - 1));
            }

            baseMoldingMachineInfoVoList.add(baseMoldingMachineInfoVo);
        }

        return baseMoldingMachineInfoVoList;
    }


    @Override
    public String checkUnique(MdmMoldingMachine docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmMoldingMachine.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "moldingMachineCode", "productTypeCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmMoldingMachine> list, List<MdmMoldingMachine> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        if (CollectionUtils.isNotEmpty(list)) {
            List<String> clsNameList = list.stream().map(MdmMoldingMachine::getMoldingMachineClsName).collect(Collectors.toList());
            String factoryCode = list.get(0).getFactoryCode();
            // 转换成型机类型为对应 id保存
            LambdaQueryWrapper<MdmMoldingMachineCls> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmMoldingMachineCls::getFactoryCode, factoryCode);
            wrapper.in(MdmMoldingMachineCls::getMoldingMachineClassName, clsNameList);
            List<MdmMoldingMachineCls> classEntityList = moldingMachineClsEntityMapper.selectList(wrapper);
            if (CollectionUtils.isNotEmpty(classEntityList)) {
                Map<String, MdmMoldingMachineCls> moldingMachineClsMap = classEntityList.stream().collect(Collectors
                        .toMap(item -> String.join("|", item.getFactoryCode(), item.getMoldingMachineClassName()),
                                Function.identity(), (s1, s2) -> s1));
                serviceCheckParams.put("moldingMachineClsMap", moldingMachineClsMap);
            }
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmMoldingMachine importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Map<String, MdmMoldingMachineCls> moldingMachineClsMap = (Map<String, MdmMoldingMachineCls>) serviceCheckParams.get("moldingMachineClsMap");
        String mapKey = String.join("|", importDocEntity.getFactoryCode(), importDocEntity.getMoldingMachineClsName());
        if (moldingMachineClsMap.containsKey(mapKey)) {
            MdmMoldingMachineCls mdmMoldingMachineCls = moldingMachineClsMap.get(mapKey);
            importDocEntity.setMoldingMachineClassId(mdmMoldingMachineCls.getId());
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /*@Override
    public AjaxResult importData(List<MdmMoldingMachine> list, boolean updateSupport, Long importLogId) {
        //1.初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmMoldingMachine> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //2.国际化初始化
        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String repeatingRecordStr = I18nUtil.getMessage("ui.data.alert.DocMoldingMachine.repeatingRecord");
        String noMoldingMachinesStr = I18nUtil.getMessage("ui.data.alert.DocMoldingMachine.noMoldingMachines");
        String noVulcanizingMachineStr = I18nUtil.getMessage("ui.data.alert.DocMoldingMachine.noVulcanizingMachine");

        //3.唯一键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> (
                ObjectUtils.defaultIfNull(item.getFactoryCode(), "") +
                        ObjectUtils.defaultIfNull(item.getProductTypeCode(), "") +
                        ObjectUtils.defaultIfNull(item.getMoldingMachineCode(), "")), Collectors.counting()));

        //3.公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmMoldingMachine docMoldingMachineExcelVo = list.get(i);
            //重复记录校验
            Long hasValue = groupMap.get(
                    ObjectUtils.defaultIfNull(docMoldingMachineExcelVo.getFactoryCode(), "") +
                            ObjectUtils.defaultIfNull(docMoldingMachineExcelVo.getProductTypeCode(), "") +
                            ObjectUtils.defaultIfNull(docMoldingMachineExcelVo.getMoldingMachineCode(), ""));
            if (hasValue > 1) {
                failureNum++;
                docMoldingMachineExcelVo.setId(-999L);
                String message = String.format(rowCountStr, i + 2) + repeatingRecordStr;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            }
            // 转换成型机类型为对应 id保存
            LambdaQueryWrapper<MdmMoldingMachineCls> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmMoldingMachineCls::getFactoryCode, docMoldingMachineExcelVo.getFactoryCode());
            wrapper.eq(MdmMoldingMachineCls::getMoldingMachineClassName, docMoldingMachineExcelVo.getMoldingMachineClsName());
            List<MdmMoldingMachineCls> classEntityList = moldingMachineClsEntityMapper.selectList(wrapper);

            if (CollectionUtils.isEmpty(classEntityList)) {
                failureNum++;
                docMoldingMachineExcelVo.setId(-999L);
                String message = String.format(rowCountStr, i + 2) + noMoldingMachinesStr;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            }
            *//*if (StringUtils.isNotEmpty(docMoldingMachineExcelVo.getLineCode())) {
                StringBuilder noExistlineCode = new StringBuilder();
                String[] lines = Convert.toStrArray(docMoldingMachineExcelVo.getLineCode());
                for (int k = 0; k < lines.length; k++) {
                    DocVulcanizingLineEntity pa = new DocVulcanizingLineEntity();
                    pa.setFactoryCode(docMoldingMachineExcelVo.getFactoryCode());
                    pa.setLineCode(lines[k]);
                    List<DocVulcanizingLineEntity> docVulcanizingLineEntities = docServiceMapper.selectDocVulcanizingLineEntityList(pa);
                    if (docVulcanizingLineEntities.isEmpty()) {
                        noExistlineCode.append(lines[k]).append(",");
                    }
                }
                if (StringUtils.isNotEmpty(noExistlineCode.toString())) {
                    failureNum++;
                    docMoldingMachineExcelVo.setId(-999L);
                    String message = noVulcanizingMachineStr + noExistlineCode.substring(0, noExistlineCode.length() - 1) + "";
                    addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                    continue;
                }
            }*//*
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docMoldingMachineExcelVo);
            if (CollectionUtils.isNotEmpty(validated)) {
                docMoldingMachineExcelVo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else if (updateSupport) {
                docMoldingMachineExcelVo.setBaseVale(null);
                docMoldingMachineExcelVo.setMoldingMachineClassId(classEntityList.get(0).getId());
                importList.add(docMoldingMachineExcelVo);
            }

        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport) {
                successNum = importList.size();
//                docMoldingMachineEntityMapper.mergeSql(importList);
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MdmMoldingMachine docMoldingMachineEntity = list.get(i);
                    // 错误记录跳过
                    if (docMoldingMachineEntity.getId() != null && docMoldingMachineEntity.getId().equals(-999L)) {
                        continue;
                    }
                    *//*if (StringUtils.isNotBlank(docMoldingMachineEntity.getProductrestriction())) {
                        String[] params = (docMoldingMachineEntity.getProductrestriction() + "-").split("-");
                        if (params.length > 1) {
                            docMoldingMachineEntity.setMinSize(new BigDecimal(params[0]));
                            docMoldingMachineEntity.setMaxSize(new BigDecimal(params[1]));
                        }
                    }*//*
                    String unique = this.checkUnique(docMoldingMachineEntity);
                    if (Objects.equals(unique, UserConstants.UNIQUE)) {
                        baseDao.insert(docMoldingMachineEntity);
                        *//*if (StringUtils.isNotBlank(docMoldingMachineEntity.getLineCode())) {
                            // 插入成型机子表
                            insertDocMoldingMachineB(docMoldingMachineEntity, docMoldingMachineEntity.getId());
                        }*//*
                    } else {
//                        failureNum++;
//                        addImportErrorLog(importLogId, i + 2,
//                                I18nUtil.getMessage(""), importErrorLogs);
                        // 唯一校验不需校验成型机类型
                        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
                        wrapper.eq(MdmMoldingMachine::getFactoryCode, docMoldingMachineEntity.getFactoryCode());
                        wrapper.eq(MdmMoldingMachine::getMoldingMachineCode, docMoldingMachineEntity.getMoldingMachineCode());
                        wrapper.eq(MdmMoldingMachine::getProductTypeCode, docMoldingMachineEntity.getProductTypeCode());
                        List<MdmMoldingMachine> docMoldingMachineEntities = entityMapper.selectList(wrapper);
                        if (!docMoldingMachineEntities.isEmpty()) {
                            docMoldingMachineEntity.setId(docMoldingMachineEntities.get(0).getId());
                            baseDao.update(docMoldingMachineEntity);
//                            docOtherServiceMapper.deleteByMachineId(docMoldingMachineEntity.getId());
                            // 插入成型机子表
//                            insertDocMoldingMachineB(docMoldingMachineEntity, docMoldingMachineEntity.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }*/


}

