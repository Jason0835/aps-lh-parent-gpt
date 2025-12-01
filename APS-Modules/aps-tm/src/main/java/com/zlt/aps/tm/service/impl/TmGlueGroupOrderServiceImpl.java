package com.zlt.aps.tm.service.impl;


import com.alibaba.nacos.common.utils.CollectionUtils;
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
import com.zlt.aps.tm.entity.TmGlueGroupOrder;
import com.zlt.aps.tm.mapper.TmGlueGroupOrderMapper;
import com.zlt.aps.tm.service.TmGlueGroupOrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * <p>
 * 胎面胶料组别顺序维护 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
@Service
public class TmGlueGroupOrderServiceImpl extends ServiceImpl<TmGlueGroupOrderMapper, TmGlueGroupOrder> implements TmGlueGroupOrderService {

    @Resource
    private TmGlueGroupOrderMapper tmGlueGroupOrderMapper;

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    public List<TmGlueGroupOrderDto> listGlueGroupOrder(TmGlueGroupOrderDto dto) {
        return tmGlueGroupOrderMapper.listGlueGroupOrder(dto);
    }

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveGlueGroupOrder(TmGlueGroupOrder entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 根据code判断胶料组号是否已经存在
     */
    public String checkGlueGroupCodeUnique(TmGlueGroupOrderDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getGlueGroupCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<TmGlueGroupOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("GLUE_GROUP_CODE", dto.getGlueGroupCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<TmGlueGroupOrder> list = tmGlueGroupOrderMapper.selectList(queryWrapper);
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
    public void deleteGlueGroupOrder(Long[] ids) {
        List<Long> glueGroupIds = Arrays.asList(ids);
        List<String> usedGlueGroupList = tmGlueGroupOrderMapper.listUserdGlueGroup(glueGroupIds);  //查询出已经被使用的胶料组别
        if (usedGlueGroupList != null && !usedGlueGroupList.isEmpty()) {
            String groupNames = "'" + String.join("'，'", usedGlueGroupList) + "'";
            throw new RuntimeException(groupNames + I18nUtil.getMessage("胶料组别已被使用，禁止删除！"));
        }

        LambdaUpdateWrapper<TmGlueGroupOrder> wrapper = new LambdaUpdateWrapper<>();
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
    public AjaxResult importData(List<TmGlueGroupOrderDto> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmGlueGroupOrderDto> importList = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getGlueGroupCode(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TmGlueGroupOrderDto glueGroupOrder = list.get(i);

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

            List<ImportErrorLog> errorLogList = ImportUtil.validated(importLogId, i + 2, glueGroupOrder);
            if (CollectionUtils.isEmpty(errorLogList)) {
                glueGroupOrder.setBaseVale(null);
                importList.add(glueGroupOrder);
            } else {
                failureNum++;
                glueGroupOrder.setId(-999L);
                importErrorLogs.addAll(errorLogList);
            }
        }

        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tmGlueGroupOrderMapper.mergeSql(importList);
            } else {
                //查询数据库已存在对象
                for (int i = 0; i < list.size(); i++) {
                    TmGlueGroupOrderDto excelItem = list.get(i);
                    // 错误记录跳过
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }

                    // 唯一性校验
                    String unique = checkGlueGroupCodeUnique(excelItem);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        //不存在插入
                        successNum++;
                        TmGlueGroupOrder tmGlueGroupOrder = new TmGlueGroupOrder();
                        BeanUtils.copyProperties(excelItem, tmGlueGroupOrder);
                        tmGlueGroupOrderMapper.insert(tmGlueGroupOrder);
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
}
