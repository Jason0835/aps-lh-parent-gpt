/**
 * Bootstrap Table Chinese translation
 * Author: Zhixin Wen<wenzhixin2010@gmail.com>
 */

$.fn.bootstrapTable.locales['en-US'] = {
	formatShowSearch: function formatShowSearch() {
		return 'Hide/show search';
	},
	formatPageGo: function formatPageGo() {
		return 'Jump';
	},
	formatCopyRows: function formatCopyRows() {
		return 'Copy line';
	},
	formatPrint: function formatPrint() {
		return 'Print';
	},
    formatLoadingMessage: function formatLoadingMessage() {
        return 'Loading...';
    },
    formatRecordsPerPage: function formatRecordsPerPage(pageNumber) {
        return pageNumber+" records per page";
    },
    formatShowingRows: function formatShowingRows(pageFrom, pageTo, totalRows, totalNotFiltered) {
        if (totalNotFiltered !== undefined && totalNotFiltered > 0 && totalNotFiltered > totalRows) {
            return "Display records "+pageFrom+" to "+pageTo+", "+totalRows+"records in total, Filter from "+totalNotFiltered+" total records.";
        }

        return "Display records "+pageFrom+" to "+pageTo+", "+totalRows+"records in total.";
    },
    formatSRPaginationPreText: function formatSRPaginationPreText() {
        return 'Previous page';
    },
    formatSRPaginationPageText: function formatSRPaginationPageText(page) {
        return "Page "+page;
    },
    formatSRPaginationNextText: function formatSRPaginationNextText() {
        return 'Next page';
    },
    formatDetailPagination: function formatDetailPagination(totalRows) {
        return totalRows+"records in total.";
    },
    formatClearSearch: function formatClearSearch() {
        return 'Clear search';
    },
    formatSearch: function formatSearch() {
        return 'Search';
    },
    formatNoMatches: function formatNoMatches() {
        return 'No matching records were found';
    },
    formatPaginationSwitch: function formatPaginationSwitch() {
        return 'Hide/show Pagination';
    },
    formatPaginationSwitchDown: function formatPaginationSwitchDown() {
        return 'Show Pagination';
    },
    formatPaginationSwitchUp: function formatPaginationSwitchUp() {
        return 'Hide Pagination';
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
        return 'Columns';
    },
    formatColumnsToggleAll: function formatColumnsToggleAll() {
        return 'Switch all';
    },
    formatFullscreen: function formatFullscreen() {
        return 'Full screen';
    },
    formatAllRows: function formatAllRows() {
        return 'All';
    },
    formatAutoRefresh: function formatAutoRefresh() {
        return 'Auto refresh';
    },
    formatExport: function formatExport() {
        return 'Export data';
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
        return 'Hide/show filter control switch';
    },
    formatFilterControlSwitchHide: function formatFilterControlSwitchHide() {
        return 'Hide filter control switch';
    },
    formatFilterControlSwitchShow: function formatFilterControlSwitchShow() {
        return 'Show filter control switch';
    }
};
$.extend($.fn.bootstrapTable.defaults, $.fn.bootstrapTable.locales['en-US']);

