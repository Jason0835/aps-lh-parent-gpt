package com.zlt.aps.tm.service.loader;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.util.Arrays;

/**
 * 成型需求共用任务基础属性装配器。
 *
 * <p>仅装配 BOM 与 RECIPE 路径完全一致的来源、施工及胶料属性；
 * 小胶种判定、班次需求、备库窗口和证据仍由数据加载服务在原调用点完成，
 * 避免改变读取顺序和业务判断时机。</p>
 */
final class TmFormingTaskBaseAssembler {

    /**
     * 组装 BOM 与 RECIPE 模式共用的胎面任务基础属性。
     *
     * @param input BOM 或 RECIPE 路径共用的基础属性输入
     * @return 已写入共用基础属性的待排任务草稿
     */
    TmTaskDraft assemble(TmFormingTaskBaseInput input) {
        TmTaskDraft taskDraft = new TmTaskDraft();
        taskDraft.setOrderNo(input.orderNo);
        taskDraft.setSourceOrderNos(input.sourceOrderNos);
        taskDraft.setMaterialCode(input.materialCode);
        taskDraft.setMaterialDesc(input.materialDesc);
        taskDraft.setEmbryoCode(input.embryoCode);
        taskDraft.setMainMaterialDesc(input.mainMaterialDesc);
        taskDraft.setCxMachineCode(input.cxMachineCode);
        taskDraft.setLhMachineCode(input.lhMachineCode);
        taskDraft.setBusinessKeySuffix(input.businessKeySuffix);
        taskDraft.setTreadCode(input.treadCode);
        this.applyRubberCategory(taskDraft, input.rubberCategory);
        taskDraft.setSmallGlueFlag(input.smallGlueFlag);
        taskDraft.setMouthPlateCode(input.mouthPlateCode);
        return taskDraft;
    }

    /**
     * 拆分胶料类别并写入主胶料、基部胶料属性。
     *
     * @param taskDraft 待排任务草稿
     * @param rubberCategory 胶料类别
     */
    private void applyRubberCategory(TmTaskDraft taskDraft, String rubberCategory) {
        if (StrUtil.isNotBlank(rubberCategory)) {
            String[] glueParts = rubberCategory.split(",");
            taskDraft.setGlueCode(glueParts[0].trim());
            if (glueParts.length > 1) {
                taskDraft.setBaseGlueCode(String.join(",", Arrays.copyOfRange(glueParts, 1, glueParts.length)));
            }
            return;
        }
        taskDraft.setGlueCode(null);
        taskDraft.setBaseGlueCode(null);
    }
}
