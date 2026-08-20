<template>
  <basic-container>
    <page-table
      tableRef="cxStructureTreadConfigTable"
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
        <el-button type="primary" plain v-hasPermi="['cx:cxStructureTreadConfig:edit']" @click="handleAdd">
          {{ $t('ui.frame.btn.add') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxStructureTreadConfig:edit']" :disabled="selection.length !== 1" @click="handleEdit(selection[0])">
          {{ $t('ui.frame.btn.update') }}
        </el-button>

        <el-button type="danger" v-hasPermi="['cx:cxStructureTreadConfig:remove']" :disabled="selection.length === 0" @click="handleDelete">
          {{ $t('ui.frame.btn.delete') }}
        </el-button>
        <el-button type="primary" plain v-hasPermi="['cx:cxStructureTreadConfig:generate']" :loading="generateLoading" @click="handleGenerate">
          {{ $t('ui.data.column.cxStructureTreadConfig.generate') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxStructureTreadConfig:updateSameStructureTreadCount']" :disabled="selection.length !== 1" @click="handleSameStructureTreadCount(selection[0])">
          {{ $t('ui.data.column.cxStructureTreadConfig.sameStructureTreadCount') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxStructureTreadConfig:import']" @click="$refs.uploadRef.handleImport()">
          {{ $t('ui.frame.btn.import') }}
        </el-button>
        <el-button v-hasPermi="['cx:cxStructureTreadConfig:export']" @click="handleExport">
          {{ $t('ui.frame.btn.export') }}
        </el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="uploadRef"
      :updateSupport="true"
      downloadUrl="/cx/cxStructureTreadConfig/importTemplate"
      uploadUrl="/cx/cxStructureTreadConfig/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog ref="infoRef" @success="getList" />
    <el-dialog
      :title="$t('ui.data.column.cxStructureTreadConfig.sameStructureTreadCount')"
      :visible.sync="sameStructureDialog.visible"
      width="520px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :append-to-body="true"
      @close="resetSameStructureDialog"
    >
      <el-form
        ref="sameStructureForm"
        :model="sameStructureDialog.form"
        :rules="sameStructureRules"
        label-position="right"
        label-width="160px"
        v-loading="sameStructureDialog.loading"
      >
        <el-form-item :label="$t('ui.data.column.cxStructureTreadConfig.structureCode')" prop="structureCode">
          <el-input v-model="sameStructureDialog.form.structureCode" disabled />
        </el-form-item>
        <el-form-item :label="$t('ui.data.column.cxStructureTreadConfig.treadCount')" prop="treadCount">
          <el-input-number
            v-model="sameStructureDialog.form.treadCount"
            :max="999"
            :precision="0"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template slot="footer">
        <el-button @click="sameStructureDialog.visible = false">{{ $t('common.button.cancel') }}</el-button>
        <el-button type="primary" :loading="sameStructureDialog.loading" @click="confirmSameStructureTreadCount">
          {{ $t('common.button.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { downloadLink } from '@/utils/request'
import { generateCxStructureTreadConfig, listCxStructureTreadConfig, removeCxStructureTreadConfig, updateSameStructureTreadCount } from '@/api/cx/cxStructureTreadConfig'
import { selectSkuStructureWithDesc } from '@/api/monthplan/skuStructure'
import TltUploadForm from '@/views/components/tltUploadForm.vue'
import infoDialog from './components/infoDialog.vue'

export default {
  name: 'CxStructureTreadConfig',
  components: { TltUploadForm, infoDialog },
  dicts: ['biz_factory_name', 'biz_yes_no'],
  provide() {
    return { parentDict: this.dict }
  },
  data() {
    const defaultQuery = {
      factoryCode: '116'
    }
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
      generateLoading: false,
      sameStructureDialog: {
        visible: false,
        loading: false,
        form: {}
      },
      structureSearchLoading: false,
      structureSearchOptions: [],
      data: [],
      selection: [],
      page: { current: 1, pageSize: 20, total: 0 },
      sort: {},
      search: { ...defaultQuery },
      query: { ...defaultQuery }
    }
  },
  computed: {
    sameStructureRules() {
      const validateTreadCount = (rule, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error(this.$t('common.rule.input')))
          return
        }
        if (!Number.isInteger(value) || value <= 0) {
          callback(new Error(this.$t('common.rule.input')))
          return
        }
        callback()
      }
      return {
        treadCount: [
          {
            validator: validateTreadCount,
            trigger: ['blur', 'change']
          }
        ]
      }
    },
    searchColumns() {
      return [
        {
          prop: 'factoryCode',
          label: this.$t('ui.data.column.factoryCode'),
          render: (form) => {
            return (
              <el-select
                v-model={form.factoryCode}
                style="width:100%;"
                clearable
                filterable
                placeholder={this.$t('common.rule.select')}
                onChange={() => this.handleSearchFactoryChange(form)}
                onClear={() => this.handleSearchFactoryChange(form)}
              >
                {(this.dict.type.biz_factory_name || []).map(item => (
                  <el-option
                    key={item.value}
                    value={item.value}
                    label={item.label}
                  />
                ))}
              </el-select>
            )
          }
        },
        {
          prop: 'structureCode',
          label: this.$t('ui.data.column.cxStructureTreadConfig.structureCode'),
          render: (form) => {
            return (
              <el-select
                v-model={form.structureCode}
                style="width:100%;"
                clearable
                filterable
                remote
                loading={this.structureSearchLoading}
                placeholder={this.$t('common.rule.select')}
                remote-method={(keyword) => this.loadStructureSearchOptions(keyword, form.factoryCode)}
                on={{
                  'visible-change': (visible) => this.handleStructureSearchVisibleChange(visible, form),
                  change: () => this.handleSearchStructureChange(form),
                  clear: () => this.handleSearchStructureClear(form)
                }}
              >
                {this.structureSearchOptions.map(item => (
                  <el-option
                    key={item.structureName}
                    value={item.structureName}
                    label={item.structureName}
                  />
                ))}
              </el-select>
            )
          }
        },
        {
          prop: 'embryoCode',
          label: this.$t('ui.data.column.cxStructureTreadConfig.embryoCode'),
          clearable: true
        },
        {
          prop: 'mainMaterialDesc',
          label: this.$t('ui.data.column.cxStructureTreadConfig.mainMaterialDesc'),
          minWidth: 350,
          align: "left",
          clearable: true
        },
        {
          prop: 'unconfiguredTreadCount',
          label: this.$t('ui.data.column.cxStructureTreadConfig.unconfiguredTreadCount'),
          type: 'select',
          dictData: this.dict.type.biz_yes_no,
          filterable: true,
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
          label: this.$t('ui.data.column.factoryCode'),
          minWidth: 140,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          }
        },
        {
          prop: 'structureCode',
          align: "left",
          halign: "left",
          label: this.$t('ui.data.column.cxStructureTreadConfig.structureCode'),
          minWidth: 160
        },
        {
          prop: 'embryoCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStructureTreadConfig.embryoCode'),
          minWidth: 140
        },
        {
          prop: 'mainMaterialDesc',
          align: 'left',
          halign: "left",
          label: this.$t('ui.data.column.cxStructureTreadConfig.mainMaterialDesc'),
          minWidth: 350
        },
        {
          prop: 'treadCount',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxStructureTreadConfig.treadCount'),
          minWidth: 140
        },
        {
          prop: 'remark',
          halign: 'center',
          label: this.$t('ui.common.column.remark'),
          minWidth: 160
        },
        {
          prop: 'updateBy',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.updateBy'),
          minWidth: 100,
        },
        {
          prop: 'updateTime',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.updateTime'),
          minWidth: 180,
        },
        {
          prop: 'option',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.btn.option'),
          minWidth: 280,
          width: 280,
          fixed: 'right',
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={['cx:cxStructureTreadConfig:edit']}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t('ui.frame.btn.update')}
                </el-button>
                <el-button
                  v-hasPermi={['cx:cxStructureTreadConfig:updateSameStructureTreadCount']}
                  class="minus"
                  type="primary"
                  onClick={() => this.handleSameStructureTreadCount(row)}
                >
                  {this.$t('ui.data.column.cxStructureTreadConfig.sameStructureTreadCount')}
                </el-button>
                <el-button
                  v-hasPermi={['cx:cxStructureTreadConfig:remove']}
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
    handleSameStructureTreadCount(row) {
      if (!row) return
      const treadCount = row.treadCount === null || row.treadCount === undefined || row.treadCount === '' ? null : row.treadCount
      this.sameStructureDialog.form = {
        id: row.id,
        factoryCode: row.factoryCode,
        structureCode: row.structureCode,
        treadCount
      }
      this.sameStructureDialog.visible = true
    },
    resetSameStructureDialog() {
      this.$refs.sameStructureForm && this.$refs.sameStructureForm.resetFields()
      this.sameStructureDialog.form = {}
    },
    confirmSameStructureTreadCount() {
      this.$refs.sameStructureForm.validate(valid => {
        if (!valid) return
        this.$confirm(this.$t('ui.data.alert.cxStructureTreadConfig.sameStructureTreadCount.confirm'), { type: 'warning' })
          .then(async () => {
            try {
              this.sameStructureDialog.loading = true
              const res = await updateSameStructureTreadCount(this.sameStructureDialog.form)
              this.$modal.msgSuccess(res.msg)
              this.sameStructureDialog.visible = false
              this.page.current = 1
              this.getList()
            } finally {
              this.sameStructureDialog.loading = false
            }
          })
      })
    },
    handleDelete(row) {
      this.$confirm(this.$t('common.confirm.delete'), { type: 'warning' }).then(() => {
        const ids = row && row.id ? [row.id] : this.selection.map(r => r.id)
        this.loading = true
        removeCxStructureTreadConfig(ids)
          .then(res => {
            this.$modal.msgSuccess(res.msg)
            this.page.current = 1
            this.getList()
          })
          .finally(() => (this.loading = false))
      })
    },
    handleExport() {
      downloadLink('/cx/cxStructureTreadConfig/export', this.formatParams(false))
    },
    handleGenerate() {
      const factoryCode = this.search.factoryCode !== undefined ? this.search.factoryCode : this.query.factoryCode
      if (!factoryCode) {
        this.$modal.msgError(this.$t('ui.data.alert.cxStructureTreadConfig.generate.factoryCodeRequired'))
        return
      }
      this.$confirm(this.$t('ui.data.alert.cxStructureTreadConfig.generate.confirm'), { type: 'warning' }).then(async () => {
        try {
          this.generateLoading = true
          const res = await generateCxStructureTreadConfig(factoryCode)
          this.$modal.msgSuccess(res.msg)
          this.page.current = 1
          this.getList()
        } finally {
          this.generateLoading = false
        }
      })
    },
    handleSearch(data) {
      this.query = { ...data }
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
    handleSearchFactoryChange(form) {
      this.$set(form, 'structureCode', undefined)
      this.structureSearchOptions = []
    },
    handleSearchStructureChange(form) {
      if (!form.factoryCode) {
        this.$set(form, 'structureCode', undefined)
      }
    },
    handleSearchStructureClear(form) {
      this.$set(form, 'structureCode', undefined)
    },
    handleStructureSearchVisibleChange(visible, form) {
      if (!visible) {
        return
      }
      this.loadStructureSearchOptions('', form.factoryCode)
    },
    async loadStructureSearchOptions(keyword, factoryCode) {
      if (!factoryCode) {
        this.structureSearchOptions = []
        return
      }
      try {
        this.structureSearchLoading = true
        const res = await selectSkuStructureWithDesc({
          factoryCode,
          structureName: keyword,
          status: 0,
          pageNum: 1,
          pageSize: 50
        })
        const rows = res.rows || []
        const optionMap = new Map()
        rows.forEach(item => {
          if (!item || !item.structureName) {
            return
          }
          if (!optionMap.has(item.structureName)) {
            optionMap.set(item.structureName, {
              structureName: item.structureName
            })
          }
        })
        this.structureSearchOptions = Array.from(optionMap.values())
      } finally {
        this.structureSearchLoading = false
      }
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
        const res = await listCxStructureTreadConfig(this.formatParams())
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
