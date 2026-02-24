package com.zlt.aps.utils;

import com.ruoyi.api.gateway.system.domain.SysDept;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class DeptInfoUtils {

    public static Optional<SysDept> getDeptInfo(List<SysDept> sysDeptList, Long deptId) {
        Optional<SysDept> oneDept = sysDeptList.stream()
                .filter(item -> {
                    long s = item.getDeptId() == null ? -1l : item.getDeptId();
                    long t = deptId.longValue();

                    if (Long.compare(s,t) == 0) {
                        return true;
                    } else {
                        return false;
                    }
                }).findFirst();
        if (!oneDept.isPresent()) {
            log.warn("获取部门信息为空：{}", deptId);
        }
        return oneDept;
    }
}
