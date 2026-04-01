<template>
  <basic-container>
    <page-table
      tableRef="ShippedNotScanVersionMainTable"
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
          type="primary"
          v-hasPermi="['monthplan:dpShippedNotScanVersion:generate']"
          @click="handleGenerate"
          >生成版本
        </el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:dpShippedNotScanVersion:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { getList, getVersionSelect, generateVersion } from "@/api/monthplan/shippedNotScanVersion";

export default {
  name: "ShippedNotScanVersion",
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
      versionList: [],
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
          label: this.$t("common.factory"),
          minWidth: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "year",
          label: this.$t("ui.data.colume.year"),
          minWidth: 80,
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
          minWidth: 80,
        },
        {
          prop: "requireVersion",
          label: "需求版本号",
          minWidth: 150,
        },
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
          prop: "sapCode",
          label: "物料编码",
          minWidth: 150,
        },
        {
          prop: "materialName",
          label: "物料描述",
          minWidth: 200,
        },
        {
          prop: "dot",
          label: "年周号",
          minWidth: 100,
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          listeners: {
            change: this.handleFactoryChange,
          },
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          listeners: {
            change: this.handleYearMonthChange,
          },
        },
        {
          prop: "requireVersion",
          label: "需求版本号",
          type: "select",
          filterable: true,
          dictData: this.versionList,
        },
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
          prop: "sapCode",
          label: "物料编码",
        },
        {
          prop: "materialName",
          label: "物料描述",
        },
        {
          prop: "dot",
          label: "年周号",
        },
      ];
    },
  },
  methods: {
    handleYearMonthChange(val) {
      this.search = {
        ...this.search,
        yearMonth: val,
      };
      this.query = {
        ...this.search,
        yearMonth: val,
      };
      this.getVersionList();
    },
    handleFactoryChange(val) {
      this.search = {
        ...this.search,
        factoryCode: val,
      };
      this.query = {
        ...this.search,
        factoryCode: val,
      };
      this.getVersionList();
    },
    async getVersionList(isGet, isSet = true) {
      if (isGet) {
        this.loading = true;
      }
      try {
        const data = await getVersionSelect(this.formatParams());
        let list = [];
        for (let i = 0; i < data.length; i++) {
          let obj = {
            label: data[i],
            value: data[i],
          };
          list.push(obj);
        }
        this.versionList = list;
        if (!isSet) return;
        if (list.length > 0) {
          this.$set(this.search, "requireVersion", list[0].value);
          this.$set(this.query, "requireVersion", list[0].value);
        } else {
          this.$set(this.search, "requireVersion", "");
          this.$set(this.query, "requireVersion", "");
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      } finally {
        if (isGet) {
          this.page = {
            current: 1,
            pageSize: 20,
            total: 0,
          };
          this.getList();
        }
      }
    },
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getVersionList(true, false);
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
        "/monthplan/dpShippedNotScanVersion/export",
        this.formatParams(false)
      );
    },
    handleGenerate() {
      const params = this.formatParams(false);
      if (!params.factoryCode) {
        this.$modal.msgWarning("请选择工厂");
        return;
      }
      if (!params.year || !params.month) {
        this.$modal.msgWarning("请选择年月");
        return;
      }
      if (!params.requireVersion) {
        this.$modal.msgWarning("请选择需求版本号");
        return;
      }
      this.$confirm("确认生成已出库未扫描版本?", "提示", {
        type: "warning",
      }).then(() => {
        this.loading = true;
        generateVersion(params)
          .then((res) => {
            this.$modal.msgSuccess("生成成功");
            this.getVersionList(true);
          })
          .finally(() => {
            this.loading = false;
          });
      });
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

      if (params.yearMonth) {
        const [year, month] = params.yearMonth.split("-");
        params.year = year;
        params.month = month;
      }

      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await getList(this.formatParams());
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
    const now = new Date();
    const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    const year = nextMonth.getFullYear();
    const month = nextMonth.getMonth() + 1;
    let defaultParams = {
      factoryCode: "116",
      yearMonth: `${year}-${month}`,
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getVersionList(true);
  },
  activated() {},
};
</script>
<style lang="scss" scoped>
</style>
