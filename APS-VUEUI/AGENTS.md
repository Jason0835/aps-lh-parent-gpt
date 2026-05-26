# 项目编码规范
## 文件格式
utf-8 no bom
## 前端规范
- 前端开发规范见@docs/前端规则.md

## 多语言

### 目录结构
前端多语言文件位于 `src/lang/`，按语言划分：
```
src/lang/
├── zh/                    # 中文
│   ├── ui_zh_CN.json      # 主UI多语言
│   ├── aps_zj_cn.json     # 组件业务多语言
│   ├── mps_zh_CH.json     # MPS业务多语言
│   ├── web_zh_CN.json     # Web业务多语言
│   ├── mix_zh_cn.json     # 混合业务多语言
│   └── index.js
├── en/                    # 英文
│   ├── ui_en_US.json
│   └── index.js
└── vi/                    # 越南语
    ├── ui_vi_VN.json
    └── index.js
```

### 命名规则
- 目录名：语言代码，如 `zh`、`en`、`vi`
- 文件名：`ui_{语言代码}.json`，如 `ui_zh_CN.json`、`ui_en_US.json`

### key 命名规范
统一使用 `.` 分隔，格式：`ui.{模块}.{类型}.{具体名称}`
- 表格列：`ui.data.column.{entityName}.{fieldName}`
- 提示信息：`ui.{module}.confirm.{action}`
- 业务模块：`ui.{module}.{type}.{key}`

### 新增/修改多语言
- 在对应语言目录的 json 文件中添加 key-value
- 确保各语言文件 key 一致，value 翻译为对应语言
