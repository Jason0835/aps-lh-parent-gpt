<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <page-table
      tableRef="reportClassAccuracyTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :data="data"
    >
      <template slot="header">
        <el-button type="primary" plain @click="handleAdd">{{
          $t("common.button.add")
        }}</el-button>
        <el-button type="error" plain @click="handleAdd">{{
          $t("common.button.delete")
        }}</el-button>
      </template>
    </page-table>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { validateAutoPlan } from "@/api/cx/cxScheduleResult";
export default {
  components: {},
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      rules: {},
      columns: [
        {
          type: "selection",
        },
        {
          field: "index",
          align: "center",
          title: this.$t("ui.data.column.scheduleResult.no"),
          // formatter: (row, column, value, index) => {
          //     var columnIndex = $.common.sprintf("<input type='hidden' name='index' value='%s'>", $.table.serialNumber(index));
          //     return columnIndex + $.table.serialNumber(index);
          // }
        },
        // {
        //     field: 'machineCode',
        //     align: 'center',
        //     title: this.$t("ui.data.column.cxScheduleResult.lhMachineCode1"),
        //      formatter: (row, column, value, index) => {
        //         var html=getMachineCodeHtml(index,value);
        //         return html;
        //     },
        // },
        {
          field: "machineName",
          align: "center",
          title: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
          render: () => {
            return <el-input />;
          },
          //  formatter: (row, column, value, index) => {
          //     var html=getMachineNameHtml(value, row, index);
          //     return html;
          // },
        },
        {
          field: "changeType",
          align: "center",
          title: this.$t("ui.data.column.moldChange.changeType"),
          render: () => {
            return <el-select />;
          },
          //  formatter: (row, column, value, index) => {
          //     var html=getDictHtml(index,value,CHANGE_TYPE);
          //     return html;
          // },
        },
        {
          field: "molds",
          align: "center",
          title: this.$t("ui.data.column.cxScheduleResult.lhMachineQty"),
          render: () => {
            return <el-input-number />;
          },
          //  formatter: (row, column, value, index) => {
          //     if($.common.isEmpty(value)){
          //         value=0;
          //     }
          //     var html = $.common.sprintf("<div class='col-sm-12'><input id='lhMachineChangeTpyeList[%s].molds' class='form-control' type='number' name='lhMachineChangeTpyeList[%s].molds' value='%s' digits='true' min='0' max='99999'></div>", index,index, value);
          //     return html;
          // },
        },
      ],
      data: [],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.lhMachineChangeMoldDesc");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await validateAutoPlan(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      }
    },
    hide() {
      this.form = {};
      this.isEdit = false;
      this.visible = false;
    },

    handleAdd() {},
    handleDelete() {},

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
