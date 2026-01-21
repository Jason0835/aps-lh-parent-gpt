<template>
  <basic-container>
    <page-table
      tableRef="DemandPlanMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
       :row-class-name="tableRowClassName"
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
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button> -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:productionMouldConfiguration:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button> -->

        <!-- <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button> -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmFinishStock:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/productionMouldConfiguration/importTemplate"
      uploadUrl="/monthplan/productionMouldConfiguration/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { getFinishList } from "@/api/monthplan/finishStock";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "VersionInventory",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: ["biz_factory_name", "biz_product_type", "biz_yes_no",'biz_brand_type'],
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
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
        },
        {
          prop: "year",
          label: this.$t("ui.data.colume.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
        },
        {
          prop: "requireVersion",
          label: this.$t("ui.data.column.finishStock.requireVersion"),
          width:150
        },

        {
          prop: "brand",
          label: this.$t("common.brand"),
          width:120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          width:180
        },

        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width:320
        },
        {
          prop: "stockQty",
          label: this.$t("ui.data.column.finishStock.stockQty"),
        },
        {
          prop: "weekYear",
          label: this.$t("ui.data.column.monthplan.weekYear"),
        },
        // {
        //   prop: "dynamicBalance",
        //   label: this.$t("ui.data.column.monthplan.dynamicBalance"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },

        // {
        //   prop: "uniformity",
        //   label: this.$t("ui.data.column.monthplan.uniformity"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, value);
        //   },
        // },

        {
          prop: "isExceedSixMonth",
          label: this.$t("ui.data.column.finishStock.isExceedSixMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },

        {
          prop: "isExceedNineMonth",
          label: this.$t("ui.data.column.finishStock.isExceedNineMonth"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },

        {
          prop: "updateTime",
          width: 180,
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
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
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "requireVersion",
          label: this.$t("ui.data.column.finishStock.requireVersion"),
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },

        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
      ];
    },
  },
  methods: {
    tableRowClassName({ row, rowIndex }) {
      if (row.isExceedNineMonth == 1) {
        return "deep-yellow";
      }
      if (row.isExceedSixMonth == 1) {
        return "light-yellow";
      }

      return "";
    },
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        // const ids = rows.map((row) => row.id).join(",");
        // console.log(ids);
        // removeProductionMouldConfiguration({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },

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
    handelSuccess() {
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleExport() {
      downloadLink(
        "/monthplan/mdmFinishStock/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handleBuild() {
      if (this.$refs.buildRef) {
        this.$refs.buildRef.show();
      }
    },

    // utils
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
    // api
    async getList() {
      try {
        this.loading = true;

        const data = await getFinishList(this.formatParams());
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
    // this.getList();
  },
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
