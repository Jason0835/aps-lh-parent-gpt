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
import moment from "moment";
import { mapState } from "vuex";

import {
  generateMoldReplacementPlan,
} from "@/api/lh/lhMoldChangePlan";
import {
  getBatchNo
} from "@/api/lh/scheduleResult.js";

import infoForm from "@/views/components/infoForm.vue";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      requireMap: {
        tireRoughStock: true,
        changeMoldTime: true,
        useMoldNumber: true,
      },
    };
  },
  computed: {
    ...mapState({
      curingMachines: (state) => state.curing.machines,
    }),
    title: function () {
      return this.$t("生成");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.scheduleTime"),
          prop: "scheduleTime",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
      ];
    },
    rules() {
      return {

        planDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      
      };
    },
  },
  watch: {

  },

  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const res = await getBatchNo(params);
        const data = await generateMoldReplacementPlan({
          lhResultBatchNo: res.msg, 
           factoryCode: params.factoryCode
        });
        this.$modal.msgSuccess(data.msg);
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
      } else {
        this.form = {
          scheduleTime:  moment().add(1, "days").format("yyyy-MM-DD 00:00:00"),
          factoryCode: ""

        }
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
