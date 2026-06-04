<template>
  <el-dialog
    :title="$t('ui.data.column.scheduleResult.validationError')"
    :visible.sync="visible"
    width="900px"
    :close-on-click-modal="false"
    :append-to-body="true"
    top="5vh"
    @close="hide"
  >
    <div class="error-summary">
      <el-alert
        :title="summaryMessage"
        type="error"
        :closable="false"
        show-icon
      />
    </div>

    <div class="error-table-container" v-if="errorList.length > 0">
      <div class="table-header">
        <span class="count-info">
          {{ $t('ui.data.column.lhScheduleResult.validation.total') }} <strong>{{ filteredList.length }}</strong> {{ $t('ui.data.column.lhScheduleResult.validation.records') }}
          <el-tag
            v-if="disabledCount > 0"
            type="danger"
            size="mini"
            style="margin-left: 8px"
          >{{ $t('ui.data.column.lhScheduleResult.validation.disabled') }} {{ disabledCount }}</el-tag>
          <el-tag
            v-if="missingCount > 0"
            type="warning"
            size="mini"
            style="margin-left: 4px"
          >{{ $t('ui.data.column.lhScheduleResult.validation.missing') }} {{ missingCount }}</el-tag>
        </span>
        <el-input
          v-model="searchText"
          :placeholder="$t('ui.data.column.lhScheduleResult.validation.searchPlaceholder')"
          prefix-icon="el-icon-search"
          clearable
          size="small"
          style="width: 280px"
        />
      </div>

      <el-table
        :data="pagedData"
        border
        stripe
        size="small"
        height="400"
        v-loading="loading"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column
          type="index"
          :label="$t('ui.data.column.lhScheduleResult.validation.index')"
          width="60"
          align="center"
        />
        <el-table-column
          prop="mouldCode"
          :label="$t('ui.data.column.lhScheduleResult.validation.mouldCode')"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="mouldType"
          :label="$t('ui.data.column.lhScheduleResult.validation.mouldType')"
          min-width="100"
          align="center"
        >
          <template slot-scope="{ row }">
            <span>{{ formatMouldType(row.mouldType) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="reason"
          :label="$t('ui.data.column.lhScheduleResult.validation.disabledReason')"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          :label="$t('ui.data.column.lhScheduleResult.validation.status')"
          width="80"
          align="center"
        >
          <template slot-scope="{ row }">
            <el-tag :type="row.status === $t('ui.data.column.lhScheduleResult.validation.disabled') ? 'danger' : 'warning'" size="mini">
              {{ row.status || $t('ui.data.column.lhScheduleResult.validation.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container" v-if="filteredList.length > pageSize">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredList.length"
        />
      </div>
    </div>

    <div v-if="validationErrors.length > 0 && errorList.length === 0" class="error-section">
      <h4 class="section-title">{{ $t('ui.data.column.lhScheduleResult.validation.errorInfo') }}</h4>
      <el-table :data="validationErrors" border size="small" max-height="300">
        <el-table-column type="index" :label="$t('ui.data.column.lhScheduleResult.validation.index')" width="60" align="center" />
        <el-table-column :label="$t('ui.data.column.lhScheduleResult.validation.errorMessage')" min-width="300">
          <template slot-scope="{ row }">
            <span>{{ row }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="errorList.length === 0 && validationErrors.length === 0" class="no-data-tip">
      <i class="el-icon-warning-outline"></i>
      <span>{{ $t('ui.data.column.lhScheduleResult.validation.noErrorDetail') }}</span>
    </div>

    <template slot="footer">
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button type="primary" @click="handleExport" v-if="errorList.length > 0">
        {{ $t('ui.data.column.lhScheduleResult.validation.exportList') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: 'ValidationErrorDialog',
  inject: ["parentDict"],
  data() {
    return {
      visible: false,
      loading: false,
      summaryMessage: '',
      errorList: [],
      filteredList: [],
      validationErrors: [],
      searchText: '',
      currentPage: 1,
      pageSize: 20,
      disabledCount: 0,
      missingCount: 0,
    };
  },
  computed: {
    pagedData() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.filteredList.slice(start, end);
    },
  },
  watch: {
    searchText() {
      this.filterData();
      this.currentPage = 1;
    },
  },
  methods: {
    show(data) {
      if (!data) return;

      const message = data.message || data.msg || '';
      this.validationErrors = data.validationErrors || [];
      this.summaryMessage = message || this.$t('ui.data.column.lhScheduleResult.validation.validationFailed');

      const details = data.validationErrorDetails || [];

      if (details.length > 0) {
        this.errorList = details.map((item, index) => ({
          id: index + 1,
          mouldCode: item.mouldCode || '',
          mouldType: item.mouldType || '',
          reason: item.reason || item.message || this.$t('ui.data.column.lhScheduleResult.validation.statusDisabled'),
          status: item.status || this.$t('ui.data.column.lhScheduleResult.validation.disabled'),
        }));
      } else if (this.validationErrors.length > 0 && typeof this.validationErrors[0] === 'object') {
        this.errorList = this.validationErrors.map((item, index) => ({
          id: index + 1,
          mouldCode: item.mouldCode || '',
          mouldType: item.mouldType || '',
          reason: item.reason || item.message || this.$t('ui.data.column.lhScheduleResult.validation.statusDisabled'),
          status: item.status || this.$t('ui.data.column.lhScheduleResult.validation.disabled'),
        }));
        this.validationErrors = [];
      } else if (message && !details.length) {
        const parsedList = this.parseMouldCodesFromMessage(message);
        if (parsedList.length > 0) {
          this.errorList = parsedList;
          this.summaryMessage = this.extractSummary(message);
        } else {
          this.errorList = [];
        }
      } else {
        this.errorList = [];
      }

      this.disabledCount = this.errorList.filter(
        (item) => item.status === this.$t('ui.data.column.lhScheduleResult.validation.disabled') || item.reason.includes(this.$t('ui.data.column.lhScheduleResult.validation.disabled'))
      ).length;
      this.missingCount = this.errorList.filter(
        (item) => item.reason.includes(this.$t('ui.data.column.lhScheduleResult.validation.missing')) || item.reason.includes(this.$t('ui.data.column.lhScheduleResult.validation.notExist'))
      ).length;

      this.filteredList = [...this.errorList];
      this.currentPage = 1;
      this.searchText = '';
      this.visible = true;
    },

    parseMouldCodesFromMessage(message) {
      if (!message) return [];
      const hmRegex = /HM\d{10,}/gi;
      const matches = message.match(hmRegex) || [];
      if (matches.length === 0) return [];

      const reasonMatch = message.match(new RegExp(this.$t('ui.data.column.lhScheduleResult.validation.mouldStatusDisabled') + '|' + this.$t('ui.data.column.lhScheduleResult.validation.notExist') + '|' + this.$t('ui.data.column.lhScheduleResult.validation.missing') + '|' + this.$t('ui.data.column.lhScheduleResult.validation.notEnabled'), 'gi'));
      const reason = reasonMatch ? reasonMatch[0] : this.$t('ui.data.column.lhScheduleResult.validation.mouldStatusDisabled');
      const status = reason.includes(this.$t('ui.data.column.lhScheduleResult.validation.disabled')) ? this.$t('ui.data.column.lhScheduleResult.validation.disabled') : this.$t('ui.data.column.lhScheduleResult.validation.abnormal');

      return [...new Set(matches)].map((code, index) => ({
        id: index + 1,
        mouldCode: code,
        mouldType: '0',
        reason: reason,
        status: status,
      }));
    },

    extractSummary(message) {
      if (!message) return '校验未通过';

      const errorCountMatch = message.match(/共\s*(\d+)\s*(条|个)/);
      const totalCount = errorCountMatch ? errorCountMatch[1] : '';

      const typeMatch = message.match(/\[(.+?)\]/g);
      const errorType = typeMatch && typeMatch.length > 0
        ? typeMatch.map(t => t.replace(/\[/g, '').replace(/\]/g, '')).join(' - ')
        : '';

      const parts = [errorType && `${errorType} 校验未通过`, totalCount && `共 ${totalCount} 条错误`].filter(Boolean);

      return parts.length > 0 ? parts.join('，') + '。' : message.substring(0, 200) + (message.length > 200 ? '...' : '');
    },

    filterData() {
      if (!this.searchText) {
        this.filteredList = [...this.errorList];
      } else {
        const keyword = this.searchText.toLowerCase();
        this.filteredList = this.errorList.filter(
          (item) =>
            (item.mouldCode && item.mouldCode.toLowerCase().includes(keyword)) ||
            (item.reason && item.reason.toLowerCase().includes(keyword))
        );
      }
    },

    handleSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
    },

    handleCurrentChange(val) {
      this.currentPage = val;
    },

    formatMouldType(value) {
      if (!value) return '';
      if (this.parentDict && this.parentDict.type && this.parentDict.type.biz_mould_Type) {
        return this.selectDictLabel(this.parentDict.type.biz_mould_Type, value);
      }
      return value;
    },

    hide() {
      this.visible = false;
      this.resetData();
    },

    resetData() {
      this.errorList = [];
      this.filteredList = [];
      this.validationErrors = [];
      this.searchText = '';
      this.currentPage = 1;
      this.summaryMessage = '';
      this.disabledCount = 0;
      this.missingCount = 0;
    },

    handleExport() {
      try {
        const header = ['序号', '模具编号', '模具类型', '原因', '状态'];
        const rows = this.filteredList.map((item, index) => [
          index + 1,
          item.mouldCode,
          this.formatMouldType(item.mouldType),
          item.reason,
          item.status,
        ]);

        let csvContent = '\uFEFF';
        csvContent += header.join(',') + '\n';
        rows.forEach((row) => {
          csvContent +=
            row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(',') +
            '\n';
        });

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `禁用模具列表_${new Date().getTime()}.csv`;
        link.click();
        URL.revokeObjectURL(link.href);

        this.$modal.msgSuccess('导出成功');
      } catch (error) {
        console.error('导出失败:', error);
        this.$modal.msgError('导出失败');
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.error-summary {
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.count-info {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.error-table-container {
  margin-top: 15px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.error-section {
  margin-bottom: 16px;
}

.section-title {
  margin-bottom: 8px;
  color: #f56c6c;
  font-size: 14px;
}

.no-data-tip {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 14px;

  i {
    font-size: 40px;
    display: block;
    margin-bottom: 10px;
  }
}
</style>
