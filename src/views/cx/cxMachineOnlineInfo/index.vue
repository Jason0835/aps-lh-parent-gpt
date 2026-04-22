<template>
  <basic-container>
    <page-table
      tableRef="cxMachineOnlineInfoTable"
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
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button v-hasPermi="['cx:cxMachineOnlineInfo:export']" @click="handleExport">
          {{ $t('ui.frame.btn.export') }}
        </el-button>
      </template>
    </page-table>
  </basic-container>
</template>

<script>
import { downloadLink } from '@/utils/request'
import { listCxMachineOnlineInfo } from '@/api/cx/cxMachineOnlineInfo'

export default {
  name: 'CxMachineOnlineInfo',
  dicts: ['biz_factory_name'],
  provide() {
    return { parentDict: this.dict }
  },
  data() {
    return {
      loading: false,
      data: [],
      page: { current: 1, pageSize: 20, total: 0 },
      sort: {},
      search: { factoryCode: '116' },
      query: { factoryCode: '116' }
    }
  },
  computed: {
    searchColumns() {
      return [
        {
          prop: 'factoryCode',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.factoryCode'),
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          clearable: true
        },
        {
          prop: 'cxCode',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.cxCode'),
          placeholder: this.$t('common.rule.input'),
          type: 'input'
        },
        {
          prop: 'onlineDate',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.onlineDate'),
          type: 'date',
          valueFormat: 'yyyy-MM-dd'
        }
      ]
    },
    columns() {
      return [
        {
          prop: 'factoryCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.factoryCode'),
          minWidth: 140,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value)
          }
        },
        {
          prop: 'onlineDate',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.onlineDate'),
          minWidth: 100
        },
        {
          prop: 'cxCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.cxCode'),
          minWidth: 140
        },
        {
          prop: 'materialCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.materialCode'),
          minWidth: 160
        },
        {
          prop: 'mesMaterialCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.mesMaterialCode'),
          minWidth: 160
        },
        {
          prop: 'specDesc',
          halign: 'center',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.specDesc'),
          minWidth: 180
        },
        {
          prop: 'embryoSpec',
          halign: 'center',
          label: this.$t('ui.data.column.cxMachineOnlineInfo.embryoSpec'),
          minWidth: 180
        },
        // {
        //   prop: 'remark',
        //   halign: 'center',
        //   label: this.$t('ui.common.column.remark'),
        //   minWidth: 160
        // },
        {
          prop: 'updateTime',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.updateTime'),
          minWidth: 160
        }
      ]
    }
  },
  methods: {
    handleExport() {
      downloadLink('/cx/cxMachineOnlineInfo/export', this.formatParams(false))
    },
    handleSearch(data) {
      this.query = data
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
        const res = await listCxMachineOnlineInfo(this.formatParams())
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
