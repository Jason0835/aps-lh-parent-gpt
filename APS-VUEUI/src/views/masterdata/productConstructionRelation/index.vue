
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
        <!-- <el-button
          type="primary"
          @click="mesCaptureing"
          plain
          >{{ $t("ui.data.column.moldLedger.mes") }}</el-button
        > -->
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
          label: this.$t("ui.data.column.mdmConstructionInfo.factoryCode"),
          width: 100,
          align: "center",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "constructionVersion",
          label: this.$t("ui.data.column.mdmConstructionInfo.constructionVersion"),
          width: 180,
          align: "center",
        },
         {
          prop: "embryoDesc",
          label: this.$t("ui.data.column.mdmConstructionInfo.embryoDesc"),
          align: "left",
          minWidth: 350,
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
          label: this.$t("ui.data.column.mdmConstructionInfo.constructionCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.mdmConstructionInfo.pattern"),
          width: 100,
          align: "center",
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.mdmConstructionInfo.specifications"),
          width: 140,
          align: "center",
        },

        {
          prop: "mouldMethod",
          label: this.$t("ui.data.column.mdmConstructionInfo.mouldMethod"),
          width: 110,
          align: "center",
        },
        {
          prop: "buildingDrumType",
          label: this.$t("ui.data.column.mdmConstructionInfo.buildingDrumType"),
          width: 90,
          align: "center",
        },
        {
          prop: "paddingCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.paddingCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "sidewallCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.sidewallCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "treadCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.treadCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "tireRingCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.tireRingCode"),
          width: 120,
          align: "center",
        },
         {
          prop: "beadCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.beadCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "rubberCoreCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.rubberCoreCode"),
          width: 120,
          align: "center",
        },

        {
          prop: "insideCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.insideCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "typeAdhesiveCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.typeAdhesiveCode"),
          width: 120,
          align: "center",
        },
{
          prop: "zeroBeltCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.zeroBeltCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "clothWrappingCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.clothWrappingCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "tireBodyCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.tireBodyCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "beltCode1",
          label: this.$t("ui.data.column.mdmConstructionInfo.beltCode1"),
          width: 120,
          align: "center",
        },
        {
          prop: "beltCode2",
          label: this.$t("ui.data.column.mdmConstructionInfo.beltCode2"),
          width: 120,
          align: "center",
        },
        {
          prop: "beltCode3",
          label: this.$t("ui.data.column.mdmConstructionInfo.beltCode3"),
          width: 120,
          align: "center",
        },
        {
          prop: "beltCode4",
          label: this.$t("ui.data.column.mdmConstructionInfo.beltCode4"),
          width: 120,
          align: "center",
        },
        {
          prop: "beltCodeLeftCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.beltCodeLeftCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "beltCodeRightCode",
          label: this.$t("ui.data.column.mdmConstructionInfo.beltCodeRightCode"),
          width: 120,
          align: "center",
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          width: 180,
          align: "left",
        },
        {
          prop: "updateBy",
          label: this.$t("ui.data.column.updateBy"),
          width: 100,
          align: "center",
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.updateTime"),
          width: 180,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [

        {
          label: this.$t("ui.data.column.mdmConstructionInfo.factoryCode"),
          prop: "factoryCode",
          type:'select',
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "constructionVersion",
          label: this.$t("ui.data.column.mdmConstructionInfo.constructionVersion"),
        },
        {
          prop: "embryoDesc",
          label: this.$t("ui.data.column.mdmConstructionInfo.embryoDesc"),
          minWidth: 350,
          align: "left",
        },
        // {
        //   label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        //   prop: "materialCode",
        // },
        // {
        //   label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        //   prop: "specifications",
        // },
        {
          label: this.$t("ui.data.column.mdmConstructionInfo.constructionCode"),
          prop: "constructionCode",
        },
        {
          label: this.$t("ui.data.column.mdmConstructionInfo.pattern"),
          prop: "pattern",
        },
        {
          label: this.$t("ui.data.column.mdmConstructionInfo.specifications"),
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
