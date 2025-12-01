package com.zlt.aps.domain;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import lombok.Data;

import java.util.Date;

@Data
public class ImportLogVo extends ImportLog {
    private Date[] dataArray;
}
