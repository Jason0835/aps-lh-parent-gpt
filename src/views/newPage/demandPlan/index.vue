<template>
  <basic-container>
    <page-table
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
          @click="generPlan"
          v-hasPermi="['monthplan:demandPlan:createMonthRequire']"
          >{{ $t("生成需求计划") }}
        </el-button>
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("生成周度需求计划") }}
        </el-button> -->
        <!-- <el-button
          type="success"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleChanged"
          >{{ $t("优先级调整") }}
        </el-button> -->
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button> -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:productionMouldConfiguration:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button> -->

        <!-- <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button> -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:demandPlan:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/productionMouldConfiguration/importTemplate"
      uploadUrl="/monthplan/productionMouldConfiguration/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
    <el-dialog
      :title="title"
      :visible="visible"
      width="400px"
      @close="hide"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :append-to-body="true"
    >
      <info-form
        class="form-item-height"
        ref="form"
        :form="form"
        :rules="rules"
        :columns="formColumns"
        label-position="right"
        label-width="160px"
        v-loading="loading"
      >
      </info-form>
      <template slot="footer">
        <el-button @click="hide">{{
          this.$t("common.button.cancel")
        }}</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">{{
          this.$t("common.button.confirm")
        }}</el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import {
  listDemandPlan,
  saveDemandPlan,
  genenrDemandPlan,
} from "@/api/monthplan/demandPlan";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoForm from "@/views/components/infoForm.vue";
// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "DemandPlan",
  components: {
    tltUpload,
    infoForm,
    // infoDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_product_type",
    "biz_order_type",
    "biz_yes_no",
    "biz_sale_type",
    "biz_brand_type",
    "biz_product_characteristics",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      title: "优先级调整",
      loading: false,
      visible: false,
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
      form: {},
      rules: {
        scam: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        isIOne: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
      formColumns: [
        {
          prop: "sam",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          type: "select",
        },
      ],
    };
  },
  computed: {
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "year",
          label: this.$t("ui.data.column.productionMouldConfiguration.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.productionMouldConfiguration.month"),
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_product_type, value);
          },
        },

        {
          prop: "locationType",
          label: this.$t("common.type"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_sale_type, value);
          },
        },
        {
          prop: "brand",
          label: this.$t("common.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "scmPriority",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          render: ({ row }) => {
            return (
              <div>
                <el-select
                  v-if={this.hasPermission("monthplan:demandPlan:edit")}
                  placeholder="请选择"
                  v-model={row.scmPriority}
                  onChange={(val) => this.handlePriorityChange(row, val)}
                >
                  {this.dict.type.biz_order_type.map((item) => (
                    <el-option
                      key={item.value}
                      label={item.label}
                      value={item.value}
                    ></el-option>
                  ))}
                </el-select>
                <span v-else>{this.selectDictLabel(this.dict.type.biz_order_type, row.scmPriority)}</span>
              </div>
            );
          },
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width:300
        },
        {
          prop: "productionType",
          label: this.$t("ui.data.DemandPlan.productionType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_product_characteristics,
              value
            );
          },
        },
        {
          prop: "yearWeek",
          label: this.$t("ui.data.column.monthplan.weekYear"),
        },
        {
          prop: "isUniformity",
          label: this.$t("ui.data.column.monthplan.dynamicBalance"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "isDynamicBalance",
          label: this.$t("ui.data.column.monthplan.uniformity"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "orderQty",
          label: this.$t("ui.data.DemandPlan.orderQty"),
        },
        {
          prop: "stockQty",
          label: this.$t("ui.data.DemandPlan.stockQty"),
        },
        {
          prop: "plannedSurplus",
          label: this.$t("ui.data.DemandPlan.plannedSurplus"),
        },
        {
          prop: "netQty",
          label: this.$t("ui.data.DemandPlan.netQty"),
        },
        {
          prop: "isProduction",
          label: this.$t("ui.data.DemandPlan.isProduction"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "postponeNetQty",
          label: this.$t("ui.data.DemandPlan.postponeNetQty"),
        },
        {
          prop: "unPostponeNetQty",
          label: this.$t("ui.data.DemandPlan.unPostponeNetQty"),
        },
        {
          prop: "heightQty",
          label: this.$t("ui.data.DemandPlan.heightQty"),
        },
        {
          prop: "midQty",
          label: this.$t("ui.data.DemandPlan.midQty"),
        },
        {
          prop: "postponeQty",
          label: this.$t("ui.data.DemandPlan.postponeQty"),
        },
        {
          prop: "cycleReserveQty",
          label: this.$t("ui.data.DemandPlan.cycleReserveQty"),
        },
        {
          prop: "conventionReserveQty",
          label: this.$t("ui.data.DemandPlan.conventionReserveQty"),
        },
        {
          prop: "isReachMinProductionQty",
          label: this.$t("ui.data.DemandPlan.isReachMinProductionQty"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "minProductionQty",
          label: this.$t("ui.data.DemandPlan.minProductionQty"),
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 180,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },

        {
          prop: "monthPlanVersion",
          label: this.$t("需求计划版本号"),
        },
        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          prop: "orderPriority)",
          label: this.$t("ui.data.DemandPlan.order"),
          type: "select",
          dictData: this.dict.type.biz_order_type,
        },
        {
          prop: "isAlternateMaterial",
          label: this.$t("是否替换料"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "isReachMinProductionQty",
          label: this.$t("不足最小投产量"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
      ];
    },
  },
  methods: {
    hasPermission(permission) {
      const permissions = this.$store.state.user.permissions || [];
      if (Array.isArray(permission)) {
        return permission.some((perm) => permissions.includes(perm));
      }
      return permissions.includes(permission);
    },
    async generPlan() {
      try {
        let res = await genenrDemandPlan(this.formatParams());
        console.log(res);
        this.getList();
      } catch (err) {}
    },
    save() {},
    hide() {
      this.$refs.form.triggerResetForm();
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    handleChanged() {
      (this.formColumns = [
        {
          prop: "scm",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          type: "select",
        },
      ]),
        (this.title = "优先级调整");
      this.visible = true;
    },
    handleRow() {
      (this.formColumns = [
        {
          prop: "ui.data.DemandPlan.isProduction",
          label: this.$t("ui.data.DemandPlan.isProduction"),
          type: "select",
        },
      ]),
        (this.title = "ui.data.DemandPlan.isProduction");
      this.visible = true;
    },
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
      }).then(() => {});
    },
    handlePriorityChange(row, val) {
      console.log(row, val);
      // let params = {
      //   id: row.id,
      //   orderPriority: val,
      // };
      saveDemandPlan(row)
        .then((res) => {
          this.$modal.msgSuccess(res.msg);
          this.getList();
        })
        .catch((err) => {
          console.log(err);
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
      downloadLink("/monthplan/demandPlan/export", this.formatParams(false));
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handleBuild() {
      if (this.$refs.buildRef) {
        this.$refs.buildRef.show();
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
        const [year, month] = params.yearMonth.split("-");
        params.year = year;
        params.month = month;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;

        const data = await listDemandPlan(this.formatParams());
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
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0"); // 月份从0开始，需要+1
    let defaultParams = {
      factoryCode: "116",
      yearMonth: `${year}-${month}`,
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
