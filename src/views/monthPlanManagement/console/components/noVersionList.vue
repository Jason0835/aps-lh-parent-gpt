<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <page-table
      tableRef="ConsoleMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="undefined"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
    </page-table>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button
        type="primary"
        :disabled="selection.length != 1"
        :loading="loading"
        @click="handleConfirm"
        >{{ this.$t("common.button.confirm") }}</el-button
      >
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import { versionListConsole, versionConfirm } from "@/api/factory/console";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
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
      map: null,
      paramsDate: {},
    };
  },
  computed: {
    title: function () {
      return this.$t("common.button.add");
    },
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },

        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthSaleOrderPlan.factoryCode"),
          align: "center",

          formatter: (row) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          label: this.$t("产品品类"),
          prop: "productTypeCode",
          align: "center",

          render: ({ row }) => {
            return (
              <div>
                <div>TBR</div>
                <div>
                  <text-button
                    onClick={() => {
                      this.handleGenerate(row);
                    }}
                  >
                    {this.$t("生成")}
                  </text-button>
                </div>
              </div>
            );
          },
        },
        {
          label: this.$t("胎别"),
          prop: "productTypeCode",
          align: "center",
        },

        {
          label: this.$t("版本号"),
          prop: "monthPlanVersion",
          align: "center",
        },
        {
          label: this.$t("创建时间"),
          prop: "createTime",
          align: "center",
        },
      ];

      return columns;
    },
    searchColumns() {
      return [];
    },
  },
  methods: {
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
    handleFinalized() {
      if (this.$refs.finRef) {
        this.$refs.finRef.show(this.selection[0]);
      }
    },
    formatParams(hasPage = false) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = undefined;
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
        const data = await versionListConsole(this.formatParams());
        console.log(data);

        this.data = data.rows;

        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    // api
    async save(params) {
      try {
        this.loading = true;

        // const res = await createSaleRequirePlan(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      this.query = {
        ...data,
      };
      this.paramsDate = {
        ...data,
      };
      this.getList();
    },
    hide() {
      this.isEdit = false;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    async handleConfirm() {
      try {
        this.loading = true;
        const params = {
          ...this.query,
          ...this.selection[0],
        };



        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.year = arr[0];
          params.month = arr[1];
          params.yearMonth = undefined;
        }

        const res = await versionConfirm(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
  },
};
</script>
