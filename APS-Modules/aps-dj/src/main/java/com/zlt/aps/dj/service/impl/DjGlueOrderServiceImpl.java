package com.zlt.aps.dj.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.dj.api.domain.dto.DjGlueGroupOrderDto;
import com.zlt.aps.dj.api.domain.dto.DjGlueOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueOrder;
import com.zlt.aps.dj.mapper.DjGlueOrderMapper;
import com.zlt.aps.dj.service.DjGlueGroupOrderService;
import com.zlt.aps.dj.service.DjGlueOrderService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.StringUtil;

/**
 * <p>
 * 垫胶胶料顺序维护 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
@Service
public class DjGlueOrderServiceImpl extends AbstractDocService<DjGlueOrder> implements DjGlueOrderService {

    @Resource
    private DjGlueOrderMapper NcGlueOrderMapper;
    @Autowired
    private DjGlueGroupOrderService ncGlueGroupOrderService;

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    public List<DjGlueOrderDto> listGlueOrder(DjGlueOrderDto dto) {
        return NcGlueOrderMapper.listGlueOrder(dto);
    }

    /**
     * 根据胶料code判断胶料组号是否已经存在
     */
    public String checkGlueCodeUnique(DjGlueOrderDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getGlueCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<DjGlueOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("GLUE_CODE", dto.getGlueCode());
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());
        }
        List<DjGlueOrder> list = NcGlueOrderMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<DjGlueOrder> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<DjGlueOrderDto> importList = new ArrayList<>();
        List<DjGlueGroupOrderDto> glueGroupOrderList = ncGlueGroupOrderService.listGlueGroupOrder(new DjGlueGroupOrderDto());
        if (CollectionUtils.isEmpty(glueGroupOrderList)) {
            // 未查询到胶料组别信息
            String message = I18nUtil.getMessage("ui.error.message.column.glueGroupIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> glueGroupOrderMap = glueGroupOrderList.stream().collect(Collectors.toMap(DjGlueGroupOrderDto::getGlueGroupCode, DjGlueGroupOrderDto::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getGlueCode(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            DjGlueOrder glueOrder = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(glueOrder.getGlueCode());
            if (hasValue > 1) {
                failureNum++;
                glueOrder.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.glueOrder.column.glueCode");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueOrder);
            String glueGroupCode = glueOrder.getGlueGroupId() != null ? "" : "";
            Long glueGroupOrderId = glueGroupOrderMap.get(glueGroupCode);
            if (glueGroupOrderId == null && !StringUtil.isEmpty(glueGroupCode)) {
                // 非法Code
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.glueGroupNotExist"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                glueOrder.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                glueOrder.setId(null);
                DjGlueOrderDto dto = new DjGlueOrderDto();
                BeanUtils.copyProperties(glueOrder, dto);
                dto.setGlueGroupId(glueGroupOrderId);
                importList.add(dto);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                NcGlueOrderMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    DjGlueOrder excelItem = list.get(i);
                    // 跳过错误记录
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 拼接DTO用于唯一性校验
                    DjGlueOrderDto dto = new DjGlueOrderDto();
                    BeanUtils.copyProperties(excelItem, dto);
                    // 唯一性校验
                    String unique = checkGlueCodeUnique(dto);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        //不存在插入
                        successNum++;
                        DjGlueOrder tmGlueOrder = new DjGlueOrder();
                        BeanUtils.copyProperties(excelItem, tmGlueOrder);
                        NcGlueOrderMapper.insert(tmGlueOrder);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.glueOrder.unique"), importErrorLogs);
                    }
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

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
