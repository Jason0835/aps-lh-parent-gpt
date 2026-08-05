<template>
  <basic-container>
    <page-table
      v-loading="loading"
      table-ref="gdyyScheduleResultMainTable"
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
          v-hasPermi="['gdyy:scheduleResult:autoSchedule']"
          type="warning"
          @click="handleAutoSchedule"
        >{{ $t('ui.data.column.scheduleResult.autoPlan') }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:edit']"
          type="warning"
          @click="handleInsert"
        >{{ $t('ui.data.column.scheduleResult.insertOrder') }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:remove']"
          type="danger"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t('ui.frame.btn.delete') }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length !== 1"
          @click="handleChangeMachine"
        >{{ $t('ui.data.column.scheduleResult.changeMachine') }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:changePlan']"
          type="primary"
          :disabled="selection.length !== 1"
          @click="handleChangePlan"
        >{{ $t('ui.data.column.scheduleResult.changePlan') }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:publish']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
        >{{ $t('ui.data.column.scheduleResult.publish') }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t('ui.frame.btn.import') }}</el-button>
        <el-button
          v-hasPermi="['gdyy:scheduleResult:export']"
          @click="handleExport"
        >{{ $t('ui.frame.btn.export') }}</el-button>
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      :update-support="true"
      download-url="/gdyy/scheduleResult/export"
      :download-params="importParams"
      upload-url="/gdyy/scheduleResult/importDataByCust"
      :upload-params="importParams"
      @uploadSuccess="getList"
    />
    <edit-dialog ref="editRef" @success="getList" />
    <change-machine-dialog ref="changeMachineRef" @success="getList" />
    <change-plan-dialog ref="changePlanRef" @success="getList" />
    <publish-dialog ref="publishRef" @success="getList" />
  </basic-container>
</template>

<script>
import moment from 'moment'
import { listGdyyScheduleResult, delGdyyScheduleResult, exportGdyyScheduleResult } from '@/api/gdyy/gdyyScheduleResult'
import { listGdyyShiftConfig } from '@/api/gdyy/gdyyShiftConfig'
import EditDialog from './components/editDialog.vue'
import ChangeMachineDialog from './components/changeMachineDialog.vue'
import ChangePlanDialog from './components/changePlanDialog.vue'
import PublishDialog from './components/publishDialog.vue'
import tltUpload from '@/components/tltUpload/tltUpload.vue'

export default {
  name: 'GdyyScheduleResult',
  components: { EditDialog, ChangeMachineDialog, ChangePlanDialog, PublishDialog, tltUpload },
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
      query: { ...defaultSearch }
    }
  },
  computed: {
    importParams() {
      // 复杂生产计划模板导入/下载共用：携带当前查询的工厂和排程日期
      return {
        factoryCode: this.query.factoryCode || this.search.factoryCode,
        scheduleDate: this.query.scheduleDate || this.search.scheduleDate
      }
    },
    columns() {
      return [
        { type: 'selection', width: 55, fixed: 'left' },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.factoryCode'),
          prop: 'factoryCode',
          minWidth: 120,
          formatter: (row, column, cellValue) => this.selectDictLabel(this.dict.type.biz_factory_name, cellValue)
        },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          align: 'center',
          minWidth: 120,
          sortable: 'custom'
        },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.isRelease'),
          prop: 'isRelease',
          minWidth: 100,
          formatter: (row, column, cellValue) => this.selectDictLabel(this.dict.type.IS_RELEASE, cellValue)
        },
        { label: this.$t('ui.data.column.gdyyScheduleResult.batchNo'), prop: 'batchNo', align: 'left', minWidth: 160 },
        { label: this.$t('ui.data.column.gdyyScheduleResult.orderNo'), prop: 'orderNo', align: 'left', minWidth: 160 },
        { label: this.$t('ui.data.column.gdyyScheduleResult.bigRollCode'), prop: 'bigRollCode', minWidth: 180 },
        ...this.buildShiftColumns(),
        { label: this.$t('ui.data.column.gdyyScheduleResult.remark'), prop: 'remark', minWidth: 160 }
      ]
    },
    searchColumns() {
      return [
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.factoryCode'),
          prop: 'factoryCode',
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true
        },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          type: 'date',
          valueFormat: 'yyyy-MM-dd'
        },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.bigRollCode'),
          prop: 'bigRollCode',
          type: 'input'
        },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.machineCode'),
          prop: 'machineCode',
          type: 'input'
        },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.isRelease'),
          prop: 'isRelease',
          type: 'select',
          dictData: this.dict.type.IS_RELEASE,
          filterable: true
        },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.batchNo'),
          prop: 'batchNo',
          type: 'input'
        },
        {
          label: this.$t('ui.data.column.gdyyScheduleResult.orderNo'),
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
  methods: {
    getDefaultScheduleDate() {
      return moment().add(1, 'days').format('YYYY-MM-DD')
    },
    buildDateList(scheduleDate) {
      const baseDate = scheduleDate || this.getDefaultScheduleDate()
      // 与后端一致：班次日期 = 排程日 + (scheduleDay - 2)，scheduleDay=1 归属排程日前一天
      return this.shiftConfig.map(item => ({
        ...item,
        dayOffset: (item.scheduleDay || 2) - 2,
        shiftDate: moment(baseDate).add((item.scheduleDay || 2) - 2, 'days').format('MM/DD')
      }))
    },
    buildShiftColumns() {
      return this.shiftConfig.map((item, index) => {
        const dateItem = this.dateList[index] || {}
        const label = `${item.shiftName} ${dateItem.shiftDate || ''}`
        // CLASS_FIELD 存储为大写（如 CLASS1），后端 JSON 字段为驼峰小写（class1PlanQty），
        // 必须转小写才能与表格数据 prop 匹配
        const classField = item.classField.toLowerCase()

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
        machineCode: '',
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
    handleInsert() {
      this.$refs.editRef.openDialog('add')
    },
    handleEdit(row) {
      this.$refs.editRef.openDialog('edit', row)
    },
    handleChangeMachine() {
      if (this.selection.length !== 1) {
        this.$message.warning(this.$t('ui.frame.msg.selectOne'))
        return
      }
      this.$refs.changeMachineRef.openDialog(this.selection[0])
    },
    handleChangePlan() {
      if (this.selection.length !== 1) {
        this.$message.warning(this.$t('ui.frame.msg.selectOne'))
        return
      }
      this.$refs.changePlanRef.openDialog(this.selection[0])
    },
    handlePublish() {
      if (this.selection.length === 0) {
        this.$message.warning(this.$t('ui.frame.msg.selectAtLeastOne'))
        return
      }
      this.$refs.publishRef.openDialog(this.selection)
    },
    handleBatchDelete() {
      const ids = this.selection.map(item => item.id)
      if (!ids.length) {
        this.$message.warning(this.$t('ui.frame.msg.selectAtLeastOne'))
        return
      }
      this.$confirm(this.$t('ui.frame.confirm.delete')).then(() => {
        delGdyyScheduleResult(ids).then(res => {
          this.$message.success(this.$t('ui.frame.msg.success'))
          this.getList()
        })
      })
    },
    handleExport() {
      exportGdyyScheduleResult(this.formatParams(false))
    },
    getList() {
      this.loading = true
      listGdyyScheduleResult(this.formatParams())
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
      listGdyyShiftConfig({ factoryCode, isActive: 1, pageNum: 1, pageSize: 200 })
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
