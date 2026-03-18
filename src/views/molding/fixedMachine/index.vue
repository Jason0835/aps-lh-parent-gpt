
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
          v-hasPermi="['monthplan:mdmCxMachineFixed:edit']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['monthplan:mdmCxMachineFixed:remove']"
          @click="handleDeleteAll"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:mdmCxMachineFixed:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmCxMachineFixed:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mdmCxMachineFixed/importTemplate"
      uploadUrl="/monthplan/mdmCxMachineFixed/importData"
      @uploadSuccess="getList"
    /> -->
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/monthplan/mdmCxMachineFixed/importTemplate"
      uploadUrl="/monthplan/mdmCxMachineFixed/importData"
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
  listCxMachineFixed,
  removeCxMachineFixed,
} from "@/api/monthplan/mdmCxMachineFixed";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MoldingFixedMachine",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm,
  },
  dicts: [
    "LINE_TYPE",
    "JOB_TYPE",
    "biz_factory_name",
    "biz_class_type",
    "biz_machine_brand",
  ],
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
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
          width: 120,
        },
        {
          prop: "cxMachineCode",
          label: this.$t("ui.data.column.workWearInfo.cxMachineCode"),
          width: 120,
        },
        {
          prop: "fixedStructure1",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure1"),
          width: 300,
          render: ({ row }) => {
            return (
              <el-popover
                placement="left"
                title={this.$t("ui.data.column.workWearInfo.fixedStructure1")}
                width="500"
                trigger="click"
              >
                <div domPropsInnerHTML={this.renderHtml(row.fixedStructure1)}></div>
                <div
                  slot="reference"
                  style="cursor: pointer;"
                  domPropsInnerHTML={this.renderHtml(row.fixedStructure1)}
                ></div>
              </el-popover>
            );
          },
        },
        {
          prop: "fixedStructure2",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure2"),
          width: 300,
          render: ({ row }) => {
            return (
              <el-popover
                placement="left"
                title={this.$t("ui.data.column.workWearInfo.fixedStructure2")}
                width="500"
                trigger="click"
                content={row.fixedStructure2}
              >
                <div domPropsInnerHTML={this.renderHtml(row.fixedStructure2)}></div>
                <div
                  slot="reference"
                  style="cursor: pointer;"
                  domPropsInnerHTML={this.renderHtml(row.fixedStructure2)}
                ></div>
              </el-popover>
            );
          },
        },
        {
          prop: "fixedStructure3",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure3"),
          width: 300,
          render: ({ row }) => {
            return (
              <el-popover
                placement="left"
                title={this.$t("ui.data.column.workWearInfo.fixedStructure3")}
                width="500"
                trigger="click"
                content={row.fixedStructure3}
              >
                <div domPropsInnerHTML={ this.renderHtml(row.fixedStructure3)}></div>
                <div
                  slot="reference"
                  style="cursor: pointer;"
                  domPropsInnerHTML={this.renderHtml(row.fixedStructure3)}
                ></div>
              </el-popover>
            );
          },
        },
        {
          prop: "fixedMaterialCode",
          label: this.$t("ui.data.column.workWearInfo.fixedMaterialCode"),
          width: 300,
          render: ({ row }) => {
            return (
              // <el-popover
              //   placement="left"
              //   title={this.$t("ui.data.column.workWearInfo.fixedMaterialCode")}
              //   width="500"
              //   trigger="click"
              //   content={row.fixedMaterialCode}
              // >
              //   <span slot="reference" style="cursor: pointer;">
              //     {row.fixedMaterialCode}
              //   </span>
              // </el-popover>
              <el-popover
                placement="left"
                title={this.$t("ui.data.column.workWearInfo.fixedMaterialCode")}
                width="500"
                trigger="click"
                content={row.fixedMaterialCode}
              >
                <div domPropsInnerHTML={this.renderHtml(row.fixedMaterialCode)}></div>
                <div
                  slot="reference"
                  style="cursor: pointer;"
                  domPropsInnerHTML={this.renderHtml(row.fixedMaterialCode)}
                ></div>
              </el-popover>
            );
          },
        },
        {
          prop: "fixedMaterialDesc",
          label: this.$t("固定物料描述"),
          width: 300,
          render: ({ row }) => {
            return (
              // <el-popover
              //   placement="left"
              //   title={this.$t("固定物料描述")}
              //   width="500"
              //   trigger="click"
              //   content={row.fixedMaterialDesc}
              // >
              //   <span slot="reference" style="cursor: pointer;">
              //     {row.fixedMaterialDesc}
              //   </span>
              // </el-popover>
              <el-popover
                placement="left"
                title={this.$t("固定物料描述")}
                width="500"
                trigger="click"
                content={row.fixedMaterialDesc}
              >
                <div domPropsInnerHTML={this.renderHtml(row.fixedMaterialDesc)}></div>
                <div
                  slot="reference"
                  style="cursor: pointer;"
                  domPropsInnerHTML={this.renderHtml(row.fixedMaterialDesc)}
                ></div>
              </el-popover>
            );
          },
        },
        {
          prop: "disableStructure",
          label: this.$t("ui.data.column.workWearInfo.disableStructure"),
          width: 300,
          render: ({ row }) => {
            return (
              // <el-popover
              //   placement="left"
              //   title={this.$t("ui.data.column.workWearInfo.disableStructure")}
              //   width="500"
              //   trigger="click"
              //   content={row.disableStructure}
              // >
              //   <span slot="reference" style="cursor: pointer;">
              //     {row.disableStructure}
              //   </span>
              // </el-popover>
              <el-popover
                placement="left"
                title={this.$t("ui.data.column.workWearInfo.disableStructure")}
                width="500"
                trigger="click"
                content={row.disableStructure}
              >
                <div domPropsInnerHTML={this.renderHtml(row.disableStructure)}></div>
                <div
                  slot="reference"
                  style="cursor: pointer;"
                  domPropsInnerHTML={this.renderHtml(row.disableStructure)}
                ></div>
              </el-popover>
            );
          },
        },
        {
          prop: "disableMaterialCode",
          label: this.$t("ui.data.column.workWearInfo.disableMaterialCode"),
          width: 300,
          render: ({ row }) => {
            return (
              // <el-popover
              //   placement="left"
              //   title={this.$t(
              //     "ui.data.column.workWearInfo.disableMaterialCode"
              //   )}
              //   width="500"
              //   trigger="click"
              //   content={row.disableMaterialCode}
              // >
              //   <span slot="reference" style="cursor: pointer;">
              //     {row.disableMaterialCode}
              //   </span>
              // </el-popover>
              <el-popover
                placement="left"
                title={this.$t("ui.data.column.workWearInfo.disableStructure")}
                width="500"
                trigger="click"
                content={row.disableMaterialCode}
              >
                <div domPropsInnerHTML={this.renderHtml(row.disableMaterialCode)}></div>
                <div
                  slot="reference"
                  style="cursor: pointer;"
                  domPropsInnerHTML={this.renderHtml(row.disableMaterialCode)}
                ></div>
              </el-popover>
            );
          },
        },
        {
          prop: "disableMaterialDesc",
          label: this.$t("不可作业物料描述"),
          width: 300,
          render: ({ row }) => {
            return (
              // <el-popover
              //   placement="left"
              //   title={this.$t("不可作业物料描述")}
              //   width="500"
              //   trigger="click"
              //   content={row.disableMaterialDesc}
              // >
              //   <span slot="reference" style="cursor: pointer;">
              //     {row.disableMaterialDesc}
              //   </span>
              // </el-popover>
              <el-popover
                placement="left"
                title={this.$t("不可作业物料描述")}
                width="500"
                trigger="click"
                content={row.disableMaterialDesc}
              >
                <div domPropsInnerHTML={this.renderHtml(row.disableMaterialDesc)}></div>
                <div
                  slot="reference"
                  style="cursor: pointer;"
                  domPropsInnerHTML={this.renderHtml(row.disableMaterialDesc)}
                ></div>
              </el-popover>
            );
          },
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          width: 200,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["monthplan:mdmCxMachineFixed:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["monthplan:mdmCxMachineFixed:remove"]}
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
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.workWearInfo.cxMachineCode"),
          prop: "cxMachineCode",
        },
        {
          prop: "fixedStructure1",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure1"),
        },
        {
          prop: "fixedStructure2",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure2"),
        },
        {
          prop: "fixedStructure3",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure3"),
        },
        {
          label: this.$t("ui.data.column.workWearInfo.disableStructure"),
          prop: "disableStructure",
          // type: "select",
          // dictData: this.dict.type.JOB_TYPE, // "JOB_TYPE",
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
        removeCxMachineFixed({ ids }).then((data) => {
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
          ids = ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeCxMachineFixed({ ids }).then((data) => {
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
      downloadLink(
        "/monthplan/mdmCxMachineFixed/export",
        this.formatParams(false)
      );
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
    renderHtml(structure) {
      return structure.replace(/[,，]/g, "<br>");
    },
    // api
    async getList() {
      try {
        this.loading = true;

        const data = await listCxMachineFixed(this.formatParams());
        // for (let i = 0; i < data.rows.length; i++) {
        //   data.rows[i].fixedStructure1 = data.rows[i].fixedStructure1.replace(
        //     /[,，]/g,
        //     "</br>"
        //   );
        //   data.rows[i].fixedStructure2 = data.rows[i].fixedStructure2.replace(
        //     /[,，]/g,
        //     "</br>"
        //   );
        //   data.rows[i].fixedStructure3 = data.rows[i].fixedStructure3.replace(
        //     /[,，]/g,
        //     "</br>"
        //   );
        // }
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
