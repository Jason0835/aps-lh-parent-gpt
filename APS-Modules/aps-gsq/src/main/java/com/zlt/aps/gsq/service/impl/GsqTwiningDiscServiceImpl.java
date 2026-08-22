package com.zlt.aps.gsq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscMachine;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSpec;
import com.zlt.aps.gsq.api.domain.vo.GsqMesTwiningDiscSyncVO;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMachineMapper;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMapper;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscSpecMapper;
import com.zlt.aps.gsq.service.IGsqTwiningDiscService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 钢丝圈缠绕盘Service实现
 * <p>单表管理 T_GSQ_TWINING_DISC（缠绕盘基础信息）；
 * 规格关系（T_GSQ_TWINING_DISC_SPEC）与机台关系（T_GSQ_TWINING_DISC_MACHINE）均按编码关联、独立页面维护，
 * 删除主表时级联逻辑删除两关系表，保证数据一致性</p>
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
    private GsqTwiningDiscSpecMapper gsqTwiningDiscSpecMapper;

    @Resource
    private GsqTwiningDiscMachineMapper gsqTwiningDiscMachineMapper;

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
     * 保存钢丝圈缠绕盘（单表保存，带唯一性校验）
     *
     * @param entity 实体
     * @return 操作结果
     */
    @Override
    public AjaxResult saveWithCheck(GsqTwiningDisc entity) {
        // 唯一性校验
        if (UserConstants.NOT_UNIQUE.equals(checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.conflict"));
        }
        // 数据来源为空默认手工维护'1'（字典lh_precision_data_source：0-MES同步，1-手工；MES同步链路会显式设置'0'）
        if (PubUtil.isEmpty(entity.getDataSource())) {
            entity.setDataSource("1");
        }
        // 设置基础字段：新增设置createBy/createTime，更新设置updateBy/updateTime
        boolean isNew = entity.getId() == null;
        String username = getCurrentUsername();
        if (isNew) {
            entity.setCreateBy(username);
            entity.setCreateTime(new Date());
        } else {
            entity.setUpdateBy(username);
            entity.setUpdateTime(new Date());
        }
        // 保存（id为空新增，id不为空更新，由框架baseDao.save内部判断）
        this.save(entity);
        return AjaxResult.success();
    }

    /**
     * 删除钢丝圈缠绕盘（删除主表，并按缠绕盘编码级联删除规格关系及机台关系）
     * <p>两关系表均按编码关联（无外键ID），删除主表后同步清理，
     * 避免残留孤儿关系数据影响排程机台分配</p>
     *
     * @param ids 主表ID集合
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult removeMainAndRelation(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.discMachine.noSelectData"));
        }
        // 查询待删除缠绕盘的编码集合（用于级联删除两关系表）
        List<GsqTwiningDisc> discList = gsqTwiningDiscMapper.selectBatchIds(ids);
        List<String> discCodes = discList.stream()
                .map(GsqTwiningDisc::getTwiningDiscCode)
                .filter(PubUtil::isNotEmpty)
                .collect(Collectors.toList());
        // 级联删除规格关系（按缠绕盘编码）
        if (CollectionUtils.isNotEmpty(discCodes)) {
            LambdaQueryWrapper<GsqTwiningDiscSpec> subWrapper = new LambdaQueryWrapper<>();
            subWrapper.in(GsqTwiningDiscSpec::getTwiningDiscCode, discCodes);
            gsqTwiningDiscSpecMapper.delete(subWrapper);
            // 级联删除机台关系（按缠绕盘编码）
            LambdaQueryWrapper<GsqTwiningDiscMachine> machineWrapper = new LambdaQueryWrapper<>();
            machineWrapper.in(GsqTwiningDiscMachine::getTwiningDiscCode, discCodes);
            gsqTwiningDiscMachineMapper.delete(machineWrapper);
        }
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
     * 查询施工信息表全部钢丝圈选项（编码+名称，去重），供页面下拉选择使用
     *
     * @return 钢丝圈选项列表（key：BEAD_CODE 钢丝圈编号、BEAD_NAME 钢丝圈名称）
     */
    @Override
    public List<Map<String, Object>> listSteelRingOptions() {
        return gsqTwiningDiscMapper.listSteelRingOptions();
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
                // 导入数据默认数据来源为"1"（手工维护），避免覆盖系统字段
                entity.setDataSource("1");
                importList.add(entity);
            } else {
                failureNum++;
                entity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 保存：updateSupport=true 走查-改-插（存在则更新原记录）；否则逐条校验唯一后 save
        try {
            if (updateSupport && !importList.isEmpty()) {
                // 批量预取已存在缠绕盘（按缠绕盘编码匹配），存在则更新原记录，不存在则新增
                Map<String, GsqTwiningDisc> existingMap = this.loadExistingDiscMap(importList);
                for (GsqTwiningDisc excelItem : importList) {
                    GsqTwiningDisc existing = existingMap.get(excelItem.getTwiningDiscCode());
                    if (existing != null) {
                        // 已存在：回填主键ID，清空新增审计字段避免覆盖原记录创建信息，保留原数据来源，补齐更新审计字段后更新
                        excelItem.setId(existing.getId());
                        excelItem.setCreateBy(null);
                        excelItem.setCreateTime(null);
                        // 保留原数据来源（导入不覆盖系统字段）
                        excelItem.setDataSource(existing.getDataSource());
                        this.setUpdateAuditFields(excelItem);
                        gsqTwiningDiscMapper.updateById(excelItem);
                    } else {
                        // 不存在：补齐新增审计字段后插入（setInsertAuditFields内会设置dataSource默认值）
                        this.setInsertAuditFields(excelItem);
                        baseDao.save(excelItem);
                    }
                    successNum++;
                }
            } else {
                for (int i = 0; i < list.size(); i++) {
                    GsqTwiningDisc excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    int unique = gsqTwiningDiscMapper.checkUnique(excelItem);
                    if (unique == 0) {
                        // 新增：补齐审计字段（含dataSource默认值）
                        this.setInsertAuditFields(excelItem);
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

    /**
     * 批量预取已存在的缠绕盘数据（导入更新模式使用）
     * 按缠绕盘编码批量查询数据库已有记录，逻辑删除由框架自动过滤
     *
     * @param importList 导入数据列表
     * @return 缠绕盘编码 -> 已存在缠绕盘记录 的映射
     */
    private Map<String, GsqTwiningDisc> loadExistingDiscMap(List<GsqTwiningDisc> importList) {
        // 提取非空缠绕盘编码并去重
        List<String> codeList = importList.stream()
                .map(GsqTwiningDisc::getTwiningDiscCode)
                .filter(PubUtil::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(codeList)) {
            return new HashMap<>();
        }
        // 按1000条一批查询，避免in条件超长；同编码多条时保留首条
        return CollUtil.split(codeList, 1000).stream()
                .flatMap(batch -> gsqTwiningDiscMapper.selectList(new LambdaQueryWrapper<GsqTwiningDisc>()
                        .in(GsqTwiningDisc::getTwiningDiscCode, batch)).stream())
                .collect(Collectors.toMap(GsqTwiningDisc::getTwiningDiscCode, Function.identity(), (oldValue, newValue) -> oldValue));
    }

    /**
     * 设置导入更新模式的更新审计字段（updateBy/updateTime）
     * 无登录上下文时回退为system
     *
     * @param entity 实体对象
     */
    private void setUpdateAuditFields(GsqTwiningDisc entity) {
        try {
            entity.setUpdateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            entity.setUpdateBy("system");
        }
        entity.setUpdateTime(new Date());
    }

    /**
     * 设置导入新增模式的审计字段（isDelete/createBy/createTime/updateBy/updateTime/dataSource）
     * 无登录上下文时回退为system
     *
     * @param entity 实体对象
     */
    private void setInsertAuditFields(GsqTwiningDisc entity) {
        entity.setIsDelete(0);
        // 导入新增数据默认数据来源为"1"（手工维护）
        if (PubUtil.isEmpty(entity.getDataSource())) {
            entity.setDataSource("1");
        }
        try {
            entity.setCreateBy(SecurityUtils.getUsername());
        } catch (Exception e) {
            entity.setCreateBy("system");
        }
        entity.setCreateTime(new Date());
        this.setUpdateAuditFields(entity);
    }

    /**
     * MES缠绕盘三表同步落库（事务性操作，供GsqMesSyncController远程调用）
     * <p>单事务处理缠绕盘清单/规格关系/机台关系，保证三表一致性：</p>
     * <p>1. 主表UPSERT：按缠绕盘编码分流，存在则仅更新MES字段（英寸/排列方式/状态/工厂/版本/来源，
     * 保留名称/数量/备注等手工维护字段），不存在则批量插入（名称默认取编码，XML显式列绕过MetaObjectHandler）；</p>
     * <p>2. 主表清理：APS中MES来源但MES最新清单已不存在的缠绕盘逻辑删除，并级联逻辑删除规格关系/机台关系；</p>
     * <p>3. 规格关系UPSERT：按缠绕盘编码+钢丝圈编号组合分流更新/插入（名称反显自施工信息表），
     * MES来源已失效的组合逻辑删除；</p>
     * <p>4. 机台关系UPSERT：按缠绕盘编码+机台编号组合分流更新/插入，MES来源已失效的组合逻辑删除</p>
     *
     * @param syncVO   MES三表聚合数据
     * @param updateBy 更新者（MES同步传"MES"）
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult syncFromMes(GsqMesTwiningDiscSyncVO syncVO, String updateBy) {
        if (syncVO == null) {
            return AjaxResult.error("同步数据为空");
        }
        List<GsqTwiningDisc> discList = syncVO.getDiscList() == null ? new ArrayList<>() : syncVO.getDiscList();
        List<GsqTwiningDiscSpec> specList = syncVO.getSpecList() == null ? new ArrayList<>() : syncVO.getSpecList();
        List<GsqTwiningDiscMachine> machineList = syncVO.getMachineList() == null ? new ArrayList<>() : syncVO.getMachineList();

        // MES三表全部为空视为异常快照（主数据全量同步出现空清单会导致误清空APS数据，防御性跳过）
        if (discList.isEmpty() && specList.isEmpty() && machineList.isEmpty()) {
            log.warn("缠绕盘MES同步：MES三表均无数据，本次跳过同步，避免误清空APS现有数据");
            return AjaxResult.success("MES无数据可同步");
        }

        Date now = new Date();

        // ===== 1. 主表UPSERT：查询APS现有全部未删除缠绕盘（含手工/MES来源），按编码索引 =====
        LambdaQueryWrapper<GsqTwiningDisc> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(GsqTwiningDisc::getIsDelete, 0);
        Map<String, GsqTwiningDisc> existDiscMap = gsqTwiningDiscMapper.selectList(existWrapper).stream()
                .filter(disc -> PubUtil.isNotEmpty(disc.getTwiningDiscCode()))
                .collect(Collectors.toMap(GsqTwiningDisc::getTwiningDiscCode, Function.identity(), (v1, v2) -> v1));

        Set<String> mesDiscCodes = new HashSet<>();
        List<GsqTwiningDisc> discInsertList = new ArrayList<>();
        for (GsqTwiningDisc mes : discList) {
            if (PubUtil.isEmpty(mes.getTwiningDiscCode())) {
                continue;
            }
            mesDiscCodes.add(mes.getTwiningDiscCode());
            GsqTwiningDisc exist = existDiscMap.get(mes.getTwiningDiscCode());
            if (exist != null) {
                // 已存在：仅更新MES维护字段，保留名称/数量/备注等手工维护字段
                LambdaUpdateWrapper<GsqTwiningDisc> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(GsqTwiningDisc::getId, exist.getId())
                        .set(GsqTwiningDisc::getProSize, mes.getProSize())
                        .set(GsqTwiningDisc::getSortType, mes.getSortType())
                        .set(GsqTwiningDisc::getStatus, PubUtil.isEmpty(mes.getStatus()) ? "0" : mes.getStatus())
                        .set(GsqTwiningDisc::getFactoryCode, mes.getFactoryCode())
                        .set(GsqTwiningDisc::getDataVersion, mes.getDataVersion())
                        .set(GsqTwiningDisc::getDataSource, "0")
                        .set(GsqTwiningDisc::getUpdateBy, updateBy)
                        .set(GsqTwiningDisc::getUpdateTime, now);
                gsqTwiningDiscMapper.update(null, updateWrapper);
            } else {
                // 不存在：组装新增记录（MES无名称字段，名称默认取编码，可由用户后续手工补充）
                mes.setTwiningDiscName(PubUtil.isNotEmpty(mes.getTwiningDiscName()) ? mes.getTwiningDiscName() : mes.getTwiningDiscCode());
                mes.setStatus(PubUtil.isEmpty(mes.getStatus()) ? "0" : mes.getStatus());
                mes.setDataSource("0");
                mes.setIsDelete(0);
                mes.setCreateBy(updateBy);
                mes.setUpdateBy(updateBy);
                discInsertList.add(mes);
            }
        }
        if (!discInsertList.isEmpty()) {
            gsqTwiningDiscMapper.batchInsertMesDisc(discInsertList);
        }

        // ===== 2. 主表清理：MES来源但MES最新清单已不存在的缠绕盘，逻辑删除并级联清理 =====
        List<Long> deleteDiscIds = new ArrayList<>();
        List<String> deleteDiscCodes = new ArrayList<>();
        for (GsqTwiningDisc exist : existDiscMap.values()) {
            // 仅清理MES来源（字典lh_precision_data_source：0-MES同步）且MES最新清单已不存在的缠绕盘，手工数据（1）保留
            if ("0".equals(exist.getDataSource()) && !mesDiscCodes.contains(exist.getTwiningDiscCode())) {
                deleteDiscIds.add(exist.getId());
                deleteDiscCodes.add(exist.getTwiningDiscCode());
            }
        }
        if (!deleteDiscIds.isEmpty()) {
            LambdaUpdateWrapper<GsqTwiningDisc> deleteWrapper = new LambdaUpdateWrapper<>();
            deleteWrapper.in(GsqTwiningDisc::getId, deleteDiscIds)
                    .set(GsqTwiningDisc::getIsDelete, 1)
                    .set(GsqTwiningDisc::getUpdateBy, updateBy)
                    .set(GsqTwiningDisc::getUpdateTime, now);
            gsqTwiningDiscMapper.update(null, deleteWrapper);
            // 级联逻辑删除规格关系（按缠绕盘编码）
            LambdaUpdateWrapper<GsqTwiningDiscSpec> subDeleteWrapper = new LambdaUpdateWrapper<>();
            subDeleteWrapper.in(GsqTwiningDiscSpec::getTwiningDiscCode, deleteDiscCodes)
                    .set(GsqTwiningDiscSpec::getIsDelete, 1)
                    .set(GsqTwiningDiscSpec::getUpdateBy, updateBy)
                    .set(GsqTwiningDiscSpec::getUpdateTime, now);
            gsqTwiningDiscSpecMapper.update(null, subDeleteWrapper);
            // 级联逻辑删除机台关系（按缠绕盘编码）
            LambdaUpdateWrapper<GsqTwiningDiscMachine> machineDeleteWrapper = new LambdaUpdateWrapper<>();
            machineDeleteWrapper.in(GsqTwiningDiscMachine::getTwiningDiscCode, deleteDiscCodes)
                    .set(GsqTwiningDiscMachine::getIsDelete, 1)
                    .set(GsqTwiningDiscMachine::getUpdateBy, updateBy)
                    .set(GsqTwiningDiscMachine::getUpdateTime, now);
            gsqTwiningDiscMachineMapper.update(null, machineDeleteWrapper);
        }

        // ===== 3. 规格关系UPSERT：按缠绕盘编码+钢丝圈编号组合分流更新/插入 =====
        if (!specList.isEmpty()) {
            // 查询APS现有全部未删除规格关系，按组合键索引
            LambdaQueryWrapper<GsqTwiningDiscSpec> existSubWrapper = new LambdaQueryWrapper<>();
            existSubWrapper.eq(GsqTwiningDiscSpec::getIsDelete, 0);
            Map<String, GsqTwiningDiscSpec> existSubMap = gsqTwiningDiscSpecMapper.selectList(existSubWrapper).stream()
                    .filter(sub -> PubUtil.isNotEmpty(sub.getTwiningDiscCode()) && PubUtil.isNotEmpty(sub.getSteelRingCode()))
                    .collect(Collectors.toMap(
                            sub -> sub.getTwiningDiscCode() + "|" + sub.getSteelRingCode(),
                            Function.identity(), (v1, v2) -> v1));

            // 批量预取钢丝圈编码->名称映射（用于反显）
            Set<String> ringCodes = specList.stream()
                    .map(GsqTwiningDiscSpec::getSteelRingCode)
                    .filter(PubUtil::isNotEmpty)
                    .collect(Collectors.toSet());
            Map<String, String> ringNameMap = new HashMap<>();
            List<String> ringCodeList = new ArrayList<>(ringCodes);
            int ringBatchSize = 1000;
            for (int i = 0; i < ringCodeList.size(); i += ringBatchSize) {
                List<String> batch = ringCodeList.subList(i, Math.min(i + ringBatchSize, ringCodeList.size()));
                gsqTwiningDiscMapper.listSteelRingInfoByCodes(batch).forEach(ring ->
                        ringNameMap.put(String.valueOf(ring.get("BEAD_CODE")),
                                ring.get("BEAD_NAME") == null ? "" : String.valueOf(ring.get("BEAD_NAME"))));
            }

            // 分流更新/插入（工厂代码为空时按MES值落库，钢丝圈名称按施工信息表反显）
            Set<String> mesSubKeys = new HashSet<>();
            List<GsqTwiningDiscSpec> subInsertList = new ArrayList<>();
            for (GsqTwiningDiscSpec mes : specList) {
                if (PubUtil.isEmpty(mes.getTwiningDiscCode()) || PubUtil.isEmpty(mes.getSteelRingCode())) {
                    continue;
                }
                String key = mes.getTwiningDiscCode() + "|" + mes.getSteelRingCode();
                mesSubKeys.add(key);
                GsqTwiningDiscSpec exist = existSubMap.get(key);
                if (exist != null) {
                    // 已存在：仅更新MES维护字段
                    LambdaUpdateWrapper<GsqTwiningDiscSpec> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(GsqTwiningDiscSpec::getId, exist.getId())
                            .set(GsqTwiningDiscSpec::getSteelRingName, ringNameMap.get(mes.getSteelRingCode()))
                            .set(GsqTwiningDiscSpec::getStatus, PubUtil.isEmpty(mes.getStatus()) ? "0" : mes.getStatus())
                            .set(GsqTwiningDiscSpec::getFactoryCode, mes.getFactoryCode())
                            .set(GsqTwiningDiscSpec::getDataVersion, mes.getDataVersion())
                            .set(GsqTwiningDiscSpec::getDataSource, "0")
                            .set(GsqTwiningDiscSpec::getUpdateBy, updateBy)
                            .set(GsqTwiningDiscSpec::getUpdateTime, now);
                    gsqTwiningDiscSpecMapper.update(null, updateWrapper);
                } else {
                    mes.setSteelRingName(ringNameMap.get(mes.getSteelRingCode()));
                    mes.setStatus(PubUtil.isEmpty(mes.getStatus()) ? "0" : mes.getStatus());
                    mes.setIsDelete(0);
                    mes.setCreateBy(updateBy);
                    mes.setUpdateBy(updateBy);
                    subInsertList.add(mes);
                }
            }
            if (!subInsertList.isEmpty()) {
                gsqTwiningDiscSpecMapper.batchInsertMesSpec(subInsertList);
            }

            // 规格关系清理：MES来源但MES最新关系已不存在的组合逻辑删除
            List<Long> deleteSubIds = existSubMap.values().stream()
                    .filter(sub -> "0".equals(sub.getDataSource()))
                    .filter(sub -> !mesSubKeys.contains(sub.getTwiningDiscCode() + "|" + sub.getSteelRingCode()))
                    .map(GsqTwiningDiscSpec::getId)
                    .collect(Collectors.toList());
            if (!deleteSubIds.isEmpty()) {
                LambdaUpdateWrapper<GsqTwiningDiscSpec> deleteWrapper = new LambdaUpdateWrapper<>();
                deleteWrapper.in(GsqTwiningDiscSpec::getId, deleteSubIds)
                        .set(GsqTwiningDiscSpec::getIsDelete, 1)
                        .set(GsqTwiningDiscSpec::getUpdateBy, updateBy)
                        .set(GsqTwiningDiscSpec::getUpdateTime, now);
                gsqTwiningDiscSpecMapper.update(null, deleteWrapper);
            }
        }

        // ===== 4. 机台关系UPSERT：按缠绕盘编码+机台编号组合分流更新/插入 =====
        LambdaQueryWrapper<GsqTwiningDiscMachine> existMachineWrapper = new LambdaQueryWrapper<>();
        existMachineWrapper.eq(GsqTwiningDiscMachine::getIsDelete, 0);
        Map<String, GsqTwiningDiscMachine> existMachineMap = gsqTwiningDiscMachineMapper.selectList(existMachineWrapper).stream()
                .filter(machine -> PubUtil.isNotEmpty(machine.getTwiningDiscCode()) && PubUtil.isNotEmpty(machine.getMachineCode()))
                .collect(Collectors.toMap(
                        machine -> machine.getTwiningDiscCode() + "|" + machine.getMachineCode(),
                        Function.identity(), (v1, v2) -> v1));

        Set<String> mesMachineKeys = new HashSet<>();
        List<GsqTwiningDiscMachine> machineInsertList = new ArrayList<>();
        for (GsqTwiningDiscMachine mes : machineList) {
            if (PubUtil.isEmpty(mes.getTwiningDiscCode()) || PubUtil.isEmpty(mes.getMachineCode())) {
                continue;
            }
            String key = mes.getTwiningDiscCode() + "|" + mes.getMachineCode();
            mesMachineKeys.add(key);
            GsqTwiningDiscMachine exist = existMachineMap.get(key);
            if (exist != null) {
                // 已存在：仅更新MES维护字段
                LambdaUpdateWrapper<GsqTwiningDiscMachine> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(GsqTwiningDiscMachine::getId, exist.getId())
                        .set(GsqTwiningDiscMachine::getStatus, PubUtil.isEmpty(mes.getStatus()) ? "0" : mes.getStatus())
                        .set(GsqTwiningDiscMachine::getFactoryCode, mes.getFactoryCode())
                        .set(GsqTwiningDiscMachine::getDataVersion, mes.getDataVersion())
                        .set(GsqTwiningDiscMachine::getDataSource, "0")
                        .set(GsqTwiningDiscMachine::getUpdateBy, updateBy)
                        .set(GsqTwiningDiscMachine::getUpdateTime, now);
                gsqTwiningDiscMachineMapper.update(null, updateWrapper);
            } else {
                mes.setStatus(PubUtil.isEmpty(mes.getStatus()) ? "0" : mes.getStatus());
                mes.setDataSource("0");
                mes.setIsDelete(0);
                mes.setCreateBy(updateBy);
                mes.setUpdateBy(updateBy);
                machineInsertList.add(mes);
            }
        }
        if (!machineInsertList.isEmpty()) {
            gsqTwiningDiscMachineMapper.batchInsertMesMachine(machineInsertList);
        }

        // 机台关系清理：MES来源（字典lh_precision_data_source：0-MES同步）但MES最新关系已不存在的组合逻辑删除
        List<Long> deleteMachineIds = existMachineMap.values().stream()
                .filter(machine -> "0".equals(machine.getDataSource()))
                .filter(machine -> !mesMachineKeys.contains(machine.getTwiningDiscCode() + "|" + machine.getMachineCode()))
                .map(GsqTwiningDiscMachine::getId)
                .collect(Collectors.toList());
        if (!deleteMachineIds.isEmpty()) {
            LambdaUpdateWrapper<GsqTwiningDiscMachine> deleteWrapper = new LambdaUpdateWrapper<>();
            deleteWrapper.in(GsqTwiningDiscMachine::getId, deleteMachineIds)
                    .set(GsqTwiningDiscMachine::getIsDelete, 1)
                    .set(GsqTwiningDiscMachine::getUpdateBy, updateBy)
                    .set(GsqTwiningDiscMachine::getUpdateTime, now);
            gsqTwiningDiscMachineMapper.update(null, deleteWrapper);
        }

        log.info("缠绕盘MES同步：主表新增={}清理={}，规格关系新增={}，机台关系新增={}清理={}",
                discInsertList.size(),
                deleteDiscIds.size(),
                syncVO.getSpecList() == null ? 0 : (int) specList.stream()
                        .filter(sub -> PubUtil.isNotEmpty(sub.getTwiningDiscCode()) && PubUtil.isNotEmpty(sub.getSteelRingCode())).count(),
                machineInsertList.size(),
                deleteMachineIds.size());
        return AjaxResult.success();
    }
}
