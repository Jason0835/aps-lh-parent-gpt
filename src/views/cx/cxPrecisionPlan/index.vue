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
          :disabled="selection.length !== 1"
          @click="handleBatchEdit"
        >
          {{ $t('ui.frame.btn.update') }}
        </el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['cx:cxPrecisionPlan:sync']"
          @click="handleSyncFromMes"
        >
          {{ $t('ui.lh.precision.plan.sync.from.mes') }}
        </el-button>
        <el-button
          type="danger"
          v-hasPermi="['cx:cxPrecisionPlan:remove']"
          :disabled="selection.length === 0"
          @click="handleDeleteAll"
        >
          {{ $t('ui.frame.btn.delete') }}
        </el-button>
        <el-button
          v-hasPermi="['cx:cxPrecisionPlan:import']"
          @click="$refs.tltUpload.handleImport()"
        >
          {{ $t('ui.frame.btn.import') }}
        </el-button>
        <el-button
          v-hasPermi="['cx:cxPrecisionPlan:export']"
          @click="handleExport"
        >
          {{ $t('ui.frame.btn.export') }}
        </el-button>
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
  dicts: ['biz_factory_name', 'class_num_three_plan'],
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
          prop: 'machineName',
          label: this.$t('ui.data.column.cxPrecisionPlan.machineName'),
          placeholder: this.$t('common.rule.input'),
          type: 'input'
        },
        {
          prop: 'planDate',
          label: this.$t('ui.data.column.cxPrecisionPlan.planDate'),
          type: 'date',
          dateType: 'daterange',
          valueFormat: 'yyyy-MM-dd',
          startPlaceholder: this.$t('common.startTime'),
          endPlaceholder: this.$t('common.endTime')
        }
      ]
    },
    columns() {
      return [
        { type: 'selection', fixed: 'left' },
        {
          prop: 'factoryCode',
          align: 'center',
          halign: 'center',
          label: this.$t('common.factory'),
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value)
          }
        },
        {
          prop: 'machineCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.machineCode'),
          minWidth: 100
        },
        {
          prop: 'machineName',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.machineName'),
          minWidth: 140
        },
        {
          prop: 'planDate',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.planDate'),
          minWidth: 100
        },
        {
          prop: 'planShift',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.planShift'),
          minWidth: 80,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.class_num_three_plan, value)
          }
        },
        {
          prop: 'planStartTime',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.planStartTime'),
          minWidth: 160
        },
        {
          prop: 'planEndTime',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.planEndTime'),
          minWidth: 160
        },
        {
          prop: 'estimatedHours',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.estimatedHours'),
          minWidth: 90
        },
        {
          prop: 'actualDate',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.actualDate'),
          minWidth: 120
        },
        {
          prop: 'lastPrecisionDate',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.lastPrecisionDate'),
          minWidth: 120
        },
        {
          prop: 'dueDate',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.dueDate'),
          minWidth: 120
        },
        {
          prop: 'cycleDays',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.cycleDays'),
          minWidth: 100
        },
        {
          prop: 'scheduleDate',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxPrecisionPlan.scheduleDate'),
          minWidth: 120
        },
        {
          prop: 'remark',
          halign: 'center',
          label: this.$t('ui.common.column.remark'),
          minWidth: 140
        },
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
    }
  },
  methods: {
    handleSyncFromMes() {
      const currentYear = new Date().getFullYear()
      this.$confirm(this.$t('ui.lh.precision.plan.sync.confirm'), {
        type: 'warning'
      }).then(() => {
        this.loading = true
        syncFromMes(currentYear)
          .then((res) => {
            this.$modal.msgSuccess(res.msg || this.$t('common.success'))
            this.getList()
          })
          .catch(() => {
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
      this.$confirm(this.$t('common.confirm.delete'), {
        type: 'warning'
      }).then(() => {
        removeCxPrecisionPlan(ids).then((data) => {
          this.$modal.msgSuccess(data.msg || this.$t('common.success'))
          this.$set(this.page, 'current', 1)
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$confirm(this.$t('common.confirm.delete'), {
        type: 'warning'
      }).then(() => {
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
        this.query.planDateBegin = data.planDate[0]
        this.query.planDateEnd = data.planDate[1]
      } else {
        this.query.planDateBegin = undefined
        this.query.planDateEnd = undefined
      }
      delete this.query.planDate
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
      const params = {
        ...this.query,
        ...this.sort
      }
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
    const today = new Date()
    const defaultDate = today.toISOString().split('T')[0]
    const defaultParams = {
      factoryCode: '116',
      planDateBegin: defaultDate,
      planDateEnd: defaultDate
    }
    this.search = {
      ...defaultParams
    }
    this.query = {
      ...defaultParams
    }
    this.getList()
  }
}
</script>

<style scoped>
</style>
