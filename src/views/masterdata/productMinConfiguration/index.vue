
<template>
  <basic-container>
    <page-table
      tableRef="productMinConfigurationMainTable"
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
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productMinConfiguration:add']"
          @click="handleAdd"
          >{{ $t("ui.confMinProd.btn.add") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          :disabled="selection.length[0]"
          v-hasPermi="['monthplan:productMinConfiguration:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:productMinConfiguration:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['monthplan:productMinConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <!-- <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:productMinConfiguration:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/productMinConfiguration/importTemplate"
      uploadUrl="/monthplan/productMinConfiguration/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <addDialog ref="addRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listProductMinConfiguration,
  editProductMinConfiguration,
  removeProductMinConfiguration,
} from "@/api/monthplan/productMinConfiguration";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import addDialog from "./components/addDialog.vue";

export default {
  name: "ProductMinConfiguration",
  components: {
    tltUpload,
    infoDialog,
    addDialog,
  },
  dicts: ["biz_product_name", "biz_factory_name"],
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
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.confMinProd.factoryCode"),
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.confMinProd.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.confMinProd.productDescription"),
        },
        {
          prop: "productType",
          label: this.$t("ui.data.column.confMinProd.productType"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_product_name,
              row.productType
            );
          },
        },
        {
          prop: "minQty",
          label: this.$t("ui.data.column.confMinProd.minQty"),
        },
        {
          prop: "upQty",
          label: this.$t("ui.data.column.confMinProd.upQty"),
        },
        // {
        //   prop: "proSize",
        //   label: this.$t("ui.data.column.confMinProd.proSize"),
        // },
        // {
        //   prop: "specifications",
        //   label: this.$t("ui.data.column.confMinProd.specifications"),
        // },
        // {
        //   prop: "pattern",
        //   label: this.$t("ui.data.column.confMinProd.pattern"),
        // },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 180,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["monthplan:productMinConfiguration:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["monthplan:productMinConfiguration:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete([row])}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
        },
      ];
      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.confMinProd.productCode"),
          prop: "productCode",
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.confMinProd.productDescription"),
        },
        {
          prop: "productType",
          label: this.$t("ui.data.column.confMinProd.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_name,
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.addRef) {
        this.$refs.addRef.show();
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
        removeProductMinConfiguration({ ids }).then((data) => {
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
          const res = await editMachine({
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
        "/monthplan/productMinConfiguration/export",
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
        const data = await listProductMinConfiguration(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    async publishSchedule() {
      try {
        this.loading = true;
        const valid = await publishValidate();
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.hasNullLhMachineCode")
          ).then(async () => {
            const result = await publishScheduleResult();
            this.$emit("success");
            this.hide();
          });
        } else {
          const result = await publishScheduleResult();
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.error(error);
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
