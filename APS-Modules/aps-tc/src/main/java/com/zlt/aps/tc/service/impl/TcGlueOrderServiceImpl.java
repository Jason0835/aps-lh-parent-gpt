package com.zlt.aps.tc.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tc.api.domain.dto.TcGlueGroupOrderDto;
import com.zlt.aps.tc.api.domain.dto.TcGlueOrderDto;
import com.zlt.aps.tc.entity.TcGlueOrder;
import com.zlt.aps.tc.mapper.TcGlueGroupOrderMapper;
import com.zlt.aps.tc.mapper.TcGlueOrderMapper;
import com.zlt.aps.tc.service.TcGlueOrderService;

/**
 * <p>
 * 胎侧胶料顺序维护 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
@Service
public class TcGlueOrderServiceImpl extends ServiceImpl<TcGlueOrderMapper, TcGlueOrder> implements TcGlueOrderService {

    @Resource
    private TcGlueOrderMapper tcGlueOrderMapper;

    @Resource
    private TcGlueGroupOrderMapper tcGlueGroupOrderMapper;

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    public List<TcGlueOrderDto> listGlueOrder(TcGlueOrderDto dto) {
        return tcGlueOrderMapper.listGlueOrder(dto);
    }

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveGlueOrder(TcGlueOrder entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 根据胶料code判断胶料组号是否已经存在
     */
    public String checkGlueCodeUnique(TcGlueOrderDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getGlueCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<TcGlueOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("GLUE_CODE", dto.getGlueCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<TcGlueOrder> list = tcGlueOrderMapper.selectList(queryWrapper);
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
        LambdaUpdateWrapper<TcGlueOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApsBaseEntity::getId, Arrays.asList(ids));
        wrapper.set(ApsBaseEntity::getDelFlag, null);
        wrapper.set(ApsBaseEntity::getUpdateBy, SecurityUtils.getUsername());
        wrapper.set(ApsBaseEntity::getUpdateTime, new Date());
        super.getBaseMapper().update(null, wrapper);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<TcGlueOrderDto> list, boolean updateSupport, Long importLogId) {

        //初始化值准备
        int successNum = 0;
        int failureNum = 0;
        List<TcGlueOrderDto> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TcGlueGroupOrderDto> glueGroupOrderList = tcGlueGroupOrderMapper.listGlueGroupOrder(new TcGlueGroupOrderDto());
        if (CollectionUtils.isEmpty(glueGroupOrderList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.glueGroupIsNull");
            ImportUtil.addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        Map<String, Long> glueGroupOrderMap = new HashMap<>();
        glueGroupOrderList.forEach(a -> glueGroupOrderMap.put(a.getGlueGroupCode(), a.getId()));

        //校验（非空校验、长度校验等）

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getGlueCode(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TcGlueOrderDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getGlueCode());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.glueOrder.column.glueCode");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            String glueGroupCode = dto.getGlueGroupCode();
            Long glueGroupId = glueGroupOrderMap.get(glueGroupCode);
            if (glueGroupId == null && StringUtils.isNotBlank(glueGroupCode)) {
                String errorMsg = I18nUtil.getMessage("ui.error.message.column.glueGroupNotExist");
                ImportUtil.addImportErrorLog(importLogId, i + 2, errorMsg, validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                dto.setId(-999L);
                importErrorLogs.addAll(validated);
            } else {
                dto.setBaseVale(null);
                dto.setGlueGroupId(glueGroupId);
                newList.add(dto);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                successNum = newList.size();
                tcGlueOrderMapper.mergeSql(newList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TcGlueOrderDto entity = list.get(i);
                    // 错误跳过
                    if (entity.getId() != null && entity.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    QueryWrapper<TcGlueOrder> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("GLUE_CODE", entity.getGlueCode());
                    queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
                    List<TcGlueOrder> exist = tcGlueOrderMapper.selectList(queryWrapper);
                    if (CollectionUtils.isEmpty(exist)) {
                        successNum++;
                        TcGlueOrder glueOrder = new TcGlueOrder();
                        BeanUtils.copyProperties(entity, glueOrder);
                        this.saveOrUpdate(glueOrder);
                    } else {
                        failureNum++;
                        ImportUtil.addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 执行sql失败，插入导入失败记录
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportUtil.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }

        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
