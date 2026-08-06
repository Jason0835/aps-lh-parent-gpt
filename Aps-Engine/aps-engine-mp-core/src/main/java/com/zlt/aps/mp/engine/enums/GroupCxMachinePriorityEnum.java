package com.zlt.aps.mp.engine.enums;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 分组-匹配成型优先级类型枚举定义
 * 需求天数能否覆盖成型剩余产能
 * 即 需求天数 - 成型剩余天数的值
 * 在[0,1]之间，则为需求能完全覆盖成型机剩余产能，成型机没有剩余产能
 * 在(1,max)之间，则为需求能覆盖成型机剩余产能，成型机没有剩余产能，剩余还有需求量
 * 在(min,0)之间，则为需求不能覆盖成型机剩余产能，即成型机还有剩余产能
 *
 * @author ZLT
 * 20260426
 */
@Getter
public enum GroupCxMachinePriorityEnum {
    /**
     * 1 SameSpecificationsFullyCovered 成型产能不剩余：同规格完全覆盖 0<=需求天数-成型剩余产能天数<=1
     */
    SAME_SPECIFICATIONS_FULLY_COVERED(1, "SameSpecificationsFullyCovered", "同规格完全覆盖"),
    /**
     * 2 sameProSizeFullyCovered 成型产能不剩余：同英寸完全覆盖 0<=需求天数-成型剩余产能天数<=1
     */
    SAME_PRO_SIZE_FULLY_COVERED(2, "sameProSizeFullyCovered", "同英寸完全覆盖"),
    /**
     * 3 SameSpecificationsBeyondCovered 成型产能不剩余：同规格超出覆盖 1<需求天数-成型剩余产能天数
     */
    SAME_SPECIFICATIONS_BEYOND_COVERED(3, "SameSpecificationsBeyondCovered", "同规格超出覆盖"),
    /**
     * 4 sameProSizeBeyondCovered 成型产能不剩余：同英寸超出覆盖 1<需求天数-成型剩余产能天数
     */
    SAME_PRO_SIZE_BEYOND_COVERED(4, "sameProSizeBeyondCovered", "同英寸超出覆盖"),
    /**
     * 5 SameSpecificationsNoCovered 成型产能剩余：同规格不覆盖 需求天数-成型剩余产能天数<0
     */
    SAME_SPECIFICATIONS_NO_COVERED(5, "SameSpecificationsNoCovered", "同规格不能覆盖"),
    /**
     * 6 sameProSizeNoCovered 成型产能剩余：同英寸不覆盖 需求天数-成型剩余产能天数<0
     */
    SAME_PRO_SIZE_NO_COVERED(6, "sameProSizeNoCovered", "同英寸不能覆盖"),
    /**
     * 7 sectionWidthFullyCovered 成型产能不剩余：断面宽完全覆盖 0<=需求天数-成型剩余产能天数<=1
     */
    SECTION_WIDTH_FULLY_COVERED(7, "sectionWidthFullyCovered", "断面宽完全覆盖"),
    /**
     * 8 sectionWidthBeyondCovered 成型产能不剩余：断面宽超出覆盖 1<需求天数-成型剩余产能天数
     */
    SECTION_WIDTH_BEYOND_COVERED(8, "sectionWidthBeyondCovered", "断面宽超出覆盖"),
    /**
     * 9 sectionWidthNoCovered 成型产能剩余：断面宽不覆盖 需求天数-成型剩余产能天数<0
     */
    SECTION_WIDTH_NO_COVERED(9, "sectionWidthNoCovered", "断面宽不能覆盖"),
    /**
     * 10 otherFullyCovered 成型产能不剩余：历史生产过完全覆盖 0<=需求天数-成型剩余产能天数<=1
     */
    OTHER_FULLY_COVERED(10, "otherFullyCovered", "历史生产过完全覆盖"),
    /**
     * 11 otherBeyondCovered 成型产能不剩余：历史生产过超出覆盖 1<需求天数-成型剩余产能天数
     */
    OTHER_BEYOND_COVERED(11, "otherBeyondCovered", "历史生产过超出覆盖"),
    /**
     * 12 otherNoCovered 成型产能剩余：历史生产过不覆盖 需求天数-成型剩余产能天数<0
     */
    OTHER_NO_COVERED(12, "otherNoCovered", "历史生产过不能覆盖"),
    /**
     * max defaultValue 默认优先级
     */
    DEFAULT_VALUE(Integer.MAX_VALUE, "defaultValue", "默认优先级");
    /**
     * 优先级值：值越低优先级越高
     */
    private Integer priorityValue;
    /**
     * 优先级编码
     */
    private String priorityCode;
    /**
     * 描述
     */
    private String desc;

    GroupCxMachinePriorityEnum(Integer priorityValue, String priorityCode, String desc) {
        this.priorityValue = priorityValue;
        this.priorityCode = priorityCode;
        this.desc = desc;
    }

    /**
     * 按优先级升序，得到列表
     *
     * @return
     */
    public static List<GroupCxMachinePriorityEnum> getPrioritySortList() {
        List<GroupCxMachinePriorityEnum> allList = Lists.newArrayList(values());
        if (CollectionUtils.isEmpty(allList)) {
            return Collections.emptyList();
        }
        allList.sort(Comparator.comparing(GroupCxMachinePriorityEnum::getPriorityValue));
        return allList;
    }
}
