package com.zlt.aps.maindata.service.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmConstructionInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.mp.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mp.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.mp.api.service.IRemoteImportLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmConstructionInfoServiceImpl.java
 * 描    述：MdmConstructionInfoServiceImpl投产胎胚施工信息业务层处理
 *@author zlt
 *@date 2025-12-10
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmConstructionInfoServiceImpl extends AbstractDocService<MdmConstructionInfo>  implements IMdmConstructionInfoService {

    private static final Pattern TIRE_FABRIC_CODE_GETTER = Pattern.compile("getTireFabricCode\\d+");
    private static final Pattern BELT_CODE_GETTER = Pattern.compile("getBeltCode\\d+");

    @Autowired
    private IRemoteImportLogService iRemoteImportLogService;

    @Autowired
    private IRemoteImportErrorLogService iRemoteImportErrorLogService;

    @Autowired
    private MdmConstructionInfoEntityMapper mdmConstructionInfoEntityMapper;

    /**
     * 返回单据类型编码
     *
     * @return 单据类型编码 MDM0124
     */
    @Override
    protected String getDocTypeCode() {
        return "MDM0124";
    }

    /**
     * 返回系统单据类型对象
     *
     * @return 系统单据类型对象
     */
    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0124");
        return sysDocType;
    }

    /**
     * 唯一性校验，不唯一时抛出业务异常
     *
     * @param docEntityVO 投产胎胚施工信息实体
     * @return 唯一性结果
     */
    @Override
    public String checkUnique(MdmConstructionInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmConstructionInfo.notUnique"));
        }
        return unique;
    }

    /**
     * 返回唯一性校验字段列表
     *
     * @return 唯一性校验字段：工厂编码、施工编码、施工版本
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "constructionCode", "constructionVersion"));
    }

    /**
     * 导入数据时的业务校验与数据处理，补充物料编码和MES物料编码
     *
     * @param importDocEntity  导入的实体对象
     * @param importErrorLogs  错误日志列表
     * @param importLogId      导入日志ID
     * @param errorRowNum      错误行号
     * @param serviceCheckParams 业务校验参数
     * @return 校验结果
     */
    @Override
    protected Boolean serviceCheckAndDataHandle(MdmConstructionInfo importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Boolean result = super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
        importDocEntity.setMaterialCode(importDocEntity.getSpecCode());
        importDocEntity.setMesMaterialCode(importDocEntity.getSpecCode());
        return result;
    }

    /**
     * 查询所有投产胎胚施工信息中的胎体布代号列表，去重排序
     *
     * @return 胎体布代号列表
     */
    @Override
    public List<String> listTireFabricCodes() {
        List<MdmConstructionInfo> constructionInfos = mdmConstructionInfoEntityMapper.selectList(null);
        return collectTireFabricCodes(constructionInfos);
    }

    /**
     * 查询所有投产胎胚施工信息中的帘布规格列表，去重排序
     *
     * @return 帘布规格列表
     */
    @Override
    public List<String> listCordSpecs() {
        List<MdmConstructionInfo> constructionInfos = mdmConstructionInfoEntityMapper.selectList(null);
        if (constructionInfos == null || constructionInfos.isEmpty()) {
            return new ArrayList<>();
        }
        return constructionInfos.stream()
                .map(MdmConstructionInfo::getCordSpec)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new))
                .stream()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询投产胎胚施工中的钢压大卷规格（ARTICLE_CROWN_SPEC）列表，去重排序
     *
     * @return 钢压大卷规格列表
     */
    @Override
    public List<String> listArticleCrownSpecs() {
        List<MdmConstructionInfo> constructionInfos = mdmConstructionInfoEntityMapper.selectList(null);
        if (constructionInfos == null || constructionInfos.isEmpty()) {
            return new ArrayList<>();
        }
        return constructionInfos.stream()
                .map(MdmConstructionInfo::getArticleCrownSpec)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new))
                .stream()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询所有投产胎胚施工信息中的钢带代码列表，去重排序
     *
     * @return 钢带代码列表
     */
    @Override
    public List<String> listSteelStripCodes() {
        List<MdmConstructionInfo> constructionInfos = mdmConstructionInfoEntityMapper.selectList(null);
        if (constructionInfos == null || constructionInfos.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> codeSet = new TreeSet<>();
        Method[] methods = MdmConstructionInfo.class.getMethods();
        constructionInfos.forEach(constructionInfo -> {
            Arrays.stream(methods)
                    .filter(method -> BELT_CODE_GETTER.matcher(method.getName()).matches())
                    .forEach(method -> collectCode(codeSet, constructionInfo, method, "读取投产胎胚施工钢带代码失败，方法：{}"));
            Optional.ofNullable(constructionInfo.getBeltCodeLeftCode()).map(String::trim).filter(s -> !s.isEmpty()).ifPresent(codeSet::add);
            Optional.ofNullable(constructionInfo.getBeltCodeRightCode()).map(String::trim).filter(s -> !s.isEmpty()).ifPresent(codeSet::add);
        });
        return new ArrayList<>(codeSet);
    }

    /**
     * 从施工信息列表中收集所有胎体布代号，通过反射读取动态字段 TireFabricCode1~N，去重排序后返回
     *
     * @param constructionInfos 投产胎胚施工信息列表
     * @return 去重排序的胎体布代号列表
     */
    List<String> collectTireFabricCodes(List<MdmConstructionInfo> constructionInfos) {
        if (constructionInfos == null || constructionInfos.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> codeSet = new TreeSet<>();
        Method[] methods = MdmConstructionInfo.class.getMethods();
        for (MdmConstructionInfo constructionInfo : constructionInfos) {
            for (Method method : methods) {
                if (!TIRE_FABRIC_CODE_GETTER.matcher(method.getName()).matches()) {
                    continue;
                }
                collectCode(codeSet, constructionInfo, method, "读取投产胎胚施工胎体布代号失败，方法：{}");
            }
        }
        return new ArrayList<>(codeSet);
    }

    /**
     * 通过反射调用 getter 方法获取字段值，非空非空白时加入集合
     *
     * @param codeSet          目标代码集合
     * @param constructionInfo 施工信息实体
     * @param method           反射获取的 getter 方法
     * @param warnMessage      反射调用失败时的告警日志模板
     */
    private void collectCode(Set<String> codeSet, MdmConstructionInfo constructionInfo, Method method, String warnMessage) {
        try {
            Object value = method.invoke(constructionInfo);
            if (value == null) {
                return;
            }
            String code = value.toString().trim();
            if (!code.isEmpty()) {
                codeSet.add(code);
            }
        } catch (ReflectiveOperationException e) {
            log.warn(warnMessage, method.getName(), e);
        }
    }

    /**
     * 异步执行投产胎胚施工信息导入，记录导入耗时和错误日志
     *
     * @param list          待导入数据列表
     * @param updateSupport 是否支持更新
     * @param importLogId   导入日志ID
     * @param importLog     导入日志对象
     * @param beginTime     导入开始时间
     * @param attributes    Servlet请求属性
     */
    @Async
    @Override
    public void importDataAsync(List<MdmConstructionInfo> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
        try {
            RequestContextHolder.setRequestAttributes(attributes, true);

            AjaxResult result = this.importData(list, updateSupport, importLogId);
            Date endTime = DateUtils.getNowDate();
            importLog.setRowCount(list.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
            RemoteImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, iRemoteImportLogService);
            RemoteImportExcelUtils.saveImportErrorLogs(result, iRemoteImportErrorLogService);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
