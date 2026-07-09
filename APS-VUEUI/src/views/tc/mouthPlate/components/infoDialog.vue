<template>
  <el-dialog
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
  >
    <info-form
      ref="form"
      v-loading="loading"
      :columns="columns"
      :form="form"
      :rules="rules"
      class="form-item-height"
      label-position="right"
      label-width="160px"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from "@/views/components/infoForm.vue";
import {saveTcMouthPlate} from "@/api/tc/mouthPlate";

export default {
  components: { infoForm },
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
        mouthPlateCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineCode: [
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
    machines() {
      return this.$store.state.tc.machines;
    },
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.tc.mouthPlate.modelName")
      );
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tc.mouthPlate.factoryCode"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
          span: 12,
          required: true,
          disabled: true,
        },
        {
          prop: "mouthPlateCode",
          label: this.$t("ui.data.column.tc.mouthPlate.mouthPlateCode"),
          span: 12,
          maxlength: 50,
          required: true,
          disabled: this.isEdit,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tc.mouthPlate.machineCode"),
          span: 12,
          required: true,
          type: "select",
          dictData: this.machines,
          props: { label: "machineCode", value: "machineCode" },
          filterable: true,
          listeners: {
            change: (value) => {
              if (value) {
                const machine = this.machines.find(m => m.machineCode === value);
                if (machine) {
                  this.form.factoryCode = machine.factoryCode;
                }
              }
            },
          },
        },
        {
          prop: "plateStatus",
          label: this.$t("ui.data.column.tc.mouthPlate.plateStatus"),
          type: "switch",
          span: 12,
          activeValue: "1",
          inactiveValue: "0",
        },
        {
          prop: "remark",
          label: this.$t("ui.common.column.remark"),
          span: 24,
          type: "textarea",
          maxlength: 900,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        const res = await saveTcMouthPlate(params);
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
          plateStatus: data.plateStatus || "0",
        };
      } else {
        this.form = {
          factoryCode: "116",
          plateStatus: "1",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
