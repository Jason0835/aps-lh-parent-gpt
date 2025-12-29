
<template>
  <basic-container>
    <page-table
      tableRef="ProductModelRelationMainTable"
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
          v-hasPermi="['maindata:relation:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          :disabled="selection.length !== 1"
          v-hasPermi="['maindata:relation:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['maindata:relation:remove']"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button type="primary" plain  v-hasPermi="['maindata:relation:mesCapture']" @click="capture">{{
          $t("ui.data.column.moldLedger.mes")
        }}</el-button>
        <el-button
          v-hasPermi="['maindata:relation:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['maindata:relation:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/maindata/relation/importTemplate"
      uploadUrl="/maindata/relation/importData"
      @uploadSuccess="getList"
    /> -->
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
    downloadUrl="/maindata/relation/importTemplate"
      uploadUrl="/maindata/relation/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
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
  listRelation,
  removeRelation,
  mesCapture,
} from "@/api/maindata/relation";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "ProductModelRelation",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm
  },
  dicts: ["biz_factory_name", "biz_brand_type", "molding_method", "biz_yes_no"],
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
        //   prop: "factoryCode",
        //   label: this.$t("ui.data.column.productmodelrelation.factoryCode"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_factory_name, value);
        //   },
        // },
        // {
        //   prop: "productCode",
        //   label: this.$t("ui.data.column.productmodelrelation.productCode"),
        // },
        // {
        //   prop: "specCode",
        //   label: this.$t("ui.data.column.productmodelrelation.specCode"),
        // },
        // {
        //   prop: "productDesc",
        //   label: this.$t(
        //     "ui.data.column.productmodelrelation.specificationsDescribe"
        //   ),
        //   width: 260,
        // },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("ui.data.column.productmodelrelation.mouldCode"),
        // },
        // {
        //   prop: "mouldNo",
        //   label: this.$t("ui.data.column.productmodelrelation.mouldNo"),
        // },
        // {
        //   prop: "mouldMethod",
        //   label: this.$t("ui.data.column.productmodelrelation.mouldMethod"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.molding_method, value);
        //   },
        // },
        // {
        //   prop: "specifications",
        //   label: this.$t("ui.data.column.productmodelrelation.specifications"),
        //   width: 120,
        // },
        // {
        //   prop: "pattern",
        //   label: this.$t("ui.data.column.productmodelrelation.pattern"),
        //   width: 140,
        // },
        // {
        //   prop: "brand",
        //   label: this.$t("ui.data.column.productmodelrelation.brand"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_brand_type, value);
        //   },
        // },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "mouldCode",
          label: this.$t("ui.data.column.moldLedger.mouldCode"),
        },
        {
          prop: "shareMouldCode",
          label: this.$t("ui.data.rubberMaterial.shareMouldCode"),
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          width:300,
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
        },
        {
          prop: "samePatternPanel",
          label: this.$t("ui.data.column.monthplan.samePatternPanel"),
          width:80,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
        },
        {
          prop: "updateTime",
          width: 180,
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["maindata:relation:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["maindata:relation:remove"]}
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
          label: this.$t("ui.data.column.moldLedger.mouldCode"),
          prop: "mouldCode",
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
        {
          prop: "mainPattern",
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
        },
        {
          label: this.$t("ui.data.column.monthplan.samePatternPanel"),
          prop: "samePatternPanel",
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
      ];
    },
  },
  methods: {
    async capture() {
      try {
        let res = await mesCapture();
        this.$modal.msgSuccess(res.msg);
        this.$set(this.page, "current", 1);
        this.getList();
      } catch (err) {
        console.log(err)
      }
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
      }).then(() => {
        const ids = rows.id
        removeRelation({ ids }).then((data) => {
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
        removeRelation({ ids }).then((data) => {
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
          const res = await editMachine({
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
      downloadLink("/maindata/relation/export", this.formatParams(false));
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    updateTableHeaderTitle() {
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
        const data = await listRelation(this.formatParams());
        console.log(data);
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
