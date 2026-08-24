package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqToolingCartCapacity;
import com.zlt.aps.tq.mapper.TqToolingCartCapacityMapper;
import com.zlt.aps.tq.service.ITqToolingCartCapacityService;
import com.zlt.aps.utils.ImportExcelValidatedUtils;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

@Slf4j
@Service
public class TqToolingCartCapacityServiceImpl extends AbstractDocService<TqToolingCartCapacity> implements ITqToolingCartCapacityService {

    @Resource
    private TqToolingCartCapacityMapper tqToolingCartCapacityMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ_TOOLING_CART_CAPACITY";
    }

    /**
     * 校验胎圈编码唯一性
     * <p>注意：此处不能过滤 IS_DELETE，逻辑删除记录仍占用数据库唯一索引（UK_TQ_TOOLING_CART_CAPACITY(BEAD_CODE)），
     * 必须与数据库唯一索引同口径校验，否则校验通过但插入时仍会撞唯一键报数据库异常</p>
     *
     * @param entity 实体（胎圈编码，编辑时含id）
     * @return UserConstants.NOT_UNIQUE=不唯一，UserConstants.UNIQUE=唯一
     */
    @Override
    public String checkUnique(TqToolingCartCapacity entity) {
        LambdaQueryWrapper<TqToolingCartCapacity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(entity.getId() != null, TqToolingCartCapacity::getId, entity.getId());
        wrapper.eq(TqToolingCartCapacity::getBeadCode, entity.getBeadCode());
        if (tqToolingCartCapacityMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("beadCode");
    }

    @Override
    public void deleteAllToolingCartCapacity() {
        tqToolingCartCapacityMapper.deleteAllToolingCartCapacity();
    }

    /**
     * 导入数据（标准导入模式）
     * <p>处理规则：</p>
     * <p>1. 勾选更新（updateSupport=true）：已存在记录按唯一键（胎圈编码）覆盖更新，逻辑删除记录同时复活；</p>
     * <p>2. 未勾选更新：纯插入，唯一键已存在（含逻辑删除记录占用的唯一键）的行标记失败并写入导入日志，不影响其余新数据导入；</p>
     * <p>3. 唯一性判断与数据库唯一索引同口径（不过滤逻辑删除），避免插入时撞唯一键导致整体失败。</p>
     *
     * @param list          导入数据
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志ID
     * @return 导入结果
     */
    @Override
    public AjaxResult importData(List<TqToolingCartCapacity> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<TqToolingCartCapacity> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.tq.toolingCartCapacity.column.conflict");

        // 循环外一次性加载全部已占用唯一键（不过滤逻辑删除，逻辑删除记录仍占用数据库唯一索引），
        // 避免循环内逐笔查询数据库，提升大数据量导入性能
        Set<String> existBeadCodeSet = this.loadExistBeadCodeSet();
        // 当前操作用户，用于补充导入记录的审计字段（mergeSql为自定义SQL，不走框架自动填充）
        String username = this.getCurrentUsername();

        for (int i = 0; i < list.size(); i++) {
            int rowNum = i + 2;
            TqToolingCartCapacity entity = list.get(i);

            // 第一轮：注解必填校验 + Excel文件内重复校验（按唯一键字段beadCode比较）
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, rowNum, entity);
            ImportExcelValidatedUtils.validatedRepeat(list, entity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(validated)) {
                // 校验不通过，该行直接跳过，不再进入唯一性判断和落库逻辑
                failureNum++;
                entity.setId(-999L);
                importErrorLogs.addAll(validated);
                continue;
            }

            // 第二轮：唯一性判断（与数据库唯一索引同口径，逻辑删除记录同样占用唯一键）
            if (existBeadCodeSet.contains(entity.getBeadCode())) {
                if (updateSupport) {
                    // 勾选更新：已存在记录按唯一键覆盖更新（mergeSql的ON DUPLICATE KEY UPDATE，同时复活逻辑删除记录）
                    entity.setCreateBy(username);
                    entity.setUpdateBy(username);
                    importList.add(entity);
                    successNum++;
                } else {
                    // 未勾选更新：唯一键已存在，标记失败并写入导入日志，继续处理后续行（不影响新数据导入）
                    failureNum++;
                    entity.setId(-999L);
                    addImportErrorLog(importLogId, rowNum, uniqueMsg, importErrorLogs);
                }
            } else {
                // 新数据：补充审计字段后待批量插入
                entity.setCreateBy(username);
                entity.setUpdateBy(username);
                importList.add(entity);
                successNum++;
            }
        }

        // 统一落库：勾选更新走mergeSql（存在则按唯一键覆盖更新），未勾选走批量插入（前置唯一性判断已保证不会撞唯一键）
        if (!importList.isEmpty()) {
            if (updateSupport) {
                tqToolingCartCapacityMapper.mergeSql(importList);
            } else {
                baseDao.saveBatch(importList);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 一次性加载全部已占用的胎圈编码集合（不过滤逻辑删除），
     * 用于导入时在内存中判断唯一键占用情况
     * <p>逻辑删除记录仍占用数据库唯一索引，因此加载时不过滤IS_DELETE，与数据库唯一索引同口径</p>
     *
     * @return 已占用的胎圈编码集合
     */
    private Set<String> loadExistBeadCodeSet() {
        List<TqToolingCartCapacity> existList = tqToolingCartCapacityMapper.selectList(
                new LambdaQueryWrapper<TqToolingCartCapacity>()
                        .select(TqToolingCartCapacity::getBeadCode));
        // 排除胎圈编码为空的记录（空值不参与唯一键比较，与数据库唯一索引口径一致）
        return existList.stream()
                .map(TqToolingCartCapacity::getBeadCode)
                .filter(code -> code != null && !code.trim().isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 获取当前登录用户名，获取失败时降级为system（兼容无登录上下文的调用场景）
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
