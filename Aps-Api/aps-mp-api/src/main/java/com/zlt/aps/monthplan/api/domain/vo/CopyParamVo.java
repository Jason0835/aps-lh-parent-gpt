package com.zlt.aps.monthplan.api.domain.vo;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 拷贝复制参数
 *
 * @author hsc
 * @date 2022/5/24
 */
@Data
public class CopyParamVo extends BaseEntity {
    
    private String factoryCode;

    private Integer fromYear;

    private Integer fromMonth;

    private Integer copyToYear;

    private Integer copyToMonth;

    private Integer generateToYear;

    private Integer generateToMonth;

    private String creatBy;
}
