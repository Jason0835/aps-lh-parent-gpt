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
      :defaultValue="defaultValue"
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
import { validateAdd, editScheduleResult } from "@/api/lh/scheduleResult";
import CuringMachineSelect from "@/views/components/CuringMachineSelect.vue";
export default {
  components: { infoForm, CuringMachineSelect },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      defaultValue: {},
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        lhMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        sapCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          render: (form) => {
            return (
              <CuringMachineSelect
                v-model={form.lhMachineCode}
                label={form.lhMachineName}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty"),
          prop: "class1PlanQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1Analysis"),
          prop: "class1Analysis",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2Analysis"),
          prop: "class2Analysis",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3PlanQty"),
          prop: "class3PlanQty",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3Analysis"),
          prop: "class3Analysis",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("添加硫化排程结果信息");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const response = await validateAdd(params);

        if (response.msg === "") {
          const data = await editScheduleResult({
            ...params,
            isSuccess: response.data.isSuccess,
            cxBatchNo: response.data.cxBatchNo,
            batchNo: response.data.batchNo,
          });
          this.$modal.msgSuccess(data.msg);
          this.$emit("success");
        } else {
          this.$confirm(result.msg, { type: "warning" }).then(async () => {
            const data = await editScheduleResult({
              ...params,
              isSuccess: response.data.isSuccess,
              cxBatchNo: response.data.cxBatchNo,
              batchNo: response.data.batchNo,
            });
            this.$modal.msgSuccess(data.msg);
            this.$emit("success");
          });
        }

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
      //设置默认值为明天
      let nowDate = new Date();
      let tomorrow = new Date(nowDate);
      tomorrow.setDate(nowDate.getDate() + 1);
      this.defaultValue = {
        scheduleDate: tomorrow.toISOString().slice(0, 10), // 保留yyyy-MM-dd
      };
      // if (data) {
      //   this.isEdit = true;
      //   this.defaultValue = {
      //     ...data,
      //   };
      // }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

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
