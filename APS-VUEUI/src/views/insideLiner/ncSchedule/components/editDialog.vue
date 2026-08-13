<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1000px"
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

import infoForm from "@/views/components/infoForm.vue";

import { editScheduleResult, getWorkClass } from "@/api/nc/ncScheduleResult.js";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      // 连续6个班次的表头（index 0 = 前日早班，index 1~6 = class1~class6），由 getWorkClass 加载
      classHeaders: [],
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.insideLiner.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.ncScheduleResult.modalName");
    },
    columns() {
      // 生成 6 个班次区块（class1~class6）：计划量可编辑，完成量/完成率只读展示
      const classColumns = [];
      for (let i = 1; i <= 6; i++) {
        classColumns.push(
          {
            label: this.classHeaders[i] || `${i}班`,
            type: "title",
          },
          {
            label: this.$t("ui.data.column.dj.scheduleResult.planQty"),
            prop: `class${i}PlanQty`,
            span: 12,
          },
          {
            label: this.$t("ui.data.column.dj.scheduleResult.finishQty"),
            prop: `class${i}FinishQty`,
            span: 12,
            disabled: true,
          },
          {
            label: this.$t("ui.data.column.scheduleResult.finish"),
            prop: `class${i}FinishRate`,
            span: 12,
            disabled: true,
          }
        );
      }
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          type: "title",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 12,
          // type: "date",
          // valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.liningCode"),
          prop: "liningCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.glueCode"),
          prop: "glueCode",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.stockQty"),
          prop: "stockQty",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.glueSeq"),
          prop: "glueSeq",
          span: 12,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          span: 12,
          type: "select",
          disabled: true,
          dictData: this.parentDict.type.IS_RELEASE,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          prop: "machineId",
          span: 12,
          type: "select",
          disabled: true,
          dictData: this.machines,
          valueKey: "id",
          labelKey: "machineName",
          valueType: "string",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 12,
          type: "textarea",
          maxlength: "300",
        },
        ...classColumns,
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let res = await editScheduleResult(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.error(error);
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
      // 加载连续6个班次的表头（index 0 = 前日早班，index 1~6 = class1~class6）
      getWorkClass({
        scheduleDate: data ? data.scheduleDate : this.form.scheduleDate,
      }).then((res) => {
        this.classHeaders = res;
      });
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
