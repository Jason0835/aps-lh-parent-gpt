package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tm.api.domain.dto.TmGlueGroupOrderDto;
import com.zlt.aps.tm.api.domain.dto.TmGlueOrderDto;
import com.zlt.aps.tm.entity.TmGlueOrder;
import com.zlt.aps.tm.mapper.TmGlueOrderMapper;
import com.zlt.aps.tm.service.TmGlueGroupOrderService;
import com.zlt.aps.tm.service.TmGlueOrderService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 胎面胶料顺序维护 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
@Service
public class TmGlueOrderServiceImpl extends ServiceImpl<TmGlueOrderMapper, TmGlueOrder> implements TmGlueOrderService {

    @Resource
    private TmGlueOrderMapper tmGlueOrderMapper;
    @Autowired
    private TmGlueGroupOrderService glueGroupOrderService;

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    public List<TmGlueOrderDto> listGlueOrder(TmGlueOrderDto dto) {
        return tmGlueOrderMapper.listGlueOrder(dto);
    }

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveGlueOrder(TmGlueOrder entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 根据胶料code判断胶料号是否已经存在
     */
    public String checkGlueCodeUnique(TmGlueOrderDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getGlueCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<TmGlueOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("GLUE_CODE", dto.getGlueCode());
        queryWrapper.eq("MACHINE_ID", dto.getMachineId());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<TmGlueOrder> list = tmGlueOrderMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 批量删除(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    public void deleteGlueOrder(Long[] ids) {
        LambdaUpdateWrapper<TmGlueOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
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
    public AjaxResult importData(List<TmGlueOrderDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmGlueOrderDto> importList = new ArrayList<>();
        List<TmGlueGroupOrderDto> glueGroupOrderList = glueGroupOrderService.listGlueGroupOrder(new TmGlueGroupOrderDto());
        if (CollectionUtils.isEmpty(glueGroupOrderList)) {
            // 未查询到胶料组别信息
            String message = I18nUtil.getMessage("ui.error.message.column.glueGroupIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> glueGroupOrderMap = glueGroupOrderList.stream().collect(Collectors.toMap(TmGlueGroupOrderDto::getGlueGroupCode, TmGlueGroupOrderDto::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a ->
                String.join(",", a.getMachineId() == null ? "" : a.getMachineName(), a.getGlueCode()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TmGlueOrderDto glueOrder = list.get(i);
            String mapKey = String.join(",", glueOrder.getMachineName() == null ? "" : glueOrder.getMachineName(), glueOrder.getGlueCode());

            //重复记录校验
            Long hasValue = groupMap.get(mapKey);
            if (hasValue > 1) {
                failureNum++;
                glueOrder.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.glueOrder.column.glueCode");
                String columnName1 = I18nUtil.getMessage("ui.data.column.machine.machineName");
                message = String.format(message, columnName + "," + columnName1);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueOrder);
            Long glueGroupOrderId = glueGroupOrderMap.get(glueOrder.getGlueGroupCode());
            if (glueGroupOrderId == null) {
                // 非法Code
                addImportErrorLog(importLogId, i + 2,
                        I18nUtil.getMessage("ui.error.message.column.glueGroupNotExist"), validated);
            }
            // TODO 机台名称转成机台id，保存SQL写入机台ID

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                glueOrder.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                glueOrder.setBaseVale(null);
                glueOrder.setGlueGroupId(glueGroupOrderId);
                importList.add(glueOrder);
            }
        }
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tmGlueOrderMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TmGlueOrderDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    String unique = checkGlueCodeUnique(excelItem);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        //不存在插入
                        successNum++;
                        TmGlueOrder tmGlueOrder = new TmGlueOrder();
                        BeanUtils.copyProperties(excelItem, tmGlueOrder);
                        tmGlueOrderMapper.insert(tmGlueOrder);
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
}
