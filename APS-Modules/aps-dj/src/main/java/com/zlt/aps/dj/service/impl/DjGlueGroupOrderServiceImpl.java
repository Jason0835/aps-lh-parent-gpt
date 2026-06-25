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
import com.zlt.aps.dj.api.domain.entity.DjGlueGroupOrder;
import com.zlt.aps.dj.mapper.DjGlueGroupOrderMapper;
import com.zlt.aps.dj.service.DjGlueGroupOrderService;
import com.zlt.bill.common.service.AbstractDocService;


/**
 * <p>
 * 垫胶胶料组别顺序维护 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
@Service
public class DjGlueGroupOrderServiceImpl extends AbstractDocService<DjGlueGroupOrder> implements DjGlueGroupOrderService {

    @Resource
    private DjGlueGroupOrderMapper ncGlueGroupOrderMapper;

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    public List<DjGlueGroupOrderDto> listGlueGroupOrder(DjGlueGroupOrderDto dto) {
        return ncGlueGroupOrderMapper.listGlueGroupOrder(dto);
    }

    /**
     * 根据code判断胶料组号是否已经存在
     */
    public String checkGlueGroupCodeUnique(DjGlueGroupOrderDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getGlueGroupCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<DjGlueGroupOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("GLUE_GROUP_CODE", dto.getGlueGroupCode());
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());
        }
        List<DjGlueGroupOrder> list = ncGlueGroupOrderMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 查询出被使用了的胶料组别
     *
     * @param glueGroupIds
     * @return
     */
    public List<String> listUserdGlueGroup(List<Long> glueGroupIds) {
        return ncGlueGroupOrderMapper.listUserdGlueGroup(glueGroupIds);
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
    public AjaxResult importData(List<DjGlueGroupOrder> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<DjGlueGroupOrderDto> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getGlueGroupCode(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            DjGlueGroupOrder glueGroupOrder = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(glueGroupOrder.getGlueGroupCode());
            if (hasValue > 1) {
                failureNum++;
                glueGroupOrder.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.glueGroup.column.glueGroupCode");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueGroupOrder);
            if (CollectionUtils.isEmpty(validated)) {
                glueGroupOrder.setId(null);
                importList.add(BeanUtils.instantiateClass(DjGlueGroupOrderDto.class));
                BeanUtils.copyProperties(glueGroupOrder, importList.get(importList.size() - 1));
            } else {
                failureNum++;
                glueGroupOrder.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                ncGlueGroupOrderMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    DjGlueGroupOrder excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 拼接DTO用于唯一性校验
                    DjGlueGroupOrderDto dto = new DjGlueGroupOrderDto();
                    BeanUtils.copyProperties(excelItem, dto);
                    // 唯一性校验
                    String unique = checkGlueGroupCodeUnique(dto);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        //不存在插入
                        successNum++;
                        DjGlueGroupOrder glueGroupOrder = new DjGlueGroupOrder();
                        BeanUtils.copyProperties(excelItem, glueGroupOrder);
                        ncGlueGroupOrderMapper.insert(glueGroupOrder);
                    } else {
                        // 存在，插入错误详细日志
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.glueGroupOrder.unique"), importErrorLogs);
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
