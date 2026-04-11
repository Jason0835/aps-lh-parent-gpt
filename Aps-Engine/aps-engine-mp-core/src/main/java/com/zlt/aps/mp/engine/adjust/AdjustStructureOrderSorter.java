package com.zlt.aps.mp.engine.adjust;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.vo.AdjustStructureOrderVo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 结构内调整，结构顺序排序
 * 1、结构内的高优先级SKU个数多的优先；（heightPriorityCount）
 * 2、结构内模具受限SKU个数多的优先；（mouldLimitCount）
 * 3、在按1和2排序的过程中，若遇到含有特殊物料的结构，将含有特殊物料的结构都拉在一起，作为一个组，特殊结构组按以下规则排序；（hasSpecialMaterial）
 * 3.1、特殊材料结构的高优先级SKU个数多的优先；（heightPriorityCount）
 * 3.2、特殊材料结构的模具受限SKU个数多的优先；（mouldLimitCount）
 * 比如：
 * 结构1 高优先级个数 = 5  特殊结构 = 否
 * 结构2 高优先级个数 = 4  特殊结构 = 是
 * 结构3 高优先级个数 = 2  特殊结构 = 否
 * 结构4 高优先级个数 = 1  特殊结构 = 是
 * 那正确的排序顺序是：
 * 结构1 -> 结构2 -> 结构4 ->结构3
 */
public class AdjustStructureOrderSorter {

    public static void sort(List<AdjustStructureOrderVo> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        // 1. 全局排序：高优先级个数降序，再模具受限个数降序
        list.sort(Comparator
                .comparingInt(AdjustStructureOrderVo::getHeightPriorityCount).reversed()
                .thenComparingInt(AdjustStructureOrderVo::getMouldLimitCount).reversed());

        // 2. 分离普通结构和特殊结构（特殊结构已按全局规则排好序）
        List<AdjustStructureOrderVo> normals = new ArrayList<>();
        List<AdjustStructureOrderVo> specials = new ArrayList<>();
        for (AdjustStructureOrderVo vo : list) {
            if (YesOrNoEnum.YES.getCode().equals(vo.getHasSpecialMaterial())) {
                specials.add(vo);
            } else {
                normals.add(vo);
            }
        }

        // 如果没有特殊结构或没有普通结构，直接返回
        if (specials.isEmpty() || normals.isEmpty()) {
            return;
        }

        // 3. 找到第一个特殊结构在原列表中的索引
        int firstSpecialIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (YesOrNoEnum.YES.getCode().equals(list.get(i).getHasSpecialMaterial())) {
                firstSpecialIndex = i;
                break;
            }
        }

        // 4. 重新组装列表
        List<AdjustStructureOrderVo> result = new ArrayList<>();
        // 添加第一个特殊结构之前的所有普通结构
        for (int i = 0; i < firstSpecialIndex; i++) {
            result.add(list.get(i));
        }
        // 添加所有特殊结构（保持顺序）
        result.addAll(specials);
        // 添加第一个特殊结构之后的所有普通结构（跳过特殊结构）
        for (int i = firstSpecialIndex; i < list.size(); i++) {
            AdjustStructureOrderVo vo = list.get(i);
            if (!YesOrNoEnum.YES.getCode().equals(list.get(i).getHasSpecialMaterial())) {
                result.add(vo);
            }
        }

        // 替换原列表
        list.clear();
        list.addAll(result);
    }
}
