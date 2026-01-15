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
import {
  saveMpTrialPlan
} from "@/api/monthplan/mpTrialPlan";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm,materialCodeSelect },
  inject: ["parentDict"],
  data() {
    const validatePositiveInteger = (rule, value, callback) => {
      if (value === "" || value === null || value === undefined) {
        if (rule.required) {
          return callback(new Error(this.$t("common.rule.noData")));
        }
        return callback();
      }
      const strValue = String(value).trim();

      // 检查是否只包含数字
      if (!/^\d+$/.test(strValue)) {
        return callback(
          new Error(this.$t("common.rule.noPoint"))
        );
      }

      // 转换为数字
      const numValue = Number(strValue);
      if (numValue > 99999999) {
        return callback(new Error(this.$t("common.rule.inoutMax")));
      }

      if (!Number.isInteger(numValue)) {
        return callback(new Error(this.$t("common.rule.peleaseInteger")));
      }

      callback();
    };
    return {
      constructionList:[],
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
        materialCode: [
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
        specifications: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        pattern: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        trialStatus: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        trialQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
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
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType:'month',
          valueFormat: "yyyy-MM",
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),
          maxlength:64,
          disabled:true

        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.pattern"),
          maxlength:64,
          disabled:true

        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.materialCode}
                v-model={form.materialCode}
                onChange={this.handleMaterialCodeChange}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "materialDesc",
          disabled:true
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.trialPlan.planDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "trialStatus",
          label: this.$t("ui.data.column.trialPlan.trialStatus"),
          type: "select",
          dictData: this.constructionList,
        },
        {
          prop: "trialQty",
          label: this.$t("common.num"),
          type: "number",
          min:0,
          max:99999999
        },

        {
          prop: "urgencyType",
          label: this.$t("ui.data.column.trialPlan.urgencyType"),
          type: "select",
          dictData: this.parentDict.type.biz_urgency_type,
        },
        {
          prop: "trialType",
          label: this.$t("ui.data.column.trialPlan.trialType"),
          type: "select",
          dictData: this.parentDict.type.biz_trial_type,
        },

        {
          prop: "destination",
          label: this.$t("ui.data.column.trialPlan.destination"),
          maxlength:50
        },
        {
          prop: "embryoNo",
          label: this.$t("ui.data.column.trialPlan.embryoNo"),
          disabled:true,
        },
        // {
        //   prop: "embryoType",
        //   label: this.$t("ui.data.column.trialPlan.embryoType"),
        //   disabled:true,
        // },
        // {
        //   prop: "embryoReleaseDate",
        //   label: this.$t("ui.data.column.trialPlan.embryoReleaseDate"),
        //   disabled:true,
        // },
        {
          prop: "textNo",
          label: this.$t("ui.data.column.trialPlan.textNo"),
          disabled:true,
        },
        // {
        //   prop: "textType",
        //   label: this.$t("ui.data.column.trialPlan.textType"),
        //   disabled:true,
        // },
        // {
        //   prop: "textReleaseDate",
        //   label: this.$t("ui.data.column.trialPlan.textReleaseDate"),
        //   disabled:true,
        // },
        {
          prop: "lhNo",
          label: this.$t("ui.data.column.trialPlan.lhNo"),
          disabled:true,
        },
        // {
        //   prop: "lhType",
        //   label: this.$t("ui.data.column.trialPlan.lhType"),
        //   disabled:true,
        // },
        // {
        //   prop: "lhReleaseDate",
        //   label: this.$t("ui.data.column.trialPlan.lhReleaseDate"),
        //   disabled:true,
        // },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          maxlength:500
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let arr=params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        const res = await saveMpTrialPlan(params);
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
      let list=this.parentDict.type.biz_construction_stage
      let distList=[]
      for (let index = 0; index < list.length; index++) {
        console.log(list[index].value)
        if(list[index].value=="1" ||list[index].value=="2"){
          distList.push(list[index])
        }

      }
      this.constructionList=distList
      this.visible = true;
      if (data) {
        this.isEdit = true;
        data.yearMonth = data.year + "-" + (data.month < 10 ? "0" + data.month : data.month);
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          factoryCode: "116",
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
    handleMaterialCodeChange(val, row) {
      if (val) {
        this.$set(this.form, "materialDesc", row.materialDesc);
        this.$set(this.form, "specifications", row.specifications);
        this.$set(this.form, "pattern", row.pattern);
        this.$set(this.form, "embryoNo", row.embryoNo);
        this.$set(this.form, "textNo", row.textNo);
        this.$set(this.form, "lhNo", row.lhNo);

      } else {
        this.$set(this.form, "materialDesc", "");
        this.$set(this.form, "specifications", '');
        this.$set(this.form, "pattern", '');
        this.$set(this.form, "embryoNo",'');
        this.$set(this.form, "textNo", '');
        this.$set(this.form, "lhNo", '');
      }
    },
  },
};
</script>
