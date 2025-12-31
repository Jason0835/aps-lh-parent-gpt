
<template>
  <basic-container>
    <page-table
      tableRef="ProductConstructionRelationMainTable"
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
          @click="mesCaptureing"
          plain
          >{{ $t("ui.data.column.moldLedger.mes") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['maindata:mdmProductConstruction:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          :disabled="selection.length !== 1"
          v-hasPermi="['maindata:mdmProductConstruction:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['maindata:mdmProductConstruction:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['monthplan:mdmConstructionInfo:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmConstructionInfo:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mdmConstructionInfo/importTemplate"
      uploadUrl="/monthplan/mdmConstructionInfo/importData"
      @uploadSuccess="getList"
    /> -->
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/monthplan/mdmConstructionInfo/importTemplate"
      uploadUrl="/monthplan/mdmConstructionInfo/importData"
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
  listMdmProductConstruction,mesCapture
} from "@/api/maindata/mdmConstruction";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "ProductConstructionRelation",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm
  },
  dicts: ["molding_method",'biz_factory_name'],
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

         {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          width: 180,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
         {
          prop: "specCode",
          label: this.$t("ui.data.column.construction.specCode"),
          width: 180,
        },
        // {
        //   prop: "materialCode",
        //   label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        //   width: 180,
        // },
        // {
        //   prop: "specifications",
        //   label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        //   width: 300,
        // },
        {
          prop: "constructionCode",
          label: this.$t("ui.data.column.skuConstruction.embryoCode"),
          width: 180,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.pattern"),
          width: 180,
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),
          width: 180,
        },

        {
          prop: "mouldMethod",
          label: this.$t("ui.data.column.construction.mouldMethod"),
          width: 180,
        },
        {
          prop: "buildingDrumType",
          label: this.$t("ui.data.column.construction.buildingDrumType"),
          width: 180,
        },
        {
          prop: "paddingCode",
          label: this.$t("ui.data.column.construction.paddingCode"),
          width: 180,
        },
        {
          prop: "sidewallCode",
          label: this.$t("ui.data.column.construction.sidewallCode"),
          width: 180,
        },
        {
          prop: "treadCode",
          label: this.$t("ui.data.column.construction.treadCode"),
          width: 180,
        },
        {
          prop: "tireRingCode",
          label: this.$t("ui.data.column.construction.tireRingCode"),
          width: 180,
        },
         {
          prop: "beadCode",
          label: this.$t("ui.data.column.construction.beadCode"),
          width: 180,
        },
        {
          prop: "rubberCore",
          label: this.$t("ui.data.column.construction.rubberCore"),
          width: 180,
        },

        {
          prop: "insideCode",
          label: this.$t("ui.data.column.construction.insideCode"),
          width: 180,
        },
        {
          prop: "typeAdhesiveCode",
          label: this.$t("ui.data.column.construction.typeAdhesiveCode"),
          width: 180,
        },
{
          prop: "zeroBeltCode",
          label: this.$t("ui.data.column.construction.zeroBeltCode"),
          width: 180,
        },
        {
          prop: "clothWrappingCode",
          label: this.$t("ui.data.column.construction.clothWrappingCode"),
          width: 180,
        },
        {
          prop: "tireBodyCode",
          label: this.$t("ui.data.column.construction.tireBodyCode"),
          width: 180,
        },
        {
          prop: "beltCode1",
          label: this.$t("ui.data.column.construction.beltCode1"),
          width: 180,
        },
        {
          prop: "beltCode2",
          label: this.$t("ui.data.column.construction.beltCode2"),
          width: 180,
        },
        {
          prop: "beltCode3",
          label: this.$t("ui.data.column.construction.beltCode3"),
          width: 180,
        },
        {
          prop: "beltCode4",
          label: this.$t("ui.data.column.construction.beltCode4"),
          width: 180,
        },
        {
          prop: "beltCodeLeftCode",
          label: this.$t("ui.data.column.construction.beltCodeLeftCode"),
          width: 180,
        },
        {
          prop: "beltCodeRightCode",
          label: this.$t("ui.data.column.construction.beltCodeRightCode"),
          width: 180,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 180,
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
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type:'select',
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "specifications",
        },
        {
          label: this.$t("ui.data.column.skuConstruction.embryoCode"),
          prop: "constructionCode",
        },
        {
          label: this.$t("ui.data.column.modelinfo.pattern"),
          prop: "pattern",
        },
        {
          label: this.$t("ui.data.column.trialPlan.specifications"),
          prop: "specifications",
        },

      ];
    },
  },
  methods: {
    async mesCaptureing(){
      try {
        this.loading = true;
        const res = await mesCapture();
        this.$modal.msgSuccess(res.msg);
        this.$set(this.page, "current", 1);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
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
        const ids = rows.map((row) => row.id).join(",");
        console.log(ids);
        // removeMdmProductConstruction({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
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
      downloadLink(
        "/monthplan/mdmConstructionInfo/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
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
        const data = await listMdmProductConstruction(this.formatParams());
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
