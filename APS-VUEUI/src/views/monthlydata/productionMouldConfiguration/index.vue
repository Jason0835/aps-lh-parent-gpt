<template>
  <basic-container>
    <page-table
      tableRef="productionMouldConfigurationMainTable"
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
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:edit']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:productionMouldConfiguration:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button> -->
        <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:building']"
          type="primary"
          plain
          @click="handleBuild"
          >{{ $t("ui.data.column.productionMouldConfiguration.build") }}
        </el-button>
        <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:productionMouldConfiguration:export']"
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
    <infoDialog ref="infoRef" @success="getList" />
    <buildDialog ref="buildRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import {
  listProductionMouldConfiguration,
  editProductionMouldConfiguration,
  removeProductionMouldConfiguration,
  buildMouldingProduct,
} from "@/api/monthplan/productionMouldConfiguration";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import buildDialog from "./components/buildDialog.vue";

export default {
  name: "ProductionMouldConfiguration",
  components: {
    tltUpload,
    infoDialog,
    buildDialog,
  },
  dicts: ["biz_factory_name", "biz_stor_type"],
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
          label: this.$t("ui.data.column.productionMouldConfiguration.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.productionMouldConfiguration.month"),
        },
        {
          prop: "factoryCode",
          label: this.$t(
            "ui.data.column.productionMouldConfiguration.factoryCode"
          ),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },

        {
          prop: "productCode",
          label: this.$t(
            "ui.data.column.productionMouldConfiguration.productCode"
          ),
        },
        {
          prop: "mouldCode",
          label: this.$t(
            "ui.data.column.productionMouldConfiguration.mouldCode"
          ),
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
          label: this.$t("ui.data.colume.must.finish.plan.productCode"),
          prop: "productCode",
        },
        {
          label: this.$t("ui.data.colume.must.finish.plan.storType"),
          prop: "locationType",
          type: "select",
          dictData: this.dict.type.biz_stor_type,
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
        removeProductionMouldConfiguration({ ids }).then((data) => {
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
          const res = await editProductionMouldConfiguration({
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
        "/monthplan/productionMouldConfiguration/export",
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
        const data = await listProductionMouldConfiguration(
          this.formatParams()
        );
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
