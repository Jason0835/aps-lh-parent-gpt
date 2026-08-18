package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSub;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMapper;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscSubMapper;
import com.zlt.aps.gsq.service.IGsqTwiningDiscService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.aps.utils.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 钢丝圈缠绕盘Service实现
 * <p>主子表结构：主表 T_GSQ_TWINING_DISC + 子表 T_GSQ_TWINING_DISC_SUB</p>
 * <p>保存/删除均级联处理子表，保证主子表数据一致性</p>
 *
 * @author zlt
 * @date 2026-07-08
 */
@Slf4j
@Service
public class GsqTwiningDiscServiceImpl extends AbstractDocService<GsqTwiningDisc>
        implements IGsqTwiningDiscService {

    @Resource
    private GsqTwiningDiscMapper gsqTwiningDiscMapper;

    @Resource
    private GsqTwiningDiscSubMapper gsqTwiningDiscSubMapper;

    /**
     * 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "GSQ_TWINING_DISC";
    }

    /**
     * 唯一性校验字段：缠绕盘编码
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return java.util.Arrays.asList("twiningDiscCode");
    }

    /**
     * 校验缠绕盘编码唯一性
     *
     * @param entity 实体
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(GsqTwiningDisc entity) {
        LambdaQueryWrapper<GsqTwiningDisc> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(entity.getId() != null, GsqTwiningDisc::getId, entity.getId());
        wrapper.eq(GsqTwiningDisc::getTwiningDiscCode, entity.getTwiningDiscCode());
        wrapper.eq(GsqTwiningDisc::getIsDelete, "0");
        if (gsqTwiningDiscMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 保存钢丝圈缠绕盘（主表+子表），事务级联保存
     * 1. 唯一性校验（缠绕盘编码）
     * 2. 保存/更新主表
     * 3. 删除旧子表（按主表ID逻辑删除）
     * 4. 保存新子表
     *
     * @param entity 实体（含 subList 子表数据）
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult saveMainAndSub(GsqTwiningDisc entity) {
        // 唯一性校验
        if (UserConstants.NOT_UNIQUE.equals(checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.conflict"));
        }
        // 设置主表基础字段：新增设置createBy/createTime，更新设置updateBy/updateTime
        boolean isNew = entity.getId() == null;
        String username = getCurrentUsername();
        if (isNew) {
            entity.setCreateBy(username);
            entity.setCreateTime(new Date());
        } else {
            entity.setUpdateBy(username);
            entity.setUpdateTime(new Date());
        }
        // 保存主表（id为空新增，id不为空更新，由框架baseDao.save内部判断）
        this.save(entity);
        Long mainId = entity.getId();
        // 删除旧子表（按主表ID，使用LambdaQueryWrapper）
        LambdaQueryWrapper<GsqTwiningDiscSub> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(GsqTwiningDiscSub::getDiscId, mainId);
        gsqTwiningDiscSubMapper.delete(deleteWrapper);
        // 保存新子表（重置主键并关联主表，补充基础字段；isDelete由BaseEntity构造器默认为0）
        List<GsqTwiningDiscSub> subList = entity.getSubList();
        if (CollectionUtils.isNotEmpty(subList)) {
            Date now = new Date();
            for (GsqTwiningDiscSub sub : subList) {
                sub.setId(null);
                sub.setDiscId(mainId);
                sub.setIsDelete(0);
                sub.setCreateBy(username);
                sub.setCreateTime(now);
                gsqTwiningDiscSubMapper.insert(sub);
            }
        }
        return AjaxResult.success();
    }

    /**
     * 删除钢丝圈缠绕盘（删除主表，并级联删除子表）
     *
     * @param ids 主表ID集合
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult removeMainAndSub(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error("未选择删除数据");
        }
        // 级联删除子表（先删子表，使用LambdaQueryWrapper）
        LambdaQueryWrapper<GsqTwiningDiscSub> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.in(GsqTwiningDiscSub::getDiscId, ids);
        gsqTwiningDiscSubMapper.delete(subWrapper);
        // 删除主表
        this.removeByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 获取当前登录用户名，获取失败时降级为system
     * （兼容Feign调用等无登录上下文场景，参照GsqMachineInfoServiceImpl的setBaseFieldValue写法）
     *
     * @return 用户名
     */
    private String getCurrentUsername() {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * 主表反显公式（主表暂无反显字段）
     */
    @Override
    public String[] getQueryFormulas() {
        return new String[]{};
    }

    /**
     * 子表反显公式：钢丝圈名称根据钢丝圈编号从施工信息表反显
     * steelRingName -> 根据 STEEL_RING_CODE 从 T_MDM_CONSTRUCTION_INFO 取 BEAD_NAME
     */
    @Override
    public String[] getSubQueryFormulas() {
        return new String[]{
                "steelRingName -> getcolvalue(T_MDM_CONSTRUCTION_INFO, BEAD_NAME, BEAD_CODE, steelRingCode)"
        };
    }

    /**
     * 根据主表ID查询子表数据并反显钢丝圈名称
     *
     * @param discId 主表ID
     * @return 子表列表（含反显名称）
     */
    @Override
    public List<GsqTwiningDiscSub> querySubListByDiscId(Long discId) {
        LambdaQueryWrapper<GsqTwiningDiscSub> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqTwiningDiscSub::getDiscId, discId);
        wrapper.eq(GsqTwiningDiscSub::getIsDelete, "0");
        wrapper.orderByAsc(GsqTwiningDiscSub::getCreateTime);
        List<GsqTwiningDiscSub> list = gsqTwiningDiscSubMapper.selectList(wrapper);
        // 反显钢丝圈名称
        AppUtils.formatData(list, getSubQueryFormulas());
        return list;
    }

    /**
     * 导入数据，并保存记录
     * 校验规则：缠绕盘编码必填；按编码校验重复
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<GsqTwiningDisc> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqTwiningDisc> importList = new ArrayList<>();

        // 按缠绕盘编码分组，识别文件内重复数据
        Map<String, Long> groupMap = list.stream()
                .filter(a -> PubUtil.isNotEmpty(a.getTwiningDiscCode()))
                .collect(Collectors.groupingBy(GsqTwiningDisc::getTwiningDiscCode, Collectors.counting()));

        // 逐行校验
        for (int i = 0; i < list.size(); i++) {
            GsqTwiningDisc entity = list.get(i);

            // 文件内重复校验
            String code = entity.getTwiningDiscCode();
            if (PubUtil.isNotEmpty(code)) {
                Long hasValue = groupMap.get(code);
                if (hasValue != null && hasValue > 1) {
                    failureNum++;
                    entity.setId(-999L);
                    String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                    String columnName = I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.twiningDiscCode");
                    message = String.format(message, columnName);
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                    continue;
                }
            }

            // 字段格式校验
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, entity);
            if (validated.isEmpty()) {
                importList.add(entity);
            } else {
                failureNum++;
                entity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 保存：updateSupport=true 走 mergeSql（存在则更新）；否则逐条校验唯一后 save
        try {
            if (updateSupport && !importList.isEmpty()) {
                successNum = importList.size();
                gsqTwiningDiscMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    GsqTwiningDisc excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = gsqTwiningDiscMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.conflict"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            log.error("导入钢丝圈缠绕盘异常", e);
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
