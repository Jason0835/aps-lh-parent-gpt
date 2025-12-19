
<template>
  <basic-container>
    <page-table
      tableRef="ProductinfoMainTable"
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
          v-hasPermi="['lean:productinfo:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          :disabled="selection.length[0]"
          v-hasPermi="['lean:productinfo:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['lean:productinfo:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['lean:productinfo:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <!-- <el-button
          @click="handleExport"
          v-hasPermi="['lean:productinfo:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
        <el-button
          v-hasPermi="['lean:productinfo:importGrossRate']"
          @click="$refs.tltUploadGross.handleImport()"
          >{{
            $t("ui.data.column.lean.productinfo.importGrossRate")
          }}</el-button
        >
        <el-button
          @click="handleGrossRateExport"
          v-hasPermi="['lean:productinfo:exportGrossRate']"
          >{{
            $t("ui.data.column.lean.productinfo.exportGrossRate")
          }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/lean/productinfo/importTemplate"
      uploadUrl="/lean/productinfo/importData"
      @uploadSuccess="getList"
    />
    <tlt-upload
      ref="tltUploadGross"
      downloadUrl="/lean/productinfo/exportGrossRate"
      uploadUrl="/lean/productinfo/importGrossRate"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listProductinfo,
  editProductinfo,
  removeProductinfo,
} from "@/api/lean/productinfo";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import TPopover from "@/views/components/tPopover.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "Productinfo",
  components: {
    tltUpload,
    infoDialog,
    TPopover,
  },
  dicts: ["TIRE_TYPE", "biz_can_not", "biz_common_type", "biz_brand_type"],
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
          prop: "productCode",
          label: this.$t("ui.data.column.lean.productinfo.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.lean.productinfo.productDesc"),
          width: 300,
        },
        {
          prop: "tireType",
          label: this.$t("ui.data.column.lean.productinfo.tireType"),
          width: 180,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.TIRE_TYPE, value);
          },
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.lean.productinfo.productTypeCode"),
          formatter: (row) => {
            return row.productTypeCode;
          },
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.lean.productinfo.proSize"),
        },
        {
          prop: "commonType",
          label: this.$t("ui.data.column.lean.productinfo.commonType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_common_type, value);
          },
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.lean.productinfo.specifications"),
          width: 120,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.lean.productinfo.pattern"),
          width: 140,
        },
        {
          prop: "brand",
          label: this.$t("ui.data.column.lean.productinfo.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        // {
        //   prop: "curingTime",
        //   label: this.$t("ui.data.column.lean.productinfo.curingTime"),
        // },
        // {
        //   prop: "hydraulicPressureCuringTime",
        //   label: this.$t(
        //     "ui.data.column.lean.productinfo.hydraulicPressureCuringTime"
        //   ),
        // },
        {
          prop: "hierarchy",
          label: this.$t("ui.data.column.lean.productinfo.hierarchy"),
        },
        {
          prop: "speed",
          label: this.$t("ui.data.column.lean.productinfo.speed"),
        },
        {
          prop: "ability",
          label: this.$t("ui.data.column.lean.productinfo.ability"),
        },
        // {
        //   prop: "specificationsPattern",
        //   label: this.$t("SAP组"),
        // },
        {
          prop: "cantProduce",
          label: this.$t("ui.data.column.lean.productinfo.cantProduce"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_can_not, value);
          },
        },
        {
          prop: "outGrossRate",
          label: this.$t("ui.data.column.lean.productinfo.outGrossRate"),
        },
        {
          prop: "inGrossRate",
          label: this.$t("ui.data.column.lean.productinfo.inGrossRate"),
        },
        {
          prop: "oeGrossRate",
          label: this.$t("ui.data.column.lean.productinfo.oeGrossRate"),
        },
      ];
      if (this.$auth.hasPermi("lean:productinfo:edit")) {
        columns.push({
          align: "center",
          halign: "center",
          fixed: "right",
          label: this.$t("ui.data.btn.option"),
          render: ({ row }) => {
            return (
              <el-button
                class="minus"
                type="success"
                onClick={() => this.handleEdit(row)}
              >
                {this.$t("ui.frame.btn.update")}
              </el-button>
            );
          },
        });
      }

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.lean.productinfo.productCode"),
          prop: "productCode",
        },
        {
          prop: "tireType",
          label: this.$t("ui.data.column.lean.productinfo.tireType"),
          type: "select",
          dictData: this.dict.type.TIRE_TYPE,
        },
        {
          prop: "commonType",
          label: this.$t("ui.data.column.lean.productinfo.commonType"),
          type: "select",
          dictData: this.dict.type.biz_common_type,
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.lean.productinfo.specifications"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.lean.productinfo.pattern"),
        },
        {
          prop: "brand",
          label: this.$t("ui.data.column.lean.productinfo.brand"),
          type: "select",
          dictData: this.dict.type.biz_brand_type,
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
        removeProductinfo({ ids }).then((data) => {
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
      downloadLink("/lean/productinfo/export", this.formatParams(false));
    },
    handleGrossRateExport() {
      downloadLink(
        "/lean/productinfo/exportGrossRate",
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
        const data = await listProductinfo(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async updateRow(value, index) {
      try {
        let data = this.data[index];

        editProductinfo();
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
