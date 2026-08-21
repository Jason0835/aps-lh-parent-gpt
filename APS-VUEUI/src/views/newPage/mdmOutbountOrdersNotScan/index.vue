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
  dicts: ["biz_factory_name"],
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
          prop: "factoryCode",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "saleBillNo",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.saleBillNo"),
          minWidth: 160,
        },
        {
          prop: "saleOrderNo",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.saleOrderNo"),
          minWidth: 160,
        },
        // {
        //   prop: "saleOrg",
        //   label: "销售组织编码",
        //   minWidth: 120,
        // },
        {
          prop: "saleOrgName",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.saleOrgName"),
          minWidth: 200,
        },
        {
          prop: "sellTo",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.sellTo"),
          minWidth: 100,
        },
        {
          prop: "billId",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.billId"),
          minWidth: 160,
        },
        // {
        //   prop: "materialCode",
        //   label: "MES物料编码",
        //   minWidth: 150,
        // },
        {
          prop: "sapCode",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.sapCode"),
          minWidth: 150,
        },
        {
          prop: "materialName",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.materialName"),
          minWidth: 200,
        },
        {
          prop: "dot",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.dot"),
          minWidth: 120,
        },
        {
          prop: "stockDate",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.stockDate"),
          minWidth: 120,
        },
        {
          prop: "scanAmount",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.scanAmount"),
          minWidth: 100,
        },
        {
          prop: "outAmount",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.outAmount"),
          minWidth: 100,
        },
        {
          prop: "noscanAmount",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.noscanAmount"),
          minWidth: 100,
        },
        {
          prop: "saleItemNo",
          align: "center",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.saleItemNo"),
          minWidth: 140,
        },
        {
          prop: "updateBy",
          align: "center",
          label: this.$t("ui.data.column.updateBy"),
          minWidth: 100,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.updateTime"),
          minWidth: 180,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "saleBillNo",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.saleBillNo"),
        },
        {
          prop: "saleOrderNo",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.saleOrderNo"),
        },
        {
          prop: "sellTo",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.sellTo"),
        },
        {
          prop: "billId",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.billId"),
        },
        // {
        //   prop: "materialCode",
        //   label: "MES物料编码",
        // },
        {
          prop: "sapCode",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.sapCode"),
        },
        {
          prop: "materialName",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.materialName"),
        },
        {
          prop: "stockDate",
          label: this.$t("ui.data.column.mdmOutbountOrdersNotScan.stockDate"),
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
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
      if (params.stockDate && params.stockDate[0]) {
        params.stockDateStart = params.stockDate[0];
        params.stockDateEnd = params.stockDate[1];
        params.stockDate = undefined;
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
    let defaultParams = {
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {},
};
</script>
<style lang="scss" scoped>
</style>
