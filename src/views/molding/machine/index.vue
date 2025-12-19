
<template>
  <basic-container>
    <page-table
      tableRef="MoldingMachineMainTable"
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
          v-hasPermi="['maindata:mdmMoldingMachine:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['maindata:mdmMoldingMachine:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          v-hasPermi="['maindata:mdmMoldingMachine:remove']"
          type="danger"
          plain
          :disabled="selection.length == 0"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >

        <el-button
          v-hasPermi="['maindata:mdmMoldingMachine:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['maindata:mdmMoldingMachine:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/maindata/mdmMoldingMachine/importTemplate"
      uploadUrl="/maindata/mdmMoldingMachine/importData"
      @uploadSuccess="getList"
    />
    <infoDialog
      ref="infoRef"
      :moldingMachineClass="moldingMachineClass"
      @success="getList"
    />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
// import { listMachine, editMachine } from "@/api/cx/machine";
import {
  listMdmMoldingMachine,
  editMdmMoldingMachine,
  removeMdmMoldingMachine,
} from "@/api/maindata/mdmMoldingMachine";
import { listMdmMoldingMachineCls } from "@/api/monthplan/mdmMoldingMachineCls";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MoldingMachine",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [
    // "CX_MACHINE_TYPE",
    //  "MACHINE_TYPE",
    "biz_factory_name",
    "biz_product_name",
    "biz_carcassCloth_type",
    "STATUS",
    "CLASS_SHIFT",
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
      moldingMachineClass: [],
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.machine.factoryCode"),
          prop: "factoryCode",
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          label: this.$t("ui.data.column.machine.productTypeCode"),
          prop: "productTypeCode",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_product_name,
              row.productTypeCode
            );
          },
        },
        {
          prop: "moldingMachineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.machineCode"),
          // sortable: "custom",
        },
        // {
        //   prop: "machineName",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.cx.machine.machineName"),
        //   sortable: "custom",
        // },
        // {
        //   prop: "moldingDrum",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.cx.machine.moldingDrum"),
        //   // sortable: "custom",
        // },
        {
          prop: "moldingDrumMax",
          align: "center",
          label: this.$t("ui.data.column.cx.machine.moldingDrumMax"),
          // sortable: "custom",
        },
        {
          prop: "moldingDrumMin",
          align: "center",
          label: this.$t("ui.data.column.cx.machine.moldingDrumMin"),
          // sortable: "custom",
        },
        {
          prop: "sectionWidthMax",
          align: "center",
          label: this.$t("ui.data.column.cx.machine.sectionWidthMax"),
          // sortable: "custom",
        },
        {
          prop: "sectionWidthMin",
          align: "center",
          label: this.$t("ui.data.column.cx.machine.sectionWidthMin"),
          // sortable: "custom",
        },
        {
          prop: "moldingMachineClsName",
          align: "center",
          label: this.$t("ui.data.column.machine.machineType"),
          width: 250,
          // sortable: "custom",
        },
        // {
        //   prop: "machineType",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.cx.machine.type"),
        //   sortable: "custom",
        //   formatter: (row, column, value, index) => {
        //     return this.selectDictLabel(
        //       this.dict.type.MACHINE_TYPE,
        //       row.machineType
        //     );
        //   },
        // },

        {
          prop: "minSize",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.dimensionMiniMum"),
          // sortable: "custom",
        },
        {
          prop: "maxSize",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.dimensionMaxiMum"),
          // sortable: "custom",
        },
        // {
        //   prop: "quata",
        //   align: "right",
        //   halign: "center",
        //   label: this.$t("ui.data.column.cx.machine.quata"),
        //   sortable: "custom",
        // },
        // {
        //   prop: "quotaRatio",
        //   align: "right",
        //   halign: "center",
        //   label: this.$t("ui.data.column.cx.machine.quotaRatio"),
        //   sortable: "custom",
        // },
        {
          prop: "classes",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.classShift"),
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.CLASS_SHIFT,
              row.classes
            );
          },
        },
        {
          prop: "carcassClothType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.carcassClothType"),
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_carcassCloth_type,
              value
            );
          },
        },
        {
          prop: "machineStatus",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.status"),
          // sortable: "custom",
          // formatter: (row, column, value, index) => {
          //   return statusTools(row);
          // },
          render: ({ row }) => {
            return (
              <el-switch
                value={row.machineStatus}
                active-value="0"
                inactive-value="1"
                onChange={(value) => this.handleChangeStatus(value, row)}
              />
            );
          },
        },
        // {
        //   prop: "operatorQty",
        //   align: "right",
        //   halign: "center",
        //   label: this.$t("ui.data.column.cx.machine.operatorQty"),
        //   sortable: "custom",
        // },
        {
          prop: "机型",
          label: "机型",
        },
        {
          prop: "反包方式",
          label: "反包方式",
        },
        {
          prop: "是否有零度供料架",
          label: "是否有零",
        },
        {
          prop: "对应硫化机上限",
          label: "对应硫化机上限",
        },
        {
          prop: "设备最大日产",
          label: "设备最大日产",
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
          // sortable: "custom",
        },
        {
          align: "center",
          halign: "center",
          fixed: "right",
          label: this.$t("ui.data.btn.option"),
          render: ({ row }) => {
            return (
              <el-button
                class="minus"
                type="success"
                onClick={() => this.handleEdit(row)}
              >
                {" "}
                {this.$t("ui.frame.btn.update")}
              </el-button>
            );
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.cx.machine.machineCode"),
          prop: "moldingMachineCode",
        },
        // {
        //   label: this.$t("ui.data.column.cx.machine.machineName"),
        //   prop: "machineName",
        // },
        {
          label: this.$t("ui.data.column.machine.machineType"),
          prop: "moldingMachineClassId",
          type: "select",
          // dictData: this.dict.type.CX_MACHINE_TYPE, // "CX_MACHINE_TYPE",
          dictData: this.moldingMachineClass,
          valueKey: "id",
          labelKey: "moldingMachineClassName",
        },
        // {
        //   label: this.$t("ui.data.column.cx.machine.type"),
        //   prop: "type",
        //   type: "select",
        //   dictData: this.dict.type.MACHINE_TYPE, // "MACHINE_TYPE",
        // },
        {
          label: this.$t("ui.data.column.cx.machine.status"),
          prop: "machineStatus",
          type: "select",
          dictData: this.dict.type.STATUS, // "STATUS",
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
        removeMdmMoldingMachine({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleChangeStatus(machineStatus, row) {
      console.log(machineStatus);
      let title =
        machineStatus === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(title, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editMdmMoldingMachine({
            ...row,
            machineStatus,
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
        "/maindata/mdmMoldingMachine/export",
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
        const data = await listMdmMoldingMachine(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getMoldingMachineClassList() {
      try {
        this.loading = true;

        const res = await listMdmMoldingMachineCls();

        this.moldingMachineClass = res.rows;
        console.log(res);
        this.loading = false;
      } catch (error) {
        this.loading = false;
      }
    },
  },
  created() {
    // // 设置默认排程时间
    // let date = moment().add(1, "days").format("YYYY-MM-DD");
    // // date = "2023-06-01"; //test
    // this.query.scheduleDate = date;
    // this.search.scheduleDate = date;
    this.getMoldingMachineClassList();
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
