package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gim
 */
public interface MesBadNumService {

    // 胚胎不良量
    @Transactional
    AjaxResult mergeBadNum(String dataVersion);
}
