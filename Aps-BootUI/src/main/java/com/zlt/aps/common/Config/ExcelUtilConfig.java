package com.zlt.aps.common.Config;

import com.ruoyi.file.api.service.IApsFileService;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExcelUtilConfig {

    @Autowired
    private IApsFileService iApsFileService;

    @Bean
    public ExportUtil exportUtil() {
        return new ExportUtil(iApsFileService);
    }

    @Bean
    public ImportUtil importUtil() {
        return new ImportUtil(iApsFileService);
    }

}
