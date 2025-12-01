package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.AppUtils;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MdmProductInfoEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmStockUpPlan;
import com.zlt.aps.monthplan.api.domain.vo.CalcStockingResultVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmStockUpPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import com.zlt.aps.monthplan.api.domain.vo.StockUpPlanExcelVo;
import com.zlt.aps.monthplan.factory.mapper.MdmStockUpPlanMapper;
import com.zlt.aps.monthplan.factory.service.IMdmStockUpPlanService;
import com.zlt.aps.monthplan.factory.service.IMpHistorySaleQtyService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmStockUpPlanServiceImpl.java
 * 描    述：MdmStockUpPlanServiceImpl备货计划业务层处理
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MdmStockUpPlanServiceImpl extends ServiceImpl<MdmStockUpPlanMapper, MdmStockUpPlan> implements IMdmStockUpPlanService {

    private final BaseDao baseDao;

    private final IMpHistorySaleQtyService mpHistorySaleQtyService;

    private final MdmProductInfoEntityMapper mdmProductInfoEntityMapper;

    private final IFactoryParamService factoryParamService;

    /**
     * 查询备货计划列表
     *
     * @param mdmStockUpPlan 备货计划
     * @return 备货计划
     */
    @Override
    public List<MdmStockUpPlanVo> selectMdmStockUpPlanList(MdmStockUpPlanVo mdmStockUpPlan) {
        List<MdmStockUpPlanVo> list = getBaseMapper().selectMdmStockUpPlanList(mdmStockUpPlan);
        // 回显创建人
        AppUtils.formatData(list, new String[]{
                "createByName->getcolvaluewithcondition(sys_user, nick_name, user_name, createBy, del_flag='0')",
        });
        return list;
    }

    @Override
    public AjaxResult createStockUpPlan(QueryCalcStockingParamVo queryCalcStockingParamVo) {
        List<MdmStockUpPlan> mdmStockUpPlanList = new ArrayList<>();
        //20250521 ZLT 会出现可能需要跨月提前值 ，因为近一个月月数据没有或是没有意义
        Integer lastMonth = factoryParamService.getStockUpLastMonth(queryCalcStockingParamVo.getFactoryCode());
        // 根据轮胎类型、月份范围查询备货记录
        List<CalcStockingResultVo> calcStockingResultVos = mpHistorySaleQtyService.selectCalcStocking(queryCalcStockingParamVo, lastMonth);
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 获取当前年当前月的下一个月
        LocalDate nextMonth = currentDate.plusMonths(1);
        // 当前年
        int currentYear = currentDate.getYear();
        // 获取下一个月的int值
        int nextMonthValue = nextMonth.getMonthValue();
        // 删除下个月对应轮胎类型的备货量数据
        getBaseMapper().deleteByParams(currentYear, nextMonthValue, queryCalcStockingParamVo.getTireType());
        String username = SecurityUtils.getUsername();
        Date nowDate = DateUtils.getNowDate();
        String factoryCode = "";
        if (CollectionUtils.isNotEmpty(calcStockingResultVos)) {
            factoryCode = calcStockingResultVos.get(0).getFactoryCode();
        }
        Set<String> noStockUpBrandSet = factoryParamService.getNoStockUpPlanBrand(factoryCode);
        // 生成备货计划
        calcStockingResultVos.stream().forEach(calcStockingResultVo -> {
            MdmStockUpPlan mdmStockUpPlan = new MdmStockUpPlan();
            mdmStockUpPlan.setYear(calcStockingResultVo.getYear()); // 年份
            mdmStockUpPlan.setMonth(calcStockingResultVo.getMonth()); // 月份
            mdmStockUpPlan.setFactoryCode(calcStockingResultVo.getFactoryCode()); // 分厂
            mdmStockUpPlan.setProductCode(calcStockingResultVo.getProductCode()); // SAP编码
            mdmStockUpPlan.setProductDesc(calcStockingResultVo.getProductDesc()); // 物料描述
            mdmStockUpPlan.setLocationType(Integer.valueOf(calcStockingResultVo.getLocationType())); // 库位类别
            mdmStockUpPlan.setAverageType(Math.toIntExact(queryCalcStockingParamVo.getMonthRange())); // 月销量平均方式
            mdmStockUpPlan.setAverageValue(Math.toIntExact(calcStockingResultVo.getAugQty())); // 月销量平均值
            mdmStockUpPlan.setFactor(calcStockingResultVo.getFactorValue()); // 备货系数
            //20250512 外贸贴牌品牌配置不备货
            if (noStockUpBrandSet.contains(calcStockingResultVo.getBrand())) {
                mdmStockUpPlan.setStockQty(BigDecimal.ZERO.longValue());
            } else {
                mdmStockUpPlan.setStockQty(Long.valueOf(calcStockingResultVo.getStockQty())); // 备货量
            }
            mdmStockUpPlan.setStockoist(username);
            mdmStockUpPlan.setApprover(username);
            mdmStockUpPlan.setStockTime(nowDate);
            mdmStockUpPlan.setApproveTime(nowDate);
            mdmStockUpPlanList.add(mdmStockUpPlan);
        });
        if (CollectionUtils.isNotEmpty(mdmStockUpPlanList)) {
            saveBatch(mdmStockUpPlanList);
        }
        return AjaxResult.success();
    }

    @Override
    public AjaxResult saveStockUpPlan(MdmStockUpPlanVo mdmStockUpPlan) {
        if (!requirementCheck(mdmStockUpPlan)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.noEmptyInfo"));
        }
        if (mdmStockUpPlan.getStockQty() < BigDecimal.ZERO.longValue()) {
            mdmStockUpPlan.setStockQty(BigDecimal.ZERO.longValue());
        }
        Long id = mdmStockUpPlan.getId();
        if (null == id) {
            String productCode = mdmStockUpPlan.getProductCode();
            MdmProductInfo productInfo = getProductInfo(productCode, mdmStockUpPlan.getFactoryCode());
            if (null == productInfo) {
                String productCodeError = I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.productCode.notExist");
                return AjaxResult.error(String.format(productCodeError, productCode));
            }
            int exist = baseMapper.existByKey(mdmStockUpPlan);
            if (exist > BigDecimal.ZERO.intValue()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.isExist"));
            }
            Integer averageType = baseMapper.getAverageType(mdmStockUpPlan);
            MdmStockUpPlan insert = new MdmStockUpPlan();
            BeanUtils.copyProperties(mdmStockUpPlan, insert);
            insert.setAverageType(averageType);
            insert.setProductDesc(productInfo.getProductDesc());
            save(insert);
            return AjaxResult.success();
        }
        String errorInfo = I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.notExist");
        MdmStockUpPlan origin = getById(id);
        if (null == origin) {
            return AjaxResult.error(errorInfo);
        }
        Integer averageType = origin.getAverageType();
        BeanUtils.copyProperties(mdmStockUpPlan, origin);
        origin.setAverageType(averageType);
        updateById(origin);
        return AjaxResult.success();
    }

    @Override
    public List<MdmStockUpPlan> getStockUpByYearAndMonth(Integer year, Integer month) {
        if (null == year || null == month) {
            return Collections.emptyList();
        }
        QueryWrapper<MdmStockUpPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.gt("STOCK_QTY", BigDecimal.ZERO.longValue());
        return getBaseMapper().selectList(queryWrapper);
    }


    @Override
    public AjaxResult importData(List<StockUpPlanExcelVo> list, boolean updateSupport, Long importLogId) {
        // 初始化
        int successNum = 0;
        AtomicInteger failureNum = new AtomicInteger();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        // 国际化提示
        String productCodeError = I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.productCode.notExist");
        String repeatError = I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.repeat");

        // 物料信息
        Map<String, MdmProductInfo> productInfoMap = new HashMap<>();
        List<String> productCodeList = list.stream().map(StockUpPlanExcelVo::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(productCodeList)) {
            List<String> factoryCodeList = list.stream().map(StockUpPlanExcelVo::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
            productInfoMap = mdmProductInfoEntityMapper.selectList(Wrappers.lambdaQuery(MdmProductInfo.class)
                            .in(MdmProductInfo::getProductCode, productCodeList)
                            .in(CollectionUtils.isNotEmpty(factoryCodeList), MdmProductInfo::getFactoryCode, factoryCodeList))
                    .stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getProductCode()), Function.identity(), (v1, v2) -> v1));
        }

        // 重复记录，分厂 + 年 + 月 + 物料编号 + 库位
        Function<StockUpPlanExcelVo, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getYear(), v.getMonth(), v.getProductCode(), v.getLocationType());
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(keyFunc, Collectors.counting()));

        // 公共校验（非空校验、长度校验等）
        Map<String, MdmProductInfo> finalProductInfoMap = productInfoMap;
        List<StockUpPlanExcelVo> importList = list.stream()
                .map(vo -> {
                    int errorNum = list.indexOf(vo) + 2;
                    List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, vo);
                    if (com.alibaba.nacos.common.utils.CollectionUtils.isNotEmpty(validated)) {
                        failureNum.getAndIncrement();
                        importErrorLogs.addAll(validated);
                        return null;
                    }

                    // 重复记录校验
                    if (groupMap.get(keyFunc.apply(vo)) > 1) {
                        failureNum.getAndIncrement();
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum, repeatError, importErrorLogs);
                        return null;
                    }

                    // 查询对应物料信息
                    MdmProductInfo mdmProductInfo = finalProductInfoMap.get(GenerageMapKeyUtils.createMapKey(vo.getFactoryCode(), vo.getProductCode()));
                    if (mdmProductInfo == null) {
                        failureNum.getAndIncrement();
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum, String.format(productCodeError, vo.getProductCode()), importErrorLogs);
                        return null;
                    }
                    vo.setProductDesc(mdmProductInfo.getProductDesc());

                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && com.alibaba.nacos.common.utils.CollectionUtils.isNotEmpty(importList)) {
                // 处理导入数据
                processImportList(importList, importErrorLogs, successNum);
                successNum = importList.size();
            } else {
//                //唯一则新增
//                for (int i = 0; i < list.size(); i++) {
//                    MpHistorySaleQtyExcelVo mpHistorySaleQtyExcelVo = list.get(i);
//                    // 错误记录跳过
//                    if (mpHistorySaleQtyExcelVo.getId() != null && mpHistorySaleQtyExcelVo.getId().equals(-999L)) {
//                        continue;
//                    }
//                    String unique = this.checkMixMasterRubberStockUnique(mixMasterRubberStock);
//                    if (UserConstants.UNIQUE.equals(unique)) {
//                        successNum++;
//                        this.insertMixMasterRubberStock(mixMasterRubberStock);
//                    } else {
//                        failureNum++;
//                        addImportErrorLog(importLogId, i + 2,
//                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
//                    }
//                }
            }
        } catch (Exception e) {
            handleException(e, list.size(), importErrorLogs, importLogId, successNum, failureNum.get());
        }
        return buildAjaxResult(successNum, failureNum.get(), importErrorLogs);
    }

    private void handleException(Exception e, int listSize, List<ImportErrorLog> importErrorLogs, Long importLogId, int successNum, int failureNum) {
        // Log exception details
        log.error("Import data failed", e);
        successNum = 0;
        failureNum = listSize;
        importErrorLogs.clear();
        addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
    }

    private AjaxResult buildAjaxResult(int successNum, int failureNum, List<ImportErrorLog> importErrorLogs) {
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 处理导入数据
     *
     * @param importList      导入数据
     * @param importErrorLogs 导入错误日志数据
     * @param successNum      成功数
     */
    private void processImportList(List<StockUpPlanExcelVo> importList, List<ImportErrorLog> importErrorLogs, int successNum) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }

        // 提取分厂、年份、月份，避免在循环中重复获取
        String factoryCode = importList.get(0).getFactoryCode();
        Integer year = importList.get(0).getYear();
        Integer month = importList.get(0).getMonth();

        // 查询是否存在已生成的对应分厂+计划年月的数据
        LambdaQueryWrapper<MdmStockUpPlan> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(MdmStockUpPlan::getIsDelete, ApsConstant.DEL_FLAG_NORMAL)
                .eq(MdmStockUpPlan::getYear, year)
                .eq(MdmStockUpPlan::getMonth, month)
                .eq(MdmStockUpPlan::getFactoryCode, factoryCode);
        List<MdmStockUpPlan> mdmStockUpPlans = getBaseMapper().selectList(lambdaQueryWrapper);

        // 存在则清除相关计划数据
        if (CollectionUtils.isNotEmpty(mdmStockUpPlans)) {
            baseDao.deleteBatch(mdmStockUpPlans);
        }

        // 构建插入数据列表
        List<MdmStockUpPlan> insertList = importList.stream()
                .map(vo -> createMdmStockUpPlanFromVo(vo))
                .collect(Collectors.toList());

        // 批量插入导入数据
        if (CollectionUtils.isNotEmpty(insertList)) {
            saveBatch(insertList);
        }
    }

    /**
     * 构建备货计划对象
     *
     * @param vo
     * @return
     */
    private MdmStockUpPlan createMdmStockUpPlanFromVo(StockUpPlanExcelVo vo) {
        Long stockQty = BigDecimal.valueOf(vo.getAverageValue().doubleValue())
                .multiply(vo.getFactor())
                .setScale(0, RoundingMode.CEILING) // 向上取整
                .longValue();
        MdmStockUpPlan mdmStockUpPlan = new MdmStockUpPlan();
        mdmStockUpPlan.setYear(vo.getYear());
        mdmStockUpPlan.setMonth(vo.getMonth());
        mdmStockUpPlan.setFactoryCode(vo.getFactoryCode());
        mdmStockUpPlan.setProductCode(vo.getProductCode());
        mdmStockUpPlan.setProductDesc(vo.getProductDesc());
        mdmStockUpPlan.setLocationType(vo.getLocationType());
        mdmStockUpPlan.setAverageType(12);
        mdmStockUpPlan.setAverageValue(vo.getAverageValue());
        mdmStockUpPlan.setFactor(vo.getFactor());
        mdmStockUpPlan.setStockQty(stockQty);
        mdmStockUpPlan.setStockoist(vo.getStockoist());
        mdmStockUpPlan.setStockTime(vo.getStockTime());
        mdmStockUpPlan.setApprover(vo.getApprover());
        mdmStockUpPlan.setApproveTime(vo.getApproveTime());
        return mdmStockUpPlan;
    }

    /**
     * 必输项判断
     *
     * @param mdmStockUpPlan
     * @return
     */
    private boolean requirementCheck(MdmStockUpPlanVo mdmStockUpPlan) {
        if (StringUtils.isBlank(mdmStockUpPlan.getFactoryCode()) || null == mdmStockUpPlan.getYear() || null == mdmStockUpPlan.getMonth()) {
            return false;
        }
        if (StringUtils.isBlank(mdmStockUpPlan.getProductCode())) {
            return false;
        }
        if (null == mdmStockUpPlan.getLocationType() || null == mdmStockUpPlan.getStockQty()) {
            return false;
        }
        return true;
    }

    /**
     * 获取物料信息
     *
     * @param productCode 物料编码
     * @param factoryCode 工厂编码
     * @return
     */
    private MdmProductInfo getProductInfo(String productCode, String factoryCode) {
        if (StringUtils.isBlank(productCode) || StringUtils.isBlank(factoryCode)) {
            return null;
        }
        QueryWrapper<MdmProductInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("PRODUCT_CODE", productCode);
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return mdmProductInfoEntityMapper.selectOne(queryWrapper);
    }

}
