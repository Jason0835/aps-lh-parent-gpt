<template>
  <basic-container>
    <page-table
      tableRef="cxPrecisionPlanMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['cx:cxPrecisionPlan:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
        >{{ $t('ui.frame.btn.update') }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['cx:cxPrecisionPlan:sync']"
          @click="handleSyncFromMes"
        >{{ $t('ui.cx.precision.plan.sync.from.mes') }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['cx:cxPrecisionPlan:remove']"
          :disabled="selection.length === 0"
          @click="handleDeleteAll"
        >{{ $t('ui.frame.btn.delete') }}</el-button>
        <el-button
          v-hasPermi="['cx:cxPrecisionPlan:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t('ui.frame.btn.import') }}</el-button>
        <el-button
          v-hasPermi="['cx:cxPrecisionPlan:export']"
          @click="handleExport"
        >{{ $t('ui.frame.btn.export') }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      :downloadUrl="importTemplateUrl"
      :uploadUrl="importUrl"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { downloadLink } from '@/utils/request'
import { listCxPrecisionPlan, removeCxPrecisionPlan, syncFromMes } from '@/api/cx/cxPrecisionPlan'
import TltUploadForm from '@/views/components/tltUploadForm.vue'
import infoDialog from './components/infoDialog.vue'

export default {
  name: 'CxPrecisionPlan',
  components: { TltUploadForm, infoDialog },
  dicts: ['biz_factory_name', 'cx_precision_plan_type', 'lh_precision_data_source'],
  provide() {
    return { parentDict: this.dict }
  },
  data() {
    return {
      importColumns: [
        {
          label: '',
          prop: 'updateSupport',
          render: (form) => {
            return (
              <el-checkbox label={this.$t('common.rule.updateSupport')} v-model={form.updateSupport}>
                {this.$t('common.rule.updateSupport')}
              </el-checkbox>
            )
          }
        }
      ],
      loading: false,
      data: [],
      selection: [],
      page: { current: 1, pageSize: 20, total: 0 },
      sort: {},
      search: {},
      query: {},
      importUrl: '/cx/cxPrecisionPlan/importData',
      importTemplateUrl: '/cx/cxPrecisionPlan/importTemplate'
    }
  },
  computed: {
    columns() {
      return [
        { type: 'selection', fixed: 'left' },
        {
          prop: 'factoryCode',
          label: this.$t('common.factory'),
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value)
        },
        { prop: 'machineCode', label: this.$t('ui.data.column.cxPrecisionPlan.machineCode') },
        {
          prop: 'precisionType',
          label: this.$t('ui.data.column.cxPrecisionPlan.accuracyType'),
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.cx_precision_plan_type, value)
        },
        { prop: 'planDate', label: this.$t('ui.data.column.cxPrecisionPlan.planDate') },
        { prop: 'actualDate', label: this.$t('ui.data.column.cxPrecisionPlan.actualDate') },
        {
          prop: 'cycle',
          label: this.$t('ui.data.column.cxPrecisionPlan.cycle'),
          formatter: (row) => this.getCycleValue(row.precisionType)
        },
        {
          prop: 'daysToDue',
          label: this.$t('ui.data.column.cxPrecisionPlan.dueDate'),
          formatter: (row) => this.getDaysToDueValue(row.planDate)
        },
        {
          prop: 'dataSource',
          label: this.$t('ui.data.column.lhPrecisionPlan.dataSource'),
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.lh_precision_data_source, value)
        },
        { prop: 'remark', label: this.$t('ui.common.column.remark') },
        {
          align: 'center',
          label: this.$t('ui.data.btn.option'),
          width: 180,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={['cx:cxPrecisionPlan:edit']}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t('ui.frame.btn.update')}
                </el-button>
                <el-button
                  v-hasPermi={['cx:cxPrecisionPlan:remove']}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t('ui.frame.btn.delete')}
                </el-button>
              </div>
            )
          }
        }
      ]
    },
    searchColumns() {
      return [
        {
          prop: 'factoryCode',
          label: this.$t('common.factory'),
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true
        },
        {
          prop: 'machineCode',
          label: this.$t('ui.data.column.cxPrecisionPlan.machineCode'),
          type: 'input'
        },
        {
          prop: 'precisionType',
          label: this.$t('ui.data.column.cxPrecisionPlan.accuracyType'),
          type: 'select',
          dictData: this.dict.type.cx_precision_plan_type,
          filterable: true
        },
        {
          prop: 'planDate',
          label: this.$t('ui.data.column.cxPrecisionPlan.planDate'),
          type: 'date',
          dateType: 'daterange',
          valueFormat: 'yyyy-MM-dd'
        },
        {
          prop: 'actualDate',
          label: this.$t('ui.data.column.cxPrecisionPlan.actualDate'),
          type: 'date',
          dateType: 'daterange',
          valueFormat: 'yyyy-MM-dd'
        }
      ]
    }
  },
  methods: {
    getCycleValue(precisionType) {
      const text = this.selectDictLabel(this.dict.type.cx_precision_plan_type, precisionType) || precisionType || ''
      if (text.includes('60')) {
        return '60'
      }
      if (text.includes('15')) {
        return '15'
      }
      return ''
    },
    getDaysToDueValue(planDate) {
      if (!planDate) {
        return ''
      }
      const target = new Date(planDate)
      if (Number.isNaN(target.getTime())) {
        return ''
      }
      const now = new Date()
      const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const startOfTarget = new Date(target.getFullYear(), target.getMonth(), target.getDate())
      return Math.floor((startOfToday.getTime() - startOfTarget.getTime()) / 86400000)
    },
    handleSyncFromMes() {
      const currentYear = new Date().getFullYear()
      this.$confirm(this.$t('ui.cx.precision.plan.sync.confirm'), { type: 'warning' }).then(() => {
        this.loading = true
        syncFromMes(currentYear).then((res) => {
          this.$modal.msgSuccess(res.msg || this.$t('common.success'))
          this.getList()
        }).catch(() => {
          this.loading = false
        })
      })
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row)
      }
    },
    handleBatchEdit() {
      if (this.selection && this.selection.length === 1) {
        this.handleEdit(this.selection[0])
      }
    },
    handleDeleteAll() {
      const ids = this.selection.map(item => item.id).join(',')
      this.$confirm(this.$t('common.confirm.delete'), { type: 'warning' }).then(() => {
        removeCxPrecisionPlan(ids).then((data) => {
          this.$modal.msgSuccess(data.msg || this.$t('common.success'))
          this.$set(this.page, 'current', 1)
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$confirm(this.$t('common.confirm.delete'), { type: 'warning' }).then(() => {
        removeCxPrecisionPlan(row.id).then((data) => {
          this.$modal.msgSuccess(data.msg || this.$t('common.success'))
          this.$set(this.page, 'current', 1)
          this.getList()
        })
      })
    },
    handleExport() {
      downloadLink('/cx/cxPrecisionPlan/export', this.formatParams(false))
    },
    handleSearch(data) {
      this.query = { ...data }
      if (data.planDate && data.planDate.length === 2) {
        this.query.planDateStart = data.planDate[0]
        this.query.planDateEnd = data.planDate[1]
      } else {
        this.query.planDateStart = undefined
        this.query.planDateEnd = undefined
      }
      if (data.actualDate && data.actualDate.length === 2) {
        this.query.actualDateStart = data.actualDate[0]
        this.query.actualDateEnd = data.actualDate[1]
      } else {
        this.query.actualDateStart = undefined
        this.query.actualDateEnd = undefined
      }
      delete this.query.planDate
      delete this.query.actualDate
      this.$set(this.page, 'current', 1)
      this.getList()
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, 'current', current)
      this.$set(this.page, 'pageSize', pageSize)
      this.getList()
    },
    handleSortChange({ prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order === 'ascending' ? 'asc' : 'desc'
        }
      } else {
        this.sort = {}
      }
      this.getList()
    },
    handleSelectionChange(rows) {
      this.selection = rows
    },
    formatParams(hasPage = true) {
      const params = { ...this.query, ...this.sort }
      if (hasPage) {
        params.pageNum = this.page.current
        params.pageSize = this.page.pageSize
      }
      return params
    },
    async getList() {
      try {
        this.loading = true
        const res = await listCxPrecisionPlan(this.formatParams())
        this.data = res.rows || []
        this.page.total = res.total || 0
      } catch (error) {
        console.error(error)
      } finally {
        this.loading = false
      }
    }
  },
  created() {
    const defaultParams = {
      factoryCode: '116'
    }
    this.search = { ...defaultParams }
    this.query = { ...defaultParams }
    this.getList()
  }
}
</script>
