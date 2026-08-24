
<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
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

          >{{ $t("ui.data.column.moldLedger.mes") }}</el-button
        > -->
        <el-button
          type="primary"
          plain
          @click="handleAdd"
          v-hasPermi="['maindata:rawSpecialMaterialStock:edit']"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="danger"
           v-hasPermi="['maindata:rawSpecialMaterialStock:remove']"
          :disabled="selection.length == 0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['maindata:rawSpecialMaterialStock:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['maindata:rawSpecialMaterialStock:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/maindata/rawSpecialMaterialStock/importTemplate"
      uploadUrl="/maindata/rawSpecialMaterialStock/importData"
      @uploadSuccess="getList"
    /> -->
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/maindata/rawSpecialMaterialStock/importTemplate"
      uploadUrl="/maindata/rawSpecialMaterialStock/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {listSpecialInfo,removeSpecialInfo} from "@/api/maindata/rawSpecial";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "SpecialRawMaterialConfig",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm
  },
  dicts: ["LINE_TYPE", "JOB_TYPE", "biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("common.rule.updateSupport")}
                v-model={form.updateSupport}
              >
                {this.$t("common.rule.updateSupport")}
              </el-checkbox>
            );
          },
        },
      ],
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
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.factoryCode"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "year",
          align: "center",
          label: this.$t("ui.data.column.monthStock.year"),
          width: 80,
        },
        {
          prop: "month",
          align: "center",
          label: this.$t("ui.data.column.monthStock.month"),
          width: 80,
        },
        {
          prop: "materialCode",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.materialCode"),
          width: 120,
        },
        {
          prop: "materialName",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.materialName"),
          width:200
        },
        {
          prop: "standardLength",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.standardLength"),
        },
        {
          prop: "warehouseStock",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.warehouseStock"),
        },
        {
          prop: "midStock",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.midStock"),
        },
        {
          prop: "workshopStock",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.workshopStock"),
        },
        {
          prop: "packageNum",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.packageNum"),
        },
        {
          prop: "totalStock",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.totalStock"),
        },
        {
          prop: "stock",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.stock"),
          width: 120,
        },
        {
          prop: "unit",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.unit"),
        },
        {
          prop: "stockDate",
          align: "center",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.stockDate"),
          width: 120,
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.rawSpecialMaterialRecord.remark"),
          align: "left",
        },
        {
          prop: "updateBy",
          align: "center",
          label: this.$t("ui.data.column.updateBy"),
          width: 100,
        },
        {
          prop: "updateTime",
          align: "center",
          width: 180,
          label: this.$t("ui.data.column.updateTime"),
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          fixed: "right",
          width: 160,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["maindata:rawSpecialMaterialStock:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["maindata:rawSpecialMaterialStock:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
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
          label: this.$t("ui.data.column.rawSpecialMaterialStock.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,

        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.materialCode"),
        },
        {
          prop: "materialName",
          label: this.$t("ui.data.column.rawSpecialMaterialStock.materialName"),
        }
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
    handleDeleteAll() {
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids = ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeSpecialInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeSpecialInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
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
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/maindata/rawSpecialMaterialStock/export", this.formatParams(false));
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

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }
      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth=''
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;

        const data = await listSpecialInfo(this.formatParams());
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
    const year = now.getFullYear(); // 2024
    const month = now.getMonth() + 1; // 注意：月份从0开始，需要+1
    let defaultParams = {
      yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
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
  activated() {

  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
