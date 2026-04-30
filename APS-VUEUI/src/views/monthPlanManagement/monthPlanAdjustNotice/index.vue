
<template>
  <basic-container>
    <page-table
      tableRef="MonthPlanAdjustNoticeMainTable"
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
          v-hasPermi="['monthplan:adjustNotice:edit']"
          plain
          @click="handleAdd"
          >{{ $t("创建调整通知单") }}
        </el-button>
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:adjustNotice:edit']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}
        </el-button> -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthPlan:adjustNotice:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button> -->
        <el-button
          v-hasPermi="['monthplan:adjustNotice:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button>
        <!-- <el-button
          @click="handleExport"
          v-hasPermi="['monthPlan:adjustNotice:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button> -->
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/factory/monthPlanAdjustNotice/importTemplate"
      uploadUrl="/factory/monthPlanAdjustNotice/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <adjustDialog ref="adjustRef" @success="getList" />
    <detailDialog ref="detailRef" @success="getList" />
  </basic-container>
</template>
<script>
import moment from "moment";

import { downloadLink } from "@/utils/request";

import {
  listMonthPlanAdjustNotice,
  saveMonthPlanAdjustNotice,
  removeMonthPlanAdjustNotice,
  submitMonthPlanAdjustNotice,
  cancelMonthPlanAdjustNotice,
  getAdjustNoticeAdjustPlan,
} from "@/api/factory/monthPlanAdjustNotice";
import { getVersionList } from "@/api/demand/requireProductionPlan";

import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import adjustDialog from "./components/adjustDialog.vue";
import detailDialog from "./components/detailDialog.vue";

export default {
  name: "MonthPlanAdjustNotice",
  components: {
    tltUpload,
    infoDialog,
    adjustDialog,
    detailDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_channel_type",
    "biz_brand_type",
    "biz_adjust_status",
  ],
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
      verList: [],
    };
  },
  computed: {
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.factoryCode"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "year",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.month"),
        },
        {
          prop: "noticeNo",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.noticeNo"),
          width: 180,
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productDesc"),
          width: 300,
        },
        {
          prop: "needQty",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.needQty"),
        },
        {
          prop: "stockAllocationQty",
          label: this.$t(
            "ui.data.column.monthPlanAdjustNotice.stockAllocationQty"
          ),
        },
        {
          prop: "planQty",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.planQty"),
        },
        {
          prop: "productionQty",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productionQty"),
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.locationType"),
          width: 120,
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.channel"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_channel_type, value);
          },
        },
        // {
        //   prop: "remark",
        //   label: this.$t("common.remark"),
        //   width: 200,
        // },
        {
          prop: "createBy",
          label: this.$t("common.createByName"),
          width: 160,
        },
        {
          prop: "createTime",
          label: this.$t("common.createTime"),
          width: 180,
        },
        {
          prop: "status",
          label: this.$t("common.status"),
          width: 180,
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_adjust_status,
              value
            );
          },
        },
        {
          prop: "option",
          label: this.$t("common.option"),
          width: 120,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                {row.status == "1" ? (
                  <text-button
                    v-hasPermi={["monthplan:adjustNotice:edit"]}
                    onClick={() => this.handleEdit(row)}
                  >
                    {this.$t("common.button.edit")}
                  </text-button>
                ) : null}
                {row.status == "1" ? (
                  <text-button
                    v-hasPermi={["monthplan:adjustNotice:submit"]}
                    onClick={() => this.handleSubmit(row)}
                  >
                    {this.$t("common.button.submit")}
                  </text-button>
                ) : null}
                {row.status == "1" ? (
                  <text-button
                    v-hasPermi={["monthplan:adjustNotice:cancel"]}
                    onClick={() => this.handleCancel(row)}
                  >
                    {this.$t("作废")}
                  </text-button>
                ) : null}
                {row.status == "2" && row.planQty > 0 ? (
                  <text-button
                  v-hasPermi={['monthplan:adjustNotice:confirmAdjust']}

                   onClick={() => this.handleAdjust(row)}>
                    {this.$t("调增")}
                  </text-button>
                ) : null}
                {row.status == "2" && row.planQty < 0 ? (
                  <text-button
                  v-hasPermi={['monthplan:adjustNotice:confirmAdjust']}

                   onClick={() => this.handleAdjust(row)}>
                    {this.$t("调减")}
                  </text-button>
                ) : null}
                {row.status == "3" &&  row.planQty != 0 ? (
                  <text-button 
                  v-hasPermi={['monthplan:adjustNotice:list']}
                   onClick={() => this.handleDetail(row)}>
                    {this.$t("查看明细")}
                  </text-button>
                ) : null}
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
          label: this.$t("ui.data.column.monthPlanAdjustNotice.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.yearMonth"),
          disabled: this.isEdit,
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "status",
          label: this.$t("common.status"),
          type: "select",
          dictData: this.dict.type.biz_adjust_status,
        },
        {
          prop: "noticeNo",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.noticeNo"),
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productDesc"),
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
        removeMonthPlanAdjustNotice({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleSubmit(row) {
      this.$confirm(this.$t("common.confirm.submit"), {
        type: "warning",
      }).then(() => {
        submitMonthPlanAdjustNotice({ id: row.id }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.getList();
        });
      });
    },
    handleCancel(row) {
      this.$confirm(this.$t("是否作废？"), {
        type: "warning",
      }).then(() => {
        cancelMonthPlanAdjustNotice({ id: row.id }).then((data) => {
          this.$modal.msgSuccess(data.msg);
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
          const res = await saveMonthPlanAdjustNotice({
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
        "/monthSetting/monthPlanAdjustNotice/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    async handleAdjust(row) {
      if (this.$refs.adjustRef) {
        try {
          this.loading = true;
          const res = await getAdjustNoticeAdjustPlan(row);
          // console.log(res);

          this.$refs.adjustRef.show({
            ...row,
            startDate: res.startDate,
            productionEndDate: res.productionEndDate,
            isNaturalMonth: res.isNaturalMonth,
            productionStartDate: res.productionStartDate,
          });

          this.loading = false;
        } catch (error) {
          this.loading = false;
        }
      }
    },
    handleDetail(row) {
      if (this.$refs.detailRef) {
        this.$refs.detailRef.show(row);
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
        const data = await listMonthPlanAdjustNotice(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getVersionList(params) {
      this.search = {
        ...this.query,
        ...params,
        monthPlanVersion: undefined,
      };

      if (!params.yearMonth || !params.factoryCode) {
        return;
      }

      try {
        this.verList = [];

        let arr = params.yearMonth.split("-");

        const res = await getVersionList({
          year: arr[0],
          month: arr[1],
          factoryCode: this.search.factoryCode,
        });
        this.verList = res;

        console.log(this.verList);
      } catch (error) {
        console.error(error);
        this.verList = [];
      }
    },
    async getAdjustNoticeAdjustPlan(params) {
      try {
        this.loading = true;
        const res = await getAdjustNoticeAdjustPlan(params);

        this.loading = false;
      } catch (error) {
        this.loading = false;
      }
    },
  },
  created() {
    const date = moment();
    let defaultParams = {
      yearMonth: date.format("yyyy-MM"),
      factoryCode: "",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getVersionList(this.search);
  },
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
