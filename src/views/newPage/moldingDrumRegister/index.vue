
<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
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
          v-hasPermi="['monthplan:mdmWorkWearInfo:edit']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['monthplan:mdmWorkWearInfo:remove']"
          :disabled="selection.length ==0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:mdmWorkWearInfo:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmWorkWearInfo:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mdmWorkWearInfo/importTemplate"
      uploadUrl="/monthplan/mdmWorkWearInfo/importData"
      @uploadSuccess="getList"
    /> -->
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/monthplan/mdmWorkWearInfo/importTemplate"
      uploadUrl="/monthplan/mdmWorkWearInfo/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listMdmWorkWearInfo,
  removeMdmWorkWearInfo,
} from "@/api/monthplan/mdmWorkWearInfo";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";
import { di } from "@fullcalendar/core/internal-common";

export default {
  name: "MoldingDrumRegister",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm
  },
  dicts: ["biz_work_unit", "biz_work_type", "biz_factory_name",'biz_available_status','biz_machine_brand','biz_class_type','cx_machine_type_code'],
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
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "workWearType",
          label: this.$t("ui.data.column.trialPlan.trialType"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_work_type, value);
          },
          width:120
        },
        {
          prop: "workWearStatus",
          label: this.$t("ui.data.column.WorkWearInfo.workWearStatus"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_available_status, value);
          },
        },
        {
          prop: "workWearName",
          label: this.$t("common.name"),
          width:180
        },
        {
          prop: "cxMachineBrandCode",
          label: this.$t("ui.data.column.docMoldingMachine.moldingMachineClassName"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_machine_brand, value);
          },
          width:120
        },
        {
          prop: "cxMachineTypeCode",
          label: this.$t("ui.data.colume.collect.type"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_class_type, value);
          },
          width:120
        },
        {
          prop: "perimeterMin",
          label: this.$t("ui.data.column.capsuleChuck.perimeterMin"),
        },
        {
          prop: "perimeterMax",
          label: this.$t("ui.data.column.capsuleChuck.perimeterMax"),
        },

        {
          prop: "specifications",
          label: this.$t("ui.data.column.specColor.specDesc"),
          width:180
        },
        {
          prop: "qty",
          label: this.$t("common.num"),
          width:120
        },
        {
          prop: "unit",
          label: this.$t("common.unit"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_work_unit, value);
          },
          width:120
        },
        {
          prop: "usedType",
          label: this.$t("ui.data.column.WorkWearInfo.usedType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.cx_machine_type_code, value);
          },
          width:180
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width:120
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 180,
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          fixed: "right",
          width:200,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["monthplan:mdmWorkWearInfo:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["monthplan:mdmWorkWearInfo:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "workWearType",
          label: this.$t("ui.data.column.trialPlan.trialType"),
          type: "select",
          dictData: this.dict.type.biz_work_type,
        },
        {
          prop: "workWearStatus",
          label: this.$t("ui.data.column.WorkWearInfo.workWearStatus"),
          type: "select",
          dictData: this.dict.type.biz_available_status,
        },
        {
          prop: "workWearName",
          label: this.$t("common.name"),
          maxlength: 64,
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.specColor.specDesc"),
          maxlength: 64,
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
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeMdmWorkWearInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteAll() {
      console.log(this.selection);
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids = ids +  this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeMdmWorkWearInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
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
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/monthplan/mdmWorkWearInfo/export", this.formatParams(false));
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
        const data = await listMdmWorkWearInfo(this.formatParams());
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
  activated() {
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
