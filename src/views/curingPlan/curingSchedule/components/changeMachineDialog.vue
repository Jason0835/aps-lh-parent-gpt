<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
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
      label-width="120px"
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
import infoForm from "@/views/components/infoForm.vue";
import { validateChangeMachine, changeMachine } from "@/api/lh/scheduleResult";
import CuringMachineSelect from "@/views/components/CuringMachineSelect.vue";
export default {
  components: { infoForm, CuringMachineSelect },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          prop: "lhMachineCode",
          render: (form) => {
            return (
              <CuringMachineSelect
                v-model={form.lhMachineCode}
                label={form.lhMachineCode}
              />
            );
          },
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.lh.scheduleResult.modelName");
    },
  },
  methods: {
    // api
    async handleChangeMachine(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await validateChangeMachine(params);
        const data2 = await changeMachine(params);
        this.$modal.msgSuccess(data2.msg);
        this.$emit("success");

        this.hide();
      } catch (error) {
        console.log(error);
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
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.handleChangeMachine({
          lhMachineCode: params.lhMachineCode,
          id: params.id,
          factoryCode: params.factoryCode,
        });
      });
    },
  },
};
</script>
