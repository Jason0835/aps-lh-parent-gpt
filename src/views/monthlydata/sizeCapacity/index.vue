<template>
  <basic-container>
    <page-table
      tableRef="SizeCapacityMainTable"
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
          v-hasPermi="['monthsetting:sizeCapacity:edit']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthsetting:sizeCapacity:edit']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthsetting:sizeCapacity:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button>
        <!-- <el-button
          v-hasPermi="['monthsetting:sizeCapacity:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button> -->
        <!-- <el-button
          @click="handleExport"
          v-hasPermi="['monthsetting:sizeCapacity:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button> -->
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthsetting/sizeCapacity/importTemplate"
      uploadUrl="/monthsetting/sizeCapacity/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import moment from "moment";

import { downloadLink } from "@/utils/request";

import {
  listSizeCapacity,
  editSizeCapacity,
  removeSizeCapacity,
} from "@/api/monthsetting/sizeCapacity";
import { getVersionList } from "@/api/demand/requireProductionPlan";

import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "SizeCapacity",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["biz_factory_name", "molding_method"],
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
        { type: "selection", fixed: "left" },
        {
          prop: "year",
          label: this.$t("ui.data.column.sizeCapacity.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.sizeCapacity.month"),
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.sizeCapacity.factoryCode"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.sizeCapacity.proSize"),
        },
        {
          prop: "mouldMethod",
          label: this.$t("ui.data.column.sizeCapacity.mouldMethod"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.molding_method,
              row.mouldMethod
            );
          },
        },
        {
          prop: "demandQty",
          label: this.$t("ui.data.column.sizeCapacity.demandQty"),
          type: "number",
        },
        {
          prop: "netDemandQty",
          label: this.$t("ui.data.column.sizeCapacity.netDemandQty"),
          type: "number",
        },
        {
          prop: "stockUpDemandQty",
          label: this.$t("ui.data.column.sizeCapacity.stockUpDemandQty"),
          type: "number",
        },
        {
          prop: "dayCapacity",
          label: this.$t("ui.data.column.sizeCapacity.dayCapacity"),
          type: "number",
        },
        {
          prop: "machineNumber",
          label: this.$t("ui.data.column.sizeCapacity.machineNumber"),
          type: "number",
        },
        {
          prop: "nextProSize",
          label: this.$t("ui.data.column.sizeCapacity.nextProSize"),
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 200,
        },
        {
          prop: "updateBy",
          label: this.$t("common.updateByName"),
          width: 160,
        },
        {
          prop: "updateTime",
          label: this.$t("common.updateTime"),
          width: 180,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          render: (form) => {
            return (
              <el-date-picker
                type="month"
                v-model={form.yearMonth}
                value-format={"yyyy-MM"}
                format="yyyy-MM"
                clearable={false}
                onChange={(val) =>
                  this.handleMonthChange({
                    yearMonth: val,
                    ...form,
                  })
                }
              />
            );
          },
        },
        {
          label: this.$t("ui.data.colume.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          render: (form) => {
            return (
              <dict-select
                options={this.dict.type.biz_factory_name}
                v-model={form.factoryCode}
                onChange={(val) =>
                  this.handleFactoryChange({
                    factoryCode: val,
                    ...form,
                  })
                }
              />
            );
          },
        },
        {
          prop: "monthPlanVersion",
          label: this.$t("ui.data.column.monthSaleOrderPlan.monthPlanVersion"),
          type: "select",
          render: (form) => {
            return (
              <el-select v-model={form.monthPlanVersion} clearable={true}>
                {this.verList.map((item) => {
                  return <el-option key={item} value={item} label={item} />;
                })}
              </el-select>
            );
          },
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.sizeCapacity.proSize"),
        },
        {
          prop: "mouldMethod",
          label: this.$t("ui.data.column.sizeCapacity.mouldMethod"),
          type: "select",
          dictData: this.dict.type.molding_method,
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
        removeSizeCapacity({ ids }).then((data) => {
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
          const res = await editSizeCapacity({
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
        "/monthsetting/sizeCapacity/export",
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
    handleMonthChange(params) {
      this.getVersionList(params);
    },
    handleFactoryChange(params) {
      this.getVersionList(params);
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
        const data = await listSizeCapacity(this.formatParams());
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
