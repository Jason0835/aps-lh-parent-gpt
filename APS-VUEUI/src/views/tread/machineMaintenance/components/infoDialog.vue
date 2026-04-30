<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="150px"
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

import infoForm from "@/views/components/infoForm.vue";
// import { editTmMachineMaintenance } from "@/api/tm/tmMachineMaintenance";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        stopDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        machineId: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        stopShift: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        stopTime: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.tread.machines,
    }),
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.machine.info")
      );
    },
    columns() {
      return [
        {
          label: this.$t("停机日期"),
          prop: "stopDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("预计开始时间"),
          prop: "预计开始时间",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("预计结束时间"),
          prop: "预计结束时间",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("时间类型"),
          prop: "stopDate",
          type: "select",
        },
        {
          label: this.$t("机台名称"),
          prop: "machineId",
          type: "select",
          dictData: this.machines,
          valueKey: "id",
          labelKey: "machineName",
        },
        {
          label: this.$t("停机时间(H)"),
          prop: "stopTime",
        },
        {
          label: this.$t("停机班次"),
          prop: "stopShift",
          type: "select",
          dictData: this.parentDict.type.CLASS_NUM,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
          maxlength: "100",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      // try {
      //   this.loading = true;
      //   const data = await editTmMachineMaintenance(params);
      //   this.$modal.msgSuccess(data.msg);
      //   this.$emit("success");
      //   this.hide();
      // } catch (error) {
      //   console.log(error);
      // } finally {
      //   this.loading = false;
      // }
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
      this.$refs.form.triggerResetForm();
      this.form = {};

      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        this.save(params);
      });
    },
  },
};
</script>
