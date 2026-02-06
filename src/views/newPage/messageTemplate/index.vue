
<template>
  <basic-container>
    <page-table
      :calcHeight="true"
      tableRef="cxFixedMachineMainTable"
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
        <el-button type="primary" plain v-hasPermi="['message:templateList:list']" @click="handleAdd">{{
          $t("ui.frame.btn.add")
        }}</el-button>
        <el-button
          type="warning"
          v-hasPermi="['message:templateList:add']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.edit") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['message:templateList:remove']"
          :disabled="selection.length == 0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['maindata:msgTemplateUserRel:bindUsers']"
          :disabled="selection.length !== 1"
          @click="handleSelect(selection[0])"
          >{{ $t("关联用户") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['monthplan:mdmDevicePlanShut:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmDevicePlanShut:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
      </template>
    </page-table>

    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/monthplan/mdmDevicePlanShut/importTemplate"
      uploadUrl="/monthplan/mdmDevicePlanShut/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
    <selectDialog ref="selectRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import { listTemplate, removeTemplate } from "@/api/newPage/messageTemplate";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import infoDialog from "./components/infoDialog.vue";
import selectDialog from "./components/selectDialog.vue";

export default {
  name: "MessageTemplate",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm,
    selectDialog
  },
  dicts: [
    "machine_type",
    "machine_stop_type",
    "biz_factory_name",
    "work_calendar_proc",
    "device_shut_machine_type",
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
            console.log(form);
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
          prop: "templateCode",
          label: this.$t("ui.data.messageTemplate.templateCode"),
          width:200,
        },

        {
          prop: "templateName",
          label: this.$t("ui.data.messageTemplate.templateName"),
          width: 200,
        },
        {
          prop: "title",
          label: this.$t("ui.data.messageTemplate.title"),
          width: 200,
        },
        {
          prop: "content",
          label: this.$t("common.messageTask.msgContent"),
          width: 400,
        },
        {
          prop: "userName",
          label: this.$t("关联用户"),
          width: 200,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 200,
        },
        {
          align: "center",
          label: this.$t("common.option"),
          fixed: "right",
          width: 240,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["message:templateList:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["message:templateList:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
                <el-button
                  v-hasPermi={["maindata:msgTemplateUserRel:bindUsers"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleSelect(row)}
                >
                  {this.$t("关联用户")}
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
          prop: "templateCode",
          label: this.$t("ui.data.messageTemplate.templateCode"),
        },
        {
          prop: "templateName",
          label: this.$t("ui.data.messageTemplate.templateName"),
        },
        {
          prop: "title",
          label: this.$t("ui.data.messageTemplate.title"),
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
    handleSelect(row) {
      if (this.$refs.selectRef) {
        this.$refs.selectRef.show(row);
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
        removeTemplate({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteAll() {
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
        removeTemplate({ ids }).then((data) => {
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
        "/monthplan/mdmDevicePlanShut/export",
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
        params.beginDate = params.createTime[0];
        params.endDate = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listTemplate(this.formatParams());
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
    console.log('pandangengx')
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
  activated() {},
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
