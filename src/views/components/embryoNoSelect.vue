<template>
  <select-dialog
    ref="dialogRef"
    :title="getTitle()"
    :value="showValue"
    @confirm="handleConfirm"
    @clear="handleClear"
    :disabled="disabled"
    @show="handleShow"
    @cancel="handleCancel"
  >
    <div class="table-container" v-loading="loading">
      <page-table
        :calcHeight="true"
        ref="tableRef"
        :columns="columns"
        :searchColumns="searchColumns"
        :data="data"
        :toolbar="false"
        :page="page"
         :search="search"
        :highlight-current-row="true"
        @current-change="handleCurrentChange"
        @row-dblclick="handleDbClick"
        @pageChange="handlePageChange"
        @selection-change="handleSelectionChange"
        @search="handleSearch"
      >
      </page-table>
    </div>
  </select-dialog>
</template>

<script>
//物料选择
import { deepClone } from "@/utils";

import selectDialog from "@/components/Table/SelectDialog.vue";
import { listProductinfo } from "@/api/lean/productinfo";
import {
  listMdmProductConstruction,mesCapture,updateMaterial
} from "@/api/maindata/mdmProductConstruction";
export default {
  components: { selectDialog },
  inject: ["parentDict"],
  model: {
    prop: "value",
    event: "change",
  },
  props: {
    value: String | Number,
    title: String,
    disabled: Boolean,
    materialCode:String,
    label: String,
    multiple: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      search:{},
      searchKey: "",
      // searchColumns: [

      //   {
      //     label: this.$t("ui.data.colume.wms.unused.productCode"),
      //     prop: "materialCode",
      //   },
      // ],
      filterKey: "",
      page: {
        current: 1,
        pageSize: 10,
        total: 0,
      },
      query: {},
      showValue: "",
      loading: false,
      valueProp: "embryoNo",
      labelProp: "embryoNo",
      data: [],
    };
  },
  computed: {
    searchColumns: function () {
      return [
              {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select", //GLUE_TYPE
          dictData: this.parentDict.type.biz_factory_name,
        },
        // {
        //   label: this.$t("ui.data.colume.wms.unused.productCode"),
        //   prop: "materialCode",
        // },
      ];
    },
    columns: function () {
      const list = [
      {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          width: 120,

          formatter: (row, column, value) => {
            return this.selectDictLabel(this.parentDict.type.biz_factory_name, value);
          },
        },
        //  {
        //   prop: "bomVersion",
        //   label: this.$t("ui.data.column.boom.boomVersion"),
        //   width: 120,
        // },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
          {
          prop: "trialStatus",
          label: this.$t("ui.data.column.trialPlan.trialStatus"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.parentDict.type.trial_status, value);
          },
        },
        {
          prop: "isZeroRack",
          label: this.$t("ui.data.column.mpMonthlySaleQty.isZeroRack"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.parentDict.type.biz_yes_no, value);
          },
        },
        {
          prop: "embryoNo",
          label: this.$t("ui.data.column.trialPlan.embryoNo"),
        },
      ];
      if (this.multiple) {
        list.unshift({
          type: "selection",
          reserveSelection:true
        });
      }

      return list;
    },
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
    async getList() {
      try {
        this.loading = true;
        const data = await listMdmProductConstruction(this.formatParams());
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
        // userName: this.filterKey,
        status: 0, //过滤，只显示启用的用户
      };
    },
    getTitle() {
      return this.title
        ? this.title
        : this.$t("ui.data.column.trialPlan.embryoNo");
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
      this.query = query;
      // this.filterKey = this.searchKey;

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

    handleShow() {
      let defaultParams = {
        factoryCode: "116",
        materialCode:this.materialCode
      };
      this.search = {
        ...defaultParams,
      };
      this.query = {
        ...defaultParams,
      };
      this.getList();
    },
    handleCancel() {
      this.data = [];
      this.query = {};
      this.searchKey = "";
      this.filterKey = "";
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
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleClear() {
      if (this.$refs.tableRef) {
        const tableRef = this.$refs.tableRef.getTableRef();
        tableRef && tableRef.setCurrentRow();
      }
      this.showValue = "";
      this.$emit("change", undefined);
    },
    handleConfirm(done) {
      if (!this.multiple) {
        if (this.currentRow) {
          this.showValue = this.currentRow[this.labelProp] + "";
          this.$emit("updateValue", this.currentRow[this.valueProp]);
          this.$emit("change", this.currentRow[this.valueProp], {
            ...this.currentRow,
          });
        }
        done();
      } else {
        if (this.selection) {
          const ids = this.selection
            .map((item) => item[this.valueProp])
            .join(",");
          this.showValue = this.selection
            .map((item) => item[this.labelProp])
            .join(",");
          this.$emit("updateValue", ids);
          this.$emit("change", ids, deepClone(this.selection));
        }else{
          this.$modal.msgWarning(this.$t('common.rule.select'));
          return
        }

        done();
      }
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
.table-container {
  height: 450px;
}
</style>
