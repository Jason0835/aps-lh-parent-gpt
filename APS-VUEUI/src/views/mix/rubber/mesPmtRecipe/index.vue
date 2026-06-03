
<template>
  <basic-container class="mes-pmt-recipe" v-loading="loading">
    <div class="top">
      <page-table
        tableRef="MixRubberMesPmtRecipeMainTable"
        :calcHeight="true"
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
        @current-change="handleCurrentChange"
        :showSummary="false"
        :selectArea="false"
      >
        <template slot="header">
          <!-- <el-button
            v-hasPermi="['setting:MesPmtRecipe:add']"
            @click="handleAdd"
            >{{ $t("ui.frame.btn.add") }}</el-button
          >
          <el-button
            v-hasPermi="['setting:MesPmtRecipe:edit']"
            @click="handleEdit(selection[0])"
            :disabled="selection.length != 1"
            >{{ $t("ui.frame.btn.modify") }}</el-button
          > -->
          <!-- <el-button
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
          <!--
          <el-button
            v-hasPermi="['setting:MesPmtRecipe:export']"
            type="warning"
            @click="handleExport"
            >{{ $t("ui.frame.btn.export") }}</el-button
          > -->
          <el-button
            v-hasPermi="['setting:MesPmtRecipe:import']"
            @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
          >
          <el-button
            v-hasPermi="['setting:MesPmtRecipe:syncMesPmtRecipe']"
            type="warning"
            @click="handleSyncMesPmtRecipe"
            >{{ $t("btn.MesPmtRecipe.syncMesPmtRecipe") }}</el-button
          >
        </template>
      </page-table>
      <tlt-upload-form
        ref="tltUploadForm"
        title="导入信息数据"
        downloadUrl="/setting/MesPmtRecipe/importTemplate"
        uploadUrl="/setting/MesPmtRecipe/importData"
        @uploadSuccess="getList"
        labelWidth="0"
        :columns="importColumns"
        :rules="importRules"
      />
      <!--
      <InfoDialog ref="infoDialogRef" @success="handelSuccess" /> -->
    </div>
    <div class="bottom">
      <page-table
        tableRef="MixRubberMesPmtRecipeSubTable"
        :calcHeight="true"
        :columns="subColumns"
        :data="subTableData"
      />
    </div>
  </basic-container>
</template>
<script>
import { listMesPmtRecipe, syncMesPmtRecipe } from "@/api/setting/MesPmtRecipe";
import { listMesPmtRecipeWeight } from "@/api/setting/MesPmtRecipeWeight";

// import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
export default {
  name: "MixRubberMesPmtRecipe",
  components: {
    // InfoDialog,
    TltUploadForm
  },
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
    // "STATUS",
    "PRODUCT_STAGE",
    "AUDIT_FLAG",
    "RECIPE_STATE",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importDefaultValue: {
        updateSupport: false,
      },
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            console.log(form);
            return (
              <el-checkbox
                label="是否更新已经存在的用户数据"
                true-label={true}
                false-label={false}
                v-model={form.updateSupport}
              >
                是否更新已经存在的用户数据
              </el-checkbox>
            );
          },
        },
      ],
      importRules: {
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },

      searchColumns: [
        {
          label: this.$t("setting.MesPmtRecipe.recipeMaterialName"),
          prop: "recipeMaterialName",
        },
        {
          label: this.$t("setting.MesPmtRecipe.machineName"),
          prop: "machineName",
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeTypeName"),
          prop: "recipeTypeName",
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeState"),
          prop: "recipeState",
          render: (form) => {
            return (
              <dict-select
                v-model={form.recipeState}
                options={this.dict.type.RECIPE_STATE}
              />
            );
          },
        },
      ],
      columns: [
        // { type: "selection", fixed: "left" },
        {
          prop: "recipeId",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.recipeId"),
        },
        {
          prop: "recipeEquipCode",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.recipeEquipCode"),
        },
        {
          prop: "machineName",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.machineName"),
        },
        // {
        //   prop: "machineName",
        //   sortable: "custom",
        //   label: this.$t("setting.MesPmtRecipe.machineName"),
        // },
        {
          prop: "recipeMaterialCode",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.recipeMaterialCode"),
        },
        {
          prop: "recipeMaterialName",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.recipeMaterialName"),
        },
        {
          prop: "recipeVersionId",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.recipeVersionId"),
        },
        {
          prop: "recipeType",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.recipeType"),
        },
        {
          prop: "productStage",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.productStage"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.PRODUCT_STAGE, value);
          },
        },
        {
          prop: "recipeTypeName",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.recipeTypeName"),
        },
        {
          prop: "summerMixTime",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.summerMixTime"),
        },
        {
          prop: "lotTotalWeight",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.lotTotalWeight"),
        },
        {
          prop: "recipeState",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.recipeState"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.RECIPE_STATE, value);
          },
        },
        {
          prop: "auditFlag",
          // sortable: "custom",
          label: this.$t("setting.MesPmtRecipe.auditFlag"),
          align: "left",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.AUDIT_FLAG, value);
          },
        },
      ],
      subColumns: [
        {
          label: this.$t("setting.MesPmtRecipeWeight.recipeId"),
          prop: "recipeId",
        },
        {
          label: this.$t("setting.MesPmtRecipeWeight.weightOrder"),
          prop: "weightOrder",
        },
        {
          label: this.$t("setting.MesPmtRecipeWeight.recipeMaterialCode"),
          prop: "recipeMaterialCode",
        },
        {
          label: this.$t("setting.MesPmtRecipeWeight.recipeMaterialName"),
          prop: "recipeMaterialName",
        },
        {
          label: this.$t("setting.MesPmtRecipeWeight.setWeight"),
          prop: "setWeight",
        },
        {
          label: this.$t("setting.MesPmtRecipeWeight.allowError"),
          prop: "allowError",
        },
      ],
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      selection: [],
      currentRow: null,
      subTableData: [],
    };
  },
  computed: {},
  watch: {
    currentRow: function () {
      this.getSubList();
    },
  },
  methods: {
    handleChangeStatus(status, row) {
      console.log(status);
      let title =
        status === "1"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(title, {
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
    handleAdd() {
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show(row);
      }
    },

    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeMachineGlueDecompose({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有机台信息？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams(false);
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportData(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleQuery() {},
    handleHistoryQuery() {},

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
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleCurrentChange(row) {
      this.currentRow = row;
    },

    //util
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
    /**
     * 获得列表参数
     * @param {boolean} hasPage
     * @returns {object}
     */
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
    // a

    //
    async getList() {
      try {
        this.loading = true;
        const data = await listMesPmtRecipe(this.formatParams());

        this.data = data.rows.map((el) => {
          return {
            ...el,
            // tempStatus: el.status,
          };
        });
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getSubList() {
      if (!this.currentRow) {
        this.subTableData = [];
        return;
      }
      try {
        this.loading = true;
        const res = await listMesPmtRecipeWeight({
          fatherRecipeId: this.currentRow.recipeId,
        });
        this.subTableData = res.rows;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    handleSyncMesPmtRecipe() {
      this.$confirm(this.$t("message.confirm.syncMesPmtRecipe"), {
        type: "warning",
      }).then(() => {
        this.loading = true;
        syncMesPmtRecipe()
          .then((data) => {
            this.loading = false;
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          })
          .catch(() => {
            this.loading = false;
          });
      });
    },
  },
  mounted() {},
  activated() {
    // this.getList();
  },
};
</script>
<style lang="scss" scope>
.mes-pmt-recipe {
  .top {
    height: 60%;
  }
  .bottom {
    height: 40%;
  }
}
</style>
