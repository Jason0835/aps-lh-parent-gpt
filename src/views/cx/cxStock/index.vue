<template>
  <basic-container>
    <page-table
      tableRef="cxStockTable"
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
        <el-button type="primary" plain v-hasPermi="['cx:cxStock:edit']" @click="handleAdd">
          {{ $t('ui.frame.btn.add') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxStock:edit']" :disabled="selection.length !== 1" @click="handleEdit(selection[0])">
          {{ $t('ui.frame.btn.update') }}
        </el-button>
        <el-button type="danger" v-hasPermi="['cx:cxStock:remove']" :disabled="selection.length === 0" @click="handleDelete">
          {{ $t('ui.frame.btn.delete') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxStock:import']" @click="$refs.uploadRef.handleImport()">
          {{ $t('ui.frame.btn.import') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxStock:export']" @click="handleExport">
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
    />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { downloadLink } from '@/utils/request'
import { listCxStock, removeCxStock, exportCxStock } from '@/api/cx/cxStock'
import TltUploadForm from '@/views/components/tltUploadForm.vue'
import infoDialog from './components/infoDialog.vue'

export default {
  name: 'CxStock',
  components: { TltUploadForm, infoDialog },
  dicts: ['biz_factory_name'],
  provide() {
    return { parentDict: this.dict }
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: { current: 1, pageSize: 20, total: 0 },
      sort: {},
      search: {},
      query: {},
      importUrl: '/cx/cxStock/importData',
      importTemplateUrl: '/cx/cxStock/importTemplate'
    }
  },
  computed: {
    searchColumns() {
      return [
        {
          prop: 'stockDate',
          label: this.$t('ui.data.column.cxStock.stockDate'),
          type: 'daterange',
          startPlaceholder: this.$t('ui.frame.placeholder.startDate'),
          endPlaceholder: this.$t('ui.frame.placeholder.endDate'),
          clearable: true
        },
        {
          prop: 'embryoCode',
          label: this.$t('ui.data.column.cxStock.embryoCode'),
          placeholder: this.$t('common.rule.input'),
          type: 'input'
        },
        {
          prop: 'factoryCode',
          label: this.$t('ui.data.column.cxStock.factoryCode'),
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          clearable: true
        }
      ]
    },
    columns() {
      return [
        { type: 'selection', fixed: 'left' },
        {
          prop: 'stockDate',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStock.stockDate'),
          minWidth: 140
        },
        {
          prop: 'embryoCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStock.embryoCode'),
          minWidth: 140
        },
        {
          prop: 'stockNum',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStock.stockNum'),
          minWidth: 120
        },
        {
          prop: 'overTimeStock',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStock.overTimeStock'),
          minWidth: 120
        },
        {
          prop: 'modifyNum',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStock.modifyNum'),
          minWidth: 120
        },
        {
          prop: 'badNum',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStock.badNum'),
          minWidth: 120
        },
        {
          prop: 'factoryCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStock.factoryCode'),
          minWidth: 140,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          }
        },
        {
          prop: 'remark',
          halign: 'center',
          label: this.$t('ui.common.column.remark'),
          minWidth: 160
        },
        {
          prop: 'option',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.btn.option'),
          minWidth: 150,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={['cx:cxStock:edit']}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t('ui.frame.btn.update')}
                </el-button>
                <el-button
                  v-hasPermi={['cx:cxStock:remove']}
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
        const ids = row ? [row.id] : this.selection.map(r => r.id)
        this.loading = true
        removeCxStock(ids)
          .then(res => {
            this.$modal.msgSuccess(res.msg)
            this.page.current = 1
            this.getList()
          })
          .finally(() => (this.loading = false))
      })
    },
    handleExport() {
      const fileName = this.$t('ui.data.column.cxStock.modelName')
      exportCxStock(this.formatParams(false), fileName)
    },
    handleSearch(data) {
      this.query = { ...data }
      if (data.stockDate && data.stockDate.length === 2) {
        this.query.startTime = data.stockDate[0]
        this.query.endTime = data.stockDate[1]
      } else {
        this.query.startTime = undefined
        this.query.endTime = undefined
      }
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
        const res = await listCxStock(this.formatParams())
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
