package com.zlt.mdm.auth.api.domain.vo;


import com.ruoyi.common.utils.StringUtils;
import com.zlt.mdm.auth.api.domain.MdmSystemData;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserSystemVo {

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 有权限的系统列表
     */
    private List<MdmSystemData> systems;

    /**
     * 把系统code取出来,得到一个集合
     *
     * @return
     */
    public Set<String> getSystemSet() {

        Set<String> systemSet = null;
        if (StringUtils.isNotNull(systems)) {
            systemSet = systems.stream().map(item -> item.getSystemCode()).collect(Collectors.toSet());
        }
        if (StringUtils.isNull(systemSet)) {
            systemSet = new HashSet<>();
        }

        return systemSet;
    }

}
