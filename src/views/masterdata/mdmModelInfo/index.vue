
<template>
  <basic-container>
    <page-table
      tableRef="MdmModelInfoMainTable"
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
          v-hasPermi="['maindata:mdmModelInfo:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['maindata:mdmModelInfo:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length !== 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          plain
          v-hasPermi="['maindata:mdmModelInfo:remove']"
          @click="handleDelete(selection)"
          :disabled="selection.length === 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['maindata:mdmModelInfo:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['maindata:mdmModelInfo:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/maindata/mdmModelInfo/importTemplate"
      uploadUrl="/maindata/mdmModelInfo/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMdmModelInfo,
  editMdmModelInfo,
  removeMdmModelInfo,
} from "@/api/maindata/mdmModelInfo";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MdmModelInfo",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [
    "biz_mould_Type",
    "biz_available_status",
    "biz_mould_air_type",
    "biz_factory_name",
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
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        // {
        //   prop: "specificationspattern",
        //   label: this.$t("ui.data.column.modelinfo.spattern"),
        //   width: 250,
        // },

        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.modelinfo.factoryCode"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.modelinfo.mouldCode"),
          width: 120,
        },
        {
          prop: "mouldNo",
          label: this.$t("ui.data.column.modelinfo.mouldNo"),
          width: 120,
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.modelinfo.specifications"),
          width: 100,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.patternt"),
          width: 250,
        },
        {
          prop: "mouldType",
          label: this.$t("ui.data.column.modelinfo.mouldType"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_mould_Type,
              row.mouldType
            );
          },
        },
        {
          prop: "mouldStatus",
          label: this.$t("ui.data.column.modelinfo.mouldStatus"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_available_status,
              row.mouldStatus + ""
            );
          },
        },
        {
          prop: "mainTrademark",
          label: this.$t("ui.data.column.modelinfo.mainTrademark"),
        },
        {
          prop: "mouldSleeve",
          label: this.$t("ui.data.column.modelinfo.mouldSleeve"),
        },
        {
          prop: "mouldRemark",
          label: this.$t("ui.data.column.modelinfo.mouldRemark"),
          width: 200,
        },
        {
          prop: "mouldAirType",
          label: this.$t("ui.data.column.modelinfo.mouldAirType"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_mould_air_type,
              row.mouldAirType
            );
          },
        },
        {
          prop: "型腔模号",
          label: this.$t("型腔模号"),
        },
        {
          prop: "可用状态",
          label: this.$t("可用状态"),
        },
        {
          prop: "物流状态",
          label: this.$t("物流状态"),
        },
        {
          prop: "规格",
          label: this.$t("规格"),
        },
        {
          prop: "主花纹",
          label: this.$t("主花纹"),
        },
        {
          prop: "模具类型",
          label: this.$t("模具类型"),
        },
        {
          prop: "花纹代号",
          label: this.$t("花纹代号"),
        },
        {
          prop: "模壳标准",
          label: this.$t("模壳标准"),
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          minWidth: 100,
          width: 200,
        },
        {
          prop: "updateBy",
          label: this.$t("ui.data.column.modelinfo.updateBy"),
        },
        // {
        //   prop: "createTime",
        //   label: this.$t("common.createTime"),
        //   width: 180,
        // },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.modelinfo.updateTime"),
          width: 180,
        },
        // {
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.btn.option"),
        //   render: ({ row }) => {
        //     return (
        //       <el-button
        //         class="minus"
        //         type="success"
        //         onClick={() => this.handleEdit(row)}
        //       >
        //         {this.$t("ui.frame.btn.update")}
        //       </el-button>
        //     );
        //   },
        // },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.modelinfo.mouldCode"),
        },
        {
          prop: "mouldNo",
          label: this.$t("ui.data.column.modelinfo.mouldNo"),
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.modelinfo.specifications"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.patternt"),
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
        removeMdmModelInfo({ ids }).then((data) => {
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
          const res = await editMdmModelInfo({
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

    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection[0];
        this.$refs.changeMachineRef.show(row);
      }
    },

    handleGotoMachineGant() {
      this.$router.push("/curingPlan/machineGantChart");
    },
    handleGotoSpecDescGant() {
      this.$router.push("/curingPlan/specDescGantChart");
    },
    // 调量
    handleChangePlan() {
      if (this.$refs.changePlanRef) {
        let row = this.selection[0];
        this.$refs.changePlanRef.show(row);
      }
    },
    async handlePublish() {
      this.publishSchedule();
    },

    async handleModifyMonthQty() {
      try {
        let row = this.selection[0];
        const valid = await hasRecordValidate(row);
        if (valid.code == 200) {
          // let params = row.embryoCode+","+row.sapCode+","+row.cxBatchNo+","+row.bomDataVersion;
          //
          // modifyQty(params).then(() => {});
        }
      } catch (error) {
        console.error(error);
      }
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
    handleAutoPlan() {
      console.log("handleAutoPlan");
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "1");
      }
    },
    handleModifyLhMachineQty() {
      if (this.$refs.qtyRef) {
        this.$refs.qtyRef.show();
      }
    },

    handleModelChange() {
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "3");
      }
    },
    handleProductStatus() {
      this.$router.push("/moldingPlanManagement/productStatus");
    },
    handleManualClose() {
      this.$confirm(
        this.$t("ui.data.column.cxScheduleResult.manualClose")
      ).then(async () => {
        const ids = this.selection.map((row) => row.id).join(",");
        const data = await manualClose({ ids });
        this.$modal.msgSuccess(data.msg);
        this.handelSuccess();
      });
    },
    handleToFinishList() {
      this.$router.push("/moldingPlanManagement/finished");
    },
    handleExport() {
      downloadLink("/maindata/mdmModelInfo/export", this.formatParams(false));
    },
    handleProducingIssue() {
      this.$confirm(this.$t("ui.biz.alter.producingIssue")).then(async () => {
        try {
          this.loading = false;
          const res = await producingIssue({
            cxMachineCode: cxMachineCode,
            embryoCode: embryoCode,
            taskType: taskType,
            scheduleDate: scheduleDate,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleLastDaySupplyPlan() {
      this.$router.push("/moldingPlanManagement/lastDaySupplyPlan");
    },

    handleChangeReleaseStatus() {
      this.$refs.releaseStatusRef.show();
    },
    handleValidateConstruction() {
      this.$confirm(
        this.$t("ui.data.column.cxScheduleResult.validateConstruction")
      ).then(async () => {
        try {
          this.loading = true;
          const ids = this.selection.map((row) => row.id).join(",");
          const res = await validateConstruction({ ids });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

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
        const data = await listMdmModelInfo(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    async publishSchedule() {
      try {
        this.loading = true;
        const valid = await publishValidate();
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.hasNullLhMachineCode")
          ).then(async () => {
            const result = await publishScheduleResult();
            this.$emit("success");
            this.hide();
          });
        } else {
          const result = await publishScheduleResult();
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.error(error);
      }
    },
  },
  created() {},
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
