package com.zlt.aps.tc.service.impl;


import com.alibaba.nacos.common.utils.CollectionUtils;
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
import com.zlt.aps.tc.entity.TcGlueGroupOrder;
import com.zlt.aps.tc.mapper.TcGlueGroupOrderMapper;
import com.zlt.aps.tc.service.TcGlueGroupOrderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * <p>
 * 胎侧胶料组别顺序维护 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-25
 */
@Service
public class TcGlueGroupOrderServiceImpl extends ServiceImpl<TcGlueGroupOrderMapper, TcGlueGroupOrder> implements TcGlueGroupOrderService {

    @Resource
    private TcGlueGroupOrderMapper tcGlueGroupOrderMapper;

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @return
     */
    public List<TcGlueGroupOrderDto> listGlueGroupOrder(TcGlueGroupOrderDto dto) {
        return tcGlueGroupOrderMapper.listGlueGroupOrder(dto);
    }

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     *
     * @param entity
     */
    public void saveGlueGroupOrder(TcGlueGroupOrder entity) {
        entity.setBaseVale(entity.getId());  //根据id是否为空给创建时间，创建人，更新时间，更新人赋值
        this.saveOrUpdate(entity);
    }

    /**
     * 根据code判断胶料组号是否已经存在
     */
    public String checkGlueGroupCodeUnique(TcGlueGroupOrderDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getGlueGroupCode())) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<TcGlueGroupOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("GLUE_GROUP_CODE", dto.getGlueGroupCode());
        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
        if (dto.getId() != null) {
            queryWrapper.ne("ID", dto.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<TcGlueGroupOrder> list = tcGlueGroupOrderMapper.selectList(queryWrapper);
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
        List<String> usedGlueGroupList = tcGlueGroupOrderMapper.listUserdGlueGroup(glueGroupIds);  //查询出已经被使用的胶料组别
        if (usedGlueGroupList != null && !usedGlueGroupList.isEmpty()) {
            String groupNames = "'" + String.join("'，'", usedGlueGroupList) + "'";
            throw new RuntimeException(groupNames + I18nUtil.getMessage("胶料组别已被使用，禁止删除！"));
        }

        LambdaUpdateWrapper<TcGlueGroupOrder> wrapper = new LambdaUpdateWrapper<>();
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
    public AjaxResult importData(List<TcGlueGroupOrderDto> list, boolean updateSupport, Long importLogId) {

        //初始化值准备
        int successNum = 0;
        int failureNum = 0;
        List<TcGlueGroupOrderDto> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getGlueGroupCode(), Collectors.counting()));

        //校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            TcGlueGroupOrderDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getGlueGroupCode());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.glueGroup.column.glueGroupCode");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> errorLogList = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isEmpty(errorLogList)) {
                dto.setBaseVale(null);
                newList.add(dto);
            } else {
                failureNum++;
                dto.setId(-999L);
                importErrorLogs.addAll(errorLogList);
            }
        }

        //新集合操作（更新或插入操作）
        try {
            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                successNum = newList.size();
                tcGlueGroupOrderMapper.mergeSql(newList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TcGlueGroupOrderDto entity = list.get(i);
                    if (entity.getId() != null && entity.getId().equals(-999L)) {
                        continue;
                    }
                    // 唯一性校验
                    QueryWrapper<TcGlueGroupOrder> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("GLUE_GROUP_CODE", entity.getGlueGroupCode());
                    queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);
                    List<TcGlueGroupOrder> exist = tcGlueGroupOrderMapper.selectList(queryWrapper);
                    if (CollectionUtils.isEmpty(exist)) {
                        successNum++;
                        TcGlueGroupOrder glueGroupOrder = new TcGlueGroupOrder();
                        BeanUtils.copyProperties(entity, glueGroupOrder);
                        this.saveOrUpdate(glueGroupOrder);
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
