<template>
  <basic-container>
    <page-table
      v-loading="loading"
      table-ref="lhMachineOnlineInfoTable"
      :calc-height="true"
      :columns="columns"
      :search-columns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      :show-summary="false"
      :select-area="false"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
    >
      <template slot="header">
        <el-button v-hasPermi="['lh:lhMachineOnlineInfo:export']" @click="handleExport">
          {{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
  </basic-container>
</template>

<script>
import { listLhMachineOnlineInfo } from '@/api/lh/lhMachineOnlineInfo'
import { downloadLink } from '@/utils/request'

export default {
  name: 'LhMachineOnlineInfo',
  dicts: ['biz_factory_name'],
  provide() {
    return {
      parentDict: this.dict
    }
  },
  data() {
    return {
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0
      },
      sort: {},
      search: {
        factoryCode: '116'
      },
      query: {
        factoryCode: '116'
      }
    }
  },
  computed: {
    columns() {
      return [
        {
          prop: 'factoryCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.lhMachineOnlineInfo.factoryCode'),
          minWidth: 140,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value)
          }
        },
        {
          prop: 'onlineDate',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.lhMachineOnlineInfo.onlineDate'),
          minWidth: 140
        },
        {
          prop: 'lhCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.lhMachineOnlineInfo.lhCode'),
          minWidth: 140
        },
        {
          prop: 'materialCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.lhMachineOnlineInfo.materialCode'),
          minWidth: 160
        },
        {
          prop: 'mesMaterialCode',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.lhMachineOnlineInfo.mesMaterialCode'),
          minWidth: 160
        },
        {
          prop: 'specDesc',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.lhMachineOnlineInfo.specDesc'),
          minWidth: 180
        },
        {
          prop: 'lrMolds',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.lhMachineOnlineInfo.lrMolds'),
          minWidth: 160
        },
        {
          prop: 'updateTime',
          align: 'center',
          halign: 'center',
          label: this.$t('ui.data.column.updateTime'),
          minWidth: 160
        }
      ]
    },
    searchColumns() {
      return [
        {
          label: this.$t('ui.data.column.lhMachineOnlineInfo.factoryCode'),
          prop: 'factoryCode',
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true
        },
        {
          label: this.$t('ui.data.column.lhMachineOnlineInfo.lhCode'),
          prop: 'lhCode',
          type: 'input'
        },
        {
          label: this.$t('ui.data.column.lhMachineOnlineInfo.onlineDate'),
          prop: 'onlineDate',
          type: 'date',
          valueFormat: 'yyyy-MM-dd'
        }
      ]
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    handleSearch(data) {
      this.query = { ...data }
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
    handleExport() {
      downloadLink('/lh/lhMachineOnlineInfo/export', this.formatParams(false))
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort
      }

      if (hasPage) {
        params.pageSize = this.page.pageSize
        params.pageNum = this.page.current
      }

      return params
    },
    async getList() {
      try {
        this.loading = true
        const params = this.formatParams()
        const res = await listLhMachineOnlineInfo(params)
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

<style lang="scss" scoped></style>

