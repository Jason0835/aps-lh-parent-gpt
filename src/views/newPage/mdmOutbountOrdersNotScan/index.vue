<template>
  <basic-container>
    <page-table
      tableRef="MdmOutbountOrdersNotScanMainTable"
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
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmOutbountOrdersNotScan:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listMdmOutbountOrdersNotScan } from "@/api/monthplan/mdmOutbountOrdersNotScan";

export default {
  name: "MdmOutbountOrdersNotScan",
  components: {},
  dicts: [],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        {
          prop: "saleBillNo",
          label: "DN号",
          minWidth: 120,
        },
        {
          prop: "saleOrderNo",
          label: "出运单号",
          minWidth: 120,
        },
        {
          prop: "saleOrg",
          label: "销售组织编码",
          minWidth: 120,
        },
        {
          prop: "saleOrgName",
          label: "销售组织名称",
          minWidth: 150,
        },
        {
          prop: "sellTo",
          label: "客户编码",
          minWidth: 100,
        },
        {
          prop: "billId",
          label: "出库单号",
          minWidth: 120,
        },
        {
          prop: "materialCode",
          label: "MES物料号",
          minWidth: 150,
        },
        {
          prop: "sapCode",
          label: "NC物料号",
          minWidth: 150,
        },
        {
          prop: "materialName",
          label: "物料描述",
          minWidth: 200,
        },
        {
          prop: "dot",
          label: "年周号要求",
          minWidth: 120,
        },
        {
          prop: "scanAmount",
          label: "扫描数量",
          minWidth: 100,
        },
        {
          prop: "outAmount",
          label: "计划数量",
          minWidth: 100,
        },
        {
          prop: "noscanAmount",
          label: "未扫描数量",
          minWidth: 100,
        },
        {
          prop: "saleItemNo",
          label: "SCM行内码",
          minWidth: 100,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "saleBillNo",
          label: "DN号",
        },
        {
          prop: "saleOrderNo",
          label: "出运单号",
        },
        {
          prop: "sellTo",
          label: "客户编码",
        },
        {
          prop: "billId",
          label: "出库单号",
        },
        {
          prop: "materialCode",
          label: "MES物料号",
        },
        {
          prop: "sapCode",
          label: "NC物料号",
        },
        {
          prop: "materialName",
          label: "物料描述",
        },
      ];
    },
  },
  methods: {
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
    },
    handleExport() {
      downloadLink(
        "/monthplan/mdmOutbountOrdersNotScan/export",
        this.formatParams(false)
      );
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }
      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listMdmOutbountOrdersNotScan(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    this.getList();
  },
  activated() {},
};
</script>
<style lang="scss" scoped>
</style>
