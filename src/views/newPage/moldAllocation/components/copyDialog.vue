<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
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
      label-width="100px"
      v-loading="loading"
    >
    </info-form>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm"
        >{{ this.$t("common.button.confirm") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import infoForm from "@/views/components/infoForm.vue";

import {
  copyMdmMouldAllocation,
} from "@/api/monthplan/mdmMouldAllocation";

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
      copyForm: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        toYearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        targeYearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],

        // productCode: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
        // locationType: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.select"),
        //     trigger: "blur",
        //   },
        // ],
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.params.copy");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t(
            "common.factory"
          ),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("从"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "toYearMonth",
          label: this.$t("到"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
      ];
    },

  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        let arr=params.yearMonth.split("-");
        params.fromyear=arr[0];
        params.frommonth=arr[1];
        params.yearMonth=''
        let to=params.toYearMonth.split("-");
        params.toyear=to[0];
        params.tomonth=to[1];
        params.toYearMonth=''

        const res = await copyMdmMouldAllocation(params);
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
    show() {
      this.visible = true;
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
