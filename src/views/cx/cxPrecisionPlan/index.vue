<template>
  <basic-container>
    <page-table
      tableRef="cxPrecisionPlanTable"
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
        <el-button type="primary" plain v-hasPermi="['cx:cxPrecisionPlan:edit']" @click="handleAdd">
          {{ $t('ui.frame.btn.add') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxPrecisionPlan:edit']" :disabled="selection.length !== 1" @click="handleEdit(selection[0])">
          {{ $t('ui.frame.btn.update') }}
        </el-button>
        <el-button type="danger" v-hasPermi="['cx:cxPrecisionPlan:remove']" :disabled="selection.length === 0" @click="handleDelete">
          {{ $t('ui.frame.btn.delete') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxPrecisionPlan:import']" @click="$refs.uploadRef.handleImport()">
          {{ $t('ui.frame.btn.import') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxPrecisionPlan:export']" @click="handleExport">
          {{ $t('ui.frame.btn.export') }}
        </el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="uploadRef"
      :updateSupport="true"
      :downloadUrl="importTemplateUrl"
      :uploadUrl="importUrl"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { downloadLink } from '@/utils/request'
import { listCxPrecisionPlan, removeCxPrecisionPlan } from '@/api/cx/cxPrecisionPlan'
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
          label: this.$t('ui.data.column.cxPrecisionPlan.factoryCode'),
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          clearable: true
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
          endPlaceholder: this.$t('common.endTime'),
          clearable: true
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
          label: this.$t('ui.data.column.cxPrecisionPlan.factoryCode'),
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
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
          return this.selectDictLabel(this.dict.type.class_num_three_plan, value);
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
          prop: 'remark',
          halign: 'center',
          label: this.$t('ui.common.column.remark'),
          minWidth: 140
        },
        {
          prop: 'option',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.btn.option'),
          minWidth: 130,
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
    handleAdd() {
      this.$refs.infoRef.show()
    },
    handleEdit(row) {
      if (row) this.$refs.infoRef.show(row)
    },
    handleDelete(row) {
      this.$confirm(this.$t('common.confirm.delete'), { type: 'warning' }).then(() => {
        const ids = row && row.id ? row.id : this.selection.map(r => r.id).join(',')
        this.loading = true
        removeCxPrecisionPlan(ids)
          .then(res => {
            this.$modal.msgSuccess(res.msg)
            this.page.current = 1
            this.getList()
          })
          .finally(() => (this.loading = false))
      })
    },
    handleExport() {
      downloadLink("/cx/cxPrecisionPlan/export", this.formatParams(false));
    },
    handleSearch(data) {
      this.query = { ...data }
      // 处理计划日期区间
      if (data.planDate && data.planDate.length === 2) {
        this.query.planDateBegin = data.planDate[0]
        this.query.planDateEnd = data.planDate[1]
      } else {
        this.query.planDateBegin = undefined
        this.query.planDateEnd = undefined
      }
      delete this.query.planDate
      this.page.current = 1
      this.getList()
    },
    handlePageChange(current, pageSize) {
      this.page.current = current
      this.page.pageSize = pageSize
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
      } finally {
        this.loading = false
      }
    }
  },
  activated() {
    this.getList()
  }
}
</script>

<style scoped>
</style>
