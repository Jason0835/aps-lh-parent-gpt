(function ($) {
    "use strict";
    $.fn.bootstrapTreeTable.locale = {
        formatLoadingMessage: function formatLoadingMessage() {
            return 'Please wait while we try to load the data';
        },
        formatRecordsPerPage: function formatRecordsPerPage(pageNumber) {
            return "en_US\u6BCF\u9875\u663E\u793A ".concat(pageNumber, " \u6761\u8BB0\u5F55");
        },
        formatShowingRows: function formatShowingRows(pageFrom, pageTo, totalRows, totalNotFiltered) {
            if (totalNotFiltered !== undefined && totalNotFiltered > 0 && totalNotFiltered > totalRows) {
                return "en_US\u663E\u793A\u7B2C ".concat(pageFrom, " \u5230\u7B2C ").concat(pageTo, " \u6761\u8BB0\u5F55\uFF0C\u603B\u5171 ").concat(totalRows, " \u6761\u8BB0\u5F55\uFF08\u4ECE ").concat(totalNotFiltered, " \u603B\u8BB0\u5F55\u4E2D\u8FC7\u6EE4\uFF09");
            }

            return "en_US\u663E\u793A\u7B2C ".concat(pageFrom, " \u5230\u7B2C ").concat(pageTo, " \u6761\u8BB0\u5F55\uFF0C\u603B\u5171 ").concat(totalRows, " \u6761\u8BB0\u5F55");
        },
        formatSRPaginationPreText: function formatSRPaginationPreText() {
            return 'previous page';
        },
        formatSRPaginationPageText: function formatSRPaginationPageText(page) {
            return "en_US\u7B2C".concat(page, "\u9875");
        },
        formatSRPaginationNextText: function formatSRPaginationNextText() {
            return 'next page';
        },
        formatDetailPagination: function formatDetailPagination(totalRows) {
            return "en_US\u603B\u5171 ".concat(totalRows, " \u6761\u8BB0\u5F55");
        },
        formatClearSearch: function formatClearSearch() {
            return 'clear';
        },
        formatSearch: function formatSearch() {
            return 'search';
        },
        formatNoMatches: function formatNoMatches() {
            return 'No matching records were found';
        },
        formatPaginationSwitch: function formatPaginationSwitch() {
            return 'Hide/show pagination';
        },
        formatPaginationSwitchDown: function formatPaginationSwitchDown() {
            return 'Show pagination';
        },
        formatPaginationSwitchUp: function formatPaginationSwitchUp() {
            return 'Hide pagination';
        },
        formatRefresh: function formatRefresh() {
            return 'Refresh';
        },
        formatToggle: function formatToggle() {
            return 'Switch';
        },
        formatToggleOn: function formatToggleOn() {
            return 'Show card view';
        },
        formatToggleOff: function formatToggleOff() {
            return 'Hide card view';
        },
        formatColumns: function formatColumns() {
            return 'Cloumn';
        },
        formatColumnsToggleAll: function formatColumnsToggleAll() {
            return 'Switch all';
        },
        formatFullscreen: function formatFullscreen() {
            return 'Full screen';
        },
        formatAllRows: function formatAllRows() {
            return 'all';
        },
        formatAutoRefresh: function formatAutoRefresh() {
            return 'Auto refresh';
        },
        formatExport: function formatExport() {
            return 'Export';
        },
        formatJumpTo: function formatJumpTo() {
            return 'Jump';
        },
        formatAdvancedSearch: function formatAdvancedSearch() {
            return 'Advanced search';
        },
        formatAdvancedCloseButton: function formatAdvancedCloseButton() {
            return 'Close';
        },
        formatFilterControlSwitch: function formatFilterControlSwitch() {
            return 'Hide/show filter control';
        },
        formatFilterControlSwitchHide: function formatFilterControlSwitchHide() {
            return 'Hide filter control';
        },
        formatFilterControlSwitchShow: function formatFilterControlSwitchShow() {
            return 'Show filter control';
        }
    };
    $.extend($.fn.bootstrapTreeTable.defaults, $.fn.bootstrapTreeTable.locale);
})(jQuery);