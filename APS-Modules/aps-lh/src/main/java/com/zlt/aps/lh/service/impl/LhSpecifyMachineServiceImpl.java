package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.aps.lh.mapper.LhSpecifyMachineEntityMapper;
import com.zlt.aps.lh.service.ILhSpecifyMachineService;
import com.zlt.aps.maindata.enums.SystemBaseEnums;
import com.zlt.aps.maindata.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhSpecifyMachineServiceImpl.java
 * 描    述：LhSpecifyMachineServiceImpl硫化定点机台信息业务层处理
 *@author zlt
 *@date 2025-03-06
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
public class LhSpecifyMachineServiceImpl extends AbstractDocService<LhSpecifyMachine>  implements ILhSpecifyMachineService {

    @Resource
    private LhSpecifyMachineEntityMapper lhSpecifyMachineEntityMapper;

    @Autowired
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0112";
    }

    /**
     * 根据工厂编号和规格代码查询
     * @param factoryCode
     * @param specCodes
     * @return
     */
    @Override
    public List<LhSpecifyMachine> queryByFactoryCodeAndSpecCodes(String factoryCode, Set<String> specCodes){
        // 将 Set 转换为 List，便于切分批次
        List<String> codeList = new ArrayList<>(specCodes);
        //定义最终返回的List
        List<LhSpecifyMachine> finalList  = new ArrayList<>();
        //判断集合的长度是多少 如果超过900条则进行切分查询
        if (codeList.size() > SystemBaseEnums.SPLIT_LENGTH.getCode()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(codeList, SystemBaseEnums.SPLIT_LENGTH.getCode());
            //将多次查询的结果汇总到finalList中
            for (List<String> splitItemList : splitList) {
                List<LhSpecifyMachine> queryList = lhSpecifyMachineEntityMapper.queryByFactoryCodeAndSpecCodes(factoryCode, splitItemList);
                finalList.addAll(queryList);
            }
        }else{
            finalList = lhSpecifyMachineEntityMapper.queryByFactoryCodeAndSpecCodes(factoryCode, codeList);
        }
        return finalList;
    }

    /**
     * 导入数据
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    @Override
    public AjaxResult importData(List<LhSpecifyMachine> list, boolean updateSupport, Long importLogId) {
        //0.初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhSpecifyMachine> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        //1.进行非空校验,Excel中数据重复校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhSpecifyMachine docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        //2.进行数据库唯一性校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhSpecifyMachine docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)){
                docEntity.setRowState(RowStateEnum.ADDED);
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                }
                importList.add(docEntity);
            }else {
                //todo 如果是存在则更新,则需要自行实现
                failureNum++;
                //数据库已经存在,不允许插入
                ImportExcelValidatedUtils.addImportErrorLog(importLogId,errorNum,
                        String.format(uniqueMsg,errorNum), importErrorLogs);
            }
        }

        if (PubUtil.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = baseDao.saveBatch(importList);

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 校验唯一性
     */
    @Override
    public String checkUnique(LhSpecifyMachine docEntityVO) {
        // 唯一性判断依据: 根据业务修改
        QueryWrapper<LhSpecifyMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getFieldValueByFieldName("id")), "ID", docEntityVO.getFieldValueByFieldName("id"));
        //校验维度 1.排程日期 硫化机台编号 物料号 规格号 是否交期
        queryWrapper.eq("SPEC_CODE", docEntityVO.getSpecCode());
        queryWrapper.eq("MACHINE_CODE", docEntityVO.getMachineCode());
        queryWrapper.eq("IS_DELETE", ApsConstant.DEL_FLAG_NORMAL);

        if (lhSpecifyMachineEntityMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0112");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "specCode", "machineCode");
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<LhSpecifyMachine> list, List<LhSpecifyMachine> importList) {
        List<String> machineCodeList = list.stream().map(item -> String.join(",", item.getFactoryCode(), item.getMachineCode())).distinct().collect(Collectors.toList());
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(machineCodeList, 100);
        List<LhMachineInfo> machineInfoList = new ArrayList<>();
        for (List<String> codeList : splitList) {
            LambdaQueryWrapper<LhMachineInfo> wrapper = new LambdaQueryWrapper<LhMachineInfo>()
                    .apply(" CONCAT(FACTORY_CODE, ',', MACHINE_CODE) IN ({0})", String.join(",", codeList));
            machineInfoList.addAll(lhMachineInfoEntityMapper.selectList(wrapper));
        }
        Map<String, LhMachineInfo> machineInfoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(machineInfoList)) {
            machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(item -> String.join(",", item.getFactoryCode(), item.getMachineCode()),
                    Function.identity(), (s1, s2) -> s1));
        }
        serviceCheckParams.put("machineInfoMap", machineInfoMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(LhSpecifyMachine importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (serviceCheckParams.containsKey("machineInfoMap")) {
            Map<String, LhMachineInfo> machineInfoMap = (Map<String, LhMachineInfo>) serviceCheckParams.get("machineInfoMap");
            String mapKey = String.join(",", importDocEntity.getFactoryCode(), importDocEntity.getMachineCode());
            if (!machineInfoMap.containsKey(mapKey)) {
                String message = I18nUtil.getMessage("ui.data.alert.lhSpecifyMachine.machineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, String.format(message, errorRowNum), importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
