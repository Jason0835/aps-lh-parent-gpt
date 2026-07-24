<template>
  <basic-container>
    <div class="summary-bar">
      <span>{{ $t('ui.tc.schedule.totalStockQty') }}：{{ summary.totalStockQty || 0 }}</span>
      <span>{{ $t('ui.tc.schedule.totalPlanQty') }}：{{ summary.totalPlanQty || 0 }}</span>
      <span>{{ $t('ui.tc.schedule.totalFinishQty') }}：{{ summary.totalFinishQty || 0 }}</span>
      <span>{{ $t('ui.tc.schedule.resultCount') }}：{{ summary.resultCount || 0 }}</span>
      <span>{{ $t('ui.tc.schedule.unplannedCount') }}：{{ unplannedCount || 0 }}</span>
      <span
        v-for="(planQty, index) in shiftPlanQtyList"
        :key="index"
      >{{ $t('ui.tc.schedule.shiftPlanQty', { shift: index + 1 }) }}：{{ planQty || 0 }}</span>
    </div>
    <page-table
      v-loading="loading"
      :calc-height="true"
      :columns="columns"
      :data="data"
      :page="page"
      :search="search"
      :search-columns="searchColumns"
      :select-area="false"
      :show-summary="false"
      table-ref="tcScheduleResultMainTable"
      @pageChange="handlePageChange"
      @refresh="getList"
      @search="handleSearch"
      @selection-change="handleSelectionChange"
    >
      <template slot="header">
        <el-button v-hasPermi="['tc:tcScheduleResult:autoPlan']" :disabled="writeTaskRunning" type="warning" @click="handleAutoPlan">
          {{ $t('ui.tc.schedule.autoPlan') }}
        </el-button>
        <el-button
          v-hasPermi="['tc:tcScheduleResult:publish']"
          :disabled="writeTaskRunning || selection.length === 0"
          type="success"
          @click="handleRelease"
        >
          {{ $t('ui.tc.schedule.publish') }}
        </el-button>
        <el-button v-hasPermi="['tc:tcScheduleResult:add']" :disabled="writeTaskRunning" type="warning" @click="handleAdd">
          {{ $t('ui.tc.schedule.insertTask') }}
        </el-button>
        <el-button
          v-hasPermi="['tc:tcScheduleResult:edit']"
          :disabled="writeTaskRunning || selection.length !== 1"
          type="warning"
          @click="handleChangeQty"
        >
          {{ $t('ui.tc.schedule.changeQty') }}
        </el-button>
        <el-button
          v-hasPermi="['tc:tcScheduleResult:changeMachine']"
          :disabled="writeTaskRunning || selection.length === 0"
          type="primary"
          @click="handleChangeMachine"
        >
          {{ $t('ui.tc.schedule.changeMachine') }}
        </el-button>
        <el-button
          v-hasPermi="['tc:tcScheduleResult:remove']"
          :disabled="writeTaskRunning || selection.length === 0"
          type="danger"
          @click="handleRemove"
        >
          {{ $t('ui.tc.schedule.delete') }}
        </el-button>
        <el-button v-hasPermi="['tc:tcScheduleResult:query']" plain type="info" @click="handleUnplanned">
          {{ $t('ui.tc.schedule.unplannedTasks') }}（{{ unplannedCount || 0 }}）
        </el-button>
      </template>
    </page-table>

    <auto-plan-dialog ref="autoPlanRef" @success="handleAutoPlanSuccess" />
    <insert-task-dialog ref="insertTaskRef" @success="handleOperationTask" />
    <change-qty-dialog ref="changeQtyRef" @success="handleOperationTask" />
    <change-machine-dialog ref="changeMachineRef" @success="handleOperationTask" />
    <unplanned-dialog ref="unplannedRef" />
    <explain-drawer ref="explainRef" />

    <el-dialog
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="$t('ui.tc.schedule.autoPlanProgress')"
      :visible.sync="autoPlanProgressVisible"
      append-to-body
      width="440px"
    >
      <div class="progress-stage">{{ autoPlanProgressStage }}</div>
      <el-progress
        :percentage="autoPlanProgressValue"
        :status="autoPlanProgressStatus"
        :stroke-width="18"
        :text-inside="true"
        text-color="#fff"
      />
      <div class="progress-hint">{{ $t('ui.tc.schedule.autoPlanProgressHint') }}</div>
    </el-dialog>

    <el-dialog
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="$t('ui.tc.schedule.operationProgress')"
      :visible.sync="operationProgressVisible"
      append-to-body
      width="440px"
    >
      <div class="progress-stage">{{ operationProgressStage }}</div>
      <el-progress
        :percentage="operationProgressValue"
        :status="operationProgressStatus"
        :stroke-width="18"
        :text-inside="true"
        text-color="#fff"
      />
      <div class="progress-hint">{{ $t('ui.tc.schedule.operationProgressHint') }}</div>
    </el-dialog>

    <el-dialog :title="$t('ui.tc.schedule.autoPlanIssues')" :visible.sync="autoPlanIssueVisible" append-to-body width="82%">
      <el-table :data="autoPlanIssues" border max-height="520">
        <el-table-column :label="$t('ui.tc.schedule.issueLevel')" prop="level" width="90" />
        <el-table-column :label="$t('ui.tc.schedule.issueStage')" prop="stageName" width="130" />
        <el-table-column :label="$t('ui.tc.schedule.issueCategory')" min-width="150" prop="category" />
        <el-table-column :label="$t('ui.tc.schedule.sidewallCode')" min-width="140" prop="sidewallCode" />
        <el-table-column :label="$t('ui.tc.schedule.shiftOrder')" prop="shiftOrder" width="90" />
        <el-table-column :label="$t('ui.tc.schedule.issueMessage')" min-width="240" prop="message" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="$t('ui.tc.schedule.releaseProgress')"
      :visible.sync="releaseProgressVisible"
      append-to-body
      width="440px"
    >
      <div class="progress-stage">{{ releaseProgressStage }}</div>
      <el-progress
        :percentage="releaseProgressValue"
        :status="releaseProgressStatus"
        :stroke-width="18"
        :text-inside="true"
        text-color="#fff"
      />
      <div class="progress-hint">{{ $t('ui.tc.schedule.releaseProgressHint') }}</div>
    </el-dialog>

    <el-dialog :title="$t('ui.tc.schedule.releaseIssues')" :visible.sync="releaseIssueVisible" append-to-body width="82%">
      <el-table :data="releaseIssues" border max-height="520">
        <el-table-column :label="$t('ui.tc.schedule.issueLevel')" prop="level" width="90" />
        <el-table-column :label="$t('ui.tc.schedule.issueStage')" prop="stageName" width="150" />
        <el-table-column :label="$t('ui.tc.schedule.issueCategory')" min-width="150" prop="category" />
        <el-table-column :label="$t('ui.tc.schedule.orderNo')" min-width="175" prop="orderNo" />
        <el-table-column :label="$t('ui.tc.schedule.issueMessage')" min-width="260" prop="message" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </basic-container>
</template>

<script>
import {
  getAutoPlanTask,
  getLatestAutoPlanTask,
  getLatestOperationTask,
  getLatestReleaseTask,
  getManualOptions,
  getOperationTask,
  getReleaseTask,
  queryScheduleBoard,
  releaseScheduleResult,
  removeScheduleResult,
  validateRelease
} from '@/api/tc/tcScheduleResult'
import AutoPlanDialog from './components/AutoPlanDialog.vue'
import ChangeMachineDialog from './components/ChangeMachineDialog.vue'
import ChangeQtyDialog from './components/ChangeQtyDialog.vue'
import ExplainDrawer from './components/ExplainDrawer.vue'
import InsertTaskDialog from './components/InsertTaskDialog.vue'
import UnplannedDialog from './components/UnplannedDialog.vue'

const formatDate = date => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const offsetDate = offset => {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  return formatDate(date)
}

export default {
  name: 'TcScheduleResult',
  components: {
    AutoPlanDialog,
    ChangeMachineDialog,
    ChangeQtyDialog,
    ExplainDrawer,
    InsertTaskDialog,
    UnplannedDialog
  },
  dicts: ['biz_factory_name', 'IS_RELEASE'],
  provide() {
    return { parentDict: this.dict }
  },
  data() {
    let storedFactoryCode = ''
    try {
      const storedScope = JSON.parse(window.sessionStorage.getItem('tcAutoPlanLatestScope') || '{}')
      storedFactoryCode = storedScope.factoryCode || ''
    } catch (error) {
      window.sessionStorage.removeItem('tcAutoPlanLatestScope')
    }
    const defaultQuery = {
      factoryCode: storedFactoryCode,
      scheduleRange: [offsetDate(0), offsetDate(2)]
    }
    return {
      loading: false,
      data: [],
      selection: [],
      page: { current: 1, pageSize: 20, total: 0 },
      search: { ...defaultQuery },
      query: { ...defaultQuery },
      summary: {},
      unplannedCount: 0,
      batchMap: {},
      dateColumns: [],
      machineOptions: [],
      autoPlanTimer: null,
      autoPlanPollTimes: 0,
      maxAutoPlanPollTimes: 120,
      autoPlanProgressVisible: false,
      autoPlanProgressValue: 0,
      autoPlanProgressStage: '',
      autoPlanProgressStatus: null,
      autoPlanIssueVisible: false,
      autoPlanIssues: [],
      releaseTimer: null,
      releasePollTimes: 0,
      maxReleasePollTimes: 240,
      releaseProgressVisible: false,
      releaseProgressValue: 0,
      releaseProgressStage: '',
      releaseProgressStatus: null,
      releaseIssueVisible: false,
      releaseIssues: [],
      operationTimer: null,
      operationPollTimes: 0,
      maxOperationPollTimes: 120,
      operationRunning: false,
      operationProgressVisible: false,
      operationProgressValue: 0,
      operationProgressStage: '',
      operationProgressStatus: null
    }
  },
  computed: {
    writeTaskRunning() {
      return this.operationRunning || this.autoPlanProgressVisible || this.releaseProgressVisible
    },
    // 各班次计划量合计列表，后端返回下标 0=1班，长度 6；为空时回退为空数组避免渲染异常
    shiftPlanQtyList() {
      return (this.summary && this.summary.shiftPlanQtyList) || []
    },
    columns() {
      const baseColumns = [
        { type: 'selection', fixed: 'left' },
        {
          prop: 'factoryCode',
          label: this.$t('ui.tc.schedule.factoryCode'),
          minWidth: 105,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value)
        },
        { prop: 'scheduleDate', label: this.$t('ui.tc.schedule.scheduleDate'), minWidth: 115, align: 'center' },
        { prop: 'batchNo', label: this.$t('ui.tc.schedule.batchNo'), minWidth: 170, showOverflowTooltip: true },
        { prop: 'orderNo', label: this.$t('ui.tc.schedule.orderNo'), minWidth: 175, showOverflowTooltip: true },
        { prop: 'machineCode', label: this.$t('ui.tc.schedule.machineCode'), minWidth: 115 },
        { prop: 'sidewallCode', label: this.$t('ui.tc.schedule.sidewallCode'), minWidth: 135 },
        { prop: 'constructionVersion', label: this.$t('ui.tc.schedule.constructionVersion'), minWidth: 125 },
        { prop: 'sidewallCraft', label: this.$t('ui.tc.schedule.sidewallCraft'), minWidth: 125, showOverflowTooltip: true },
        { prop: 'glueCode', label: this.$t('ui.tc.schedule.glueCode'), minWidth: 115 },
        { prop: 'baseGlueCode', label: this.$t('ui.tc.schedule.baseGlueCode'), minWidth: 115 },
        { prop: 'mouthPlateCode', label: this.$t('ui.tc.schedule.mouthPlateCode'), minWidth: 120 }
      ]
      const shiftColumns = Array.from({ length: 6 }, (item, index) => {
        const shiftOrder = index + 1
        return {
          label: this.shiftLabel(shiftOrder),
          children: [
            { prop: `class${shiftOrder}Sequence`, label: this.$t('ui.tc.schedule.sequence'), width: 72, align: 'center' },
            { prop: `class${shiftOrder}PlanQty`, label: this.$t('ui.tc.schedule.planQty'), width: 88, align: 'right' },
            { prop: `class${shiftOrder}FinishQty`, label: this.$t('ui.tc.schedule.finishQty'), width: 88, align: 'right' }
          ]
        }
      })
      return baseColumns.concat(shiftColumns, [
        {
          prop: 'releaseStatus',
          label: this.$t('ui.tc.schedule.releaseStatus'),
          minWidth: 105,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.IS_RELEASE, value)
        },
        { prop: 'dataSource', label: this.$t('ui.tc.schedule.dataSource'), minWidth: 100 },
        { prop: 'currentTaskVersion', label: this.$t('ui.tc.schedule.taskVersion'), minWidth: 90, align: 'center', formatter: row => row.currentTaskVersion == null ? row.taskVersion : row.currentTaskVersion },
        {
          label: this.$t('ui.data.btn.option'),
          fixed: 'right',
          width: 110,
          align: 'center',
          render: ({ row }) => (
            <el-button type='text' onClick={() => this.$refs.explainRef.showResult(row.id)}>
              {this.$t('ui.tc.schedule.viewExplain')}
            </el-button>
          )
        }
      ])
    },
    searchColumns() {
      return [
        {
          prop: 'factoryCode',
          label: this.$t('ui.tc.schedule.factoryCode'),
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true
        },
        {
          prop: 'scheduleRange',
          label: this.$t('ui.tc.schedule.scheduleDateRange'),
          type: 'date',
          dateType: 'daterange',
          valueFormat: 'yyyy-MM-dd'
        },
        {
          prop: 'machineCode',
          label: this.$t('ui.tc.schedule.machineCode'),
          type: 'select',
          dictData: this.machineOptions,
          labelKey: 'machineCode',
          valueKey: 'machineCode',
          filterable: true
        },
        { prop: 'sidewallCode', label: this.$t('ui.tc.schedule.sidewallCode') },
        { prop: 'glueCode', label: this.$t('ui.tc.schedule.glueCode') },
        { prop: 'mouthPlateCode', label: this.$t('ui.tc.schedule.mouthPlateCode') },
        {
          prop: 'releaseStatus',
          label: this.$t('ui.tc.schedule.releaseStatus'),
          type: 'select',
          dictData: this.dict.type.IS_RELEASE,
          filterable: true
        },
        {
          prop: 'assignStatus',
          label: this.$t('ui.tc.schedule.assignStatus'),
          type: 'select',
          dictData: [
            { label: this.$t('ui.tc.schedule.assigned'), value: 'ASSIGNED' },
            { label: this.$t('ui.tc.schedule.unplanned'), value: 'UNPLANNED' }
          ],
          filterable: true
        }
      ]
    }
  },
  created() {
    if (this.query.factoryCode) {
      this.getList()
    }
    this.restoreLatestAutoPlanTask(true)
    this.restoreLatestReleaseTask(true)
    this.restoreLatestOperationTask(true)
  },
  beforeDestroy() {
    this.clearAutoPlanTimer()
    this.clearReleaseTimer()
    this.clearOperationTimer()
  },
  methods: {
    shiftLabel(shiftOrder) {
      const option = this.dateColumns.find(item => item.shiftOrder === shiftOrder)
      return option ? `${shiftOrder}. ${option.shiftName || option.shiftCode || ''}` : `${this.$t('ui.tc.schedule.shift')} ${shiftOrder}`
    },
    formatQuery(includePage = true) {
      const scheduleRange = this.query.scheduleRange || []
      const params = {
        ...this.query,
        startDate: scheduleRange[0],
        endDate: scheduleRange[1]
      }
      delete params.scheduleRange
      if (includePage) {
        params.pageNum = this.page.current
        params.pageSize = this.page.pageSize
      }
      return params
    },
    async getList() {
      const params = this.formatQuery()
      if (!params.factoryCode || !params.startDate || !params.endDate) {
        this.$modal.msgWarning(this.$t('ui.tc.schedule.queryRequired'))
        return
      }
      this.loading = true
      try {
        const board = await queryScheduleBoard(params)
        const scheduledPage = board.scheduledPage || {}
        this.data = scheduledPage.rows || []
        this.page.total = Number(scheduledPage.total || 0)
        this.summary = board.summary || {}
        this.unplannedCount = Number(board.unplannedCount || 0)
        this.batchMap = board.batchMap || {}
        this.dateColumns = board.dateColumns || []
        this.selection = []
        await this.loadMachineOptions(params.factoryCode, params.startDate)
      } finally {
        this.loading = false
      }
    },
    async loadMachineOptions(factoryCode, scheduleDate) {
      if (!factoryCode || !scheduleDate) return
      try {
        const options = await getManualOptions({ factoryCode, scheduleDate })
        this.machineOptions = options.machineList || []
      } catch (error) {
        this.machineOptions = []
      }
    },
    handleSearch(query) {
      this.query = { ...query }
      this.page.current = 1
      this.getList()
      this.restoreLatestAutoPlanTask()
      this.restoreLatestReleaseTask()
    },
    handlePageChange(current, pageSize) {
      this.page.current = current
      this.page.pageSize = pageSize
      this.getList()
    },
    handleSelectionChange(rows) {
      this.selection = rows
    },
    handleAutoPlan() {
      const range = this.query.scheduleRange || []
      this.$refs.autoPlanRef.show(this.query.factoryCode, range[0])
    },
    handleAutoPlanSuccess(scheduleDate, task) {
      if (scheduleDate) {
        this.query.scheduleRange = [scheduleDate, scheduleDate]
        this.search = { ...this.query }
        window.sessionStorage.setItem('tcAutoPlanLatestScope', JSON.stringify({
          factoryCode: this.query.factoryCode,
          scheduleDate
        }))
      }
      if (task && task.taskId) {
        this.pollAutoPlanTask(task.taskId)
      } else {
        this.getList()
      }
    },
    buildReleaseRequest() {
      const row = this.selection[0]
      return {
        factoryCode: row.factoryCode,
        scheduleDate: row.scheduleDate,
        items: this.selection.map(item => ({
          resultId: item.id,
          expectedTaskVersion: Number(item.currentTaskVersion == null
            ? (item.taskVersion == null ? 0 : item.taskVersion)
            : item.currentTaskVersion)
        }))
      }
    },
    async handleRelease() {
      const scopeKeySet = new Set(this.selection.map(item => `${item.factoryCode}|${item.scheduleDate}|${item.batchNo}`))
      if (scopeKeySet.size !== 1) {
        this.$modal.msgWarning(this.$t('ui.tc.schedule.sameScopeRequired'))
        return
      }
      const invalidRow = this.selection.find(item => !['0', '2', '4', '5'].includes(String(item.releaseStatus || '0')))
      if (invalidRow) {
        this.$modal.msgWarning(this.$t('ui.tc.schedule.releaseStatusInvalid'))
        return
      }
      const requestData = this.buildReleaseRequest()
      const validateResult = await validateRelease(requestData)
      if (!validateResult.allowed) {
        const issues = Array.isArray(validateResult.issues) ? validateResult.issues : []
        this.showReleaseIssues(issues)
        this.$modal.msgWarning(issues.length > 0
          ? issues[0].message
          : this.$t('ui.tc.schedule.releaseValidateFailed'))
        return
      }
      await this.$confirm(this.$t('ui.tc.schedule.confirmPublish', { count: this.selection.length }), { type: 'warning' })
      const task = await releaseScheduleResult(requestData)
      window.sessionStorage.setItem('tcReleaseLatestScope', JSON.stringify({
        factoryCode: requestData.factoryCode,
        scheduleDate: requestData.scheduleDate
      }))
      this.pollReleaseTask(task.taskId)
    },
    handleAdd() {
      if (this.writeTaskRunning) return
      const range = this.query.scheduleRange || []
      this.$refs.insertTaskRef.show(this.query.factoryCode, range[0])
    },
    handleChangeQty() {
      if (this.writeTaskRunning) return
      const row = this.selection[0]
      if (this.isManualBlocked(row)) return
      this.$refs.changeQtyRef.show(row)
    },
    handleChangeMachine() {
      if (this.writeTaskRunning) return
      if (this.selection.some(row => this.isManualBlocked(row))) return
      const scopeKeySet = new Set(this.selection.map(item => `${item.factoryCode}|${item.scheduleDate}|${item.batchNo}`))
      if (scopeKeySet.size !== 1) {
        this.$modal.msgWarning(this.$t('ui.tc.schedule.sameScopeRequired'))
        return
      }
      this.$refs.changeMachineRef.show(this.selection)
    },
    isManualBlocked(row) {
      if (row && ['3', '4'].includes(String(row.releaseStatus))) {
        this.$modal.msgWarning(this.$t('ui.tc.schedule.releaseBlocked'))
        return true
      }
      return false
    },
    handleRemove() {
      if (this.writeTaskRunning) return
      const invalidRow = this.selection.find(item => !['0', '2', '5'].includes(String(item.releaseStatus)))
      if (invalidRow) {
        this.$modal.msgWarning(this.$t('ui.tc.schedule.removeReleaseBlocked'))
        return
      }
      this.$confirm(this.$t('ui.tc.schedule.confirmRemoveWholeRow'), { type: 'warning' }).then(async() => {
        const task = await removeScheduleResult(this.selection.map(item => item.id))
        this.page.current = 1
        this.handleOperationTask(task)
      })
    },
    handleOperationTask(task) {
      if (!task || !task.taskId) return
      const scheduleDate = String(task.scheduleDate || '').substring(0, 10)
      window.sessionStorage.setItem('tcOperationLatestScope', JSON.stringify({
        factoryCode: task.factoryCode,
        scheduleDate
      }))
      this.pollOperationTask(task.taskId, task)
    },
    pollOperationTask(taskId, initialTask) {
      this.clearOperationTimer()
      this.operationPollTimes = 0
      this.operationRunning = true
      this.operationProgressVisible = true
      this.operationProgressValue = Number((initialTask && initialTask.progress) || 0)
      this.operationProgressStage = (initialTask && (initialTask.currentStageName || initialTask.currentStage)) || ''
      this.operationProgressStatus = null
      const poll = () => {
        getOperationTask(taskId).then(task => {
          this.operationPollTimes += 1
          this.operationProgressValue = Math.min(100, Math.max(0, Number(task.progress || 0)))
          this.operationProgressStage = task.currentStageName || task.currentStage || ''
          if (task.taskStatus === 'SUCCESS') {
            this.clearOperationTimer()
            this.operationRunning = false
            this.operationProgressValue = 100
            this.operationProgressStatus = 'success'
            this.$modal.msgSuccess(this.$t('ui.tc.schedule.operationSuccess'))
            this.getList()
            window.setTimeout(() => { this.operationProgressVisible = false }, 600)
            return
          }
          if (task.taskStatus === 'FAILED') {
            this.clearOperationTimer()
            this.operationRunning = false
            this.operationProgressStatus = 'exception'
            this.$modal.msgError(task.message || this.$t('ui.tc.schedule.operationFailed'))
            window.setTimeout(() => { this.operationProgressVisible = false }, 3000)
            return
          }
          if (this.operationPollTimes >= this.maxOperationPollTimes) {
            this.clearOperationTimer()
            this.operationRunning = false
            this.operationProgressStatus = 'exception'
            this.$modal.msgWarning(this.$t('ui.tc.schedule.operationTimeout'))
            window.setTimeout(() => { this.operationProgressVisible = false }, 3000)
            return
          }
          this.operationTimer = window.setTimeout(poll, 3000)
        }).catch(() => {
          this.clearOperationTimer()
          this.operationRunning = false
          this.operationProgressStatus = 'exception'
          this.$modal.msgWarning(this.$t('ui.tc.schedule.operationTimeout'))
        })
      }
      poll()
    },
    restoreLatestOperationTask(preferStoredScope = false) {
      const range = this.query.scheduleRange || []
      let factoryCode = this.query.factoryCode
      let scheduleDate = range[0]
      if (preferStoredScope) {
        try {
          const storedScope = JSON.parse(window.sessionStorage.getItem('tcOperationLatestScope') || '{}')
          factoryCode = storedScope.factoryCode || factoryCode
          scheduleDate = storedScope.scheduleDate || scheduleDate
        } catch (error) {
          window.sessionStorage.removeItem('tcOperationLatestScope')
        }
      } else if (!range[0] || range[0] !== range[1]) {
        return
      }
      if (!factoryCode || !scheduleDate) return
      getLatestOperationTask({ factoryCode, scheduleDate }).then(task => {
        if (task && task.taskId && ['PENDING', 'RUNNING'].includes(task.taskStatus)) {
          this.pollOperationTask(task.taskId, task)
        }
      }).catch(() => {})
    },
    handleUnplanned() {
      this.$refs.unplannedRef.show(this.formatQuery(false))
    },
    pollAutoPlanTask(taskId) {
      this.clearAutoPlanTimer()
      this.autoPlanPollTimes = 0
      this.autoPlanProgressVisible = true
      this.autoPlanProgressValue = 0
      this.autoPlanProgressStatus = null
      const poll = () => {
        getAutoPlanTask(taskId).then(task => {
          this.autoPlanPollTimes += 1
          this.autoPlanProgressValue = Math.min(100, Math.max(0, Number(task.progress || 0)))
          this.autoPlanProgressStage = task.currentStageName || task.currentStage || ''
          if (task.taskStatus === 'SUCCESS') {
            this.clearAutoPlanTimer()
            this.autoPlanProgressValue = 100
            this.autoPlanProgressStatus = 'success'
            this.autoPlanProgressStage = this.$t('ui.tc.schedule.autoPlanSuccess')
            this.showAutoPlanIssues(task.issues)
            window.setTimeout(() => { this.autoPlanProgressVisible = false }, 600)
            this.getList()
            return
          }
          if (task.taskStatus === 'FAILED') {
            this.clearAutoPlanTimer()
            this.autoPlanProgressStatus = 'exception'
            this.autoPlanProgressStage = this.$t('ui.tc.schedule.autoPlanFailed')
            this.showAutoPlanIssues(task.issues)
            this.$modal.msgError(task.message || this.$t('ui.tc.schedule.autoPlanFailed'))
            window.setTimeout(() => { this.autoPlanProgressVisible = false }, 3000)
            return
          }
          if (this.autoPlanPollTimes >= this.maxAutoPlanPollTimes) {
            this.clearAutoPlanTimer()
            this.autoPlanProgressStatus = 'exception'
            this.$modal.msgWarning(this.$t('ui.tc.schedule.autoPlanTimeout'))
            window.setTimeout(() => { this.autoPlanProgressVisible = false }, 3000)
            return
          }
          this.autoPlanTimer = window.setTimeout(poll, 3000)
        }).catch(() => {
          this.clearAutoPlanTimer()
          this.autoPlanProgressStatus = 'exception'
          this.$modal.msgWarning(this.$t('ui.tc.schedule.autoPlanTimeout'))
        })
      }
      poll()
    },
    restoreLatestAutoPlanTask(preferStoredScope = false) {
      const range = this.query.scheduleRange || []
      let factoryCode = this.query.factoryCode
      let scheduleDate = range[0]
      if (preferStoredScope) {
        try {
          const storedScope = JSON.parse(window.sessionStorage.getItem('tcAutoPlanLatestScope') || '{}')
          factoryCode = storedScope.factoryCode || factoryCode
          scheduleDate = storedScope.scheduleDate || scheduleDate
        } catch (error) {
          window.sessionStorage.removeItem('tcAutoPlanLatestScope')
        }
      } else if (!range[0] || range[0] !== range[1]) {
        return
      }
      if (!factoryCode || !scheduleDate) return
      getLatestAutoPlanTask({ factoryCode, scheduleDate }).then(task => {
        if (task && task.taskId && ['PENDING', 'RUNNING'].includes(task.taskStatus)) {
          this.query.factoryCode = factoryCode
          this.query.scheduleRange = [scheduleDate, scheduleDate]
          this.search = { ...this.query }
          this.pollAutoPlanTask(task.taskId)
        }
      }).catch(() => {})
    },
    pollReleaseTask(taskId) {
      this.clearReleaseTimer()
      this.releasePollTimes = 0
      this.releaseProgressVisible = true
      this.releaseProgressValue = 0
      this.releaseProgressStatus = null
      const poll = () => {
        getReleaseTask(taskId).then(task => {
          this.releasePollTimes += 1
          this.releaseProgressValue = Math.min(100, Math.max(0, Number(task.progress || 0)))
          this.releaseProgressStage = task.currentStageName || task.currentStage || ''
          if (task.taskStatus === 'SUCCESS') {
            this.clearReleaseTimer()
            this.releaseProgressValue = 100
            this.releaseProgressStatus = 'success'
            this.releaseProgressStage = this.$t('ui.tc.schedule.releaseSuccess')
            this.showReleaseIssues(task.issues)
            window.setTimeout(() => { this.releaseProgressVisible = false }, 600)
            this.getList()
            return
          }
          if (task.taskStatus === 'FAILED') {
            this.clearReleaseTimer()
            this.releaseProgressStatus = 'exception'
            this.releaseProgressStage = this.$t('ui.tc.schedule.releaseFailed')
            this.showReleaseIssues(task.issues)
            this.$modal.msgError(task.message || this.$t('ui.tc.schedule.releaseFailed'))
            window.setTimeout(() => { this.releaseProgressVisible = false }, 3000)
            this.getList()
            return
          }
          if (this.releasePollTimes >= this.maxReleasePollTimes) {
            this.clearReleaseTimer()
            this.releaseProgressStatus = 'exception'
            this.$modal.msgWarning(this.$t('ui.tc.schedule.releasePollTimeout'))
            window.setTimeout(() => { this.releaseProgressVisible = false }, 3000)
            return
          }
          this.releaseTimer = window.setTimeout(poll, 3000)
        }).catch(() => {
          this.clearReleaseTimer()
          this.releaseProgressStatus = 'exception'
          this.$modal.msgWarning(this.$t('ui.tc.schedule.releasePollTimeout'))
        })
      }
      poll()
    },
    restoreLatestReleaseTask(preferStoredScope = false) {
      const range = this.query.scheduleRange || []
      let factoryCode = this.query.factoryCode
      let scheduleDate = range[0]
      if (preferStoredScope) {
        try {
          const storedScope = JSON.parse(window.sessionStorage.getItem('tcReleaseLatestScope') || '{}')
          factoryCode = storedScope.factoryCode || factoryCode
          scheduleDate = storedScope.scheduleDate || scheduleDate
        } catch (error) {
          window.sessionStorage.removeItem('tcReleaseLatestScope')
        }
      } else if (!range[0] || range[0] !== range[1]) {
        return
      }
      if (!factoryCode || !scheduleDate) return
      getLatestReleaseTask({ factoryCode, scheduleDate }).then(task => {
        if (task && task.taskId && ['PENDING', 'RUNNING'].includes(task.taskStatus)) {
          this.pollReleaseTask(task.taskId)
        }
      }).catch(() => {})
    },
    showAutoPlanIssues(issues) {
      this.autoPlanIssues = Array.isArray(issues) ? issues : []
      this.autoPlanIssueVisible = this.autoPlanIssues.length > 0
    },
    showReleaseIssues(issues) {
      this.releaseIssues = Array.isArray(issues) ? issues : []
      this.releaseIssueVisible = this.releaseIssues.length > 0
    },
    clearAutoPlanTimer() {
      if (this.autoPlanTimer) {
        window.clearTimeout(this.autoPlanTimer)
        this.autoPlanTimer = null
      }
    },
    clearReleaseTimer() {
      if (this.releaseTimer) {
        window.clearTimeout(this.releaseTimer)
        this.releaseTimer = null
      }
    },
    clearOperationTimer() {
      if (this.operationTimer) {
        window.clearTimeout(this.operationTimer)
        this.operationTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.summary-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 10px;
  padding: 10px 14px;
  color: #606266;
  background: #f5f7fa;
  border-radius: 4px;
}
.progress-stage {
  margin-bottom: 12px;
  color: #606266;
  text-align: center;
}
.progress-hint {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  text-align: center;
}
</style>
