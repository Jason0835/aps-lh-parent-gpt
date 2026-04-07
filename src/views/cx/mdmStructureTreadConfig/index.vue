<template>
  <basic-container>
    <page-table
      tableRef="mdmStructureTreadConfigTable"
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
        <el-button type="primary" plain v-hasPermi="['cx:mdmStructureTreadConfig:edit']" @click="handleAdd">
          {{ $t('ui.frame.btn.add') }}
        </el-button>
        <el-button v-hasPermi="['cx:mdmStructureTreadConfig:edit']" :disabled="selection.length !== 1" @click="handleEdit(selection[0])">
          {{ $t('ui.frame.btn.update') }}
        </el-button>
        <el-button type="danger" v-hasPermi="['cx:mdmStructureTreadConfig:remove']" :disabled="selection.length === 0" @click="handleDelete">
          {{ $t('ui.frame.btn.delete') }}
        </el-button>
        <el-button v-hasPermi="['cx:mdmStructureTreadConfig:import']" @click="$refs.uploadRef.handleImport()">
          {{ $t('ui.frame.btn.import') }}
        </el-button>
        <el-button v-hasPermi="['cx:mdmStructureTreadConfig:export']" @click="handleExport">
          {{ $t('ui.frame.btn.export') }}
        </el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="uploadRef"
      :updateSupport="true"
      downloadUrl="/cx/mdmStructureTreadConfig/importTemplate"
      uploadUrl="/cx/mdmStructureTreadConfig/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { downloadLink } from '@/utils/request'
import { listMdmStructureTreadConfig, removeMdmStructureTreadConfig, exportMdmStructureTreadConfig } from '@/api/cx/mdmStructureTreadConfig'
import TltUploadForm from '@/views/components/tltUploadForm.vue'
import infoDialog from './components/infoDialog.vue'

export default {
  name: 'MdmStructureTreadConfig',
  components: { TltUploadForm, infoDialog },
  dicts: ['biz_factory_name'],
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
      query: {}
    }
  },
  computed: {
    searchColumns() {
      return [
        {
          prop: 'factoryCode',
          label: this.$t('ui.data.column.mdmStructureTreadConfig.factoryCode'),
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          clearable: true
        },
        {
          prop: 'structureCode',
          label: this.$t('ui.data.column.mdmStructureTreadConfig.structureCode'),
          placeholder: this.$t('common.rule.input'),
          type: 'input'
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
          label: this.$t('ui.data.column.mdmStructureTreadConfig.factoryCode'),
          minWidth: 140,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          }
        },
        {
          prop: 'structureCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.mdmStructureTreadConfig.structureCode'),
          minWidth: 140
        },
        {
          prop: 'treadCount',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.mdmStructureTreadConfig.treadCount'),
          minWidth: 140
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
                  v-hasPermi={['cx:mdmStructureTreadConfig:edit']}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t('ui.frame.btn.update')}
                </el-button>
                <el-button
                  v-hasPermi={['cx:mdmStructureTreadConfig:remove']}
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
        const ids = row && row.id ? [row.id] : this.selection.map(r => r.id)
        this.loading = true
        removeMdmStructureTreadConfig(ids)
          .then(res => {
            this.$modal.msgSuccess(res.msg)
            this.page.current = 1
            this.getList()
          })
          .finally(() => (this.loading = false))
      })
    },
    handleExport() {
      downloadLink('/cx/mdmStructureTreadConfig/export', this.formatParams(false))
    },
    handleSearch(data) {
      this.query = data
      if (data.stockDateRange && data.stockDateRange.length === 2) {
        this.query.stockDateBegin = data.stockDateRange[0]
        this.query.stockDateEnd = data.stockDateRange[1]
      } else {
        this.query.stockDateBegin = undefined
        this.query.stockDateEnd = undefined
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
        const res = await listMdmStructureTreadConfig(this.formatParams())
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
