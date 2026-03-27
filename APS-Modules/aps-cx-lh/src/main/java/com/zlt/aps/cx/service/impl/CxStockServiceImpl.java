package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.mapper.entity.CxStockEntityMapper;
import com.zlt.aps.cx.service.ICxStockService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxStock;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxStockServiceImpl.java
 * 描    述：CxStockServiceImpl成型库存信息业务层处理
 *@author zlt
 *@date 2025-02-17
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
public class CxStockServiceImpl extends AbstractDocService<CxStock>  implements ICxStockService {

    @Autowired
    private CxStockEntityMapper cxStockEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "9002CX";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("9002CX");
        return sysDocType;
    }

    @Override
    public String checkUnique(CxStock docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.cxStock.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("stockDate", "embryoCode");
    }

    /**
     * 根据日期查询库存
     * @param scheduleDate 查询的库存日期
     * @return List<CxStock> 有效库存列表
     */
    @Override
    public List<CxStock> queryStockByDate(Date scheduleDate) {
        // 获取日期字符串（格式：yyyy-MM-dd）
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String scheduleDateStr = dateFormat.format(scheduleDate);

        // 构建 MyBatis Plus 查询条件
        QueryWrapper<CxStock> queryWrapper = new QueryWrapper<>();
        queryWrapper
                // 匹配今日日期
                .eq("STOCK_DATE", scheduleDateStr);

        // 执行查询
        List<CxStock> scheduleDateStocks = cxStockEntityMapper.selectList(queryWrapper);

        if (scheduleDateStocks == null || scheduleDateStocks.isEmpty()) {
            return Collections.emptyList();
        }
        // 处理查询结果
        return scheduleDateStocks;
    }

    /**
     * 根据日期查询库存
     * @param scheduleDate 查询的库存日期
     * @param embryo 生胎
     * @return List<CxStock> 有效库存列表
     */
    @Override
    public CxStock queryStockByEmbryo(Date scheduleDate,String embryo) {
        // 获取日期字符串（格式：yyyy-MM-dd）
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String scheduleDateStr = dateFormat.format(scheduleDate);

        // 构建 MyBatis Plus 查询条件
        QueryWrapper<CxStock> queryWrapper = new QueryWrapper<>();
        queryWrapper
                // 匹配今日日期
                .eq("STOCK_DATE", scheduleDateStr);
        queryWrapper
                // 匹配胎胚号
                .eq("EMBRYO_CODE", embryo);

        // 执行查询
        return cxStockEntityMapper.selectOne(queryWrapper);
    }

    @Override
    public AjaxResult importData(List<CxStock> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxStock> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockDate() + a.getEmbryoCode()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            CxStock dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getStockDate() + dto.getEmbryoCode());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stock.stockDate");
                String columnName2 = I18nUtil.getMessage("ui.data.column.stock.embryoCode");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {

                Long stockNum = dto.getStockNum() == null ? 0L : dto.getStockNum();
                Long modifyNum = dto.getModifyNum() == null ? 0L : dto.getModifyNum();
                long badNum = dto.getBadNum() == null ? 0L : Long.valueOf(dto.getBadNum());
                long dd = stockNum + modifyNum - badNum;
                if (dd < 0) {
                    failureNum++;
                    dto.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.stock.stockNumValidate"), importErrorLogs);
                    continue;
                }
                //校验胎胚版本是否存在
                //Joran 2021-12-16 与测试沟通确认导入不进行施工版本校验，先让导入进去
                /*CxProductConstructionInfo pc=new CxProductConstructionInfo();
                pc.setEmbryoCode(dto.getEmbryoCode());
                pc.setEmbryoVersion(dto.getBomDataVersion());
                List<CxProductConstructionInfo> pcList= cxProductConstructionInfoMapper.selectCxProductConstructionInfoList(pc);
                if (CollectionUtils.isEmpty(pcList)) {
                    failureNum++;
                    dto.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.productConstruction.errorEmbryoVersion"), importErrorLogs);
                    continue;
                }*/
                //Joran 2021-12-16 与测试沟通确认导入不进行施工版本校验，先让导入进去

                dto.setBaseVale(null);
                newList.add(dto);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
//                    cxStockEntityMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxStock newItem = list.get(i);
                        //过滤错误的记录
                        if (newItem.getId() != null && newItem.getId() == -999L) {
                            continue;
                        }
                        String unique = this.checkUnique(newItem);
                        if (UserConstants.UNIQUE.equals(unique)) {
                            successNum++;
                            cxStockEntityMapper.insert(newItem);
                        } else {
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                        }

                    }
                }
            } catch (Exception e) {
                log.error("导入数据失败：{}", e.getMessage());
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

}


