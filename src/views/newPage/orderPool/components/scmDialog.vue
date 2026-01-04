<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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

import {
  getSCMDataCheck,
  getSCMData,
  savePoData,
  lockPool,
} from "@/api/newPage/salesOrderPool";

import infoForm from "@/views/components/infoForm.vue";
import Year from "@/components/Crontab/year.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      isLock: false,
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
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
        lineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        jobType: [
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
      return this.$t("common.rule.select");
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
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
      ];
    },
  },
  methods: {
    async SCMBtn(params) {
      try {
        this.loading = true;
        let arr = params.yearMonth.split("-");
        let obj = {
          factoryCode: params.factoryCode,
          Year: arr[0],
          month: arr[1],
        };
        if (this.isLock) {
          let res = await lockPool(obj);
          this.loading = false;
          this.$modal.msgSuccess(this.$t('common.msg.success.operate'));
          this.$emit("success");
          this.hide();
        } else {
          let res = await getSCMDataCheck(obj);

          if (res.data == 1) {

            this.$confirm(res.msg, {
              type: "warning",
            }).then(() => {
              getSCMData(obj).then((data) => {
                this.loading = false;
                this.$modal.msgSuccess(data.msg);
                this.$emit("success");

              });
            });
          } else {
            this.$modal.msgSuccess(res.msg);
            // this.$set(this.page, "current", 1);
            this.$emit("success");
          }
          this.hide();
        }
      } catch (err) {
        console.log(err);
        this.loading = false;
      }
    },
    // api
    async save(params) {
      try {
        this.loading = true;
        let obj = {
          salCodePo: params.salCodePo,
          scmPriority: params.scmPriority,
        };
        const res = await savePoData(obj);
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
      if (data) {
        this.isLock = true;
      } else {
        this.isLock = false;
      }
      this.visible = true;
      this.loading = false;
      const now = new Date();
      const currentYear = now.getFullYear();
      const currentMonth = now.getMonth() + 1;

      // 计算下个月
      let nextYear = currentYear;
      let nextMonth = currentMonth + 1;

      if (nextMonth > 12) {
        nextMonth = 1;
        nextYear = currentYear + 1;
      }
      this.form = {
        yearMonth:
          nextYear + "-" + (nextMonth < 10 ? "0" + nextMonth : nextMonth),
        factoryCode: "116",
      };
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.SCMBtn);
    },
  },
};
</script>
