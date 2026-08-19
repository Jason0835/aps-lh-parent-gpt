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

import { getConfigKey } from "@/api/system/config";
import { editLoss } from "@/api/nc/loss";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      factoryCode: "",
      form: {},
      rules: {
        machineType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        specDimension: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        lossRate: [
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
      machines: (state) => state.insideLiner.machines,
    }),
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.nc.lossSetting.column.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.nc.lossSetting.column.liningCode"),
          prop: "liningCode",
          span: 24,
        },
        {
          label: this.$t("ui.data.column.loss.line"),
          prop: "machineCode",
          span: 24,
          type: "select",
          dictData: this.machines,
          props: {
            value: "machineCode",
            label: "machineName",
          },
        },
        {
          label: this.$t("ui.data.column.loss.lossRate"),
          prop: "lossRate",
          span: 24,
          required: true,
          type: "number",
          min: 0,
          max: 99.99,
          precision: 2,
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

        const res = await editLoss(params);
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
      // 加载内衬机台下拉数据（机台信息存于 vuex state.insideLiner.machines），按当前工厂编码过滤，避免带出其他厂的机台
      const loadMachines = () => {
        this.$store.dispatch("insideLiner/getMachineList", {
          factoryCode: this.factoryCode,
        });
      };
      // 获取当前工厂编码（保存时需要带工厂参数，机台下拉过滤也需要）
      if (!this.factoryCode) {
        getConfigKey("sys.factory.code").then((response) => {
          this.factoryCode = response.msg;
          loadMachines();
        });
      } else {
        loadMachines();
      }
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
      this.$refs.form.triggerConfirm((params) => {
        this.save({
          ...params,
          factoryCode: params.factoryCode || this.factoryCode,
        });
      });
    },
  },
};
</script>
