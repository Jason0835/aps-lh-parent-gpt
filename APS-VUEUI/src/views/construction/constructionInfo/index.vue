
<template>
  <basic-container>
    <page-table
      tableRef="ProductConstructionMainTable"
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
        <!-- <el-button type="primary" plain @click="mesCap">{{
          $t("ui.data.column.moldLedger.mes")
        }}</el-button> -->
        <el-button
          v-hasPermi="['monthplan:mdmBomInfo:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmBomInfo:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          @click="handleGoDetail"
          v-hasPermi="['mdm:mdmMaterialConsumeDetail:list']"
          >{{ $t("胎胚原材料清单") }}</el-button
        >
        <!-- <el-button
          type="primary"
          v-hasPermi="['cx:productConstruction:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['cx:machine:edit']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:productConstruction:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['cx:productConstruction:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/maindata/mdmBomInfo/importTemplate"
      uploadUrl="/maindata/mdmBomInfo/importData"
      @uploadSuccess="getList"
    /> -->
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/maindata/mdmBomInfo/importTemplate"
      uploadUrl="/maindata/mdmBomInfo/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <!-- <infoDialog ref="infoRef" @success="getList" />
    <addDialog ref="addRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listConstructionInfo, mesCapture } from "@/api/cx/constructionInfo.js";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";

// import infoDialog from "./components/infoDialog.vue";
// import addDialog from "./components/addDialog.vue";

export default {
  name: "ConstructionInfo",
  components: {
    tltUpload,
    TltUploadForm,
    // infoDialog,
    // addDialog,
  },
  dicts: ["LINE_TYPE", "production_stage", "biz_factory_name"],
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
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },

        {
          prop: "parentMaterialCode",
          label: this.$t("ui.data.column.boom.fatherCode"),
          width: 180,
        },

        {
          prop: "parentMaterialName",
          label: this.$t("ui.data.column.boom.fatherName"),
          width: 180,
        },
        {
          prop: "parentVersion",
          label: this.$t("ui.data.column.boom.parentVersion"),
          width: 180,
        },
        {
          prop: "bomVersion",
          label: this.$t("ui.data.column.boom.boomVersion"),
          width: 180,
        },
        {
          prop: "productionStage",
          label: this.$t("ui.data.column.cx.bom.productionStage"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.production_stage, value);
          },
          width: 180,
        },
        // {
        //   prop: "parentMaterialCode",
        //   label: "父物料版本信息",
        // },
        {
          prop: "childMaterialCode",
          label: this.$t("ui.data.column.boom.chirenCode"),
          width: 180,
        },
        {
          prop: "childMaterialName",
          label: this.$t("ui.data.column.boom.chirenName"),
          width: 180,
        },
        {
          prop: "childCode",
          label: this.$t("ui.data.column.boom.chirenType"),
          width: 180,
        },
        {
          prop: "childMaterialVersion",
          label: this.$t("ui.data.column.boom.childMaterialVersion"),
          width: 180,
        },
        {
          prop: "unit",
          label: this.$t("ui.data.column.boom.unit"),
          width: 180,
        },
        {
          prop: "dosage",
          label: this.$t("ui.data.column.boom.dosage"),
          width: 180,
        },
        {
          prop: "dosageForm",
          label: this.$t("ui.data.column.boom.dosageForm"),
          width: 180,
        },
        {
          prop: "status",
          label: this.$t("common.status"),
          render: ({ row }) => {
            return (
              <div>
                {row.status == 1
                  ? this.$t("common.job.column.normal")
                  : this.$t(
                      "financialManagement.exportSalesInvoice.button.repeal"
                    )}
              </div>
            );
          },
          minWidth: 100,
        },
        // {
        //   prop: "updateTime",
        //   width: 180,
        //   label: this.$t("ui.data.column.scheduleAdjust.updata"),
        // },
      ];
      return columns;
    },
    searchColumns() {
      let searchColumns = [
        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.boom.fatherCode"),
          prop: "parentMaterialCode",
        },
        {
          label: this.$t("ui.data.column.boom.fatherName"),
          prop: "parentMaterialName",
        },
        {
          label: this.$t("ui.data.column.boom.boomVersion"),
          prop: "bomVersion",
        },
        {
          label: this.$t("ui.data.column.boom.chirenCode"),
          prop: "childCode",
        },
        {
          label: this.$t("ui.data.column.boom.chirenName"),
          prop: "childMaterialName",
        },
      ];
      return searchColumns;
    },
  },
  methods: {
    handleGoDetail() {
      this.$router.push({
        name: "ConsumptionDetails",
      });
    },
    mesCap() {
      this.loading = true;
      mesCapture()
        .then((response) => {
          this.$message.success(response.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        })
        .catch((error) => {
          console.log(error);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleAdd() {
      // if (this.$refs.addRef) {
      //   this.$refs.addRef.show();
      // }
    },
    handleEdit() {
      // if (this.$refs.infoRef) {
      //   this.$refs.infoRef.show(this.selection[0]);
      // }
    },
    handleDelete(row) {
      // this.$confirm(this.$t("common.confirm.delete"), {
      //   type: "warning",
      // }).then(() => {
      //   const ids = row.id;
      //   this.loading = true;
      //   removeProductConstruction({ ids })
      //     .then((data) => {
      //       this.$modal.msgSuccess(data.msg);
      //       this.$set(this.page, "current", 1);
      //       this.getList();
      //     })
      //     .catch((error) => {
      //       console.log(error);
      //       this.loading = false;
      //     });
      // });
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
      downloadLink("/maindata/mdmBomInfo/export", this.formatParams(false));
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
        const data = await listConstructionInfo(this.formatParams());
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
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
