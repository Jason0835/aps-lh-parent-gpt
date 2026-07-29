<template>
  <basic-container>
    <page-table
      tableRef="cd15ScheduleResultMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      :isReset="true"
      :showSummary="false"
      :selectArea="false"
      @refresh="getList"
      @search="handleSearch"
      @reset="handleReset"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
    >
      <template slot="header">
        <el-button type="warning" v-hasPermi="['cd15:cd15ScheduleResult:autoSchedule']" @click="handleAutoSchedule">
          {{ $t("ui.data.column.scheduleResult.autoPlan") }}
        </el-button>
        <el-button type="warning" v-hasPermi="['cd15:cd15ScheduleResult:insert']" @click="handleInsert">
          {{ $t("ui.data.column.scheduleResult.insertOrder") }}
        </el-button>
        <el-button type="primary" v-hasPermi="['cd15:cd15ScheduleResult:changeMachine']" :disabled="selection.length !== 1" @click="handleTransferMachine">
          {{ $t("ui.data.column.scheduleResult.changeMachine") }}
        </el-button>
        <el-button type="primary" v-hasPermi="['cd15:cd15ScheduleResult:adjustQty']" :disabled="selection.length !== 1" @click="handleChangeQty">
          {{ $t("ui.data.column.scheduleResult.changePlan") }}
        </el-button>
        <el-button type="danger" v-hasPermi="['cd15:cd15ScheduleResult:remove']" :disabled="selection.length === 0" @click="handleBatchDelete">
          {{ $t("ui.frame.btn.delete") }}
        </el-button>
        <el-button v-hasPermi="['cd15:cd15ScheduleResult:publish']" :disabled="selection.length === 0" @click="handlePublish">
          {{ $t("ui.data.column.scheduleResult.publish") }}
        </el-button>
        <el-button v-hasPermi="['cd15:cd15ScheduleResult:export']" @click="handleExport">
          {{ $t("ui.frame.btn.export") }}
        </el-button>
        <el-button type="primary" v-hasPermi="['cd15:cd15ScheduleResult:list']" @click="handleShowUnscheduleResult">
          {{ $t("ui.data.column.scheduleResult.unscheduleResult") }}
        </el-button>
      </template>
    </page-table>

    <auto-schedule-dialog
      ref="autoScheduleRef"
      @success="handleAutoScheduleSuccess"
    />
    <insert-order-dialog
      ref="insertOrderRef"
      @success="handleInsertSuccess"
    />
    <change-machine-dialog
      ref="changeMachineRef"
      @success="handleChangeMachineSuccess"
    />
    <change-qty-dialog
      ref="changeQtyRef"
      @success="handleChangeQtySuccess"
    />
    <el-dialog
      :title="scheduleTaskTitle"
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
    <el-dialog
      :title="$t('ui.data.column.scheduleResult.unscheduleResult')"
      :visible.sync="unscheduleResultDialogVisible"
      width="80%"
      append-to-body
    >
      <page-table
        v-loading="unscheduleLoading"
        :calcHeight="false"
        :columns="unscheduleColumns"
        :data="unscheduleData"
        :page="unschedulePage"
        :search="unscheduleSearch"
        :searchColumns="unscheduleSearchColumns"
        :showSummary="false"
        :selectArea="false"
        @search="handleUnscheduleSearch"
        @reset="handleUnscheduleReset"
        @pageChange="handleUnschedulePageChange"
      />
    </el-dialog>
  </basic-container>
</template>

<script>
import moment from 'moment'
import {
  delScheduleResult,
  exportScheduleResult,
  getAutoScheduleTask,
  getChangeQtyTask,
  getInsertTask,
  getTransferMachineTask,
  listScheduleResult,
  publishScheduleResult
} from '@/api/cd15/scheduleResult'
import { getCd15MachineEnableOptions } from '@/api/cd15/cd15MachineInfo'
import { listUnscheduleResult } from '@/api/cd15/unscheduleResult'
import AutoScheduleDialog from './components/autoScheduleDialog.vue'
import InsertOrderDialog from './components/insertOrderDialog.vue'
import ChangeMachineDialog from './components/changeMachineDialog.vue'
import ChangeQtyDialog from './components/changeQtyDialog.vue'

const SHIFT_CONFIG = [
  { classField: 'class1', shiftKey: 'middleShift', dayOffset: -1 },
  { classField: 'class2', shiftKey: 'nightShift', dayOffset: 0 },
  { classField: 'class3', shiftKey: 'morningShift', dayOffset: 0 },
  { classField: 'class4', shiftKey: 'middleShift', dayOffset: 0 },
  { classField: 'class5', shiftKey: 'nightShift', dayOffset: 1 },
  { classField: 'class6', shiftKey: 'morningShift', dayOffset: 1 }
]

export default {
  name: 'Cd15ScheduleResult',
  components: { AutoScheduleDialog, InsertOrderDialog, ChangeMachineDialog, ChangeQtyDialog },
  dicts: ['biz_factory_name', 'IS_RELEASE'],
  provide() {
    return {
      parentDict: this.dict
    }
  },
  data() {
    const defaultScheduleDate = moment().add(1, 'days').format('YYYY-MM-DD')
    return {
      loading: false,
      data: [],
      selection: [],
      machineOptions: [],
      dateList: this.buildDateList(defaultScheduleDate),
      page: { current: 1, pageSize: 20, total: 0 },
      sort: {},
      search: { factoryCode: '116', scheduleDate: defaultScheduleDate },
      query: { factoryCode: '116', scheduleDate: defaultScheduleDate },
      autoScheduleTimer: null,
      autoSchedulePollTimes: 0,
      maxAutoSchedulePollTimes: 120,
      autoScheduleProgressVisible: false,
      autoScheduleProgressValue: 0,
      autoScheduleProgressStage: '',
      autoScheduleProgressStatus: null,
      autoScheduleProgressHint: '',
      scheduleTaskTitle: this.$t('ui.data.column.cd15ScheduleResult.autoScheduleProgress'),
      unscheduleResultDialogVisible: false,
      unscheduleLoading: false,
      unscheduleData: [],
      unschedulePage: { current: 1, pageSize: 20, total: 0, pageSizes: [10, 20, 50, 100] },
      unscheduleSearch: { factoryCode: '116', scheduleDate: defaultScheduleDate },
      unscheduleQuery: { factoryCode: '116', scheduleDate: defaultScheduleDate }
    }
  },
  computed: {
    columns() {
      return [
        { type: 'selection', fixed: 'left' },
        {
          label: this.$t('ui.data.column.cd15ScheduleResult.factoryCode'),
          prop: 'factoryCode',
          minWidth: 110,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value)
        },
        { label: this.$t('ui.data.column.cd15ScheduleResult.scheduleDate'), prop: 'scheduleDate', minWidth: 120, sortable: 'custom' },
        {
          label: this.$t('ui.data.column.cd15ScheduleResult.releaseStatus'),
          prop: 'releaseStatus',
          minWidth: 100,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.IS_RELEASE, value)
        },
        { label: this.$t('ui.data.column.cd15ScheduleResult.groupNo'), prop: 'groupNo', minWidth: 150 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.cxBatchNo'), prop: 'cxBatchNo', minWidth: 220, showOverflowTooltip: true },
        { label: this.$t('ui.data.column.cd15ScheduleResult.cxMachineCodes'), prop: 'cxMachineCodes', minWidth: 180, showOverflowTooltip: true },
        { label: this.$t('ui.data.column.cd15ScheduleResult.steelStripCode'), prop: 'steelStripCode', minWidth: 150 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.cuttingAngle'), prop: 'cuttingAngle', minWidth: 110 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.bigRollCode'), prop: 'bigRollCode', minWidth: 140 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.machineCode'), prop: 'machineCode', minWidth: 120 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.storageLaneCode'), prop: 'storageLaneCode', minWidth: 160, showOverflowTooltip: true },
        { label: this.$t('ui.data.column.cd15ScheduleResult.stockQty'), prop: 'stockQty', minWidth: 110, align: 'right' },
        { label: this.$t('ui.data.column.cd15ScheduleResult.planSurplusQty'), prop: 'planSurplusQty', minWidth: 140, align: 'right' },
        ...this.buildShiftColumns(),
        { label: this.$t('ui.common.column.remark'), prop: 'remark', minWidth: 160 }
      ]
    },
    searchColumns() {
      return [
        { label: this.$t('ui.data.column.cd15ScheduleResult.factoryCode'), prop: 'factoryCode', type: 'select', dictData: this.dict.type.biz_factory_name, filterable: true },
        { label: this.$t('ui.data.column.cd15ScheduleResult.scheduleDate'), prop: 'scheduleDate', type: 'date', valueFormat: 'yyyy-MM-dd' },
        { label: this.$t('ui.data.column.cd15ScheduleResult.steelStripCode'), prop: 'steelStripCode' },
        { label: this.$t('ui.data.column.cd15ScheduleResult.bigRollCode'), prop: 'bigRollCode' },
        { label: this.$t('ui.data.column.cd15ScheduleResult.machineCode'), prop: 'machineCode', type: 'select', dictData: this.machineOptions, filterable: true, clearable: true },
        { label: this.$t('ui.data.column.cd15ScheduleResult.releaseStatus'), prop: 'releaseStatus', type: 'select', dictData: this.dict.type.IS_RELEASE, clearable: true }
      ]
    },
    unscheduleColumns() {
      return [
        { label: this.$t('ui.data.column.cd15UnscheduleResult.scheduleDate'), prop: 'scheduleDate', align: 'center', minWidth: 120 },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.batchNo'), prop: 'batchNo', align: 'left', minWidth: 160 },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.steelStripCode'), prop: 'steelStripCode', minWidth: 150 },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.bigRollCode'), prop: 'bigRollCode', minWidth: 140 },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.cuttingAngle'), prop: 'cuttingAngle', minWidth: 110 },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.demandQty'), prop: 'demandQty', minWidth: 120, align: 'right' },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.scheduledQty'), prop: 'scheduledQty', minWidth: 120, align: 'right' },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.unscheduledQty'), prop: 'unscheduledQty', minWidth: 120, align: 'right' },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.failStage'), prop: 'failStage', minWidth: 140 },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.unscheduleReasonCode'), prop: 'unscheduleReasonCode', minWidth: 180 },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.unscheduledReason'), prop: 'unscheduledReason', minWidth: 260, showOverflowTooltip: true }
      ]
    },
    unscheduleSearchColumns() {
      return [
        { label: this.$t('ui.data.column.cd15UnscheduleResult.factoryCode'), prop: 'factoryCode', type: 'select', dictData: this.dict.type.biz_factory_name, filterable: true },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.scheduleDate'), prop: 'scheduleDate', type: 'date', valueFormat: 'yyyy-MM-dd' },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.steelStripCode'), prop: 'steelStripCode' },
        { label: this.$t('ui.data.column.cd15UnscheduleResult.batchNo'), prop: 'batchNo' }
      ]
    }
  },
  created() {
    this.getList()
    this.loadMachineOptions()
  },
  beforeDestroy() {
    this.clearAutoScheduleTimer()
  },
  methods: {
    buildDateList(scheduleDate) {
      return SHIFT_CONFIG.map(item => ({ ...item, shiftDate: moment(scheduleDate).add(item.dayOffset, 'days').format('MM/DD') }))
    },
    buildShiftColumns() {
      return SHIFT_CONFIG.map((item, index) => {
        const classField = item.classField
        const shiftName = this.$t(`ui.data.column.scheduleResult.${item.shiftKey}`)
        const dateItem = this.dateList[index] || {}
        return {
          label: `${shiftName} ${dateItem.shiftDate || ''}`,
          align: 'center',
          children: [
            { label: this.$t('ui.data.column.scheduleResult.plan'), prop: `${classField}PlanQty`, minWidth: 110 },
            { label: this.$t('ui.data.column.scheduleResult.actual'), prop: `${classField}FinishQty`, minWidth: 110, formatter: (row, column, cellValue) => cellValue === 0 || cellValue === '0' ? '' : cellValue },
            { label: this.$t('ui.data.column.scheduleResult.produceOrder'), prop: `${classField}ProduceOrder`, minWidth: 130 },
            { label: this.$t('ui.data.column.scheduleResult.finishRate'), prop: `${classField}FinishRate`, minWidth: 110 },
            { label: this.$t('ui.data.column.scheduleResult.analysis'), prop: `${classField}Analysis`, minWidth: 140 }
          ]
        }
      })
    },
    handleSearch(params) {
      this.page.current = 1
      this.query = { ...params }
      this.search = { ...this.search, ...params }
      this.dateList = this.buildDateList(this.query.scheduleDate || this.search.scheduleDate)
      this.getList()
      this.loadMachineOptions()
    },
    handleReset() {
      const scheduleDate = moment().add(1, 'days').format('YYYY-MM-DD')
      this.search = { factoryCode: '116', scheduleDate, steelStripCode: '', bigRollCode: '', machineCode: '', releaseStatus: '' }
      this.query = { ...this.search }
      this.dateList = this.buildDateList(scheduleDate)
      this.page.current = 1
      this.getList()
      this.loadMachineOptions()
    },
    handlePageChange(current, pageSize) {
      this.page.current = current
      this.page.pageSize = pageSize
      this.getList()
    },
    handleSortChange(sort) {
      this.sort = sort || {}
      this.getList()
    },
    handleSelectionChange(selection) {
      this.selection = selection || []
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
        this.dateList = this.buildDateList(scheduleDate)
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
      this.pollScheduleTask(taskId, getAutoScheduleTask, {
        titleKey: 'ui.data.column.cd15ScheduleResult.autoScheduleProgress',
        hintKey: 'ui.data.column.cd15ScheduleResult.autoScheduleProgressHint',
        successKey: 'ui.data.column.cd15ScheduleResult.autoScheduleSuccess',
        failedKey: 'ui.data.column.cd15ScheduleResult.autoScheduleFailed'
      })
    },
    pollScheduleTask(taskId, taskGetter, options = {}) {
      this.clearAutoScheduleTimer()
      this.autoSchedulePollTimes = 0
      this.autoScheduleProgressVisible = true
      this.autoScheduleProgressValue = 0
      this.autoScheduleProgressStage = ''
      this.autoScheduleProgressStatus = null
      this.autoScheduleProgressHint = this.$t(options.hintKey || 'ui.data.column.cd15ScheduleResult.autoScheduleProgressHint')
      this.scheduleTaskTitle = this.$t(options.titleKey || 'ui.data.column.cd15ScheduleResult.autoScheduleProgress')
      const successKey = options.successKey || 'ui.data.column.cd15ScheduleResult.autoScheduleSuccess'
      const failedKey = options.failedKey || 'ui.data.column.cd15ScheduleResult.autoScheduleFailed'
      const poll = () => {
        taskGetter(taskId).then(res => {
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
            this.autoScheduleProgressStage = this.$t(successKey)
            window.setTimeout(() => { this.closeAutoScheduleProgress() }, 600)
            this.$modal.msgSuccess(this.$t(successKey))
            this.getList()
            if (this.unscheduleResultDialogVisible) {
              this.getUnscheduleList()
            }
            return
          }
          if (task.taskStatus === 'FAILED') {
            this.clearAutoScheduleTimer()
            this.autoScheduleProgressStatus = 'exception'
            this.autoScheduleProgressStage = this.$t(failedKey)
            this.$modal.msgError(task.errorMessage || this.$t(failedKey))
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
    handleShowUnscheduleResult() {
      this.unscheduleResultDialogVisible = true
      this.unscheduleSearch = {
        factoryCode: this.search.factoryCode,
        scheduleDate: this.search.scheduleDate
      }
      this.unscheduleQuery = { ...this.unscheduleSearch }
      this.unschedulePage.current = 1
      this.getUnscheduleList()
    },
    handleUnscheduleSearch(data) {
      this.unscheduleQuery = Object.keys(data || {}).reduce((result, key) => {
        const value = data[key]
        if (value !== null && value !== undefined && value !== '') {
          result[key] = value
        }
        return result
      }, {})
      this.unschedulePage.current = 1
      this.getUnscheduleList()
    },
    handleUnscheduleReset() {
      const scheduleDate = moment().add(1, 'days').format('YYYY-MM-DD')
      this.unscheduleSearch = { factoryCode: '116', scheduleDate }
      this.unscheduleQuery = { ...this.unscheduleSearch }
      this.unschedulePage.current = 1
      this.getUnscheduleList()
    },
    handleUnschedulePageChange(current, pageSize) {
      this.unschedulePage.current = current
      this.unschedulePage.pageSize = pageSize
      this.getUnscheduleList()
    },
    getUnscheduleList() {
      this.unscheduleLoading = true
      listUnscheduleResult({
        ...this.unscheduleQuery,
        pageNum: this.unschedulePage.current,
        pageSize: this.unschedulePage.pageSize
      }).then(res => {
        this.unscheduleData = res.rows || res.data || []
        this.unschedulePage.total = res.total || 0
      }).finally(() => {
        this.unscheduleLoading = false
      })
    },
    handleInsert() {
      this.$refs.insertOrderRef.show({
        factoryCode: this.search.factoryCode,
        scheduleDate: this.search.scheduleDate
      })
    },
    handleInsertSuccess(scheduleDate, payload) {
      if (scheduleDate) {
        this.query = { ...this.query, scheduleDate }
        this.search = { ...this.search, scheduleDate }
        this.dateList = this.buildDateList(scheduleDate)
      }
      const data = payload || {}
      if (data.taskId) {
        this.handleTaskResult({ data }, getInsertTask).then(() => this.getList())
      } else {
        this.getList()
      }
    },
    handleTransferMachine() {
      this.$refs.changeMachineRef.show(this.selection[0])
    },
    handleChangeMachineSuccess(scheduleDate, payload) {
      if (scheduleDate) {
        this.query = { ...this.query, scheduleDate }
        this.search = { ...this.search, scheduleDate }
        this.dateList = this.buildDateList(scheduleDate)
      }
      const data = payload || {}
      if (data.taskId) {
        this.pollScheduleTask(data.taskId, getTransferMachineTask, {
          titleKey: 'ui.data.column.cd15ScheduleResult.changeMachineTitle',
          hintKey: 'ui.data.column.cd15ScheduleResult.changeMachineProgressHint',
          successKey: 'ui.data.column.cd15ScheduleResult.changeMachineSuccess',
          failedKey: 'ui.data.column.cd15ScheduleResult.changeMachineFailed'
        })
      } else {
        this.getList()
      }
    },
    handleChangeQty() {
      this.$refs.changeQtyRef.show(this.selection[0])
    },
    handleChangeQtySuccess(scheduleDate, payload) {
      if (scheduleDate) {
        this.query = { ...this.query, scheduleDate }
        this.search = { ...this.search, scheduleDate }
        this.dateList = this.buildDateList(scheduleDate)
      }
      const data = payload || {}
      if (data.taskId) {
        this.pollScheduleTask(data.taskId, getChangeQtyTask, {
          titleKey: 'ui.data.column.cd15ScheduleResult.changeQtyTitle',
          hintKey: 'ui.data.column.cd15ScheduleResult.changeQtyProgressHint',
          successKey: 'ui.data.column.cd15ScheduleResult.changeQtySuccess',
          failedKey: 'ui.data.column.cd15ScheduleResult.changeQtyFailed'
        })
      } else {
        this.getList()
      }
    },
    handleTaskResult(res, taskGetter) {
      const data = res && res.data ? res.data : {}
      if (data.taskId) {
        return taskGetter(data.taskId).then(taskRes => {
          const task = taskRes && taskRes.data ? taskRes.data : taskRes
          if (task && task.taskStatus === 'SUCCESS') {
            this.$modal.msgSuccess(res.msg || this.$t('ui.message.operation.success'))
          }
        })
      }
      this.$modal.msgSuccess(res.msg || this.$t('ui.message.operation.success'))
      return Promise.resolve()
    },
    handleBatchDelete() {
      const ids = this.selection.map(item => item.id).join(',')
      if (!ids) {
        return
      }
      this.$confirm(this.$t('common.confirm.delete'), { type: 'warning' }).then(() => {
        delScheduleResult({ ids }).then(res => {
          this.$modal.msgSuccess(res.msg)
          this.selection = []
          this.getList()
        })
      })
    },
    handlePublish() {
      const ids = this.selection.map(item => item.id).join(',')
      return this.$confirm(this.$t('ui.biz.alter.makeSurePublish'), { type: 'warning' })
        .then(() => publishScheduleResult({
          ids,
          factoryCode: this.query.factoryCode,
          scheduleDate: this.query.scheduleDate
        }))
        .then(res => {
          this.$modal.msgSuccess(res.msg)
          this.getList()
        })
    },
    handleExport() {
      exportScheduleResult(this.formatParams(false))
    },
    formatParams(hasPage = true) {
      const params = { ...this.query, orderByColumn: this.sort.prop, isAsc: this.sort.order }
      if (hasPage) {
        params.pageNum = this.page.current
        params.pageSize = this.page.pageSize
      }
      return params
    },
    async getList() {
      this.loading = true
      try {
        const res = await listScheduleResult(this.formatParams())
        this.data = res.rows || res.data || []
        this.page.total = res.total || 0
      } finally {
        this.loading = false
      }
    },
    loadMachineOptions() {
      getCd15MachineEnableOptions({ factoryCode: this.query.factoryCode || this.search.factoryCode }).then(res => {
        const rows = Array.isArray(res) ? res : (res.rows || res.data || [])
        this.machineOptions = rows.map(item => ({
          label: item.machineCode || item.label || item.value,
          value: item.machineCode || item.value
        }))
      })
    }
  }
}
</script>
