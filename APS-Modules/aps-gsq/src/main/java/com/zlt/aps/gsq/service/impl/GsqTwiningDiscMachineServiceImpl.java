package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscMachine;
import com.zlt.aps.gsq.mapper.GsqMachineInfoMapper;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMachineMapper;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMapper;
import com.zlt.aps.gsq.service.IGsqTwiningDiscMachineService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 钢丝圈缠绕盘-机台关系Service实现
 * <p>维护缠绕盘可安装使用的机台清单，保存前校验缠绕盘/机台存在性及组合唯一性</p>
 *
 * @author zlt
 * @date 2026-08-20
 */
@Slf4j
@Service
public class GsqTwiningDiscMachineServiceImpl extends AbstractDocService<GsqTwiningDiscMachine>
        implements IGsqTwiningDiscMachineService {

    @Resource
    private GsqTwiningDiscMachineMapper gsqTwiningDiscMachineMapper;

    @Resource
    private GsqTwiningDiscMapper gsqTwiningDiscMapper;

    @Resource
    private GsqMachineInfoMapper gsqMachineInfoMapper;

    /**
     * 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "GSQ_TWINING_DISC_MACHINE";
    }

    /**
     * 唯一性校验字段：缠绕盘编码+机台编号组合（组合唯一由checkUnique自定义实现）
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return java.util.Arrays.asList("twiningDiscCode", "machineCode");
    }

    /**
     * 校验缠绕盘+机台组合唯一性
     *
     * @param entity 实体（缠绕盘编码+机台编号）
     * @return UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一
     */
    @Override
    public String checkUnique(GsqTwiningDiscMachine entity) {
        LambdaQueryWrapper<GsqTwiningDiscMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(entity.getId() != null, GsqTwiningDiscMachine::getId, entity.getId());
        wrapper.eq(GsqTwiningDiscMachine::getTwiningDiscCode, entity.getTwiningDiscCode());
        wrapper.eq(GsqTwiningDiscMachine::getMachineCode, entity.getMachineCode());
        if (gsqTwiningDiscMachineMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 保存缠绕盘-机台关系（带业务校验）
     * 校验规则：
     * 1. 缠绕盘编码必须存在于缠绕盘主表（未逻辑删除）；
     * 2. 机台编号必须存在于机台信息表（未逻辑删除）；
     * 3. 缠绕盘+机台组合唯一；
     * 4. 状态为空默认启用'0'，数据来源为空默认'1'（手工维护），工厂代码为空时继承缠绕盘主表的工厂代码
     *
     * @param entity 实体
     * @return 操作结果
     */
    @Override
    public AjaxResult saveWithCheck(GsqTwiningDiscMachine entity) {
        // 1. 缠绕盘编码存在性校验
        LambdaQueryWrapper<GsqTwiningDisc> discWrapper = new LambdaQueryWrapper<>();
        discWrapper.eq(GsqTwiningDisc::getTwiningDiscCode, entity.getTwiningDiscCode());
        GsqTwiningDisc disc = gsqTwiningDiscMapper.selectOne(discWrapper);
        if (disc == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.discMachine.discNotExists"));
        }
        // 2. 机台编号存在性校验
        LambdaQueryWrapper<GsqMachineInfo> machineWrapper = new LambdaQueryWrapper<>();
        machineWrapper.eq(GsqMachineInfo::getMachineCode, entity.getMachineCode());
        GsqMachineInfo machine = gsqMachineInfoMapper.selectOne(machineWrapper);
        if (machine == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.discMachine.machineNotExists"));
        }
        // 3. 组合唯一性校验
        if (UserConstants.NOT_UNIQUE.equals(this.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.discMachine.conflict"));
        }
        // 4. 补充默认值：状态默认启用，数据来源默认手工，工厂代码为空时继承缠绕盘主表
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
        // 5. 设置基础字段后保存（id为空新增，id不为空更新，由框架baseDao.save内部判断）
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
     * 按缠绕盘编码查询机台关系列表（含反显字段）
     *
     * @param twiningDiscCode 缠绕盘编码
     * @return 机台关系列表
     */
    @Override
    public List<GsqTwiningDiscMachine> listByDiscCode(String twiningDiscCode) {
        GsqTwiningDiscMachine query = new GsqTwiningDiscMachine();
        query.setTwiningDiscCode(twiningDiscCode);
        return gsqTwiningDiscMachineMapper.listDiscMachine(query);
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
