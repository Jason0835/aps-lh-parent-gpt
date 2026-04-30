<!--
 * @Description: 商品选择弹窗
 *
-->
<template>
  <select-dialog
    ref="dialogRef"
    :loading="loading"
    :title="getTitle()"
    :value="showValue"
    @confirm="handleConfirm"
    @clear="handleClear"
    :disabled="disabled"
    @show="handleShow"
    @cancel="handleCancel"
  >
    <div class="content">
      <page-table
        v-loading="loading"
        ref="tableRef"
        :columns="columns"
        :calcHeight="true"
        :searchColumns="searchColumns"
        :data="data"
        :toolbar="false"
        :page="page"
        :highlight-current-row="true"
        @search="handleSearch"
        @current-change="handleCurrentChange"
        @row-dblclick="handleDbClick"
        @pageChange="handlePageChange"
      ></page-table>
    </div>
  </select-dialog>
</template>

<script>
import selectDialog from "@/components/Table/SelectDialog.vue";
import { selectMesPmtRecipeByParams } from "@/api/setting/MesPmtRecipe";
export default {
  components: { selectDialog },

  model: {
    prop: "value",
    event: "change",
  },
  props: {
    value: String | Number,
    title: String,
    disabled: Boolean,
    label: String,
    valueProp: {
      type: String,
      default: "recipeType",
    },
    labelProp: {
      type: String,
      default: "recipeTypeName",
    },
    productStage: {
      type: Array,
      default: () => [],
    },
    params: Object | undefined
  },
  data() {
    return {
      columns: [
        {
          label: this.$t("setting.MesPmtRecipe.recipeId"),
          prop: "recipeId",
          minWidth: 100,
        },
        {
          label: this.$t("setting.MesPmtRecipe.mixArea"),
          prop: "mixArea",
          minWidth: 250,
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeEquipCode"),
          prop: "recipeEquipCode",
          minWidth: 250,
        },
        {
          label: this.$t("setting.MesPmtRecipe.machineName"),
          prop: "recipeEquipCode",
          minWidth: 250,
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeMaterialCode"),
          prop: "recipeEquipCode",
          minWidth: 250,
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeMaterialName"),
          prop: "recipeMaterialName",
          minWidth: 250,
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeVersionId"),
          prop: "recipeVersionId",
          minWidth: 250,
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeType"),
          prop: "recipeType",
          minWidth: 250,
        },
        {
          label: this.$t("setting.MesPmtRecipe.recipeTypeName"),
          prop: "recipeTypeName",
          minWidth: 250,
        },
        {
          label: this.$t("setting.MesPmtRecipe.productStage"),
          prop: "productStage",
          minWidth: 250,
          formatter: (row) => {
            return this.selectDictLabel(this.productStage, row.productStage);
          },
        },
      ],
      searchColumns: [],
      searchKey: "",
      filterKey: "",
      page: {
        current: 1,
        pageSize: 10,
        total: 0,
      },
      query: {},
      showValue: "",
      loading: false,
      // valueProp: "gcode",
      // labelProp: "gnameI18n",
      data: [],
    };
  },
  watch: {
    value: function (val) {
      if (!val) {
        this.showValue = "";
      }
    },

    label: function (val) {
      if (!!val) {
        this.showValue = val;
      } else {
        this.showValue = this.value;
      }
    },
  },

  methods: {
    getTitle() {
      return this.title ? this.title : this.$t("setting.MesPmtRecipe.modelName");
    },
    async getList() {
      try {
        this.loading = true;
        const data = await selectMesPmtRecipeByParams(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    //
    formatParams() {
      return {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        ...(this.params ? this.params: {}),
      };
    },

    /**
     * 重置
     */
    handleSearchReset() {
      this.searchKey = "";
      let query = {};
      this.handleSearch(query);
    },
    handleSearch(query) {
      // this.filterKey = this.searchKey;
      this.query = query;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },

    handleChange(val, row) {
      this.$emit("change", val, row);
    },

    handleShow(params) {
      if (params) {
        this.query = {
          ...params,
        };
      }
      console.log(this.$store);
      this.getList();
    },
    handleCancel() {
      this.data = [];
      this.searchKey = "";
      this.filterKey = "";
      this.query = {};
      this.page = {
        current: 1,
        pageSize: 10,
        total: 0,
      };
    },
    handleCurrentChange(row) {
      this.currentRow = row;
    },
    handleDbClick(row) {
      this.currentRow = row;
      this.handleConfirm(() => {
        this.$refs.dialogRef.hide();
      });
    },
    handleClear() {
      if (this.$refs.tableRef) {
        const tableRef = this.$refs.tableRef.getTableRef();
        tableRef && tableRef.setCurrentRow();
        this.showValue = "";
        this.$emit("change", undefined);
      }
    },
    handleConfirm(done) {
      if (this.currentRow) {
        this.showValue = this.currentRow[this.labelProp] + "";
        this.$emit("change", this.currentRow[this.valueProp], {
          ...this.currentRow,
        });
      }

      done();
    },
  },
  mounted() {
    if (this.label) {
      this.showValue = this.label;
    } else {
      this.showValue = this.value;
    }
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.header {
  margin-bottom: 10px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  .input {
    max-width: 300px;
    flex: 1 1 auto;
  }
  .btn {
    margin-left: 10px;
    flex: 0 0 auto;
  }
}
.content {
  // display: flex;
  // flex-direction: column;
  // height: 100%;
  // flex: 1;
  height: 450px;
}
</style>
