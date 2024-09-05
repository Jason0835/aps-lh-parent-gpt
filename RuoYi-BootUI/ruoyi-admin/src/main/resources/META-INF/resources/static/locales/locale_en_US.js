ui_locale = 'en';
ui_locale_tar = 'en-US';
ui_locale_server = 'en_US';

frame = {
    loading: "Data loading..."
    , contextMenu: {
        close_current: 'Close current',
        close_other: 'Close other',
        close_left: 'Close left',
        close_right: 'Close right',
        close_all: 'Close all',
        full: 'Full screen display',
        refresh: 'Refresh page',
        open: 'Open a new window'
    },
    login: {
        usernameRequired: "Please enter your user name",
        passwordRequired: "Please enter your password"
    },
    title: {
        sysAlter: 'System prompt',
        data: " data ",
        sysWin: "System window"
    },
    alter: {
        kickout: "enYou have logged in elsewhere. Please change your password or log in again",
        pwdOnlyNum: "The password can only be 0-9 numbers",
        pwdOnlyEng: "The password can only be a-z and A-Z letters",
        pwdMustNumEng: "The password must contain letters and numbers",
        pwdMustNumEngTag: "The password must contain letters, numbers, and special symbols",
        mustChooseOneRecord: "Please select at least one record",
        serverTimeout: "Server timeout, please try again later!",
        serverError: "Server error, please contact administrator!",
        doingWaiting: "Processing...",
        saveSucc: "Saved successfully, refreshing data...",
        //导出
        exportingExcel: "Exporting data, please wait...",
        exportAll2Excel: "Export all ",
        //导入
        importExcel: "Please select a file with the suffix 'xls' or 'xlsx'.",
        //tree
        chooseNode: "Please select a node and submit",
        notChooseRootNode: "Root node cannot be selected",
        notChooseParentNode: "Cannot select parent node",
        notChooseLastNode: "Cannot select parent node",
        //del
        isDelOneBefore: "Delete this item ",
        isDelOneAfter: "?",
        isDelSelectBefore: "Delete the selected ",
        isDelSelectAll: "Are you sure you want to delete all data?",
        isDelSelectAfter: " pieces of data?",
        isDelAllBefore: "Clear all ",
        isDelAllAfter: "?",
        all: "all ",
    },
    btn: {
        import: "Import "
        , export: "Export "
        , cancel: "Cancel "
        , ok: "Ok "
        , commit: "Commit "
        , close: "Close "
        , add: "Add "
        , modify: "Modify "
    }

};

//Joran 2020-12-09处理bootstrap-suggest.js国际化问题start
localLayer={
    btn:['confirm', 'cancel'],
    context:{
        info:'info',
        maxInput:'Maximum input',
        countText:'words',
        noPicture:'No pictures',
        tips:'The current picture address is abnor<br/>Do you want to continue to see the next one?',
        next:'Next',
        stop:'No more',
    }
};
suggest={
    msg:{
        handleError:"Return data format error!",
        searchingTip: 'Searching...',
        consoleWarn:'Not a standard bootstrap drop-down or initialized:',
    }

}
bootstrapDualListbox={
    filterPlaceHolder: 'Filter',
    filterTextClear: 'show all',
    moveSelectedLabel: 'Add',
    moveAllLabel: 'Add all',
    removeSelectedLabel: 'Remove selected',
    removeAllLabel: 'Remove all',
    infoText: 'Showing all {0}',                                                        // text when all options are visible / false for no info text
    infoTextFiltered: '<span class="label label-warning">Filtered</span> {0} from {1}', // when not all of the options are visible due to the filter
    infoTextEmpty: 'Empty list'
}
//Joran 2020-12-09处理bootstrap-suggest.js国际化问题end