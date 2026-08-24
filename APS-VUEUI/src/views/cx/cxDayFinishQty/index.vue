<template>
  <basic-container>
    <page-table
      tableRef="cxDayFinishQtyMainTable"
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
        <el-button v-hasPermi="['cx:cxDayFinishQty:export']" @click="handleExport">
          {{ $t('ui.frame.btn.export') }}
        </el-button>
      </template>
    </page-table>
  </basic-container>
</template>

<script>
import { downloadLink } from '@/utils/request'
import { listCxDayFinishQty } from '@/api/cx/cxDayFinishQty'

export default {
  name: 'CxDayFinishQty',
  dicts: ['biz_factory_name', 'trial_status'],
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
    columns() {
      return [
        {
          prop: 'factoryCode',
          align: 'center',
          label: this.$t('ui.data.column.factoryCode'),
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value)
        },
        {
          prop: 'finishDate',
          align: 'center',
          label: this.$t('ui.data.column.cxDayFinishQty.finishDate'),
          minWidth: 120
        },
        {
          prop: 'embryoCode',
          align: 'center',
          label: this.$t('ui.data.column.cxDayFinishQty.embryoCode'),
          minWidth: 160
        },
        {
          prop: 'exampleType',
          align: 'center',
          label: this.$t('ui.data.column.cxDayFinishQty.exampleType'),
          minWidth: 120,
          formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.trial_status, value);
          },
        },
        {
          prop: 'dayFinishQty',
          align: 'center',
          label: this.$t('ui.data.column.cxDayFinishQty.dayFinishQty'),
          minWidth: 120
        },
        {
          prop: 'bomDataVersion',
          align: 'center',
          label: this.$t('ui.data.column.cxDayFinishQty.bomDataVersion'),
          minWidth: 200
        },
        {
          prop: 'dataVersion',
          align: 'center',
          label: this.$t('ui.data.column.cxDayFinishQty.dataVersion'),
          minWidth: 200
        },
        {
          prop: 'remark',
          label: this.$t('ui.common.column.remark'),
          minWidth: 160
        }
      ]
    },
    searchColumns() {
      return [
        {
          prop: 'factoryCode',
          label: this.$t('ui.data.column.factoryCode'),
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true
        },
        {
          prop: 'finishDate',
          label: this.$t('ui.data.column.cxDayFinishQty.finishDate'),
          type: 'date',
          dateType: 'daterange',
          valueFormat: 'yyyy-MM-dd'
        },
        {
          prop: 'embryoCode',
          label: this.$t('ui.data.column.cxDayFinishQty.embryoCode'),
          type: 'input'
        },
        {
          prop: 'exampleType',
          label: this.$t('ui.data.column.cxDayFinishQty.exampleType'),
          type: "select",
          dictData: this.dict.type.trial_status,
        }
      ]
    }
  },
  created() {
    this.getList()
  },
  methods: {
    handleExport() {
      downloadLink('/cx/cxDayFinishQty/export', this.formatParams(false))
    },
    handleSearch(data) {
      this.query = { ...data }
      if (data.finishDate && data.finishDate.length === 2) {
        this.query.finishDateStart = data.finishDate[0]
        this.query.finishDateEnd = data.finishDate[1]
      } else {
        this.query.finishDateStart = undefined
        this.query.finishDateEnd = undefined
      }
      delete this.query.finishDate
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
        const res = await listCxDayFinishQty(this.formatParams())
        this.data = res.rows || []
        this.page.total = res.total || 0
      } catch (error) {
        console.error(error)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
