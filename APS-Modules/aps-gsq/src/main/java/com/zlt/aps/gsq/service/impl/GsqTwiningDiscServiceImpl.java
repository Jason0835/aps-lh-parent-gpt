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
import com.zlt.aps.gsq.api.domain.vo.GsqTwiningDiscImportVo;
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * 主子表平铺导入：按缠绕盘编码分组组装主表+子表明细后级联保存
     * <p>导入模板一行 = 主表字段（缠绕盘编号/名称/状态/英寸/数量/主表备注）
     * + 子表字段（钢丝圈编号/名称/明细备注）；同一缠绕盘多行明细时主表字段以首行为准</p>
     * <p>校验规则：
     * 1. 第一轮（逐行）：缠绕盘编码+钢丝圈编号组合文件内重复校验、注解校验（必填/格式/长度）；
     * 2. 第二轮（业务，批量预取后校验）：钢丝圈编号必须存在于施工信息表（名称未填时按编号反显）；
     * 3. 保存：主表新增或更新（已存在且不允许更新时报唯一冲突），更新时级联替换旧子表明细</p>
     *
     * @param list          平铺导入数据集合
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importMainAndSubData(List<GsqTwiningDiscImportVo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        // 通过第一轮校验的数据及对应Excel行号（行号用于错误日志定位）
        Map<GsqTwiningDiscImportVo, Integer> voRowMap = new LinkedHashMap<>();

        // ===== 第一轮校验：文件内组合重复 + 注解校验 =====
        // 缠绕盘编码因多行明细会合法重复，重复判定使用"缠绕盘编码|钢丝圈编号"组合键
        Map<String, Long> groupMap = list.stream()
                .filter(vo -> PubUtil.isNotEmpty(vo.getTwiningDiscCode()) && PubUtil.isNotEmpty(vo.getSteelRingCode()))
                .collect(Collectors.groupingBy(vo -> vo.getTwiningDiscCode() + "|" + vo.getSteelRingCode(), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            GsqTwiningDiscImportVo vo = list.get(i);
            int rowNum = i + 2;

            // 文件内组合重复校验（同缠绕盘+同钢丝圈不允许重复）
            if (PubUtil.isNotEmpty(vo.getTwiningDiscCode()) && PubUtil.isNotEmpty(vo.getSteelRingCode())) {
                Long count = groupMap.get(vo.getTwiningDiscCode() + "|" + vo.getSteelRingCode());
                if (count != null && count > 1) {
                    failureNum++;
                    String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                    String columnName = I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.twiningDiscCode") + "+"
                            + I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.steelRingCode");
                    addImportErrorLog(importLogId, rowNum, String.format(message, columnName), importErrorLogs);
                    continue;
                }
            }

            // 字段格式校验（必填/格式/长度）
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, rowNum, vo);
            if (validated.isEmpty()) {
                voRowMap.put(vo, rowNum);
            } else {
                failureNum++;
                importErrorLogs.addAll(validated);
            }
        }

        // ===== 批量预取：数据库已存在缠绕盘 + 施工信息表钢丝圈编码->名称映射 =====
        Map<String, GsqTwiningDisc> existDiscMap = new HashMap<>();
        Map<String, String> steelRingNameMap = new HashMap<>();
        if (!voRowMap.isEmpty()) {
            existDiscMap = this.selectExistDiscMap(voRowMap.keySet());
            steelRingNameMap = this.selectSteelRingNameMap(voRowMap.keySet());
        }

        // ===== 第二轮校验：业务校验（钢丝圈存在性 + 名称反显）=====
        List<GsqTwiningDiscImportVo> businessPassList = new ArrayList<>();
        for (Map.Entry<GsqTwiningDiscImportVo, Integer> entry : voRowMap.entrySet()) {
            GsqTwiningDiscImportVo vo = entry.getKey();
            // 钢丝圈编号存在性校验：必须存在于施工信息表
            if (!steelRingNameMap.containsKey(vo.getSteelRingCode())) {
                failureNum++;
                String message = String.format(
                        I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.steelRingNotExists"), vo.getSteelRingCode());
                addImportErrorLog(importLogId, entry.getValue(), message, importErrorLogs);
                continue;
            }
            // 钢丝圈名称未填写时按编号从施工信息表反显
            if (PubUtil.isEmpty(vo.getSteelRingName())) {
                vo.setSteelRingName(steelRingNameMap.get(vo.getSteelRingCode()));
            }
            businessPassList.add(vo);
        }

        // ===== 分组保存：按缠绕盘编码分组（保持文件顺序），组装主子表级联保存 =====
        try {
            Map<String, List<GsqTwiningDiscImportVo>> discGroupMap = businessPassList.stream()
                    .collect(Collectors.groupingBy(GsqTwiningDiscImportVo::getTwiningDiscCode,
                            LinkedHashMap::new, Collectors.toList()));
            String username = getCurrentUsername();
            Date now = new Date();

            for (Map.Entry<String, List<GsqTwiningDiscImportVo>> entry : discGroupMap.entrySet()) {
                List<GsqTwiningDiscImportVo> rows = entry.getValue();
                GsqTwiningDiscImportVo firstRow = rows.get(0);
                GsqTwiningDisc existDisc = existDiscMap.get(entry.getKey());

                // 已存在且不允许更新：该缠绕盘全部行导入失败
                if (existDisc != null && !updateSupport) {
                    failureNum += rows.size();
                    for (GsqTwiningDiscImportVo row : rows) {
                        addImportErrorLog(importLogId, voRowMap.get(row),
                                I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.conflict"), importErrorLogs);
                    }
                    continue;
                }

                // 组装主表（主表字段以首行为准，状态为空时默认正常0）
                GsqTwiningDisc disc = existDisc != null ? existDisc : new GsqTwiningDisc();
                disc.setTwiningDiscCode(firstRow.getTwiningDiscCode());
                disc.setTwiningDiscName(firstRow.getTwiningDiscName());
                disc.setStatus(PubUtil.isEmpty(firstRow.getStatus()) ? "0" : firstRow.getStatus());
                disc.setProSize(firstRow.getProSize());
                disc.setQty(firstRow.getQty());
                disc.setRemark(firstRow.getMainRemark());
                disc.setIsDelete(0);
                if (existDisc != null) {
                    disc.setUpdateBy(username);
                    disc.setUpdateTime(now);
                } else {
                    disc.setCreateBy(username);
                    disc.setCreateTime(now);
                }
                // 保存/更新主表（id为空新增，id不为空更新，由框架baseDao.save内部判断）
                baseDao.save(disc);
                Long mainId = disc.getId();

                // 更新场景：级联删除旧子表后整体替换（与saveMainAndSub保持一致）
                if (existDisc != null) {
                    LambdaQueryWrapper<GsqTwiningDiscSub> deleteWrapper = new LambdaQueryWrapper<>();
                    deleteWrapper.eq(GsqTwiningDiscSub::getDiscId, mainId);
                    gsqTwiningDiscSubMapper.delete(deleteWrapper);
                }

                // 组装子表明细并批量保存（关联主表ID，补充基础字段）
                List<GsqTwiningDiscSub> subList = rows.stream().map(row -> {
                    GsqTwiningDiscSub sub = new GsqTwiningDiscSub();
                    sub.setDiscId(mainId);
                    sub.setSteelRingCode(row.getSteelRingCode());
                    sub.setSteelRingName(row.getSteelRingName());
                    sub.setRemark(row.getSubRemark());
                    sub.setIsDelete(0);
                    sub.setCreateBy(username);
                    sub.setCreateTime(now);
                    return sub;
                }).collect(Collectors.toList());
                baseDao.saveBatch(subList);
                successNum += rows.size();
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
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 批量查询数据库中已存在的缠绕盘（用于唯一性判断与更新定位，1000条一批）
     *
     * @param voCollection 通过格式校验的导入数据集合（取缠绕盘编码）
     * @return 缠绕盘编码 -> 已存在缠绕盘实体
     */
    private Map<String, GsqTwiningDisc> selectExistDiscMap(Collection<GsqTwiningDiscImportVo> voCollection) {
        Set<String> codes = voCollection.stream()
                .map(GsqTwiningDiscImportVo::getTwiningDiscCode)
                .collect(Collectors.toSet());
        Map<String, GsqTwiningDisc> resultMap = new HashMap<>();
        // 分批查询，避免in条件过长
        List<String> codeList = new ArrayList<>(codes);
        int batchSize = 1000;
        for (int i = 0; i < codeList.size(); i += batchSize) {
            List<String> batch = codeList.subList(i, Math.min(i + batchSize, codeList.size()));
            LambdaQueryWrapper<GsqTwiningDisc> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(GsqTwiningDisc::getTwiningDiscCode, batch);
            wrapper.eq(GsqTwiningDisc::getIsDelete, "0");
            gsqTwiningDiscMapper.selectList(wrapper)
                    .forEach(disc -> resultMap.put(disc.getTwiningDiscCode(), disc));
        }
        return resultMap;
    }

    /**
     * 批量查询施工信息表钢丝圈编码->名称映射（用于存在性校验与名称反显，1000条一批）
     *
     * @param voCollection 通过格式校验的导入数据集合（取钢丝圈编号）
     * @return 钢丝圈编码 -> 钢丝圈名称
     */
    private Map<String, String> selectSteelRingNameMap(Collection<GsqTwiningDiscImportVo> voCollection) {
        Set<String> codes = voCollection.stream()
                .map(GsqTwiningDiscImportVo::getSteelRingCode)
                .collect(Collectors.toSet());
        Map<String, String> resultMap = new HashMap<>();
        // 分批查询，避免in条件过长
        List<String> codeList = new ArrayList<>(codes);
        int batchSize = 1000;
        for (int i = 0; i < codeList.size(); i += batchSize) {
            List<String> batch = codeList.subList(i, Math.min(i + batchSize, codeList.size()));
            gsqTwiningDiscMapper.listSteelRingInfoByCodes(batch).forEach(ring ->
                    resultMap.put(String.valueOf(ring.get("BEAD_CODE")),
                            ring.get("BEAD_NAME") == null ? "" : String.valueOf(ring.get("BEAD_NAME"))));
        }
        return resultMap;
    }
}
