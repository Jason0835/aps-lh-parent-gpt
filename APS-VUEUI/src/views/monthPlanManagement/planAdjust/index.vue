
<template>
  <basic-container>
    <page-table
      tableRef="MonthPlanProdResultMainTable"
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
        <div class="header-left">
          <div class="curr-date">
            <label>{{ $t("ui.data.column.planAdjust.currentDate") }}</label>
            <el-input class="curr-date_value" :value="currentDay" disabled />
          </div>

          <el-button
            v-hasPermi="['monthplan:planAdjust:add']"
            type="primary"
            plain
            :disabled="infoDisabled"
            @click="handleAdd"
            >{{ $t("ui.data.column.planAdjust.insertSpec") }}</el-button
          >
          <el-button
            v-hasPermi="['monthplan:planAdjust:adjustNumber']"
            type="primary"
            plain
            :disabled="infoDisabled || selection.length !== 1"
            @click="handleEdit(selection[0])"
            >{{ $t("ui.data.column.planAdjust.adjust") }}</el-button
          >
          <el-button
            v-hasPermi="['monthplan:planAdjust:import']"
            @click="$refs.tltUpload.handleImport()"
            >{{ $t("ui.frame.btn.import") }}</el-button
          >
          <el-button
            @click="handleExport"
            v-hasPermi="['monthplan:planAdjust:export']"
            >{{ $t("ui.frame.btn.export") }}</el-button
          >
          <span class="table-tip">
            {{ $t("ui.data.column.planAdjust.tip1") }}</span
          >
        </div>

        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:factoryMonthPlanProdResult:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-buttonW
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:factoryMonthPlanProdResult:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
      </template>
    </page-table>

    <infoDialog
      :startAdjustDate="startAdjustDate"
      ref="infoRef"
      @success="saveAdjustFactoryMonthPlan"
    />
    <adjustDialog
      :startAdjustDate="startAdjustDate"
      ref="adjustRef"
      @success="saveAdjustPlan"
    />
    <tlt-upload
      ref="tltUpload"
      :updateSupport="true"
      :downloadUrl="undefined"
      uploadUrl="/factory/monthPlanAdjust/importData"
      @uploadSuccess="getList"
    >
      <template slot="tip">
        <div class="upload-tip">
          <div class="tip-row text-center title">数据注意事项</div>

          <div class="tip-row">
            1.当您需要插入新规格时：<span class="text-red"
              >请将"排产制造单号"字段留空</span
            >，系统将自动为新规格分配资源。
          </div>
          <div class="tip-row">
            2.当您需要对现有规格进行调整时：<span class="text-red"
              >请保持原有"排产制造单号"不变</span
            >，系统将识别为规格调整而非新增。
          </div>
        </div>
      </template>
    </tlt-upload>
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMonthPlanProdResult,
  getProductionMonthType,
} from "@/api/factory/monthPlanProdResult.js";
import {
  getAdjustControlInfo,
  adjustFactoryMonthPlan,
} from "@/api/factory/monthPlanAdjust.js";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import adjustDialog from "./components/adjustDialog.vue";

export default {
  name: "PlanAdjust",
  components: {
    tltUpload,
    infoDialog,
    adjustDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_yes_no",
    "biz_channel_type",
    "biz_brand_type",
    "biz_construction_stage",
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
      dailyVisible: true,
      currentDay: moment().format("yyyy-MM-DD"),
      startAdjustDate: null,
      productionStartDate: null,
    };
  },
  computed: {
    infoDisabled: function () {
      return !this.startAdjustDate;
    },
    columns() {
      let columns = [
        {
          type: "selection",
        },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.物料编码"),
        //   prop: "productCode",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.物料描述"),
        //   prop: "productDesc",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.factoryCode"),
          prop: "factoryCode",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.constructionStage"),
          prop: "constructionStage",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_construction_stage,
              value
            );
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.year"),
          prop: "year",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.month"),
          prop: "month",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productCode"),
          prop: "productCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.specCode"),
          prop: "specCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.embryoCode"),
          prop: "embryoCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productDesc"),
          prop: "productDesc",
          minWidth: 100,
          width: 250,
          // sortable: "custom",
        },

        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.施工号"),
        //   prop: "productDesc",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.locationType"),
          prop: "locationType",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_stor_type, value);
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.channel"),
          prop: "channel",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_channel_type, value);
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.brand"),
          prop: "brand",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.specifications"),
          prop: "specifications",
          minWidth: 100,
          // sortable: "custom",
        },

        {
          label: this.$t("ui.data.column.mouldingDayResult.proSize"),
          prop: "proSize",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.pattern"),
          prop: "pattern",
          minWidth: 140,
          // sortable: "custom",
        },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.levelCode"),
        //   prop: "levelCode",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.类型标识"),
        //   prop: "productDesc",
        //   minWidth: 100,
        //   sortable: "custom",
        // },

        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.BOI"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },

        {
          label: this.$t("ui.data.column.mouldingDayResult.prodReqPlan"),
          prop: "prodReqPlan",
          minWidth: 100,
        },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.备库计划"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.预计超欠产"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.理论生产需求计划"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.totalQty"),
          prop: "totalQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.factProdReqQty"),
          prop: "factProdReqQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.differenceQty"),
          prop: "differenceQty",
          minWidth: 100,
        },

        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.成型机编号"),
        //   prop: "productDesc",
        //   minWidth: 100,
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.mouldNo"),
          prop: "mouldNo",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.mouldQty"),
          prop: "mouldQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.beginDate"),
          prop: "beginDate",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.endDay"),
          prop: "endDay",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.isDeliveryDate"),
          prop: "isDeliveryDate",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.reason"),
          prop: "reason",
          minWidth: 140,
        },
        {
          label: this.$t("common.remark"),
          prop: "remark",
          minWidth: 100,
          // sortable: "custom",
        },
      ];
      if (this.dailyVisible) {
        if (this.productionStartDate) {
          //
          let start = moment(this.productionStartDate);
          let end = moment(this.productionStartDate).add(1, "M");
          let list = [];

          while (start.isBefore(end)) {
            list.push(start.format("DD"));
            start.add(1, "d");
          }
          // console.log(list);
          for (let i = 0; i < list.length; i++) {
            let dayNumStr = list[i];
            columns.push({
              // label: `${i + 1}号`,
              label: this.$t("ui.data.column.mouldingDayResult.day", {
                day: Number(dayNumStr),
              }),
              prop: `day${i + 1}`,
              minWidth: "80px",
              type: "number",
            });
          }
        } else {
          //显示每日数据
          const date = moment(this.query.yearMonth);

          const days = date.daysInMonth();

          for (let i = 0; i < days; i++) {
            columns.push({
              // label: `${i + 1}号`,
              label: this.$t("ui.data.column.mouldingDayResult.day", {
                day: i + 1,
              }),
              prop: `day${i + 1}`,
              minWidth: "80px",
              type: "number",
            });
          }
        }
      }
      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.mouldingDayResult.yearMonth"),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productCode"),
          prop: "productCode",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.specCode"),
          prop: "specCode",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productDesc"),
          prop: "productDesc",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.channel"),
          prop: "channel",
          type: "select",
          dictData: this.dict.type.biz_channel_type,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.brand"),
          prop: "brand",
          type: "select",
          dictData: this.dict.type.biz_brand_type,
        },
        {
          prop: "locationType",
          label: this.$t(
            "ui.data.column.LocationChannelConfiguration.locationType"
          ),
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.specifications"),
          prop: "specifications",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.proSize"),
          prop: "proSize",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.pattern"),
          prop: "pattern",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.mouldNo"),
          prop: "mouldNo",
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
        removemouldingDayResult({ ids }).then((data) => {
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
          const res = await editMouldingDayResult({
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
      this.updateList();
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
      downloadLink("/factory/monthPlanAdjust/export", this.formatParams(false));
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleSave() {},

    // utils
    updateTableHeaderlabel() {
      //  TODO 更新表头标题
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

        const data = await listMonthPlanProdResult(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    async getProductionMonthType() {
      try {
        const res = await getProductionMonthType(this.formatParams(false));
        console.log(res);
        if (res.productionStartDate) {
          this.productionStartDate = res.productionStartDate;
        } else {
          this.productionStartDate = null;
        }
      } catch (error) {
        console.log(error);
      }
    },
    async updateList() {
      this.loading = true;
      await this.getProductionMonthType();
      await this.getList();
    },

    async getAdjustControlInfo(params) {
      try {
        const params = this.formatParams(false);

        const res = await getAdjustControlInfo({
          year: params.year,
          month: params.month,
          factoryCode: params.factoryCode,
        });
        this.startAdjustDate = res.startAdjustDate;
        console.log(res);
      } catch (error) {
        console.error(error);
        // this.verList = [];
      }
    },

    async saveAdjustFactoryMonthPlan(params, callback) {
      try {
        const res = await adjustFactoryMonthPlan(params);
        callback("success");
        if (res.planSubtractList && res.planSubtractList.length) {
          this.$refs.adjustRef.show(res);
        }

        this.getList();
      } catch (error) {
        console.error(error);
        callback("error");
      }
    },
    async saveAdjustPlan(params, callback) {
      try {
        const res = await adjustFactoryMonthPlan(params);
        if (res.planSubtractList && res.planSubtractList.length) {
          callback(0, res);
        } else {
          callback(1);
          this.getList();
        }
      } catch (error) {
        console.error(error);
        callback("error");
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
    this.getAdjustControlInfo();
  },
  activated() {
    this.updateList();
  },
};
</script>
<style lang="scss" scoped>
.header-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  .curr-date {
    margin-bottom: 5px;
    &_value {
      width: 160px;
      margin: 0 10px;
    }
  }
}
.more-btn {
  margin: 2px 0;
  width: 100%;
}
.table-tip {
  line-height: 28px;
  font-size: 12px;
  color: #ff5722;
  margin-left: 10px;
}
.upload-tip {
  text-align: left;
  // margin-top: 10px;
  padding: 10px;
  .title {
    font-size: 15px;
  }
  .tip-row {
    margin-bottom: 5px;
  }
}
.text-red {
  color: red;
}
</style>
