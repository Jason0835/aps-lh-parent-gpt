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
      @search="handleSearch"
      @reset="handleReset"
      @refresh="getList"
      @pageChange="handlePageChange"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['cd90:scheduleResult:autoSchedule']"
          type="warning"
          @click="handleAutoSchedule"
        >{{ $t('ui.data.column.scheduleResult.autoPlan') }}</el-button>
        <el-button
          v-hasPermi="['cd90:scheduleResult:insert']"
          type="warning"
          @click="handleInsert"
        >{{ $t('ui.data.column.scheduleResult.insertOrder') }}</el-button>
        <el-button
          v-hasPermi="['cd90:scheduleResult:remove']"
          type="danger"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t('ui.frame.btn.delete') }}</el-button>
        <el-button
          v-hasPermi="['cd90:scheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length !== 1"
          @click="handleChangeMachine"
        >{{ $t('ui.data.column.scheduleResult.changeMachine') }}</el-button>
        <el-button
          v-hasPermi="['cd90:scheduleResult:adjustQty']"
          type="primary"
          :disabled="selection.length !== 1"
          @click="handleChangePlan"
        >{{ $t('ui.data.column.scheduleResult.changePlan') }}</el-button>
        <el-button
          v-hasPermi="['cd90:scheduleResult:publish']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
        >{{ $t('ui.data.column.scheduleResult.publish') }}</el-button>
        <el-button
          v-hasPermi="['cd90:scheduleResult:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t('ui.frame.btn.import') }}</el-button>
        <el-button
          v-hasPermi="['cd90:scheduleResult:export']"
          @click="handleExport"
        >{{ $t('ui.frame.btn.export') }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :update-support="true"
      download-url="/cd90/cd90ScheduleResult/importTemplate"
      upload-url="/cd90/cd90ScheduleResult/importData"
      label-width="0"
      :columns="importColumns"
      @uploadSuccess="getList"
    />
    <release-status-dialog
      ref="releaseStatusRef"
      :schedule-date="query.scheduleDate"
      @success="getList"
    />
  </basic-container>
</template>

<script>
import moment from 'moment'
import { autoScheduleResult, getAutoScheduleTask, listScheduleResult, delScheduleResult, exportScheduleResult } from '@/api/cd90/scheduleResult'
import { listTireFabricCodes } from '@/api/cd90/specifyMachine'
import { getCd90MachineEnableOptions } from '@/api/cd90/cd90MachineInfo'
import TltUploadForm from '@/views/components/tltUploadForm.vue'
import ReleaseStatusDialog from './components/releaseStatusDialog.vue'

const SHIFT_CONFIG = [
  { classField: 'class1', shiftKey: 'middleShift', dayOffset: -1 },
  { classField: 'class2', shiftKey: 'nightShift', dayOffset: 0 },
  { classField: 'class3', shiftKey: 'morningShift', dayOffset: 0 },
  { classField: 'class4', shiftKey: 'middleShift', dayOffset: 0 },
  { classField: 'class5', shiftKey: 'nightShift', dayOffset: 1 },
  { classField: 'class6', shiftKey: 'morningShift', dayOffset: 1 }
]

export default {
  name: 'Cd90ScheduleResult',
  components: { TltUploadForm, ReleaseStatusDialog },
  dicts: ['biz_factory_name', 'IS_RELEASE'],
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
      importColumns: [
        {
          label: '',
          prop: 'updateSupport',
          render: form => (
            <el-checkbox
              label={this.$t('common.rule.updateSupport')}
              v-model={form.updateSupport}
            >
              {this.$t('common.rule.updateSupport')}
            </el-checkbox>
          )
        }
      ],
      loading: false,
      data: [],
      selection: [],
      clothOptions: [],
      machineOptions: [],
      dateList: this.buildDateList(defaultScheduleDate),
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
        pageSizes: [10, 20, 50, 100]
      },
      sort: {},
      search: { ...defaultSearch },
      query: { ...defaultSearch }
    }
  },
  computed: {
    columns() {
      return [
        { type: 'selection', width: 55, fixed: 'left' },
        {
          label: this.$t('ui.data.column.cd90ScheduleResult.factoryCode'),
          prop: 'factoryCode',
          minWidth: 120,
          formatter: (row, column, cellValue) => this.selectDictLabel(this.dict.type.biz_factory_name, cellValue)
        },
        {
          label: this.$t('ui.data.column.cd90ScheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          minWidth: 120,
          sortable: 'custom'
        },
        {
          label: this.$t('ui.data.column.cd90ScheduleResult.isRelease'),
          prop: 'isRelease',
          minWidth: 100,
          formatter: (row, column, cellValue) => this.selectDictLabel(this.dict.type.IS_RELEASE, cellValue)
        },
        { label: this.$t('ui.data.column.cd90ScheduleResult.batchNo'), prop: 'batchNo', minWidth: 120 },
        { label: this.$t('ui.data.column.cd90ScheduleResult.orderNo'), prop: 'orderNo', minWidth: 140 },
        { label: this.$t('ui.data.column.cd90ScheduleResult.clothCode'), prop: 'clothCode', minWidth: 140 },
        { label: this.$t('ui.data.column.cd90ScheduleResult.machineCode'), prop: 'machineCode', minWidth: 120 },
        { label: this.$t('ui.data.column.cd90ScheduleResult.bigRollCode'), prop: 'bigRollCode', minWidth: 140 },
        { label: this.$t('ui.data.column.cd90ScheduleResult.storageLaneCode'), prop: 'storageLaneCode', minWidth: 140 },
        { label: this.$t('ui.data.column.cd90ScheduleResult.stockQty'), prop: 'stockQty', minWidth: 120 },
        { label: this.$t('ui.data.column.cd90ScheduleResult.supplyTime'), prop: 'supplyTime', minWidth: 160 },
        ...this.buildShiftColumns(),
        { label: this.$t('ui.data.column.cd90ScheduleResult.remark'), prop: 'remark', minWidth: 160 }
      ]
    },
    searchColumns() {
      return [
        {
          label: this.$t('ui.data.column.cd90ScheduleResult.factoryCode'),
          prop: 'factoryCode',
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true
        },
        {
          label: this.$t('ui.data.column.cd90ScheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          type: 'date',
          valueFormat: 'yyyy-MM-dd'
        },
        {
          label: this.$t('ui.data.column.cd90ScheduleResult.clothCode'),
          prop: 'clothCode',
          type: 'select',
          dictData: this.clothOptions,
          filterable: true,
          clearable: true
        },
        {
          label: this.$t('ui.data.column.cd90ScheduleResult.machineCode'),
          prop: 'machineCode',
          type: 'select',
          dictData: this.machineOptions,
          filterable: true,
          clearable: true
        },
        {
          label: this.$t('ui.data.column.cd90ScheduleResult.isRelease'),
          prop: 'isRelease',
          type: 'select',
          dictData: this.dict.type.IS_RELEASE,
          filterable: true
        }
      ]
    }
  },
  created() {
    this.getList()
    this.loadClothOptions()
    this.loadMachineOptions()
  },
  methods: {
    getDefaultScheduleDate() {
      return moment().add(1, 'days').format('YYYY-MM-DD')
    },
    buildDateList(scheduleDate) {
      const baseDate = scheduleDate || this.getDefaultScheduleDate()
      return SHIFT_CONFIG.map(item => ({
        ...item,
        shiftDate: moment(baseDate).add(item.dayOffset, 'days').format('MM/DD')
      }))
    },
    buildShiftColumns() {
      return SHIFT_CONFIG.map((item, index) => {
        const dateItem = this.dateList[index] || {}
        const shiftName = this.$t(`ui.data.column.scheduleResult.${item.shiftKey}`)
        const label = `${shiftName} ${dateItem.shiftDate || ''}`
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
              label: this.$t('ui.data.column.scheduleResult.actual'),
              prop: `${classField}FinishQty`,
              minWidth: 110
            },
            {
              label: this.$t('ui.data.column.scheduleResult.produceOrder'),
              prop: `${classField}ProduceOrder`,
              minWidth: 150
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
            }
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
      this.loadMachineOptions()
    },
    handleReset() {
      const defaultSearch = {
        factoryCode: '116',
        scheduleDate: this.getDefaultScheduleDate()
      }
      this.search = { ...defaultSearch }
      this.query = { ...defaultSearch }
      this.page.current = 1
      this.updateDateList(defaultSearch.scheduleDate)
      this.getList()
      this.loadMachineOptions()
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
      const params = {
        factoryCode: this.query.factoryCode,
        scheduleDate: this.query.scheduleDate,
        forceRegenerate: false
      }
      this.submitAutoSchedule(params)
    },
    submitAutoSchedule(params) {
      autoScheduleResult(params).then(res => {
        const data = res.data || {}
        if (data.needConfirm) {
          return this.$confirm(res.msg, this.$t('ui.message.tips'), { type: 'warning' })
            .then(() => this.submitAutoSchedule({ ...params, forceRegenerate: true }))
        }
        this.$modal.msgSuccess(res.msg)
        if (data.taskId) this.pollAutoScheduleTask(data.taskId)
      })
    },
    pollAutoScheduleTask(taskId) {
      const poll = () => {
        getAutoScheduleTask(taskId).then(res => {
          const task = res.data || {}
          if (task.taskStatus === 'SUCCESS') {
            this.$modal.msgSuccess('自动排程执行完成')
            this.getList()
            return
          }
          if (task.taskStatus === 'FAILED') {
            this.$modal.msgError(task.errorMessage || '自动排程执行失败')
            return
          }
          setTimeout(poll, 2000)
        })
      }
      poll()
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
      this.$message.warning('\u8be5\u6d41\u7a0b\u5165\u53e3\u5df2\u9884\u7559\uff0c\u5f85 cd90 \u5bf9\u5e94\u63a5\u53e3\u548c\u5f39\u7a97\u5b8c\u6210\u540e\u63a5\u5165')
    },
    handleBatchDelete() {
      const ids = this.selection.map(item => item.id)
      if (!ids.length) {
        this.$message.warning(this.$t('ui.message.pleaseSelectData'))
        return
      }
      this.$confirm(this.$t('ui.message.deleteConfirm'), this.$t('ui.message.tips'), { type: 'warning' })
        .then(() => delScheduleResult({ ids: ids.join(',') }))
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
    loadClothOptions() {
      listTireFabricCodes().then(res => {
        const rows = Array.isArray(res) ? res : (res.rows || res.data || [])
        this.clothOptions = rows.map(item => ({
          label: item.label || item.clothCode || item.code || item,
          value: item.value || item.clothCode || item.code || item
        }))
      })
    },
    loadMachineOptions() {
      getCd90MachineEnableOptions({ factoryCode: this.query.factoryCode || this.search.factoryCode }).then(res => {
        const rows = Array.isArray(res) ? res : (res.rows || res.data || [])
        this.machineOptions = rows.map(item => ({
          label: item.label || item.machineName || item.machineCode || item.code || item,
          value: item.value || item.machineCode || item.code || item
        }))
      })
    }
  }
}
</script>
