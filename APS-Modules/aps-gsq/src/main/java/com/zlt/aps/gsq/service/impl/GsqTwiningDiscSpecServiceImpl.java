package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSpec;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMapper;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscSpecMapper;
import com.zlt.aps.gsq.service.IGsqTwiningDiscSpecService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 钢丝圈缠绕盘-规格关系Service实现
 * <p>维护缠绕盘与钢丝圈规格的对应关系（多对多），保存前校验缠绕盘/钢丝圈存在性及组合唯一性，
 * 数据表T_GSQ_TWINING_DISC_SPEC（与机台关系表对称，统一TWINING_DISC_CODE编码关联）</p>
 *
 * @author zlt
 * @date 2026-08-21
 */
@Slf4j
@Service
public class GsqTwiningDiscSpecServiceImpl extends AbstractDocService<GsqTwiningDiscSpec>
        implements IGsqTwiningDiscSpecService {

    @Resource
    private GsqTwiningDiscSpecMapper gsqTwiningDiscSpecMapper;

    @Resource
    private GsqTwiningDiscMapper gsqTwiningDiscMapper;

    /**
     * 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "GSQ_TWINING_DISC_SPEC";
    }

    /**
     * 唯一性校验字段：缠绕盘编码+钢丝圈编号组合（组合唯一由checkUnique自定义实现）
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return java.util.Arrays.asList("twiningDiscCode", "steelRingCode");
    }

    /**
     * 校验缠绕盘+钢丝圈规格组合唯一性
     *
     * @param entity 实体（缠绕盘编码+钢丝圈编号）
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(GsqTwiningDiscSpec entity) {
        LambdaQueryWrapper<GsqTwiningDiscSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(entity.getId() != null, GsqTwiningDiscSpec::getId, entity.getId());
        wrapper.eq(GsqTwiningDiscSpec::getTwiningDiscCode, entity.getTwiningDiscCode());
        wrapper.eq(GsqTwiningDiscSpec::getSteelRingCode, entity.getSteelRingCode());
        if (gsqTwiningDiscSpecMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 保存缠绕盘-规格关系（带业务校验）
     * 校验规则：
     * 1. 缠绕盘编码必须存在于缠绕盘主表（未逻辑删除）；
     * 2. 钢丝圈编号必须存在于施工信息表（名称为空时按编号反显）；
     * 3. 缠绕盘+钢丝圈组合唯一；
     * 4. 状态为空默认启用'0'，工厂代码为空时继承缠绕盘主表的工厂代码
     *
     * @param entity 实体
     * @return 操作结果
     */
    @Override
    public AjaxResult saveWithCheck(GsqTwiningDiscSpec entity) {
        // 1. 缠绕盘编码存在性校验
        LambdaQueryWrapper<GsqTwiningDisc> discWrapper = new LambdaQueryWrapper<>();
        discWrapper.eq(GsqTwiningDisc::getTwiningDiscCode, entity.getTwiningDiscCode());
        GsqTwiningDisc disc = gsqTwiningDiscMapper.selectOne(discWrapper);
        if (disc == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.discMachine.discNotExists"));
        }
        // 2. 钢丝圈编号存在性校验（同时取名称用于反显）
        List<Map<String, Object>> ringInfoList = gsqTwiningDiscMapper
                .listSteelRingInfoByCodes(java.util.Collections.singletonList(entity.getSteelRingCode()));
        Map<String, String> ringNameMap = ringInfoList.stream()
                .collect(Collectors.toMap(
                        ring -> String.valueOf(ring.get("BEAD_CODE")),
                        ring -> ring.get("BEAD_NAME") == null ? "" : String.valueOf(ring.get("BEAD_NAME")),
                        (v1, v2) -> v1));
        if (!ringNameMap.containsKey(entity.getSteelRingCode())) {
            return AjaxResult.error(String.format(
                    I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.steelRingNotExists"), entity.getSteelRingCode()));
        }
        // 3. 组合唯一性校验
        if (UserConstants.NOT_UNIQUE.equals(this.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.discSpec.conflict"));
        }
        // 4. 钢丝圈名称为空时按编号从施工信息表反显
        if (entity.getSteelRingName() == null || entity.getSteelRingName().isEmpty()) {
            entity.setSteelRingName(ringNameMap.get(entity.getSteelRingCode()));
        }
        // 5. 补充默认值：状态默认启用、数据来源默认手工维护，工厂代码为空时继承缠绕盘主表
        if (entity.getStatus() == null || entity.getStatus().isEmpty()) {
            entity.setStatus("0");
        }
        if (entity.getDataSource() == null || entity.getDataSource().isEmpty()) {
            // 字典lh_precision_data_source：0-MES同步，1-手工维护（页面新增默认手工）
            entity.setDataSource("1");
        }
        if (entity.getFactoryCode() == null || entity.getFactoryCode().isEmpty()) {
            entity.setFactoryCode(disc.getFactoryCode());
        }
        // 6. 设置基础字段后保存（id为空新增，id不为空更新，由框架baseDao.save内部判断）
        boolean isNew = entity.getId() == null;
        String username = this.getCurrentUsername();
        if (isNew) {
            entity.setCreateBy(username);
            entity.setCreateTime(new Date());
        } else {
            entity.setUpdateBy(username);
            entity.setUpdateTime(new Date());
        }
        this.save(entity);
        return AjaxResult.success();
    }

    /**
     * 主表反显公式（列表SQL已join反显，无需公式反显）
     */
    @Override
    public String[] getQueryFormulas() {
        return new String[]{};
    }

    /**
     * 获取当前登录用户名，获取失败时降级为system
     * （兼容Feign调用等无登录上下文场景）
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
}
