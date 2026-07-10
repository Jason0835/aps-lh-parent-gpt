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
      </template>
    </page-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="520px" append-to-body>
      <el-form ref="actionForm" :model="actionForm" label-width="110px">
        <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.factoryCode')">
          <el-input v-model="actionForm.factoryCode" />
        </el-form-item>
        <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.scheduleDate')">
          <el-date-picker v-model="actionForm.scheduleDate" type="date" value-format="yyyy-MM-dd" style="width: 100%;" />
        </el-form-item>
        <el-form-item v-if="dialogType !== 'autoSchedule'" :label="$t('ui.data.column.cd15ScheduleResult.machineCode')">
          <el-input v-model="actionForm.machineCode" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'transferMachine'" :label="$t('ui.data.column.cd15ScheduleResult.targetMachineCode')">
          <el-select v-model="actionForm.targetMachineCode" filterable clearable style="width: 100%;">
            <el-option v-for="item in machineOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dialogType === 'insert' || dialogType === 'changeQty'" :label="$t('ui.data.column.cd15ScheduleResult.steelStripCode')">
          <el-input v-model="actionForm.steelStripCode" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'insert' || dialogType === 'changeQty'" :label="$t('ui.data.column.scheduleResult.plan')">
          <el-input-number v-model="actionForm.class1PlanQty" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'insert'" :label="$t('ui.data.column.scheduleResult.produceOrder')">
          <el-input-number v-model="actionForm.class1ProduceOrder" :min="1" :precision="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item :label="$t('ui.common.column.remark')">
          <el-input v-model="actionForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">{{ $t("ui.frame.btn.cancel") }}</el-button>
        <el-button type="primary" @click="submitAction">{{ $t("ui.frame.btn.submit") }}</el-button>
      </span>
    </el-dialog>
  </basic-container>
</template>

<script>
import moment from 'moment'
import {
  autoSchedule,
  changeQty,
  delScheduleResult,
  exportScheduleResult,
  getAutoScheduleTask,
  getChangeQtyTask,
  getInsertTask,
  getTransferMachineTask,
  insert,
  listScheduleResult,
  publishScheduleResult,
  transferMachine,
  validateChangeQty,
  validateInsert,
  validateTransferMachine
} from '@/api/cd15/scheduleResult'
import { getCd15MachineEnableOptions } from '@/api/cd15/cd15MachineInfo'

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
  dicts: ['biz_factory_name', 'IS_RELEASE'],
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
      dialogVisible: false,
      dialogType: '',
      dialogTitle: '',
      actionForm: {}
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
        { label: this.$t('ui.data.column.cd15ScheduleResult.steelStripCode'), prop: 'steelStripCode', minWidth: 150 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.cuttingAngle'), prop: 'cuttingAngle', minWidth: 110 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.bigRollCode'), prop: 'bigRollCode', minWidth: 140 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.machineCode'), prop: 'machineCode', minWidth: 120 },
        { label: this.$t('ui.data.column.cd15ScheduleResult.storageLaneCode'), prop: 'storageLaneCode', minWidth: 160, showOverflowTooltip: true },
        { label: this.$t('ui.data.column.cd15ScheduleResult.stockQty'), prop: 'stockQty', minWidth: 110, align: 'right' },
        { label: this.$t('ui.data.column.cd15ScheduleResult.monthSurplusQty'), prop: 'monthSurplusQty', minWidth: 140, align: 'right' },
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
    }
  },
  created() {
    this.getList()
    this.loadMachineOptions()
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
            { label: this.$t('ui.data.column.scheduleResult.actual'), prop: `${classField}FinishQty`, minWidth: 110 },
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
      this.search = { factoryCode: '116', scheduleDate }
      this.query = { ...this.search }
      this.dateList = this.buildDateList(scheduleDate)
      this.page.current = 1
      this.getList()
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
      this.openActionDialog('autoSchedule')
    },
    handleInsert() {
      this.openActionDialog('insert')
    },
    handleTransferMachine() {
      this.openActionDialog('transferMachine', this.selection[0])
    },
    handleChangeQty() {
      this.openActionDialog('changeQty', this.selection[0])
    },
    openActionDialog(type, row = {}) {
      this.dialogType = type
      this.dialogTitle = this.resolveDialogTitle(type)
      this.actionForm = {
        factoryCode: row.factoryCode || this.query.factoryCode || this.search.factoryCode,
        scheduleDate: row.scheduleDate || this.query.scheduleDate || this.search.scheduleDate,
        machineCode: row.machineCode,
        sourceMachineCode: row.machineCode,
        steelStripCode: row.steelStripCode,
        scheduleResultId: row.id,
        class1PlanQty: row.class1PlanQty,
        class1ProduceOrder: row.class1ProduceOrder
      }
      this.dialogVisible = true
    },
    resolveDialogTitle(type) {
      const titleMap = {
        autoSchedule: this.$t('ui.data.column.scheduleResult.autoPlan'),
        insert: this.$t('ui.data.column.scheduleResult.insertOrder'),
        transferMachine: this.$t('ui.data.column.scheduleResult.changeMachine'),
        changeQty: this.$t('ui.data.column.scheduleResult.changePlan')
      }
      return titleMap[type] || ''
    },
    submitAction() {
      const actionMap = {
        autoSchedule: () => autoSchedule(this.actionForm).then(res => this.handleTaskResult(res, getAutoScheduleTask)),
        insert: () => validateInsert(this.actionForm).then(() => insert(this.actionForm)).then(res => this.handleTaskResult(res, getInsertTask)),
        transferMachine: () => validateTransferMachine(this.actionForm).then(() => transferMachine(this.actionForm)).then(res => this.handleTaskResult(res, getTransferMachineTask)),
        changeQty: () => validateChangeQty(this.actionForm).then(() => changeQty(this.actionForm)).then(res => this.handleTaskResult(res, getChangeQtyTask))
      }
      const action = actionMap[this.dialogType]
      if (!action) {
        return
      }
      action().then(() => {
        this.dialogVisible = false
        this.getList()
      })
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
      publishScheduleResult({ ids, factoryCode: this.query.factoryCode, scheduleDate: this.query.scheduleDate }).then(res => {
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
