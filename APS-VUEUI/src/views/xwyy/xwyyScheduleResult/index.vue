<template>
  <basic-container>
    <page-table
      v-loading="loading"
      table-ref="scheduleResultMainTable"
      :calc-height="true"
      :columns="columns"
      :data="data"
      :page="page"
      :search="search"
      :search-columns="searchColumns"
      :show-summary="false"
      :select-area="false"
      :is-reset="true"
      @search="handleSearch"
      @reset="handleReset"
      @refresh="getList"
      @pageChange="handlePageChange"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['xwyy:scheduleResult:autoSchedule']"
          type="warning"
          @click="handleAutoSchedule"
        >{{ $t('ui.data.column.scheduleResult.autoPlan') }}</el-button>
        <el-button
          v-hasPermi="['xwyy:scheduleResult:insert']"
          type="warning"
          @click="handleInsert"
        >{{ $t('ui.data.column.scheduleResult.insertOrder') }}</el-button>
        <el-button
          v-hasPermi="['xwyy:scheduleResult:remove']"
          type="danger"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t('ui.frame.btn.delete') }}</el-button>
        <el-button
          v-hasPermi="['xwyy:scheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length !== 1"
          @click="handleChangeMachine"
        >{{ $t('ui.data.column.scheduleResult.changeMachine') }}</el-button>
        <el-button
          v-hasPermi="['xwyy:scheduleResult:adjustQty']"
          type="primary"
          :disabled="selection.length !== 1"
          @click="handleChangePlan"
        >{{ $t('ui.data.column.scheduleResult.changePlan') }}</el-button>
        <el-button
          v-hasPermi="['xwyy:scheduleResult:publish']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
        >{{ $t('ui.data.column.scheduleResult.publish') }}</el-button>
        <el-button
          v-hasPermi="['xwyy:scheduleResult:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t('ui.frame.btn.import') }}</el-button>
        <el-button
          v-hasPermi="['xwyy:scheduleResult:export']"
          @click="handleExport"
        >{{ $t('ui.frame.btn.export') }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      :update-support="true"
      upload-url="/xwyy/xwyyScheduleResult/importDataByCust"
      :upload-params="importParams"
      @uploadSuccess="getList"
    />
    <auto-schedule-dialog
      ref="autoScheduleRef"
      @success="handleAutoScheduleSuccess"
    />
    <el-dialog
      :title="$t('ui.data.column.xwyyScheduleResult.autoScheduleProgress')"
      :visible.sync="autoScheduleProgressVisible"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      append-to-body
    >
      <div style="text-align:center;margin-bottom:12px;color:#606266;font-size:14px;">
        {{ autoScheduleProgressStage }}
      </div>
      <el-progress
        :percentage="autoScheduleProgressValue"
        :status="autoScheduleProgressStatus"
        :stroke-width="18"
        :text-inside="true"
      />
      <div style="margin-top:10px;color:#909399;font-size:12px;text-align:center;">
        {{ autoScheduleProgressHint }}
      </div>
    </el-dialog>
    <release-status-dialog
      ref="releaseStatusRef"
      :schedule-date="query.scheduleDate"
      @success="getList"
    />
  </basic-container>
</template>

<script>
import moment from 'moment'
import { listScheduleResult, removeScheduleResult, exportScheduleResult, autoScheduleResult, getAutoScheduleTask } from '@/api/xwyy/xwyyScheduleResult'
import { listShiftConfig } from '@/api/xwyy/xwyyShiftConfig'
import AutoScheduleDialog from './components/autoScheduleDialog.vue'
import ReleaseStatusDialog from './components/releaseStatusDialog.vue'

export default {
  name: 'XwyyScheduleResult',
  components: { AutoScheduleDialog, ReleaseStatusDialog },
  dicts: ['biz_factory_name', 'IS_RELEASE', 'PRODUCTION_STATUS', 'DATA_SOURCE'],
  provide() {
    return {
      parentDict: this.dict
    }
  },
  data() {
    const defaultScheduleDate = this.getDefaultScheduleDate()
    const defaultSearch = {
      factoryCode: '116',
      scheduleDate: defaultScheduleDate
    }

    return {
      loading: false,
      data: [],
      selection: [],
      shiftConfig: [],
      dateList: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
        pageSizes: [10, 20, 50, 100]
      },
      sort: {},
      search: { ...defaultSearch },
      query: { ...defaultSearch },
      autoScheduleTimer: null,
      autoSchedulePollTimes: 0,
      maxAutoSchedulePollTimes: 120,
      autoScheduleProgressVisible: false,
      autoScheduleProgressValue: 0,
      autoScheduleProgressStage: '',
      autoScheduleProgressStatus: null,
      autoScheduleProgressHint: ''
    }
  },
  computed: {
    importParams() {
      return {
        factoryCode: this.query.factoryCode || this.search.factoryCode
      }
    },
    columns() {
      return [
        { type: 'selection', width: 55, fixed: 'left' },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.factoryCode'),
          prop: 'factoryCode',
          minWidth: 120,
          formatter: (row, column, cellValue) => this.selectDictLabel(this.dict.type.biz_factory_name, cellValue)
        },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          align: 'center',
          minWidth: 120,
          sortable: 'custom'
        },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.isRelease'),
          prop: 'isRelease',
          minWidth: 100,
          formatter: (row, column, cellValue) => this.selectDictLabel(this.dict.type.IS_RELEASE, cellValue)
        },
        { label: this.$t('ui.data.column.xwyyScheduleResult.batchNo'), prop: 'batchNo', align: 'left', minWidth: 160 },
        { label: this.$t('ui.data.column.xwyyScheduleResult.orderNo'), prop: 'orderNo', align: 'left', minWidth: 160 },
        { label: this.$t('ui.data.column.xwyyScheduleResult.bigRollCode'), prop: 'bigRollCode', minWidth: 180 },
        ...this.buildShiftColumns(),
        { label: this.$t('ui.data.column.xwyyScheduleResult.remark'), prop: 'remark', minWidth: 160 }
      ]
    },
    searchColumns() {
      return [
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.factoryCode'),
          prop: 'factoryCode',
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true
        },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          type: 'date',
          valueFormat: 'yyyy-MM-dd'
        },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.bigRollCode'),
          prop: 'bigRollCode',
          type: 'input'
        },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.machineId'),
          prop: 'machineId',
          type: 'input'
        },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.isRelease'),
          prop: 'isRelease',
          type: 'select',
          dictData: this.dict.type.IS_RELEASE,
          filterable: true
        },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.batchNo'),
          prop: 'batchNo',
          type: 'input'
        },
        {
          label: this.$t('ui.data.column.xwyyScheduleResult.orderNo'),
          prop: 'orderNo',
          type: 'input'
        }
      ]
    }
  },
  created() {
    this.getList()
    this.loadShiftConfig()
  },
  beforeDestroy() {
    this.clearAutoScheduleTimer()
  },
  methods: {
    getDefaultScheduleDate() {
      return moment().add(1, 'days').format('YYYY-MM-DD')
    },
    buildDateList(scheduleDate) {
      const baseDate = scheduleDate || this.getDefaultScheduleDate()
      return this.shiftConfig.map(item => ({
        ...item,
        dayOffset: (item.scheduleDay || 1) - 1,
        shiftDate: moment(baseDate).add((item.scheduleDay || 1) - 1, 'days').format('MM/DD')
      }))
    },
    buildShiftColumns() {
      return this.shiftConfig.map((item, index) => {
        const dateItem = this.dateList[index] || {}
        const label = `${item.shiftName} ${dateItem.shiftDate || ''}`
        const classField = item.classField

        return {
          label,
          align: 'center',
          children: [
            {
              label: this.$t('ui.data.column.scheduleResult.plan'),
              prop: `${classField}PlanQty`,
              minWidth: 110
            },
            {
              label: this.$t('ui.data.column.scheduleResult.finish'),
              prop: `${classField}FinishQty`,
              minWidth: 110
            },
            {
              label: this.$t('ui.data.column.scheduleResult.produceOrder'),
              prop: `${classField}ProduceOrder`,
              minWidth: 110
            },
            {
              label: this.$t('ui.data.column.scheduleResult.finishRate'),
              prop: `${classField}FinishRate`,
              minWidth: 110
            },
            {
              label: this.$t('ui.data.column.scheduleResult.analysis'),
              prop: `${classField}Analysis`,
              minWidth: 140
            },
          ]
        }
      })
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        orderByColumn: this.sort.orderByColumn,
        isAsc: this.sort.isAsc
      }
      if (hasPage) {
        params.pageNum = this.page.current
        params.pageSize = this.page.pageSize
      }
      return params
    },
    updateDateList(scheduleDate) {
      this.dateList = this.buildDateList(scheduleDate || this.query.scheduleDate || this.search.scheduleDate)
    },
    handleSearch(data) {
      this.search = { ...this.search, ...data }
      this.query = Object.keys(data || {}).reduce((result, key) => {
        const value = data[key]
        if (value !== null && value !== undefined && value !== '') {
          result[key] = value
        }
        return result
      }, {})
      this.page.current = 1
      this.updateDateList(this.query.scheduleDate)
      this.getList()
      this.loadShiftConfig()
    },
    handleReset() {
      const defaultSearch = {
        factoryCode: '116',
        scheduleDate: this.getDefaultScheduleDate(),
        bigRollCode: '',
        machineId: '',
        isRelease: '',
        batchNo: '',
        orderNo: ''
      }
      this.search = { ...defaultSearch }
      this.query = { ...defaultSearch }
      this.page.current = 1
      this.updateDateList(defaultSearch.scheduleDate)
      this.getList()
      this.loadShiftConfig()
    },
    handlePageChange(current, pageSize) {
      this.page.current = current
      this.page.pageSize = pageSize
      this.getList()
    },
    handleSelectionChange(selection) {
      this.selection = selection
    },
    handleSortChange({ prop, order }) {
      this.sort = {
        orderByColumn: prop,
        isAsc: order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : undefined
      }
      this.getList()
    },
    handleAutoSchedule() {
      this.$refs.autoScheduleRef.show({
        factoryCode: this.search.factoryCode,
        scheduleDate: this.search.scheduleDate
      })
    },
    handleAutoScheduleSuccess(scheduleDate, payload) {
      if (scheduleDate) {
        this.query = { ...this.query, scheduleDate }
        this.search = { ...this.search, scheduleDate }
        this.updateDateList(scheduleDate)
      }
      const data = payload || {}
      if (data.taskId) {
        this.pollAutoScheduleTask(data.taskId)
      } else {
        this.page.current = 1
        this.getList()
      }
    },
    pollAutoScheduleTask(taskId) {
      this.clearAutoScheduleTimer()
      this.autoSchedulePollTimes = 0
      this.autoScheduleProgressVisible = true
      this.autoScheduleProgressValue = 0
      this.autoScheduleProgressStage = ''
      this.autoScheduleProgressStatus = null
      this.autoScheduleProgressHint = this.$t('ui.data.column.xwyyScheduleResult.autoScheduleProgressHint')
      const poll = () => {
        getAutoScheduleTask(taskId).then(res => {
          this.autoSchedulePollTimes += 1
          const task = (res && res.data) ? res.data : (res || {})
          if (task.progress != null) {
            this.autoScheduleProgressValue = Math.min(100, Math.max(0, task.progress))
          }
          if (task.currentStageName) {
            this.autoScheduleProgressStage = task.currentStageName
          }
          if (task.taskStatus === 'SUCCESS') {
            this.clearAutoScheduleTimer()
            this.autoScheduleProgressValue = 100
            this.autoScheduleProgressStatus = 'success'
            this.autoScheduleProgressStage = this.$t('ui.data.column.xwyyScheduleResult.autoScheduleSuccess')
            window.setTimeout(() => { this.closeAutoScheduleProgress() }, 600)
            this.$modal.msgSuccess(this.$t('ui.data.column.xwyyScheduleResult.autoScheduleSuccess'))
            this.getList()
            return
          }
          if (task.taskStatus === 'FAILED') {
            this.clearAutoScheduleTimer()
            this.autoScheduleProgressStatus = 'exception'
            this.autoScheduleProgressStage = this.$t('ui.data.column.xwyyScheduleResult.autoScheduleFailed')
            this.$modal.msgError(task.errorMessage || this.$t('ui.data.column.xwyyScheduleResult.autoScheduleFailed'))
            window.setTimeout(() => { this.closeAutoScheduleProgress() }, 3000)
            return
          }
          if (this.autoSchedulePollTimes >= this.maxAutoSchedulePollTimes) {
            this.clearAutoScheduleTimer()
            this.autoScheduleProgressStatus = 'exception'
            this.autoScheduleProgressStage = this.$t('ui.data.column.cxScheduleResult.scheduleTimeout')
            this.$modal.msgWarning(this.$t('ui.data.column.cxScheduleResult.scheduleTimeout'))
            window.setTimeout(() => { this.closeAutoScheduleProgress() }, 3000)
            return
          }
          this.autoScheduleTimer = window.setTimeout(poll, 3000)
        }).catch(() => {
          this.clearAutoScheduleTimer()
          this.autoScheduleProgressStatus = 'exception'
          this.autoScheduleProgressStage = this.$t('ui.data.column.cxScheduleResult.scheduleTimeout')
          this.$modal.msgWarning(this.$t('ui.data.column.cxScheduleResult.scheduleTimeout'))
          window.setTimeout(() => { this.closeAutoScheduleProgress() }, 3000)
        })
      }
      poll()
    },
    closeAutoScheduleProgress() {
      this.autoScheduleProgressVisible = false
      this.autoScheduleProgressValue = 0
      this.autoScheduleProgressStage = ''
      this.autoScheduleProgressStatus = null
      this.autoScheduleProgressHint = ''
    },
    clearAutoScheduleTimer() {
      if (this.autoScheduleTimer) {
        window.clearTimeout(this.autoScheduleTimer)
        this.autoScheduleTimer = null
      }
    },
    handleInsert() {
      this.showPendingActionMessage()
    },
    handleChangeMachine() {
      this.showPendingActionMessage()
    },
    handleChangePlan() {
      this.showPendingActionMessage()
    },
    handlePublish() {
      const ids = this.selection.map(item => item.id).join(',')
      this.$refs.releaseStatusRef.show(ids, this.query.scheduleDate, this.query.factoryCode)
    },
    showPendingActionMessage() {
      this.$message.warning('\u8be5\u6d41\u7a0b\u5165\u53e3\u5df2\u9884\u7559\uff0c\u5f85 xwyy \u5bf9\u5e94\u63a5\u53e3\u548c\u5f39\u7a97\u5b8c\u6210\u540e\u63a5\u5165')
    },
    handleBatchDelete() {
      const ids = this.selection.map(item => item.id)
      if (!ids.length) {
        this.$message.warning(this.$t('ui.message.pleaseSelectData'))
        return
      }
      this.$confirm(this.$t('ui.message.deleteConfirm'), this.$t('ui.message.tips'), { type: 'warning' })
        .then(() => removeScheduleResult({ ids: ids.join(',') }))
        .then(() => {
          this.$message.success(this.$t('ui.message.deleteSuccess'))
          this.getList()
        })
    },
    handleExport() {
      exportScheduleResult(this.formatParams(false))
    },
    getList() {
      this.loading = true
      listScheduleResult(this.formatParams())
        .then(res => {
          const rows = res.rows || res.data || []
          this.data = rows
          this.page.total = res.total || 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadShiftConfig() {
      const factoryCode = this.query.factoryCode || this.search.factoryCode
      if (!factoryCode) return
      listShiftConfig({ factoryCode, isActive: 1, pageNum: 1, pageSize: 200 })
        .then(res => {
          const rows = res.rows || res.data || []
          // 按 scheduleDay、dayShiftOrder 排序
          rows.sort((a, b) => {
            const dayA = a.scheduleDay || 1
            const dayB = b.scheduleDay || 1
            if (dayA !== dayB) return dayA - dayB
            return (a.dayShiftOrder || 0) - (b.dayShiftOrder || 0)
          })
          this.shiftConfig = rows
          this.updateDateList(this.query.scheduleDate || this.search.scheduleDate)
        })
    }
  }
}
</script>
