ui_locale = 'zh';
ui_locale_tar = 'zh-CN';
ui_locale_server = 'zh_CN';

frame = {
    loading: "数据加载中，请稍后..."
    , contextMenu: {
        close_current: '关闭当前',
        close_other: '关闭其他',
        close_left: '关闭左侧',
        close_right: '关闭右侧',
        close_all: '全部关闭',
        full: '全屏显示',
        refresh: '刷新页面',
        open: '新窗口打开'
    },
    login: {
        usernameRequired: "请输入您的用户名",
        passwordRequired: "请输入您的密码"
    },
    title: {
        sysAlter: '系统提示',
        data: "数据",
        sysWin: "系统窗口"
    },
    alter: {
        kickout: "您已在别处登录，请您修改密码或重新登录",
        pwdOnlyNum: "密码只能为0-9数字",
        pwdOnlyEng: "密码只能为a-z和A-Z字母",
        pwdMustNumEng: "密码必须包含字母以及数字",
        pwdMustNumEngTag: "密码必须包含字母、数字、以及特殊符号",
        mustChooseOneRecord: "请至少选择一条记录",
        serverTimeout: "服务器超时，请稍后再试！",
        serverError: "服务器错误，请联系管理员！",
        doingWaiting: "正在处理中，请稍后…",
        saveSucc: "保存成功,正在刷新数据请稍后……",
        //导出
        exportingExcel: "正在导出数据，请稍后...",
        exportAll2Excel: "确定导出所有 ",
        //导入
        importExcel: "请选择后缀为 “xls”或“xlsx”的文件。",
        //tree
        chooseNode: "请选择节点后提交",
        notChooseRootNode: "不能选择根节点",
        notChooseParentNode: "不能选择父节点",
        notChooseLastNode: "不能选择父节点",
        //del
        isDelOneBefore: "确定删除该条",
        isDelOneAfter: "吗？",
        isDelSelectBefore: "确认要删除选中的",
        isDelSelectAll: "确认要删除全部数据吗?",
        isDelSelectAfter: "条数据吗?",
        isDelAllBefore: "确定清空所有",
        isDelAllAfter: "?",
        all: "所有",
    },
    btn: {
        import: "导入"
        , export: "导出"
        , cancel: "取消"
        , ok: "确定"
        , commit: "确认"
        , close: "关闭"
        , add: "添加"
        , modify: "修改"
    }

};

//Joran 2020-12-09处理bootstrap-suggest.js国际化问题start
localLayer={
    btn:['确定', '取消'],
    context:{
        info:'信息',
        maxInput:'最多输入',
        countText:'个字数',
        noPicture:'没有图片',
        tips:'当前图片地址异常<br>是否继续查看下一张?',
        next:'下一张',
        stop:'不看了',
    }
};
suggest={
    msg:{
        handleError:"返回数据格式错误!",
        searchingTip: '搜索中...',
        consoleWarn:'不是一个标准的 bootstrap 下拉式菜单或已初始化:'
    }

}
bootstrapDualListbox={
    filterPlaceHolder: '过滤器',
    filterTextClear: '展示所有',
    moveSelectedLabel: "添加",
    moveAllLabel: '添加所有',
    removeSelectedLabel: "移除",
    removeAllLabel: '移除所有',
    infoText: '共{0}个',                                                  // text when all options are visible / false for no info text
    infoTextFiltered: '<span class="label label-warning">过滤</span> {0} from {1}', // when not all of the options are visible due to the filter
    infoTextEmpty: '无数据'
}
//Joran 2020-12-09处理bootstrap-suggest.js国际化问题end
