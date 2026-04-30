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

import infoForm from "@/views/components/infoForm.vue";

import {
  editTwiningDisc,
  checkSerialNumberUnique,
} from "@/api/gsq/twiningDisc";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        serialNumber: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        name: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        spec: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        orderWay: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
      },
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.beadRing.machines,
    }),
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.gsq.twiningDisc.column.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.twiningDisc.column.serialNumber"),
          prop: "serialNumber",
          span: 24,
          maxlength: "20",
          required: true,
          // disabled: true,
        },
        {
          label: this.$t("ui.twiningDisc.column.name"),
          prop: "name",
          span: 24,
          maxlength: "16",
        },
        {
          label: this.$t("ui.twiningDisc.column.spec"),
          prop: "spec",
          span: 24,
          maxlength: "10",
        },
        {
          label: this.$t("ui.twiningDisc.column.orderWay"),
          prop: "orderWay",
          span: 24,
          maxlength: "20",
        },
        {
          label: this.$t("ui.twiningDisc.column.purpose"),
          prop: "purpose",
          span: 24,
          maxlength: "40",
        },
        {
          label: this.$t("ui.twiningDisc.column.twiningNum"),
          prop: "twiningNum",
          span: 24,
          type: "number",
          min: 0,
          max: 9999,
          precision: 0,
          maxlength: "40",
        },
        {
          label: this.$t("ui.twiningDisc.column.inTime"),
          prop: "inTime",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.twiningDisc.column.scrapTime"),
          prop: "scrapTime",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.twiningDisc.column.scrapReason"),
          prop: "scrapReason",
          span: 24,
          maxlength: "50",
        },
        {
          label: this.$t("ui.twiningDisc.column.machine"),
          prop: "machineId",
          span: 24,
          required: true,
          type: "select",
          dictData: this.machines,
          labelKey: "machineName",
          valueKey: "id",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "300",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editTwiningDisc(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    checkSerialNumberUnique(rule, value, callback) {
      return new Promise((resolve, reject) => {
        checkSerialNumberUnique({
          id: this.form.id,
          serialNumber: this.form.serialNumber,
        })
          .then((res) => {
            if (res === 0) {
              resolve();
            } else {
              reject(
                new Error(this.$t("ui.twiningDisc.alter.isSerialNumberExist"))
              );
            }
          })
          .catch((error) => {
            console.error(error);
            reject(new Error("验证失败，请稍后再试"));
          });
      });
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
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        try {
          this.loading = true;
          await this.checkSerialNumberUnique();
          this.save(params);
        } catch (error) {
          console.log(error);
          this.$modal.msgError(error.message);
          this.loading = false;
        }
      });
    },
  },
};
</script>
