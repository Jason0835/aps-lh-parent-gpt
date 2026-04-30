<template>
  <basic-container>
    <page-table
      tableRef="EstimateExceedShortMainTable"
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
        <!--<el-button-->
        <!--  type="primary"-->
        <!--  plain-->
        <!--  v-hasPermi="['monthplan:estimateExceedShort:add']"-->
        <!--  @click="handleAdd"-->
        <!--&gt;{{ $t("ui.frame.btn.add") }}-->
        <!--</el-button-->
        <!--&gt;-->
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:estimateExceedShort:edit']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:estimateExceedShort:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button>
        <el-button
          v-hasPermi="['monthplan:estimateExceedShort:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:estimateExceedShort:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/lean/estimateExceedShort/importTemplate"
      uploadUrl="/lean/estimateExceedShort/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import {
  listEstimateExceedShort,
  editEstimateExceedShort,
  removeEstimateExceedShort,
} from "@/api/mdm/estimateExceedShort";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "EstimateExceedShort",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_stor_type", "biz_yes_no"],
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
        { type: "selection", fixed: "left" },
        {
          prop: "year",
          label: this.$t("ui.data.colume.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.colume.factory"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.estimateExceedShort.productCode"),
        },
        {
          prop: "storType",
          label: this.$t("ui.data.column.estimateExceedShort.storType"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "exceedShortQty",
          label: this.$t("ui.data.column.estimateExceedShort.exceedShortQty"),
        },
        {
          prop: "isImport",
          label: this.$t("ui.data.column.estimateExceedShort.isImport"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_yes_no,
              row.isImport
            );
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.colume.year"),
          prop: "year",
          type: "date",
          dateType: "year",
          valueFormat: "yyyy",
        },
        {
          label: this.$t("ui.data.colume.month"),
          prop: "month",
          type: "date",
          dateType: "month",
          valueFormat: "MM",
        },
        {
          label: this.$t("ui.data.colume.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.estimateExceedShort.storType"),
          prop: "locationType",
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.estimateExceedShort.productCode"),
        },
      ];
    },
  },
  methods: {
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
        const ids = rows.map((row) => row.id).join(",");
        console.log(ids);
        removeEstimateExceedShort({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let label =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(label, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editEstimateExceedShort({
            ...row,
            status,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
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
        "/lean/estimateExceedShort/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
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

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listEstimateExceedShort(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {},
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
