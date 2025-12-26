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
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="160px"
      v-loading="loading"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { mapState } from "vuex";

// import { editCxSpecifyMachine } from "@/api/cx/cxSpecifyMachine";
import {
  listMdmDevicePlanShut,
  editMdmDevicePlanShu,
} from "@/api/monthplan/scheduledShutdown";
import machineSelect from "@/views/components/machineSelect.vue";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm ,machineSelect},
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        procCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        machineStopType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        beginDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        endDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],

      },
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
      {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "procCode",
          label: this.$t("schedule.scheduleReport.procedure"),
          type: "select",
          dictData: this.parentDict.type.work_calendar_proc,

        },
      {
          prop: "machineType",
          label: this.$t("ui.data.column.cx.machine.type"),
          type: "select",
          dictData: this.parentDict.type.device_shut_machine_type,
          listeners: {
            change: this.handleMachineTypeChange,
          },
        },

        {
          prop: "machineCode",
          label: this.$t("ui.data.column.dispatcherlog.machineId"),

          render: (form) => {
            return (
              <machineSelect
                disabled={form.machineType?false:true}
                factoryCode={form.factoryCode}
                key={form.machineCode}
                machineType={form.machineType}
                v-model={form.machineCode}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          prop: "machineStopType",
          label: this.$t("ui.data.column.scheduledShutdown.machineStopType"),
          type: "select",
          dictData: this.parentDict.type.machine_stop_type,
        },
        {
          prop: "beginDate",
          label: this.$t("ui.data.column.scheduledShutdown.beginDate"),
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },
        {
          prop: "endDate",
          label: this.$t("ui.data.column.scheduledShutdown.endDate"),
          type: "date",
          dateType: "datetime",
          valueFormat: "yyyy-MM-dd HH:mm:ss",
        },

        {
          prop: "remark",
          label: this.$t("ui.remark"),
          maxlength: 200,
        },
      ];
    },
  },
  methods: {
    handleMachineTypeChange(){
      this.$set(this.form,'machineCode','')
    },
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMdmDevicePlanShu(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
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
      } else {
        this.form = {
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    handleMaterialCodeChange(){

    },
  },
};
</script>
